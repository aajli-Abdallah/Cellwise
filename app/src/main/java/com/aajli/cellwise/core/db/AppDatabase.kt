/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise.core.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.aajli.cellwise.feature.game.data.GameDao
import com.aajli.cellwise.feature.game.data.GameEntity

/** Register every feature entity here. Schema changes need a Migration in Migrations.kt (never destructive). */
@Database(entities = [GameEntity::class], version = 3, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "cellwise.db")
                .addMigrations(*Migrations.ALL)
                .build()
                .also { instance = it }
        }
    }
}
