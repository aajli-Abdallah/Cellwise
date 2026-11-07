/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise.feature.stats

import com.aajli.cellwise.engine.Difficulty
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/** One finished or running game, reduced to what statistics need. Built from the `games` table. */
data class GameRecord(
    val id: String,
    val difficulty: Difficulty,
    val status: String, // IN_PROGRESS | WON | ABANDONED
    val elapsedSec: Long,
    val mistakes: Int,
    val endedAt: Long,  // finishedAt, or last update for abandoned games
    val hints: Int = 0,
    val daily: Boolean = false
)

data class LevelStats(
    val played: Int,        // won + abandoned (games in progress are not counted)
    val won: Int,
    val abandoned: Int,
    val bestSec: Long?,
    val avgSec: Long?,
    val avgMistakes: Double?,
    val flawless: Int       // wins without any mistake
) {
    val winRatePercent: Int? get() = if (played == 0) null else (won * 100.0 / played).roundToInt()
}

data class DayCount(val date: LocalDate, val wins: Int)

data class StatsSummary(
    val overall: LevelStats,
    val perLevel: List<Pair<Difficulty, LevelStats>>,
    val currentStreak: Int,
    val bestStreak: Int,
    val winsPerDay: List<DayCount>,
    val history: List<GameRecord>
) {
    val hasData: Boolean get() = overall.played > 0

    companion object {
        val EMPTY = StatsSummary(
            Stats.levelStats(emptyList()),
            Difficulty.values().map { it to Stats.levelStats(emptyList()) },
            0, 0, emptyList(), emptyList()
        )
    }
}

/** Pure statistics. Always recomputed from history records; nothing is stored as an aggregate. */
object Stats {

    fun levelStats(records: List<GameRecord>): LevelStats {
        val won = records.filter { it.status == "WON" }
        val abandoned = records.count { it.status == "ABANDONED" }
        return LevelStats(
            played = won.size + abandoned,
            won = won.size,
            abandoned = abandoned,
            bestSec = won.minOfOrNull { it.elapsedSec },
            avgSec = if (won.isEmpty()) null else won.sumOf { it.elapsedSec } / won.size,
            avgMistakes = if (won.isEmpty()) null else won.sumOf { it.mistakes }.toDouble() / won.size,
            flawless = won.count { it.mistakes == 0 }
        )
    }

    fun compute(
        records: List<GameRecord>, nowMillis: Long, zone: ZoneId,
        days: Int = 14, historyLimit: Int = 50
    ): StatsSummary {
        val finished = records.filter { it.status != "IN_PROGRESS" }.sortedBy { it.endedAt }

        var run = 0; var best = 0
        for (r in finished) {
            if (r.status == "WON") { run++; if (run > best) best = run } else run = 0
        }

        fun dayOf(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
        val winsByDay = finished.filter { it.status == "WON" }.groupingBy { dayOf(it.endedAt) }.eachCount()
        val today = dayOf(nowMillis)
        val perDay = (days - 1 downTo 0).map { off -> today.minusDays(off.toLong()).let { DayCount(it, winsByDay[it] ?: 0) } }

        return StatsSummary(
            overall = levelStats(finished),
            perLevel = Difficulty.values().map { d -> d to levelStats(finished.filter { it.difficulty == d }) },
            currentStreak = run,
            bestStreak = best,
            winsPerDay = perDay,
            history = finished.asReversed().take(historyLimit)
        )
    }
}
