package com.aajli.cellwise

import com.aajli.cellwise.core.backup.BackupCodec
import com.aajli.cellwise.engine.Difficulty
import com.aajli.cellwise.engine.SudokuSolver
import com.aajli.cellwise.feature.daily.Daily
import com.aajli.cellwise.feature.daily.SeededRandom
import com.aajli.cellwise.feature.game.backup.GameBackupMapper
import com.aajli.cellwise.feature.game.data.GameEntity
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class DailyTest {
    @Test fun sameDateSamePuzzle() {
        val a = Daily.generate(20400); val b = Daily.generate(20400)
        assertArrayEquals(a.givens, b.givens); assertArrayEquals(a.solution, b.solution)
        assertEquals(1, SudokuSolver.countSolutions(a.givens))
        assertTrue(!a.givens.contentEquals(Daily.generate(20401).givens))
        assertEquals(Daily.levelFor(20400), a.rating.level)
    }

    /**
     * Golden value: if this fails, the generator, the solver order or the seeding changed and EVERY past and future
     * daily puzzle changes. Accepted by the owner, but it must be a conscious decision (update the constant and docs/DAILY.md).
     */
    @Test fun goldenPuzzleForDay20400() {
        assertEquals(
            "000000706900740001040100000700058020008000600060270005000002060100093002302000000",
            Daily.generate(20400).givens.joinToString("")
        )
        // 20401 is a Hard day: its value changed in 1.6.0 (Hard now excludes puzzles that need guessing).
        assertEquals(
            "000060210009050080000000009700004001000003006502100007100700000000040600058090000",
            Daily.generate(20401).givens.joinToString("")
        )
        assertEquals(   // Easy day: unchanged by the 1.6.0 generator work
            "005907638004183057038000000050400003090238060100006090000000980840325700571809300",
            Daily.generate(20402).givens.joinToString("")
        )
    }

    @Test fun randomIsReproducible() {
        val a = SeededRandom(42); val b = SeededRandom(42)
        repeat(50) { assertEquals(a.nextInt(1000), b.nextInt(1000)) }
        assertEquals(0, SeededRandom(1).nextInt(1))
    }

    @Test fun levelByWeekday() {
        val monday = LocalDate.of(2026, 11, 2).toEpochDay().toInt()
        val expected = listOf(Difficulty.EASY, Difficulty.EASY, Difficulty.MEDIUM, Difficulty.MEDIUM, Difficulty.MEDIUM, Difficulty.HARD, Difficulty.HARD)
        for (i in 0..6) assertEquals(expected[i], Daily.levelFor(monday + i))
    }

    @Test fun streaks() {
        val t = 1000
        val i = Daily.compute(listOf(t, t - 1, t - 2, t - 5, t - 6, t - 7, t - 8), t)
        assertEquals(3, i.streak); assertEquals(4, i.best); assertTrue(i.doneToday); assertEquals(7, i.totalWon)
        assertEquals(listOf(true, true, false, false, true, true, true), i.week)   // t-6 .. t
        // today not solved yet: the streak is still alive through yesterday
        assertEquals(2, Daily.compute(listOf(t - 1, t - 2), t).streak)
        assertTrue(!Daily.compute(listOf(t - 1, t - 2), t).doneToday)
        // missed yesterday: streak is over, best is kept
        val m = Daily.compute(listOf(t - 2, t - 3, t - 4), t)
        assertEquals(0, m.streak); assertEquals(3, m.best)
        assertEquals(0, Daily.compute(emptyList(), t).best)
        assertEquals(1, Daily.compute(listOf(t, t, t), t).streak)  // duplicates count once
    }

    @Test fun backupUpgradeToDaily() {
        val sol = "534678912672195348198342567859761423426853791713924856961537284287419635345286179"
        val puz = "530070000600195000098000060800060003400803001700020006060000280000419005000080079"
        val notes = List(81) { "0" }.joinToString(",")
        val e = GameEntity("a", "EASY", puz, sol, sol, notes, 0, 5, "WON", 1, 2, 2, dailyDay = 20400)
        assertEquals(e, GameBackupMapper.fromMap(GameBackupMapper.toMap(e)))
        val old = GameBackupMapper.toMap(e) - "dailyDay" - "hints" - "imported" - "rating"
        val file = BackupCodec.build("1.2.0", 1L, mapOf("games" to mapOf("schema" to 1, "items" to listOf(old))))
        val p = BackupCodec.parse(file, "1.4.0")
        assertEquals(2, p.upgradedSteps)
        @Suppress("UNCHECKED_CAST")
        val item = (p.sections["games"]!!["items"] as List<Map<String, Any?>>)[0]
        assertEquals(0L, item["dailyDay"]); assertEquals(0L, item["hints"])
        assertEquals(1, BackupCodec.parse(BackupCodec.build("1.3.0", 1L, mapOf("games" to mapOf("items" to emptyList<Any?>()))), "1.4.0").upgradedSteps)
        assertEquals(0, GameBackupMapper.fromMap(old)!!.dailyDay)
    }
}
