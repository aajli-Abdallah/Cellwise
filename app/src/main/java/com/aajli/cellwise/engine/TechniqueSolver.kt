/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise.engine

/**
 * Human solving techniques. Difficulty order = declaration order (use `ordinal` to compare).
 * [rank] is a stable id stored in the database (`games.rating`): never renumber, only add.
 * GUESS = the techniques below are not enough (advanced logic or trial and error).
 */
enum class Technique(val rank: Int) {
    NAKED_SINGLE(1), HIDDEN_SINGLE(2), POINTING(3), BOX_LINE(4), NAKED_PAIR(5), HIDDEN_PAIR(6),
    NAKED_TRIPLE(10), HIDDEN_TRIPLE(11), X_WING(7), XY_WING(12), SWORDFISH(13), GUESS(9);

    companion object { fun fromRank(r: Int): Technique? = values().firstOrNull { it.rank == r } }
}

/** Units: indices 0-8 rows, 9-17 columns, 18-26 boxes. */
object Units {
    val ROWS = Array(9) { r -> IntArray(9) { r * 9 + it } }
    val COLS = Array(9) { c -> IntArray(9) { it * 9 + c } }
    val BOXES = Array(9) { b -> IntArray(9) { (b / 3) * 27 + (b % 3) * 3 + (it / 3) * 9 + it % 3 } }
    val ALL: Array<IntArray> = ROWS + COLS + BOXES
    fun boxOf(cell: Int) = (cell / 27) * 3 + (cell % 9) / 3
    val PEERS: Array<IntArray> = Array(81) { c ->
        (ROWS[c / 9] + COLS[c % 9] + BOXES[boxOf(c)]).filter { it != c }.distinct().toIntArray()
    }
}

/**
 * One solving step: a placement (digit != 0) or eliminations (elims = cell to mask of removed digits).
 * [unit] is the unit index (see Units) the technique works in, or -1.
 */
class Step(
    val technique: Technique, val cell: Int = -1, val digit: Int = 0, val unit: Int = -1,
    val elims: List<Pair<Int, Int>> = emptyList()
)

/** Candidate-based solver that only uses the techniques above, always the simplest available first. Pure Kotlin. */
class TechniqueSolver(start: IntArray) {
    val grid = start.copyOf()
    val cand = IntArray(81)
    var valid = start.size == 81

    init {
        if (valid) {
            for (i in 0 until 81) {
                val v = grid[i]
                if (v !in 0..9) { valid = false; break }
                if (v != 0 && Units.PEERS[i].any { grid[it] == v }) { valid = false; break }
            }
        }
        if (valid) for (i in 0 until 81) {
            if (grid[i] != 0) continue
            var m = 0x3FE
            for (p in Units.PEERS[i]) if (grid[p] != 0) m = m and (1 shl grid[p]).inv()
            cand[i] = m
            if (m == 0) valid = false
        }
    }

    fun solved() = valid && grid.all { it != 0 }

    fun place(cell: Int, d: Int) {
        grid[cell] = d
        cand[cell] = 0
        for (p in Units.PEERS[cell]) cand[p] = cand[p] and (1 shl d).inv()
    }

    fun apply(s: Step) {
        if (s.digit != 0) place(s.cell, s.digit)
        else for ((c, m) in s.elims) cand[c] = cand[c] and m.inv()
    }

    private fun has(c: Int, bit: Int) = grid[c] == 0 && cand[c] and bit != 0

    fun nextStep(): Step? {
        if (!valid) return null
        return nakedSingle() ?: hiddenSingle() ?: pointing() ?: boxLine() ?: nakedPair() ?: hiddenPair() ?: nakedTriple() ?:
            hiddenTriple() ?: xWing() ?: xyWing() ?: swordfish()
    }

    private fun nakedSingle(): Step? {
        for (i in 0 until 81) if (grid[i] == 0 && Integer.bitCount(cand[i]) == 1)
            return Step(Technique.NAKED_SINGLE, i, Integer.numberOfTrailingZeros(cand[i]))
        return null
    }

    private fun hiddenSingle(): Step? {
        for (u in 0 until 27) for (d in 1..9) {
            val bit = 1 shl d
            var n = 0; var at = -1
            for (c in Units.ALL[u]) if (has(c, bit)) { n++; at = c }
            if (n == 1) return Step(Technique.HIDDEN_SINGLE, at, d, u)
        }
        return null
    }

    /** Candidates of a digit inside a box all in one row/column: remove it from the rest of that line. */
    private fun pointing(): Step? {
        for (b in 0 until 9) for (d in 1..9) {
            val bit = 1 shl d
            val cells = Units.BOXES[b].filter { has(it, bit) }
            if (cells.size < 2) continue
            val lines = ArrayList<IntArray>()
            if (cells.all { it / 9 == cells[0] / 9 }) lines += Units.ROWS[cells[0] / 9]
            if (cells.all { it % 9 == cells[0] % 9 }) lines += Units.COLS[cells[0] % 9]
            for (line in lines) {
                val el = line.filter { has(it, bit) && Units.boxOf(it) != b }.map { it to bit }
                if (el.isNotEmpty()) return Step(Technique.POINTING, unit = 18 + b, elims = el)
            }
        }
        return null
    }

    /** Candidates of a digit inside a row/column all in one box: remove it from the rest of that box. */
    private fun boxLine(): Step? {
        for (u in 0 until 18) for (d in 1..9) {
            val bit = 1 shl d
            val cells = Units.ALL[u].filter { has(it, bit) }
            if (cells.size < 2) continue
            val bx = Units.boxOf(cells[0])
            if (!cells.all { Units.boxOf(it) == bx }) continue
            val el = Units.BOXES[bx].filter { has(it, bit) && it !in cells }.map { it to bit }
            if (el.isNotEmpty()) return Step(Technique.BOX_LINE, unit = u, elims = el)
        }
        return null
    }

    private fun nakedPair(): Step? {
        for (u in 0 until 27) {
            val two = Units.ALL[u].filter { grid[it] == 0 && Integer.bitCount(cand[it]) == 2 }
            for (i in two.indices) for (j in i + 1 until two.size) {
                if (cand[two[i]] != cand[two[j]]) continue
                val m = cand[two[i]]
                val el = Units.ALL[u].filter { grid[it] == 0 && it != two[i] && it != two[j] && cand[it] and m != 0 }
                    .map { it to (cand[it] and m) }
                if (el.isNotEmpty()) return Step(Technique.NAKED_PAIR, unit = u, elims = el)
            }
        }
        return null
    }

    private fun hiddenPair(): Step? {
        for (u in 0 until 27) {
            val pos = Array(10) { d -> if (d == 0) emptyList() else Units.ALL[u].filter { has(it, 1 shl d) } }
            for (d1 in 1..8) {
                if (pos[d1].size != 2) continue
                for (d2 in d1 + 1..9) {
                    if (pos[d2] != pos[d1]) continue
                    val keep = (1 shl d1) or (1 shl d2)
                    val el = pos[d1].filter { cand[it] and keep.inv() != 0 }.map { it to (cand[it] and keep.inv()) }
                    if (el.isNotEmpty()) return Step(Technique.HIDDEN_PAIR, unit = u, elims = el)
                }
            }
        }
        return null
    }

    /** Three cells of a unit whose candidates together are only three digits: remove those digits from the rest of the unit. */
    private fun nakedTriple(): Step? {
        for (u in 0 until 27) {
            val cs = Units.ALL[u].filter { grid[it] == 0 && Integer.bitCount(cand[it]) in 2..3 }
            for (i in cs.indices) for (j in i + 1 until cs.size) for (k in j + 1 until cs.size) {
                val m = cand[cs[i]] or cand[cs[j]] or cand[cs[k]]
                if (Integer.bitCount(m) != 3) continue
                val el = Units.ALL[u].filter { grid[it] == 0 && it != cs[i] && it != cs[j] && it != cs[k] && cand[it] and m != 0 }
                    .map { it to (cand[it] and m) }
                if (el.isNotEmpty()) return Step(Technique.NAKED_TRIPLE, unit = u, elims = el)
            }
        }
        return null
    }

    /** Three digits of a unit that fit only the same three cells: remove every other candidate from those cells. */
    private fun hiddenTriple(): Step? {
        for (u in 0 until 27) {
            val cellsOf = Array(10) { d -> if (d == 0) emptyList() else Units.ALL[u].filter { has(it, 1 shl d) } }
            val ds = (1..9).filter { cellsOf[it].size in 2..3 }
            for (a in ds.indices) for (b in a + 1 until ds.size) for (c in b + 1 until ds.size) {
                val cells = (cellsOf[ds[a]] + cellsOf[ds[b]] + cellsOf[ds[c]]).distinct()
                if (cells.size != 3) continue
                val keep = (1 shl ds[a]) or (1 shl ds[b]) or (1 shl ds[c])
                val el = cells.filter { cand[it] and keep.inv() != 0 }.map { it to (cand[it] and keep.inv()) }
                if (el.isNotEmpty()) return Step(Technique.HIDDEN_TRIPLE, unit = u, elims = el)
            }
        }
        return null
    }

    /** Pivot {A,B} with wings {A,C} and {B,C} that both see it: C cannot be in any cell that sees both wings. */
    private fun xyWing(): Step? {
        for (p in 0 until 81) {
            if (grid[p] != 0 || Integer.bitCount(cand[p]) != 2) continue
            val a = cand[p]
            val wings = Units.PEERS[p].filter { grid[it] == 0 && Integer.bitCount(cand[it]) == 2 }
            for (i in wings.indices) for (j in i + 1 until wings.size) {
                val w1 = wings[i]; val w2 = wings[j]
                val x = cand[w1]; val y = cand[w2]
                val c = x and y
                if (Integer.bitCount(c) != 1 || c and a != 0) continue
                if (Integer.bitCount(x and a) != 1 || Integer.bitCount(y and a) != 1 || (x and a) == (y and a)) continue
                val seesW2 = Units.PEERS[w2].toSet()
                val el = Units.PEERS[w1].filter { it != p && it != w2 && it in seesW2 && has(it, c) }.map { it to c }
                if (el.isNotEmpty()) return Step(Technique.XY_WING, elims = el)
            }
        }
        return null
    }

    /** X-Wing with three rows (or columns): the digit's candidates of three lines sit in the same three crossing lines. */
    private fun swordfish(): Step? {
        for (d in 1..9) {
            val bit = 1 shl d
            val rowCols = Array(9) { r -> (0 until 9).filter { c -> has(r * 9 + c, bit) } }
            val rows = (0 until 9).filter { rowCols[it].size in 2..3 }
            for (a in rows.indices) for (b in a + 1 until rows.size) for (c in b + 1 until rows.size) {
                val cols = (rowCols[rows[a]] + rowCols[rows[b]] + rowCols[rows[c]]).distinct()
                if (cols.size != 3) continue
                val el = (0 until 9).filter { it != rows[a] && it != rows[b] && it != rows[c] }
                    .flatMap { r -> cols.map { col -> r * 9 + col } }.filter { has(it, bit) }.map { it to bit }
                if (el.isNotEmpty()) return Step(Technique.SWORDFISH, unit = rows[a], elims = el)
            }
            val colRows = Array(9) { c -> (0 until 9).filter { r -> has(r * 9 + c, bit) } }
            val cs = (0 until 9).filter { colRows[it].size in 2..3 }
            for (a in cs.indices) for (b in a + 1 until cs.size) for (c in b + 1 until cs.size) {
                val rows3 = (colRows[cs[a]] + colRows[cs[b]] + colRows[cs[c]]).distinct()
                if (rows3.size != 3) continue
                val el = (0 until 9).filter { it != cs[a] && it != cs[b] && it != cs[c] }
                    .flatMap { col -> rows3.map { r -> r * 9 + col } }.filter { has(it, bit) }.map { it to bit }
                if (el.isNotEmpty()) return Step(Technique.SWORDFISH, unit = 9 + cs[a], elims = el)
            }
        }
        return null
    }

    private fun xWing(): Step? {
        for (d in 1..9) {
            val bit = 1 shl d
            val rowCols = Array(9) { r -> (0 until 9).filter { c -> has(r * 9 + c, bit) } }
            for (r1 in 0 until 8) {
                if (rowCols[r1].size != 2) continue
                for (r2 in r1 + 1 until 9) {
                    if (rowCols[r2] != rowCols[r1]) continue
                    val el = (0 until 9).filter { it != r1 && it != r2 }
                        .flatMap { r -> rowCols[r1].map { c -> r * 9 + c } }.filter { has(it, bit) }.map { it to bit }
                    if (el.isNotEmpty()) return Step(Technique.X_WING, unit = r1, elims = el)
                }
            }
            val colRows = Array(9) { c -> (0 until 9).filter { r -> has(r * 9 + c, bit) } }
            for (c1 in 0 until 8) {
                if (colRows[c1].size != 2) continue
                for (c2 in c1 + 1 until 9) {
                    if (colRows[c2] != colRows[c1]) continue
                    val el = (0 until 9).filter { it != c1 && it != c2 }
                        .flatMap { c -> colRows[c1].map { r -> r * 9 + c } }.filter { has(it, bit) }.map { it to bit }
                    if (el.isNotEmpty()) return Step(Technique.X_WING, unit = 9 + c1, elims = el)
                }
            }
        }
        return null
    }
}
