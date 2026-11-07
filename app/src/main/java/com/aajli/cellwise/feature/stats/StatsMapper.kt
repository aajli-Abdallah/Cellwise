/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise.feature.stats

import com.aajli.cellwise.engine.Difficulty
import com.aajli.cellwise.feature.game.data.GameEntity

/** Stats only reads the `games` table; it never writes to it. Returns null for unreadable rows. */
fun GameEntity.toRecord(): GameRecord? {
    val d = runCatching { Difficulty.valueOf(difficulty) }.getOrNull() ?: return null
    return GameRecord(id, d, status, elapsedSec, mistakes, finishedAt ?: updatedAt, hints, dailyDay != 0)
}
