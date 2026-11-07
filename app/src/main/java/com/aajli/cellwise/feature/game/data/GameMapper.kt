/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise.feature.game.data

import com.aajli.cellwise.engine.Difficulty
import com.aajli.cellwise.feature.game.GameState

fun GameState.toEntity(elapsedSec: Long, status: String, now: Long, finishedAt: Long?) = GameEntity(
    id = id,
    difficulty = difficulty.name,
    puzzle = givens.joinToString(""),
    solution = solution.joinToString(""),
    currentValues = values.joinToString(""),
    notes = notes.joinToString(","),
    mistakes = mistakes,
    elapsedSec = elapsedSec,
    status = status,
    createdAt = createdAt,
    updatedAt = now,
    finishedAt = finishedAt,
    hints = hints,
    imported = if (imported) 1 else 0,
    rating = rating,
    dailyDay = dailyDay
)

/** Returns null when the stored row is corrupt (wrong sizes or unknown values). */
fun GameEntity.toState(): GameState? = runCatching {
    fun digits(s: String) = s.map { it - '0' }.also { require(it.size == 81 && it.all { d -> d in 0..9 }) }
    GameState(
        id = id,
        difficulty = Difficulty.valueOf(difficulty),
        givens = digits(puzzle),
        solution = digits(solution),
        values = digits(currentValues),
        notes = notes.split(",").map { it.toInt() }.also { require(it.size == 81) },
        mistakes = mistakes,
        createdAt = createdAt,
        hints = hints,
        imported = imported == 1,
        rating = rating,
        dailyDay = dailyDay
    )
}.getOrNull()
