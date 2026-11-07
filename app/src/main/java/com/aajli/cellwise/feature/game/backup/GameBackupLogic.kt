/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise.feature.game.backup

import com.aajli.cellwise.core.backup.ImportMode
import com.aajli.cellwise.engine.Difficulty
import com.aajli.cellwise.feature.game.data.GameEntity

/** Maps `games` rows to/from backup JSON maps, with validation. Pure Kotlin. */
object GameBackupMapper {
    private val STATUSES = setOf("IN_PROGRESS", "WON", "ABANDONED")
    private val DIGITS81 = Regex("[0-9]{81}")

    fun toMap(e: GameEntity): Map<String, Any?> = linkedMapOf(
        "id" to e.id, "difficulty" to e.difficulty, "puzzle" to e.puzzle, "solution" to e.solution,
        "currentValues" to e.currentValues, "notes" to e.notes, "mistakes" to e.mistakes,
        "elapsedSec" to e.elapsedSec, "status" to e.status, "createdAt" to e.createdAt,
        "updatedAt" to e.updatedAt, "finishedAt" to e.finishedAt,
        "hints" to e.hints, "imported" to e.imported, "rating" to e.rating, "dailyDay" to e.dailyDay
    )

    /** Returns null when the item is not a valid game. */
    fun fromMap(m: Map<String, Any?>): GameEntity? {
        val id = (m["id"] as? String)?.takeIf { it.isNotBlank() } ?: return null
        val difficulty = (m["difficulty"] as? String)?.takeIf { d -> Difficulty.values().any { it.name == d } } ?: return null
        val puzzle = (m["puzzle"] as? String)?.takeIf { DIGITS81.matches(it) } ?: return null
        val solution = (m["solution"] as? String)?.takeIf { DIGITS81.matches(it) && '0' !in it } ?: return null
        val values = (m["currentValues"] as? String)?.takeIf { DIGITS81.matches(it) } ?: return null
        val notes = (m["notes"] as? String) ?: return null
        val noteParts = notes.split(",")
        if (noteParts.size != 81 || noteParts.any { it.toIntOrNull() == null }) return null
        for (i in 0 until 81) if (puzzle[i] != '0' && (values[i] != puzzle[i] || solution[i] != puzzle[i])) return null
        val mistakes = (m["mistakes"] as? Number)?.toInt()?.takeIf { it >= 0 } ?: return null
        val elapsed = (m["elapsedSec"] as? Number)?.toLong()?.takeIf { it >= 0 } ?: return null
        val status = (m["status"] as? String)?.takeIf { it in STATUSES } ?: return null
        val created = (m["createdAt"] as? Number)?.toLong() ?: return null
        val updated = (m["updatedAt"] as? Number)?.toLong() ?: return null
        val finished = (m["finishedAt"] as? Number)?.toLong()
        // Fields added in 1.3.0: missing in older files (also filled by the 1.3.0 upgrade step), so default to 0.
        val hints = (m["hints"] as? Number)?.toInt()?.takeIf { it >= 0 } ?: 0
        val imported = if ((m["imported"] as? Number)?.toInt() == 1) 1 else 0
        val rating = (m["rating"] as? Number)?.toInt()?.takeIf { it >= 0 } ?: 0
        val dailyDay = (m["dailyDay"] as? Number)?.toInt()?.takeIf { it >= 0 } ?: 0   // added in 1.4.0
        return GameEntity(id, difficulty, puzzle, solution, values, notes, mistakes, elapsed, status, created, updated, finished, hints, imported, rating, dailyDay)
    }
}

class GameMergePlan(
    val toWrite: List<GameEntity>,
    val added: Int, val updated: Int, val unchanged: Int, val removed: Int
)

/**
 * Merge rules for the `games` table:
 *  - MERGE: match by id; unknown ids are added; same id -> the row with the newest updatedAt wins (equal rows are unchanged).
 *  - REPLACE: the file becomes the table; local ids missing from the file are removed.
 *  - After either mode only the newest IN_PROGRESS game stays in progress; older ones become ABANDONED.
 */
object GameMerge {
    fun plan(local: List<GameEntity>, incoming: List<GameEntity>, mode: ImportMode): GameMergePlan {
        val localById = local.associateBy { it.id }
        val file = incoming.groupBy { it.id }.mapValues { (_, v) -> v.maxByOrNull { it.updatedAt }!! }

        var added = 0; var updated = 0; var unchanged = 0
        val writes = LinkedHashMap<String, GameEntity>()
        for ((id, g) in file) {
            val l = localById[id]
            when {
                l == null -> { added++; writes[id] = g }
                l == g -> unchanged++
                mode == ImportMode.REPLACE || g.updatedAt > l.updatedAt -> { updated++; writes[id] = g }
                else -> unchanged++
            }
        }
        val removed = if (mode == ImportMode.REPLACE) localById.keys.count { it !in file } else 0

        val finalRows = if (mode == ImportMode.REPLACE) file.keys.associateWith { writes[it] ?: localById[it]!! }
        else localById + writes
        val active = finalRows.values.filter { it.status == "IN_PROGRESS" }.sortedByDescending { it.updatedAt }
        for (extra in active.drop(1)) writes[extra.id] = extra.copy(status = "ABANDONED")

        return GameMergePlan(writes.values.toList(), added, updated, unchanged, removed)
    }
}
