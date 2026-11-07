/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise.feature.daily

import com.aajli.cellwise.engine.Difficulty
import com.aajli.cellwise.engine.Puzzle
import com.aajli.cellwise.engine.SudokuGenerator
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.random.Random

/**
 * Own pseudo-random generator (SplitMix64) so a date gives the same bits on every phone and Kotlin version.
 * Only the bit source is ours; the generator algorithm itself is part of the determinism (see docs/DAILY.md).
 */
class SeededRandom(seed: Long) : Random() {
    private var state = seed

    override fun nextBits(bitCount: Int): Int {
        if (bitCount == 0) return 0
        state += -7046029254386353131L                       // 0x9E3779B97F4A7C15
        var z = state
        z = (z xor (z ushr 30)) * -4658895280553007687L      // 0xBF58476D1CE4E5B9
        z = (z xor (z ushr 27)) * -7723592293110705685L      // 0x94D049BB133111EB
        z = z xor (z ushr 31)
        return (z ushr (64 - bitCount)).toInt()
    }
}

/** What the Home card and the statistics need about the daily puzzle. [today] and the week are epoch days. */
data class DailyInfo(
    val today: Int,
    val level: Difficulty,
    val streak: Int,          // consecutive days solved, still alive if today is not solved yet but yesterday was
    val best: Int,
    val doneToday: Boolean,
    val week: List<Boolean>,  // last 7 days, oldest first, last = today
    val totalWon: Int
) {
    companion object {
        val EMPTY = DailyInfo(0, Difficulty.EASY, 0, 0, false, List(7) { false }, 0)
    }
}

/**
 * Daily puzzle: the date (epoch day of the phone's local date) decides everything, offline and identical on every phone.
 * Level by weekday: Mon-Tue Easy, Wed-Fri Medium, Sat-Sun Hard.
 * Changing the generator, the solver iteration order or this seeding changes the puzzle of every date (accepted).
 */
object Daily {
    fun today(): Int = LocalDate.now().toEpochDay().toInt()

    fun levelFor(epochDay: Int): Difficulty = when (LocalDate.ofEpochDay(epochDay.toLong()).dayOfWeek) {
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY -> Difficulty.EASY
        DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY -> Difficulty.MEDIUM
        else -> Difficulty.HARD
    }

    fun generate(epochDay: Int): Puzzle =
        SudokuGenerator.generate(levelFor(epochDay), SeededRandom((epochDay.toLong() + 0x5EED) * -7046029254386353131L))

    /** [wonDays] = epoch days on which the daily puzzle was solved. */
    fun compute(wonDays: Collection<Int>, today: Int): DailyInfo {
        val won = wonDays.toSet()
        var best = 0; var run = 0; var prev = Int.MIN_VALUE
        for (d in won.sorted()) {
            run = if (prev != Int.MIN_VALUE && d == prev + 1) run + 1 else 1
            if (run > best) best = run
            prev = d
        }
        val doneToday = today in won
        var cur = 0
        var d = if (doneToday) today else today - 1
        while (d in won) { cur++; d-- }
        return DailyInfo(today, levelFor(today), cur, best, doneToday, (6 downTo 0).map { (today - it) in won }, won.size)
    }
}
