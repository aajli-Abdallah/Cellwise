/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise

import com.aajli.cellwise.engine.Difficulty
import com.aajli.cellwise.feature.stats.GameRecord
import com.aajli.cellwise.feature.stats.Stats
import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneOffset

class StatsTest {
    private val day = 86_400_000L
    private val now = 100 * day + 12 * 3_600_000L
    private fun r(id: String, d: Difficulty, st: String, t: Long, m: Int, end: Long) = GameRecord(id, d, st, t, m, end)

    private val rows = listOf(
        r("r4", Difficulty.MEDIUM, "WON", 600, 1, now),
        r("r1", Difficulty.EASY, "WON", 300, 0, now - 2 * day),
        r("r3", Difficulty.EASY, "ABANDONED", 50, 0, now - day + 3_600_000L),
        r("r2", Difficulty.EASY, "WON", 200, 2, now - day),
        r("r5", Difficulty.HARD, "IN_PROGRESS", 10, 0, now)
    )

    @Test fun levelsAndOverall() {
        val s = Stats.compute(rows, now, ZoneOffset.UTC)
        assertEquals(4, s.overall.played); assertEquals(3, s.overall.won); assertEquals(1, s.overall.abandoned)
        assertEquals(200L, s.overall.bestSec); assertEquals(366L, s.overall.avgSec)
        assertEquals(1, s.overall.flawless); assertEquals(75, s.overall.winRatePercent)
        val easy = s.perLevel.first { it.first == Difficulty.EASY }.second
        assertEquals(2, easy.won); assertEquals(1, easy.abandoned); assertEquals(250L, easy.avgSec)
        assertEquals(0, s.perLevel.first { it.first == Difficulty.HARD }.second.played)
    }

    @Test fun streaksDaysAndHistory() {
        val s = Stats.compute(rows, now, ZoneOffset.UTC)
        assertEquals(2, s.bestStreak); assertEquals(1, s.currentStreak)
        assertEquals(14, s.winsPerDay.size)
        assertEquals(1, s.winsPerDay.last().wins)             // today
        assertEquals(1, s.winsPerDay[12].wins)                // yesterday (abandoned not counted)
        assertEquals(1, s.winsPerDay[11].wins)
        assertEquals(0, s.winsPerDay[0].wins)
        assertEquals(listOf("r4", "r3", "r2", "r1"), s.history.map { it.id })
    }

    @Test fun emptyIsSafe() {
        val s = Stats.compute(emptyList(), now, ZoneOffset.UTC)
        assertTrue(!s.hasData); assertEquals(null, s.overall.bestSec); assertEquals(null, s.overall.winRatePercent)
    }
}
