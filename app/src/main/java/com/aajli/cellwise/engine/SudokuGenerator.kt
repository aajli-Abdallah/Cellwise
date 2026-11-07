/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise.engine

import kotlin.random.Random

/** Generates puzzles with a unique solution whose technique rating matches the requested level. Original algorithm. */
object SudokuGenerator {
    private const val MAX_ATTEMPTS = 40

    fun generate(difficulty: Difficulty, rnd: Random = Random.Default): Puzzle {
        var best: Puzzle? = null
        var bestGap = Int.MAX_VALUE
        repeat(MAX_ATTEMPTS) {
            val p = attempt(difficulty, rnd)
            val gap = Math.abs(p.rating.level.ordinal - difficulty.ordinal)
            if (gap == 0) return p
            if (gap < bestGap) { best = p; bestGap = gap }
        }
        return best!!
    }

    private fun attempt(difficulty: Difficulty, rnd: Random): Puzzle {
        val solution = SudokuSolver.randomSolution(rnd)
        val givens = solution.copyOf()
        var clues = 81
        val target = difficulty.targetClues

        // Pass 1: remove 180-degree symmetric pairs (classic newspaper look).
        for (i in (0..40).shuffled(rnd)) {
            if (clues <= target) break
            val j = 80 - i
            val a = givens[i]; val b = givens[j]
            givens[i] = 0; givens[j] = 0
            if (SudokuSolver.countSolutions(givens, 2) == 1) clues -= if (i == j) 1 else 2
            else { givens[i] = a; givens[j] = b }
        }
        // Pass 2: single removals if the clue target was not reached.
        if (clues > target) {
            for (i in (0 until 81).shuffled(rnd)) {
                if (clues <= target) break
                if (givens[i] == 0) continue
                val a = givens[i]
                givens[i] = 0
                if (SudokuSolver.countSolutions(givens, 2) == 1) clues-- else givens[i] = a
            }
        }

        var rating = SudokuRater.rate(givens)!!
        // Too easy for the level: keep removing clues (never overshooting the level) until it is reached.
        if (rating.level.ordinal < difficulty.ordinal) {
            for (i in (0 until 81).filter { givens[it] != 0 }.shuffled(rnd)) {
                val a = givens[i]
                givens[i] = 0
                if (SudokuSolver.countSolutions(givens, 2) != 1) { givens[i] = a; continue }
                val r = SudokuRater.rate(givens)!!
                if (r.level.ordinal > difficulty.ordinal) { givens[i] = a; continue }
                rating = r
                if (rating.level.ordinal >= difficulty.ordinal) break
            }
        }
        return Puzzle(givens, solution, rating)
    }
}
