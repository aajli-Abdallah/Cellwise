/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise.feature.puzzleimport

import com.aajli.cellwise.core.UiMsg
import com.aajli.cellwise.engine.Rating
import com.aajli.cellwise.engine.SudokuRater
import com.aajli.cellwise.engine.SudokuSolver
import com.aajli.cellwise.engine.Units

/** State of the "import a grid" screen (kept in the ViewModel). */
data class ImportUi(
    val grid: List<Int> = List(81) { 0 },
    val selected: Int = -1,
    val message: UiMsg? = null,
    val busy: Boolean = false
)

sealed class ImportValidation {
    data class TooFewClues(val clues: Int) : ImportValidation()
    data class Conflicts(val cells: Set<Int>) : ImportValidation()
    object NoSolution : ImportValidation()
    object Multiple : ImportValidation()
    class Ok(val solution: IntArray, val rating: Rating) : ImportValidation()
}

/**
 * Plain puzzle code: 81 characters, row by row, digits 1-9 for clues and 0 for empty cells (a `.` is also read as empty).
 * Easy to read, type and send in any chat. The level is not part of the code: the importer rates the puzzle itself.
 */
object PuzzleCode {
    private val RUN = Regex("(?<![0-9.])[0-9.]{81}(?![0-9.])")

    fun encode(givens: List<Int>): String = givens.joinToString("")
    fun encode(givens: IntArray): String = givens.joinToString("")

    /** Finds a code inside any text (a shared message, a copied line). Null when there is no 81-character run. */
    fun extract(text: String): IntArray? =
        RUN.find(text)?.value?.let { s -> IntArray(81) { if (s[it] == '.') 0 else s[it] - '0' } }
}

/** Pure logic for typing in a printed grid: text parsing, duplicate detection, solver validation. */
object PuzzleImport {
    const val MIN_CLUES = 17

    class ParsedText(val cells: Int, val grid: IntArray?)

    private val BLANKS = setOf('0', '.', '-', '_', 'x', 'X', '*', '·')
    private val BORDER = setOf('+', '═', '─', '┼', '╬', '━', '┏', '┓', '┗', '┛', '╋')

    /** Digits 1-9 are clues; 0 . - _ x * are empty cells; everything else (spaces, |) is ignored; ASCII border lines are skipped. */
    fun parseText(text: String): ParsedText {
        PuzzleCode.extract(text)?.let { return ParsedText(81, it) }   // a shared code inside a message
        val out = ArrayList<Int>()
        for (line in text.lines()) {
            if (line.any { it in BORDER }) continue
            for (ch in line) when {
                ch in '1'..'9' -> out += ch - '0'
                ch in BLANKS -> out += 0
            }
        }
        return ParsedText(out.size, if (out.size == 81) out.toIntArray() else null)
    }

    /** Cells whose digit repeats in their row, column or box. */
    fun conflicts(grid: IntArray): Set<Int> {
        val bad = HashSet<Int>()
        for (u in Units.ALL) {
            val seen = HashMap<Int, Int>()
            for (c in u) {
                val v = grid[c]
                if (v == 0) continue
                val prev = seen[v]
                if (prev != null) { bad += c; bad += prev } else seen[v] = c
            }
        }
        return bad
    }

    fun validate(grid: IntArray): ImportValidation {
        val bad = conflicts(grid)
        if (bad.isNotEmpty()) return ImportValidation.Conflicts(bad)
        val clues = grid.count { it != 0 }
        if (clues < MIN_CLUES) return ImportValidation.TooFewClues(clues)
        val n = SudokuSolver.countSolutions(grid, 2)
        if (n == 0) return ImportValidation.NoSolution
        if (n > 1) return ImportValidation.Multiple
        val solution = SudokuSolver.solve(grid) ?: return ImportValidation.NoSolution
        val rating = SudokuRater.rate(grid) ?: return ImportValidation.NoSolution
        return ImportValidation.Ok(solution, rating)
    }
}
