/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise.engine

/** Starting clue target per level. The final level is decided by the technique rating (see SudokuRater). */
enum class Difficulty(val targetClues: Int) {
    EASY(40), MEDIUM(32), HARD(26), EXPERT(24)
}

/** A puzzle (0 = empty), its unique solution and its technique rating. 81 cells, row by row. */
class Puzzle(val givens: IntArray, val solution: IntArray, val rating: Rating)
