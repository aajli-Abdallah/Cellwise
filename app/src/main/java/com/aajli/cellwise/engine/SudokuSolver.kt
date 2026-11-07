/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise.engine

import kotlin.random.Random

/** Pure Kotlin backtracking solver (bitmasks + minimum-remaining-values). No Android imports. */
object SudokuSolver {

    fun countSolutions(grid: IntArray, limit: Int = 2): Int {
        val s = Search(grid, limit, null)
        if (!s.valid) return 0
        s.run()
        return s.count
    }

    fun solve(grid: IntArray): IntArray? {
        val s = Search(grid, 1, null)
        if (!s.valid) return null
        s.run()
        return s.first
    }

    fun randomSolution(rnd: Random): IntArray {
        val s = Search(IntArray(81), 1, rnd)
        s.run()
        return s.first!!
    }

    private class Search(start: IntArray, val limit: Int, val rnd: Random?) {
        val g = start.copyOf()
        val row = IntArray(9)
        val col = IntArray(9)
        val box = IntArray(9)
        var valid = start.size == 81
        var count = 0
        var first: IntArray? = null

        init {
            if (valid) for (i in 0 until 81) {
                val v = g[i]
                if (v == 0) continue
                if (v !in 1..9) { valid = false; break }
                val bit = 1 shl v
                val r = i / 9; val c = i % 9; val b = boxOf(i)
                if ((row[r] or col[c] or box[b]) and bit != 0) { valid = false; break }
                row[r] = row[r] or bit; col[c] = col[c] or bit; box[b] = box[b] or bit
            }
        }

        fun boxOf(i: Int) = (i / 27) * 3 + (i % 9) / 3

        fun cands(i: Int): Int = 0x3FE and (row[i / 9] or col[i % 9] or box[boxOf(i)]).inv()

        fun flip(i: Int, d: Int) {
            val bit = 1 shl d
            row[i / 9] = row[i / 9] xor bit
            col[i % 9] = col[i % 9] xor bit
            box[boxOf(i)] = box[boxOf(i)] xor bit
        }

        fun run() { dfs() }

        /** Returns true when the search must stop (limit reached). */
        private fun dfs(): Boolean {
            var best = -1; var bestMask = 0; var bestCnt = 10
            for (i in 0 until 81) {
                if (g[i] != 0) continue
                val m = cands(i)
                val n = Integer.bitCount(m)
                if (n == 0) return false
                if (n < bestCnt) { best = i; bestMask = m; bestCnt = n; if (n == 1) break }
            }
            if (best == -1) {
                count++
                if (first == null) first = g.copyOf()
                return count >= limit
            }
            val digits = (1..9).filter { bestMask and (1 shl it) != 0 }.let { if (rnd != null) it.shuffled(rnd) else it }
            for (d in digits) {
                g[best] = d; flip(best, d)
                val stop = dfs()
                flip(best, d); g[best] = 0
                if (stop) return true
            }
            return false
        }
    }
}
