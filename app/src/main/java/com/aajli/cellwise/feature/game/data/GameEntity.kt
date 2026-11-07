/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise.feature.game.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One game, in progress or finished. Doubles as the history snapshot used by statistics (v1.1.0):
 * it keeps its own puzzle, solution, time and mistakes, never derived from current data.
 * status: IN_PROGRESS | WON | ABANDONED. Schema v2 added hints, imported, rating.
 */
@Entity(tableName = "games", indices = [Index("status")])
data class GameEntity(
    @PrimaryKey val id: String,
    val difficulty: String,
    val puzzle: String,
    val solution: String,
    val currentValues: String,
    val notes: String,
    val mistakes: Int,
    val elapsedSec: Long,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
    val finishedAt: Long?,
    @ColumnInfo(defaultValue = "0") val hints: Int = 0,
    @ColumnInfo(defaultValue = "0") val imported: Int = 0,  // 1 = typed in from a printed grid
    @ColumnInfo(defaultValue = "0") val rating: Int = 0,    // rank of the hardest technique needed, 0 = unknown
    @ColumnInfo(defaultValue = "0") val dailyDay: Int = 0   // epoch day of the daily puzzle, 0 = not a daily (schema v3)
)
