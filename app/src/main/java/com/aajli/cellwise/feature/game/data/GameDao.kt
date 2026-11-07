/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise.feature.game.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games WHERE status = 'IN_PROGRESS' ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getActive(): GameEntity?

    @Upsert
    suspend fun upsert(game: GameEntity)

    @Query("SELECT * FROM games")
    fun observeAll(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games")
    suspend fun getAll(): List<GameEntity>

    @Upsert
    suspend fun upsertAll(games: List<GameEntity>)

    @Query("DELETE FROM games")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM games")
    suspend fun count(): Int

    @Query("SELECT dailyDay FROM games WHERE dailyDay != 0 AND status = 'WON'")
    fun observeDailyWins(): Flow<List<Int>>

    @Query("SELECT COUNT(*) FROM games WHERE status != 'IN_PROGRESS'")
    suspend fun countFinished(): Int
}
