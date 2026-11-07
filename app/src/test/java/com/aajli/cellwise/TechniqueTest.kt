package com.aajli.cellwise

import com.aajli.cellwise.engine.*
import com.aajli.cellwise.feature.game.GameRules
import com.aajli.cellwise.feature.game.GameState
import com.aajli.cellwise.feature.puzzleimport.ImportValidation
import com.aajli.cellwise.feature.puzzleimport.PuzzleImport
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class TechniqueTest {
    private fun grid(s: String) = IntArray(81) { s[it].digitToInt() }
    private val known = "530070000600195000098000060800060003400803001700020006060000280000419005000080079"
    private val knownSol = "534678912672195348198342567859761423426853791713924856961537284287419635345286179"
    private val inkala = "800000000003600000070090200050007000000045700000100030001000068008500010090000400"

    @Test fun ratingKnownPuzzles() {
        assertEquals(Difficulty.EASY, SudokuRater.rate(grid(known))!!.level)
        val r = SudokuRater.rate(grid(inkala))!!
        assertEquals(Difficulty.EXPERT, r.level); assertTrue(!r.solved); assertEquals(Technique.GUESS, r.hardest)
        assertEquals(null, SudokuRater.rate(grid(known.replaceRange(0, 2, "55"))))
        assertEquals(Difficulty.MEDIUM, Rating(true, Technique.NAKED_PAIR).level)
        assertEquals(Difficulty.HARD, Rating(true, Technique.HIDDEN_PAIR).level)
        assertEquals(Difficulty.HARD, Rating(true, Technique.XY_WING).level)
        assertEquals(Difficulty.EXPERT, Rating(true, Technique.SWORDFISH).level)
        assertEquals(Difficulty.EXPERT, Rating(false, Technique.GUESS).level)
        assertEquals(setOf(1, 2, 3, 4, 5, 6, 7, 9, 10, 11, 12, 13), Technique.values().map { it.rank }.toSet())   // stored ids never change
    }

    /** Every step of every technique must agree with the real solution (placements correct, no solution digit eliminated). */
    @Test fun stepsNeverContradictSolution() {
        val rnd = Random(11)
        val used = HashSet<Technique>()
        for (d in Difficulty.values()) repeat(25) {
            val p = SudokuGenerator.generate(d, rnd)
            val s = TechniqueSolver(p.givens)
            assertTrue(s.valid)
            while (!s.solved()) {
                val st = s.nextStep() ?: break
                used += st.technique
                if (st.digit != 0) assertEquals(p.solution[st.cell], st.digit)
                s.apply(st)
                for (i in 0 until 81) if (s.grid[i] == 0) assertTrue(s.cand[i] and (1 shl p.solution[i]) != 0)
            }
            if (s.solved()) assertArrayEquals(p.solution, s.grid)
        }
        assertTrue("techniques used: $used", used.containsAll(listOf(Technique.NAKED_SINGLE, Technique.HIDDEN_SINGLE, Technique.POINTING, Technique.NAKED_PAIR, Technique.XY_WING)))
    }

    @Test fun generatorMatchesLevel() {
        val rnd = Random(3)
        for (d in Difficulty.values()) {
            var ok = 0
            repeat(10) {
                val p = SudokuGenerator.generate(d, rnd)
                assertEquals(1, SudokuSolver.countSolutions(p.givens))
                if (p.rating.level == d) ok++
            }
            assertTrue("$d matched $ok/10", ok >= 9)
        }
    }

    @Test fun hintsLeadToTheSolution() {
        val rnd = Random(5)
        for (d in Difficulty.values()) repeat(5) {
            val p = SudokuGenerator.generate(d, rnd)
            val v = p.givens.copyOf()
            var n = 0
            while (v.any { it == 0 }) {
                val h = HintEngine.next(v, p.solution)!!
                assertEquals(0, v[h.cell]); assertEquals(p.solution[h.cell], h.digit)
                v[h.cell] = h.digit; n++
            }
            assertTrue(n <= 81)
        }
    }

    private fun state(sel: Int = -1): GameState {
        val giv = grid(known).toList(); val sol = grid(knownSol).toList()
        return GameState("t", Difficulty.EASY, giv, sol, giv, List(81) { 0 }, 0, 0L, selected = sel)
    }

    @Test fun autoNotesCheckAndHints() {
        val s = GameRules.autoNotes(state())
        assertEquals(0, s.notes[0])                              // given cell has no notes
        val c2 = s.notes[2]                                      // empty cell: candidates exclude row/col/box digits
        assertTrue(c2 and (1 shl 4) != 0)                        // the solution digit stays a candidate
        assertTrue(c2 and (1 shl 5) == 0)                        // 5 is in its row
        // hint on a clean grid
        val h = GameRules.requestHint(state())
        assertTrue(h.hint != null); assertEquals(1, h.hints); assertEquals(h.hint!!.cell, h.selected)
        assertEquals(h, GameRules.requestHint(h))                // second request does not count again
        val r = GameRules.revealHint(h)
        assertEquals(h.hint!!.digit, r.values[h.hint!!.cell]); assertEquals(null, r.hint); assertEquals(0, r.mistakes)
        // wrong digit blocks hints and check reports it
        val wrong = GameRules.enterDigit(state(2), 9)
        val blocked = GameRules.requestHint(wrong)
        assertEquals(null, blocked.hint); assertEquals(1, blocked.checkWrong); assertTrue(blocked.showErrors); assertEquals(0, blocked.hints)
        assertEquals(0, GameRules.check(state()).checkWrong)
        // changing the board clears the panel
        assertEquals(-1, GameRules.enterDigit(blocked.copy(selected = 2), 4).checkWrong)
        assertTrue(!GameRules.dismissPanel(blocked).showErrors)
    }

    @Test fun importParsing() {
        val ascii = """
            +-------+-------+-------+
            | 5 3 . | . 7 . | . . . |
            | 6 . . | 1 9 5 | . . . |
            | . 9 8 | . . . | . 6 . |
            +-------+-------+-------+
            | 8 . . | . 6 . | . . 3 |
            | 4 . . | 8 . 3 | . . 1 |
            | 7 . . | . 2 . | . . 6 |
            +-------+-------+-------+
            | . 6 . | . . . | 2 8 . |
            | . . . | 4 1 9 | . . 5 |
            | . . . | . 8 . | . 7 9 |
            +-------+-------+-------+
        """.trimIndent()
        assertArrayEquals(grid(known), PuzzleImport.parseText(ascii).grid)
        assertArrayEquals(grid(known), PuzzleImport.parseText(known).grid)
        assertEquals(80, PuzzleImport.parseText(known.dropLast(1)).cells); assertEquals(null, PuzzleImport.parseText(known.dropLast(1)).grid)
        assertArrayEquals(grid(known), PuzzleImport.parseText(known.replace('0', '.').chunked(9).joinToString("\n")).grid)
    }

    @Test fun importValidation() {
        val ok = PuzzleImport.validate(grid(known))
        assertTrue(ok is ImportValidation.Ok)
        assertArrayEquals(grid(knownSol), (ok as ImportValidation.Ok).solution); assertEquals(Difficulty.EASY, ok.rating.level)

        assertTrue(PuzzleImport.validate(grid(known.replaceRange(0, 2, "55"))) is ImportValidation.Conflicts)
        assertEquals(setOf(0, 1), (PuzzleImport.validate(grid(known.replaceRange(0, 2, "55"))) as ImportValidation.Conflicts).cells)
        assertTrue(PuzzleImport.validate(IntArray(81).also { it[0] = 1 }) is ImportValidation.TooFewClues)

        val sparse = grid(known); var removed = 0
        for (i in 0 until 81) if (sparse[i] != 0 && removed < 12) { sparse[i] = 0; removed++ }
        assertTrue(PuzzleImport.validate(sparse) is ImportValidation.Multiple)

        // no duplicate, enough clues, but unsolvable
        var found = false
        val base = grid(known)
        loop@ for (i in 0 until 81) if (base[i] != 0) for (d in 1..9) if (d != base[i]) {
            val g = base.copyOf(); g[i] = d
            if (PuzzleImport.conflicts(g).isEmpty() && SudokuSolver.countSolutions(g, 2) == 0) {
                assertTrue(PuzzleImport.validate(g) is ImportValidation.NoSolution); found = true; break@loop
            }
        }
        assertTrue(found)
    }

    @Test fun advancedTechniquesAreSoundOnManyPuzzles() {
        // 120 random Hard/Expert-sized puzzles: every elimination of the new techniques must keep the solution digit.
        val rnd = Random(21)
        val used = HashSet<Technique>()
        repeat(120) {
            val p = SudokuGenerator.generate(if (it % 2 == 0) Difficulty.HARD else Difficulty.EXPERT, rnd)
            val s = TechniqueSolver(p.givens)
            while (!s.solved()) {
                val st = s.nextStep() ?: break
                used += st.technique
                if (st.digit != 0) assertEquals(p.solution[st.cell], st.digit)
                s.apply(st)
                for (i in 0 until 81) if (s.grid[i] == 0) assertTrue(s.cand[i] and (1 shl p.solution[i]) != 0)
            }
        }
        assertTrue("used: $used", used.contains(Technique.XY_WING) && used.contains(Technique.HIDDEN_PAIR))
    }

    @Test fun expertAndHardAreDifferent() {
        val rnd = Random(8)
        repeat(5) {
            val e = SudokuGenerator.generate(Difficulty.EXPERT, rnd)
            assertEquals(Difficulty.EXPERT, e.rating.level); assertEquals(1, SudokuSolver.countSolutions(e.givens))
            val h = SudokuGenerator.generate(Difficulty.HARD, rnd)
            assertEquals(Difficulty.HARD, h.rating.level); assertTrue(SudokuRater.rate(h.givens)!!.solved)
        }
    }

    @Test fun puzzleCodeShareAndExtract() {
        val code = known
        assertEquals(code, com.aajli.cellwise.feature.puzzleimport.PuzzleCode.encode(grid(code)))
        val shared = "Cellwise puzzle · Expert\n$code\nIn Cellwise: Import → Paste text."     // letters like x must not break it
        assertArrayEquals(grid(code), com.aajli.cellwise.feature.puzzleimport.PuzzleCode.extract(shared))
        assertArrayEquals(grid(code), PuzzleImport.parseText(shared).grid)
        assertEquals(null, com.aajli.cellwise.feature.puzzleimport.PuzzleCode.extract(code + "1"))        // 82 digits: not a code
        assertArrayEquals(grid(code), com.aajli.cellwise.feature.puzzleimport.PuzzleCode.extract(code.replace('0', '.')))
        assertEquals(null, com.aajli.cellwise.feature.puzzleimport.PuzzleCode.extract("hello 12345"))
    }
}
