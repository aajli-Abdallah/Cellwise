/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise.feature.game.backup

import androidx.room.withTransaction
import com.aajli.cellwise.core.backup.BackupSection
import com.aajli.cellwise.core.backup.ImportMode
import com.aajli.cellwise.core.backup.SectionPlan
import com.aajli.cellwise.core.db.AppDatabase
import com.aajli.cellwise.feature.game.data.GameEntity

/** Backup section for the `games` table (history, statistics source, game in progress). Rules: see GameMerge. */
class GamesBackupSection(private val db: AppDatabase) : BackupSection {
    override val key = "games"
    private val dao get() = db.gameDao()

    override suspend fun export(): Map<String, Any?> = mapOf(
        "schema" to 1,
        "items" to dao.getAll().sortedBy { it.createdAt }.map { GameBackupMapper.toMap(it) }
    )

    @Suppress("UNCHECKED_CAST")
    private fun items(body: Map<String, Any?>): Pair<List<GameEntity>, Int> {
        val ok = ArrayList<GameEntity>()
        var invalid = 0
        for (x in (body["items"] as? List<*>) ?: emptyList<Any?>()) {
            val e = (x as? Map<String, Any?>)?.let { GameBackupMapper.fromMap(it) }
            if (e == null) invalid++ else ok += e
        }
        return ok to invalid
    }

    override suspend fun plan(body: Map<String, Any?>, mode: ImportMode): SectionPlan {
        val (list, invalid) = items(body)
        val p = GameMerge.plan(dao.getAll(), list, mode)
        return SectionPlan(key, p.added, p.updated, p.unchanged, p.removed, invalid)
    }

    override suspend fun apply(body: Map<String, Any?>, mode: ImportMode) {
        val (list, _) = items(body)
        val p = GameMerge.plan(dao.getAll(), list, mode)
        db.withTransaction {
            if (mode == ImportMode.REPLACE) {
                // The table is cleared, so every incoming row must be written, not only the changed ones.
                val demoted = p.toWrite.associateBy { it.id }
                dao.deleteAll()
                dao.upsertAll(list.groupBy { it.id }.map { (id, v) -> demoted[id] ?: v.maxByOrNull { it.updatedAt }!! })
            } else {
                dao.upsertAll(p.toWrite)
            }
        }
    }
}
