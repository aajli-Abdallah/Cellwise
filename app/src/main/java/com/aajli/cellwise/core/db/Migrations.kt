/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise.core.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Ordered list of Room migrations. Never use destructive fallback. */
object Migrations {
    /** 1.3.0: hints used, imported-puzzle flag, technique rating of each game. */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE games ADD COLUMN hints INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE games ADD COLUMN imported INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE games ADD COLUMN rating INTEGER NOT NULL DEFAULT 0")
        }
    }

    /** 1.4.0: epoch day of the daily puzzle a game belongs to (0 = not a daily). */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE games ADD COLUMN dailyDay INTEGER NOT NULL DEFAULT 0")
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
}
