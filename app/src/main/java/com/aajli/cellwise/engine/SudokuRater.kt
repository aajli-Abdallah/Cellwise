/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise.engine

/**
 * Technique-based rating. [hardest] = hardest technique needed (GUESS when the techniques are not enough).
 * Levels: EASY = singles only; MEDIUM = up to locked candidates / naked pairs; HARD = hidden pairs, triples, X-Wing, XY-Wing;
 * EXPERT = Swordfish or more than the techniques the app knows (advanced logic or trial and error).
 */
class Rating(val solved: Boolean, val hardest: Technique) {
    val level: Difficulty
        get() = when {
            hardest.ordinal <= Technique.HIDDEN_SINGLE.ordinal -> Difficulty.EASY
            hardest.ordinal <= Technique.NAKED_PAIR.ordinal -> Difficulty.MEDIUM
            hardest.ordinal <= Technique.XY_WING.ordinal -> Difficulty.HARD
            else -> Difficulty.EXPERT
        }
}

object SudokuRater {
    /** Returns null when the grid is invalid (duplicates or a cell without candidates). */
    fun rate(givens: IntArray): Rating? {
        val s = TechniqueSolver(givens)
        if (!s.valid) return null
        var hardest = Technique.NAKED_SINGLE
        while (!s.solved()) {
            val step = s.nextStep() ?: break
            s.apply(step)
            if (step.technique.ordinal > hardest.ordinal) hardest = step.technique
        }
        return if (s.solved()) Rating(true, hardest) else Rating(false, Technique.GUESS)
    }
}
