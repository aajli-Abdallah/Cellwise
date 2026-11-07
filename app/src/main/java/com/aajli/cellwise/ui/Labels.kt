/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise.ui

import com.aajli.cellwise.R
import com.aajli.cellwise.engine.Difficulty

fun Difficulty.labelRes(): Int = when (this) {
    Difficulty.EASY -> R.string.difficulty_easy
    Difficulty.MEDIUM -> R.string.difficulty_medium
    Difficulty.HARD -> R.string.difficulty_hard
    Difficulty.EXPERT -> R.string.difficulty_expert
}

fun Difficulty.descRes(): Int = when (this) {
    Difficulty.EASY -> R.string.level_easy_desc
    Difficulty.MEDIUM -> R.string.level_medium_desc
    Difficulty.HARD -> R.string.level_hard_desc
    Difficulty.EXPERT -> R.string.level_expert_desc
}
