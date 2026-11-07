/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise

import com.aajli.cellwise.core.formatTime
import com.aajli.cellwise.engine.Difficulty
import com.aajli.cellwise.engine.SudokuGenerator
import com.aajli.cellwise.engine.SudokuSolver
import com.aajli.cellwise.feature.game.GameRules
import com.aajli.cellwise.feature.game.GameState
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class EngineTest {
    private fun grid(s: String) = IntArray(81) { s[it].digitToInt() }

    private val known = "530070000600195000098000060800060003400803001700020006060000280000419005000080079"
    private val knownSol = "534678912672195348198342567859761423426853791713924856961537284287419635345286179"

    private fun valid(g: IntArray): Boolean {
        for (u in 0 until 9) {
            val r = (0 until 9).map { g[u * 9 + it] }.toSet()
            val c = (0 until 9).map { g[it * 9 + u] }.toSet()
            val b = (0 until 9).map { g[(u / 3) * 27 + (u % 3) * 3 + (it / 3) * 9 + it % 3] }.toSet()
            if (r.size != 9 || c.size != 9 || b.size != 9 || 0 in r) return false
        }
        return true
    }

    @Test fun solvesKnownPuzzle() {
        assertArrayEquals(grid(knownSol), SudokuSolver.solve(grid(known)))
        assertEquals(1, SudokuSolver.countSolutions(grid(known)))
    }

    @Test fun detectsInvalidAndMultiple() {
        assertEquals(0, SudokuSolver.countSolutions(grid(known.replaceRange(0, 2, "55"))))
        assertEquals(2, SudokuSolver.countSolutions(IntArray(81)))
    }

    @Test fun generatedPuzzlesAreUniqueAndConsistent() {
        for (d in Difficulty.values()) {
            val p = SudokuGenerator.generate(d, Random(42))
            assertTrue(valid(p.solution))
            assertEquals(1, SudokuSolver.countSolutions(p.givens))
            for (i in 0 until 81) if (p.givens[i] != 0) assertEquals(p.solution[i], p.givens[i])
            assertTrue(p.givens.count { it != 0 } >= 17)               // clue count now depends on the technique rating
        }
    }

    private fun state(): GameState {
        val sol = grid(knownSol).toList(); val giv = grid(known).toList()
        return GameState("t", Difficulty.EASY, giv, sol, giv, List(81) { 0 }, 0, 0L)
    }

    @Test fun rulesMistakesNotesAndWin() {
        var s = GameRules.select(state(), 2)               // solution digit 4
        s = GameRules.enterDigit(s, 9)
        assertEquals(1, s.mistakes); assertEquals(9, s.values[2])
        s = GameRules.enterDigit(s, 4)
        assertEquals(1, s.mistakes); assertEquals(4, s.values[2])
        assertEquals(s.values, GameRules.enterDigit(GameRules.select(s, 0), 1).values) // given cell untouched
        s = GameRules.erase(s); assertEquals(0, s.values[2])
        s = GameRules.toggleNotes(GameRules.select(s, 2))
        s = GameRules.enterDigit(GameRules.enterDigit(s, 4), 7)
        assertEquals((1 shl 4) or (1 shl 7), s.notes[2])
        s = GameRules.toggleNotes(s)
        for (i in 0 until 81) if (s.values[i] == 0) s = GameRules.enterDigit(GameRules.select(s, i), s.solution[i])
        assertTrue(s.won)
    }

    @Test fun timeFormat() {
        assertEquals("00:09", formatTime(9)); assertEquals("12:05", formatTime(725)); assertEquals("1:01:01", formatTime(3661))
    }
}
