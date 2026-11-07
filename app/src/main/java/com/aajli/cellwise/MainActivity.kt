/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.IntentCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aajli.cellwise.feature.about.AboutScreen
import com.aajli.cellwise.feature.backup.ui.BackupScreen
import com.aajli.cellwise.feature.game.GameState
import com.aajli.cellwise.feature.game.ui.GameScreen
import com.aajli.cellwise.feature.home.HomeScreen
import com.aajli.cellwise.feature.puzzleimport.PuzzleCode
import com.aajli.cellwise.feature.puzzleimport.ui.ImportScreen
import com.aajli.cellwise.feature.settings.SettingsScreen
import com.aajli.cellwise.feature.stats.ui.StatsScreen
import com.aajli.cellwise.ui.LocalReduceMotion
import com.aajli.cellwise.ui.labelRes
import com.aajli.cellwise.ui.theme.CellwiseTheme
import com.aajli.cellwise.ui.theme.isDark

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appSettings by vm.settings.collectAsStateWithLifecycle()
            val dark = isDark(appSettings.themeMode, isSystemInDarkTheme())
            // Status/navigation bar icons must follow the app theme, not only the system one.
            DisposableEffect(dark) {
                val transparent = android.graphics.Color.TRANSPARENT
                val style = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent)
                this@MainActivity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }
            val ctx = LocalContext.current
            val systemNoAnim = remember {
                android.provider.Settings.Global.getFloat(ctx.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
            }
            CompositionLocalProvider(LocalReduceMotion provides (appSettings.reduceMotion || systemNoAnim)) {
                CellwiseTheme(dynamic = appSettings.dynamicColors, themeMode = appSettings.themeMode, palette = appSettings.palette) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        Box(Modifier.fillMaxSize().systemBarsPadding()) { AppRoot(vm) }
                    }
                }
            }
        }
        if (savedInstanceState == null) handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    /** A backup file opened from another app (WhatsApp, Files, mail...) goes straight to the import preview. */
    private fun handleIntent(i: Intent?) {
        if (i == null) return
        val uri: Uri? = when (i.action) {
            Intent.ACTION_VIEW -> i.data
            Intent.ACTION_SEND -> IntentCompat.getParcelableExtra(i, Intent.EXTRA_STREAM, Uri::class.java)
            else -> null
        }
        if (uri != null) vm.openBackupUri(uri)
    }

    override fun onResume() { super.onResume(); vm.onForeground(true) }
    override fun onPause() { vm.onForeground(false); super.onPause() }
}

@Composable
fun AppRoot(vm: AppViewModel) {
    val screen by vm.screen.collectAsStateWithLifecycle()
    val game by vm.game.collectAsStateWithLifecycle()
    val elapsed by vm.elapsed.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val generating by vm.generating.collectAsStateWithLifecycle()
    val reminderDue by vm.reminderDue.collectAsStateWithLifecycle()
    val daily by vm.daily.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    val view = LocalView.current
    DisposableEffect(screen, settings.keepScreenOn) {
        view.keepScreenOn = screen == Screen.GAME && settings.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }
    BackHandler(enabled = screen != Screen.HOME) { vm.back() }

    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.saveBackupTo(uri)
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.openBackupUri(uri)
    }
    LaunchedEffect(Unit) {
        vm.shareEvents.collect { uri ->
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, ctx.getString(R.string.app_name) + " backup")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            ctx.startActivity(Intent.createChooser(send, null))
        }
    }

    when (screen) {
        Screen.HOME -> HomeScreen(
            active = game?.takeIf { !it.won }, elapsed = elapsed, generating = generating, reminderDue = reminderDue,
            onContinue = vm::continueGame, onNew = vm::newGame, onStats = vm::openStats, onBackup = vm::openBackup,
            onSettings = vm::openSettings, onAbout = vm::openAbout, onSnooze = vm::snoozeReminder, onImport = vm::openImport,
            daily = daily, onDaily = vm::playDaily
        )
        Screen.GAME -> game?.let { g ->
            GameScreen(
                state = g, elapsed = elapsed, settings = settings,
                onBack = vm::back, onSelect = vm::select, onDigit = vm::digit,
                onErase = vm::erase, onUndo = vm::undo, onToggleNotes = vm::toggleNotes,
                onHint = vm::hint, onCheck = vm::check, onAutoNotes = vm::autoNotes, onReveal = vm::reveal, onDismissPanel = vm::dismissPanel,
                onNewSame = { vm.newGame(g.difficulty) }, onHome = vm::leaveFinished,
                onShareCode = { sharePuzzle(ctx, g) }
            )
        }
        Screen.SETTINGS -> SettingsScreen(settings = settings, onChange = vm::updateSettings, onBack = vm::back)
        Screen.ABOUT -> AboutScreen(onBack = vm::back)
        Screen.STATS -> {
            val stats by vm.stats.collectAsStateWithLifecycle()
            StatsScreen(stats = stats, daily = daily, onBack = vm::back)
        }
        Screen.IMPORT -> {
            val importUi by vm.importUi.collectAsStateWithLifecycle()
            ImportScreen(
                ui = importUi, hasActive = game?.won == false, onBack = vm::back, onSelect = vm::importSelect,
                onDigit = vm::importDigit, onErase = vm::importErase, onClear = vm::importClear,
                onText = vm::importText, onStart = vm::importStart
            )
        }
        Screen.BACKUP -> {
            val ui by vm.backup.collectAsStateWithLifecycle()
            BackupScreen(
                ui = ui, onBack = vm::back, onShare = vm::shareBackup,
                onSaveFile = { saver.launch("Cellwise-backup-v${BuildConfig.VERSION_NAME}-${java.time.LocalDate.now()}.json") }, onPickFile = { picker.launch(arrayOf("*/*")) },
                onRestoreLocal = vm::restoreLocal, onCancelPreview = vm::cancelPreview,
                onImport = vm::confirmImport, onDismissMessage = vm::dismissMessage
            )
        }
    }
}

/** Shares the puzzle as plain text containing the 81-character code (digits, 0 = empty). */
private fun sharePuzzle(ctx: Context, g: GameState) {
    val text = ctx.getString(R.string.share_text, ctx.getString(g.difficulty.labelRes()), PuzzleCode.encode(g.givens))
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    ctx.startActivity(Intent.createChooser(send, null))
}
