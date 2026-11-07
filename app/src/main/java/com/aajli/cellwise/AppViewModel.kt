/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aajli.cellwise.core.UiMsg
import com.aajli.cellwise.core.backup.BackupError
import com.aajli.cellwise.core.backup.BackupException
import com.aajli.cellwise.core.backup.BackupManager
import com.aajli.cellwise.core.backup.BackupReminder
import com.aajli.cellwise.core.backup.ImportMode
import com.aajli.cellwise.core.db.AppDatabase
import com.aajli.cellwise.core.settings.Settings
import com.aajli.cellwise.core.settings.SettingsBackupSection
import com.aajli.cellwise.core.settings.SettingsStore
import com.aajli.cellwise.engine.Difficulty
import com.aajli.cellwise.engine.SudokuGenerator
import com.aajli.cellwise.feature.backup.BackupUi
import com.aajli.cellwise.feature.daily.Daily
import com.aajli.cellwise.feature.daily.DailyInfo
import com.aajli.cellwise.feature.game.GameRules
import com.aajli.cellwise.feature.game.GameState
import com.aajli.cellwise.feature.game.backup.GamesBackupSection
import com.aajli.cellwise.feature.game.data.GameEntity
import com.aajli.cellwise.feature.game.data.toEntity
import com.aajli.cellwise.feature.game.data.toState
import com.aajli.cellwise.feature.puzzleimport.ImportUi
import com.aajli.cellwise.feature.puzzleimport.ImportValidation
import com.aajli.cellwise.feature.puzzleimport.PuzzleImport
import com.aajli.cellwise.feature.stats.Stats
import com.aajli.cellwise.feature.stats.StatsSummary
import com.aajli.cellwise.feature.stats.toRecord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneId
import java.util.UUID

enum class Screen { HOME, GAME, SETTINGS, ABOUT, STATS, BACKUP, IMPORT }

/**
 * The single ViewModel. Feature logic lives in pure classes (GameRules, Stats, GameMerge, engine);
 * this only wires state, timer and database access. Every database write goes through [dbQueue]
 * (one ordered writer), including backup imports, so saves can never overtake an import.
 */
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)
    private val dao = db.gameDao()
    private val settingsStore = SettingsStore(app)
    val settings: StateFlow<Settings> = settingsStore.state

    /** Recomputed from the games table; only active while the Statistics screen is collecting it. */
    val stats: StateFlow<StatsSummary> = dao.observeAll()
        .map { rows -> Stats.compute(rows.mapNotNull { it.toRecord() }, System.currentTimeMillis(), ZoneId.systemDefault()) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsSummary.EMPTY)

    private val _today = MutableStateFlow(Daily.today())

    /** Daily puzzle state (streak, week, done today), recomputed from the games table and the date. */
    val daily: StateFlow<DailyInfo> = combine(dao.observeDailyWins(), _today) { wins, today -> Daily.compute(wins, today) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyInfo.EMPTY)

    private val backupManager = BackupManager(
        app, listOf(GamesBackupSection(db), SettingsBackupSection(settingsStore))
    ) { dao.count() > 0 }

    private val _screen = MutableStateFlow(Screen.HOME)
    val screen: StateFlow<Screen> = _screen.asStateFlow()
    private val _game = MutableStateFlow<GameState?>(null)
    val game: StateFlow<GameState?> = _game.asStateFlow()
    private val _elapsed = MutableStateFlow(0L)
    val elapsed: StateFlow<Long> = _elapsed.asStateFlow()
    private val _generating = MutableStateFlow(false)
    val generating: StateFlow<Boolean> = _generating.asStateFlow()
    private val _backup = MutableStateFlow(BackupUi())
    val backup: StateFlow<BackupUi> = _backup.asStateFlow()
    private val _reminderDue = MutableStateFlow(false)
    val reminderDue: StateFlow<Boolean> = _reminderDue.asStateFlow()
    private val _import = MutableStateFlow(ImportUi())
    val importUi: StateFlow<ImportUi> = _import.asStateFlow()
    private val shareChannel = Channel<Uri>(Channel.BUFFERED)
    val shareEvents = shareChannel.receiveAsFlow()

    private val undoStack = ArrayDeque<Pair<List<Int>, List<Int>>>()
    private val dbQueue = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    private var foreground = true

    init {
        viewModelScope.launch(Dispatchers.IO) {
            for (job in dbQueue) {
                try { job() } catch (e: CancellationException) { throw e } catch (e: Exception) { /* a failed job must not stop the writer */ }
            }
        }
        viewModelScope.launch {
            val e = withContext(Dispatchers.IO) { dao.getActive() }
            if (e != null) {
                val s = e.toState()
                if (s != null) { _game.value = s; _elapsed.value = e.elapsedSec }
                else enqueue { dao.upsert(e.copy(status = "ABANDONED")) } // corrupt row
            }
            try { backupManager.autoBackupIfDue() } catch (e2: CancellationException) { throw e2 } catch (e2: Exception) { }
            refreshReminder()
        }
        viewModelScope.launch {
            var ticks = 0
            while (true) {
                delay(1000)
                val g = _game.value
                if (foreground && _screen.value == Screen.GAME && g != null && !g.won) {
                    _elapsed.value += 1
                    if (++ticks % 15 == 0) persist(g)
                }
            }
        }
    }

    private fun enqueue(job: suspend () -> Unit) { dbQueue.trySend(job) }

    private fun persist(s: GameState, status: String = if (s.won) "WON" else "IN_PROGRESS") {
        val now = System.currentTimeMillis()
        val e: GameEntity = s.toEntity(_elapsed.value, status, now, if (status == "WON") now else null)
        enqueue { dao.upsert(e) }
    }

    // ---------- game ----------

    fun newGame(d: Difficulty) {
        if (_generating.value) return
        viewModelScope.launch {
            _generating.value = true
            try {
                val p = withContext(Dispatchers.Default) { SudokuGenerator.generate(d) }
                beginGame(p.rating.level, p.givens, p.solution, false, p.rating.hardest.rank)
            } finally {
                _generating.value = false
            }
        }
    }

    /** Starts a game from a generated or imported puzzle; the game in progress (if any) is recorded as abandoned. */
    private fun beginGame(d: Difficulty, givens: IntArray, solution: IntArray, imported: Boolean, rating: Int, dailyDay: Int = 0) {
        _game.value?.let { if (!it.won) persist(it, "ABANDONED") } // uses the old elapsed time
        val s = GameState(
            id = UUID.randomUUID().toString(), difficulty = d,
            givens = givens.toList(), solution = solution.toList(), values = givens.toList(),
            notes = List(81) { 0 }, mistakes = 0, createdAt = System.currentTimeMillis(),
            imported = imported, rating = rating, dailyDay = dailyDay
        )
        undoStack.clear()
        _elapsed.value = 0
        _game.value = s
        persist(s)
        _screen.value = Screen.GAME
    }

    /** Today's daily puzzle (same for every phone). The date is read when the button is pressed, so midnight is handled. */
    fun playDaily() {
        if (_generating.value) return
        val day = Daily.today()
        _today.value = day
        viewModelScope.launch {
            _generating.value = true
            try {
                val p = withContext(Dispatchers.Default) { Daily.generate(day) }
                beginGame(p.rating.level, p.givens, p.solution, false, p.rating.hardest.rank, day)
            } finally {
                _generating.value = false
            }
        }
    }

    fun continueGame() {
        val g = _game.value
        if (g != null && !g.won) _screen.value = Screen.GAME
    }

    /** Dismiss the "solved" dialog and go home. */
    fun leaveFinished() { _game.value = null; _screen.value = Screen.HOME }

    fun back() {
        when (_screen.value) {
            Screen.GAME -> _game.value?.let { if (!it.won) persist(it) }
            Screen.BACKUP -> _backup.update { it.copy(preview = null, message = null) }
            else -> {}
        }
        _screen.value = Screen.HOME
    }

    fun openSettings() { _screen.value = Screen.SETTINGS }
    fun openAbout() { _screen.value = Screen.ABOUT }
    fun openStats() { _screen.value = Screen.STATS }
    fun updateSettings(t: (Settings) -> Settings) = settingsStore.update(t)

    fun onForeground(f: Boolean) {
        foreground = f
        if (f) _today.value = Daily.today()
        if (!f) _game.value?.let { if (!it.won) persist(it) }
        else viewModelScope.launch { refreshReminder() }
    }

    private fun mutate(f: (GameState) -> GameState) {
        val g = _game.value ?: return
        val n = f(g)
        if (n == g) return
        val boardChanged = n.values != g.values || n.notes != g.notes
        if (boardChanged) {
            undoStack.addLast(g.values to g.notes)
            if (undoStack.size > 200) undoStack.removeFirstOrNull()
        }
        _game.value = n
        if (boardChanged || n.hints != g.hints) persist(n) // selection, notes mode and panels are not saved
    }

    fun select(i: Int) = mutate { GameRules.select(it, i) }
    fun digit(d: Int) = mutate { GameRules.enterDigit(it, d) }
    fun erase() = mutate { GameRules.erase(it) }
    fun toggleNotes() = mutate { GameRules.toggleNotes(it) }
    fun hint() = mutate { GameRules.requestHint(it) }
    fun reveal() = mutate { GameRules.revealHint(it) }
    fun check() = mutate { GameRules.check(it) }
    fun autoNotes() = mutate { GameRules.autoNotes(it) }
    fun dismissPanel() = mutate { GameRules.dismissPanel(it) }

    fun undo() {
        val g = _game.value ?: return
        if (g.won) return
        val u = undoStack.removeLastOrNull() ?: return
        val n = g.copy(values = u.first, notes = u.second, hint = null, checkWrong = -1, showErrors = false)
        _game.value = n
        persist(n)
    }

    // ---------- import a printed grid ----------

    fun openImport() { _screen.value = Screen.IMPORT }
    fun importSelect(i: Int) = _import.update { it.copy(selected = i, message = null) }

    /** Sets the selected cell and moves the cursor to the next one. */
    fun importDigit(d: Int) = _import.update { s ->
        if (s.selected < 0) s
        else s.copy(grid = s.grid.toMutableList().also { it[s.selected] = d }, selected = minOf(s.selected + 1, 80), message = null)
    }

    fun importErase() = _import.update { s ->
        if (s.selected < 0) s else s.copy(grid = s.grid.toMutableList().also { it[s.selected] = 0 }, message = null)
    }

    fun importClear() { _import.value = ImportUi() }
    fun importText(grid: IntArray) { _import.update { it.copy(grid = grid.toList(), selected = -1, message = null) } }

    fun importStart() {
        val s = _import.value
        if (s.busy) return
        viewModelScope.launch {
            _import.update { it.copy(busy = true, message = null) }
            val v = withContext(Dispatchers.Default) { PuzzleImport.validate(s.grid.toIntArray()) }
            when (v) {
                is ImportValidation.Ok -> {
                    beginGame(v.rating.level, s.grid.toIntArray(), v.solution, true, v.rating.hardest.rank)
                    _import.value = ImportUi()
                }
                is ImportValidation.Conflicts -> _import.update { it.copy(busy = false, message = UiMsg(R.string.import_err_conflicts)) }
                is ImportValidation.TooFewClues -> _import.update { it.copy(busy = false, message = UiMsg(R.string.import_err_few)) }
                is ImportValidation.NoSolution -> _import.update { it.copy(busy = false, message = UiMsg(R.string.import_err_none)) }
                is ImportValidation.Multiple -> _import.update { it.copy(busy = false, message = UiMsg(R.string.import_err_multi)) }
            }
        }
    }

    // ---------- backup ----------

    private suspend fun refreshReminder() {
        val finished = withContext(Dispatchers.IO) { dao.countFinished() }
        _reminderDue.value = BackupReminder.isDue(backupManager.lastSharedAt, backupManager.snoozedUntil, finished, System.currentTimeMillis())
    }

    fun snoozeReminder() {
        backupManager.snoozedUntil = System.currentTimeMillis() + 2 * 86_400_000L
        _reminderDue.value = false
    }

    fun openBackup() { _screen.value = Screen.BACKUP; refreshBackup() }

    private fun refreshBackup() {
        viewModelScope.launch {
            val local = withContext(Dispatchers.IO) { backupManager.listLocal() }
            _backup.update { it.copy(local = local, lastSharedAt = backupManager.lastSharedAt) }
        }
    }

    private fun errorMsg(e: Exception): UiMsg {
        if (e !is BackupException) return UiMsg(R.string.backup_err_generic)
        return when (e.error) {
            BackupError.NOT_JSON -> UiMsg(R.string.backup_err_not_json)
            BackupError.WRONG_FORMAT -> UiMsg(R.string.backup_err_format)
            BackupError.NEWER_VERSION -> UiMsg(R.string.backup_err_newer, listOf(e.detail))
            BackupError.EMPTY -> UiMsg(R.string.backup_err_empty)
            BackupError.TOO_LARGE -> UiMsg(R.string.backup_err_large)
            BackupError.READ_FAILED -> UiMsg(R.string.backup_err_read)
            BackupError.WRITE_FAILED -> UiMsg(R.string.backup_err_write)
        }
    }

    fun shareBackup() {
        if (_backup.value.busy) return
        viewModelScope.launch {
            _backup.update { it.copy(busy = true, message = null) }
            try {
                val uri = backupManager.writeShareFile()
                backupManager.lastSharedAt = System.currentTimeMillis()
                shareChannel.send(uri)
                _backup.update { it.copy(message = UiMsg(R.string.backup_msg_exported)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _backup.update { it.copy(message = errorMsg(e)) }
            } finally {
                _backup.update { it.copy(busy = false) }
            }
            refreshBackup()
            refreshReminder()
        }
    }

    fun openBackupUri(uri: Uri) =
        loadPreview(getApplication<Application>().getString(R.string.backup_source_file)) { backupManager.readText(uri) }

    fun restoreLocal(name: String) = loadPreview(name) { backupManager.readLocal(name) }

    /** Saves a backup to a file the user chose (system "save as" dialog). Not counted as an off-phone copy. */
    fun saveBackupTo(uri: Uri) {
        if (_backup.value.busy) return
        viewModelScope.launch {
            _backup.update { it.copy(busy = true, message = null) }
            try {
                backupManager.writeTo(uri)
                _backup.update { it.copy(message = UiMsg(R.string.backup_msg_saved)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _backup.update { it.copy(message = errorMsg(e)) }
            } finally {
                _backup.update { it.copy(busy = false) }
            }
        }
    }

    private fun loadPreview(label: String, read: suspend () -> String) {
        if (_backup.value.busy) return
        if (_screen.value == Screen.GAME) _game.value?.let { if (!it.won) persist(it) }
        _screen.value = Screen.BACKUP
        viewModelScope.launch {
            _backup.update { it.copy(busy = true, message = null, preview = null) }
            try {
                val p = backupManager.preview(read(), label)
                _backup.update { it.copy(preview = p) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _backup.update { it.copy(message = errorMsg(e)) }
            } finally {
                _backup.update { it.copy(busy = false) }
            }
            refreshBackup()
        }
    }

    fun cancelPreview() { _backup.update { it.copy(preview = null) } }
    fun dismissMessage() { _backup.update { it.copy(message = null) } }

    fun confirmImport(mode: ImportMode) {
        val p = _backup.value.preview ?: return
        if (_backup.value.busy) return
        viewModelScope.launch {
            _backup.update { it.copy(busy = true, message = null) }
            try {
                val done = CompletableDeferred<Unit>()
                enqueue {
                    try { backupManager.apply(p, mode); done.complete(Unit) } catch (t: Throwable) { done.completeExceptionally(t) }
                }
                done.await()
                reloadActive()
                val plan = (if (mode == ImportMode.MERGE) p.merge else p.replace).firstOrNull { it.key == "games" }
                _backup.update { it.copy(preview = null, message = UiMsg(R.string.backup_msg_imported, listOf(plan?.added ?: 0, plan?.updated ?: 0))) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _backup.update { it.copy(message = errorMsg(e)) }
            } finally {
                _backup.update { it.copy(busy = false) }
            }
            refreshBackup()
            refreshReminder()
        }
    }

    /** After an import the game in progress may have changed: reload it from the database. */
    private suspend fun reloadActive() {
        val e = withContext(Dispatchers.IO) { dao.getActive() }
        val s = e?.toState()
        undoStack.clear()
        _game.value = s
        _elapsed.value = if (s != null) (e?.elapsedSec ?: 0L) else 0L
    }
}
