/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise.engine

/**
 * A hint: put [digit] in [cell]. [technique] is the technique that proves it, [unit] the row/column/box
 * to look at (Units index, or -1) and [via] the last elimination technique needed first, if any.
 * GUESS means no listed technique applied and the digit comes from the solution.
 */
class Hint(val technique: Technique, val cell: Int, val digit: Int, val unit: Int, val via: Technique?)

object HintEngine {
    /** Next logical placement for the current board (givens + the player's digits). Notes are ignored. */
    fun next(values: IntArray, solution: IntArray?): Hint? {
        val s = TechniqueSolver(values)
        var via: Technique? = null
        while (s.valid) {
            val step = s.nextStep() ?: break
            if (step.digit != 0) return Hint(step.technique, step.cell, step.digit, step.unit, via)
            s.apply(step)
            via = step.technique
        }
        if (solution == null) return null
        val empty = values.indices.filter { values[it] == 0 }
        if (empty.isEmpty()) return null
        val cell = if (s.valid) empty.minByOrNull { Integer.bitCount(s.cand[it]) }!! else empty.first()
        return Hint(Technique.GUESS, cell, solution[cell], -1, null)
    }
}
