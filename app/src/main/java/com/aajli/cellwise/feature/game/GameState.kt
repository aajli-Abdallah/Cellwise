/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise.feature.game

import com.aajli.cellwise.engine.Difficulty
import com.aajli.cellwise.engine.Hint
import com.aajli.cellwise.engine.HintEngine
import com.aajli.cellwise.engine.Units

/**
 * Immutable in-memory game. Lists have 81 entries, row by row; notes are bitmasks (bit d = digit d).
 * Saved fields: everything up to [rating]. The rest is transient UI state (never persisted).
 */
data class GameState(
    val id: String,
    val difficulty: Difficulty,
    val givens: List<Int>,
    val solution: List<Int>,
    val values: List<Int>,
    val notes: List<Int>,
    val mistakes: Int,
    val createdAt: Long,
    val selected: Int = -1,
    val notesMode: Boolean = false,
    val won: Boolean = false,
    val hints: Int = 0,          // hints requested
    val imported: Boolean = false,
    val rating: Int = 0,         // rank of the hardest technique needed (0 = unknown)
    val dailyDay: Int = 0,       // epoch day when this is the daily puzzle, 0 otherwise
    // transient
    val showErrors: Boolean = false,
    val hint: Hint? = null,
    val checkWrong: Int = -1     // -1 = no check panel, otherwise number of wrong cells found
) {
    /** How many of digit [d] are correctly placed (0..9). */
    fun placed(d: Int): Int = (0 until 81).count { values[it] == d && solution[it] == d }
}

object GameRules {

    fun sharesUnit(a: Int, b: Int): Boolean =
        a / 9 == b / 9 || a % 9 == b % 9 || (a / 27 == b / 27 && (a % 9) / 3 == (b % 9) / 3)

    private fun List<Int>.withAt(i: Int, v: Int): List<Int> = toMutableList().also { it[i] = v }

    fun select(s: GameState, i: Int): GameState = s.copy(selected = i)

    fun toggleNotes(s: GameState): GameState = s.copy(notesMode = !s.notesMode)

    fun enterDigit(s: GameState, d: Int): GameState {
        val i = s.selected
        if (i < 0 || s.won || s.givens[i] != 0 || d !in 1..9) return s
        if (s.notesMode) {
            if (s.values[i] != 0) return s
            return s.copy(notes = s.notes.withAt(i, s.notes[i] xor (1 shl d)))
        }
        if (s.values[i] == d) return s
        val correct = d == s.solution[i]
        var notes = s.notes.withAt(i, 0)
        if (correct) {
            val mask = (1 shl d).inv()
            notes = notes.mapIndexed { idx, m -> if (idx != i && sharesUnit(idx, i)) m and mask else m }
        }
        val values = s.values.withAt(i, d)
        return s.copy(
            values = values, notes = notes,
            mistakes = s.mistakes + if (correct) 0 else 1,
            won = values == s.solution,
            hint = null, checkWrong = -1, showErrors = false
        )
    }

    fun erase(s: GameState): GameState {
        val i = s.selected
        if (i < 0 || s.won || s.givens[i] != 0) return s
        if (s.values[i] == 0 && s.notes[i] == 0) return s
        return s.copy(values = s.values.withAt(i, 0), notes = s.notes.withAt(i, 0), hint = null, checkWrong = -1, showErrors = false)
    }

    /** Fills every empty cell's notes with the digits not yet used in its row, column and box. */
    fun autoNotes(s: GameState): GameState {
        if (s.won) return s
        val notes = List(81) { i ->
            if (s.values[i] != 0) 0 else {
                var m = 0x3FE
                for (p in Units.PEERS[i]) if (s.values[p] != 0) m = m and (1 shl s.values[p]).inv()
                m
            }
        }
        return s.copy(notes = notes)
    }

    fun wrongCells(s: GameState): List<Int> = (0 until 81).filter { s.values[it] != 0 && s.values[it] != s.solution[it] }

    /** Marks wrong digits in red until the next change and shows how many there are. */
    fun check(s: GameState): GameState =
        if (s.won) s else s.copy(showErrors = true, checkWrong = wrongCells(s).size, hint = null)

    /**
     * Next logical step. If the grid contains wrong digits they are shown instead (a hint on a wrong grid is meaningless).
     * Each new hint counts in [GameState.hints].
     */
    fun requestHint(s: GameState): GameState {
        if (s.won || s.hint != null) return s
        val wrong = wrongCells(s)
        if (wrong.isNotEmpty()) return s.copy(showErrors = true, checkWrong = wrong.size)
        val h = HintEngine.next(s.values.toIntArray(), s.solution.toIntArray()) ?: return s
        return s.copy(hint = h, selected = h.cell, checkWrong = -1, hints = s.hints + 1)
    }

    /** Places the digit of the current hint. */
    fun revealHint(s: GameState): GameState {
        val h = s.hint ?: return s
        val r = enterDigit(s.copy(selected = h.cell, notesMode = false), h.digit)
        return r.copy(notesMode = s.notesMode, hint = null)
    }

    fun dismissPanel(s: GameState): GameState = s.copy(hint = null, checkWrong = -1, showErrors = false)
}
