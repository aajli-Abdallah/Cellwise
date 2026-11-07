/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise.core.backup

import com.aajli.cellwise.core.CoreInfo

enum class BackupError { NOT_JSON, WRONG_FORMAT, NEWER_VERSION, EMPTY, TOO_LARGE, READ_FAILED, WRITE_FAILED }

class BackupException(val error: BackupError, val detail: String = "") : Exception("$error $detail")

/** Dotted versions compared number by number: 1.4.0 < 1.4.01 < 1.10.0. Missing parts count as 0. */
object Versions {
    private fun parts(v: String) = v.split('.').map { it.trim().toIntOrNull() ?: 0 }
    fun compare(a: String, b: String): Int {
        val x = parts(a); val y = parts(b)
        for (i in 0 until maxOf(x.size, y.size)) {
            val d = x.getOrElse(i) { 0 }.compareTo(y.getOrElse(i) { 0 })
            if (d != 0) return d
        }
        return 0
    }
}

/** Upgrade step applied to the whole file map when the file is older than [toVersion]. */
class BackupUpgrade(val toVersion: String, val apply: (MutableMap<String, Any?>) -> Unit)

/** Ordered upgrades hook. One entry per format change, never remove old ones so old backups stay importable. */
object BackupUpgrades {
    /** Adds missing fields (value 0) to every item of the games section. */
    @Suppress("UNCHECKED_CAST")
    private fun addGameFields(root: MutableMap<String, Any?>, vararg fields: String) {
        val sections = (root["sections"] as? Map<String, Any?>)?.toMutableMap() ?: return
        val games = (sections["games"] as? Map<String, Any?>)?.toMutableMap() ?: return
        games["items"] = (games["items"] as? List<*>)?.map { item ->
            (item as? Map<String, Any?>)?.toMutableMap()?.apply { for (f in fields) putIfAbsent(f, 0L) } ?: item
        }
        sections["games"] = games
        root["sections"] = sections
    }

    val ALL: List<BackupUpgrade> = listOf(
        BackupUpgrade("1.3.0") { addGameFields(it, "hints", "imported", "rating") },   // schema v2
        BackupUpgrade("1.4.0") { addGameFields(it, "dailyDay") }                       // schema v3
    )
}

class ParsedBackup(
    val fileVersion: String,      // version found in the file (or the default)
    val versionAssumed: Boolean,  // true when the file had no appVersion
    val createdAt: Long,
    val upgradedSteps: Int,
    val sections: Map<String, Map<String, Any?>>
)

object BackupCodec {
    const val FORMAT = "cellwise-backup"
    /** Used when a file has no appVersion: 1.2.0 is the first version that wrote backups. */
    const val DEFAULT_VERSION = "1.2.0"

    fun build(appVersion: String, createdAt: Long, sections: Map<String, Map<String, Any?>>): String =
        MiniJson.write(
            linkedMapOf(
                "format" to FORMAT, "appVersion" to appVersion, "createdAt" to createdAt,
                "copyright" to CoreInfo.COPYRIGHT, "sections" to sections
            )
        )

    @Suppress("UNCHECKED_CAST")
    fun parse(text: String, currentVersion: String, upgrades: List<BackupUpgrade> = BackupUpgrades.ALL): ParsedBackup {
        val root = try { MiniJson.parse(text.removePrefix("\uFEFF")) } catch (e: MiniJson.JsonException) {
            throw BackupException(BackupError.NOT_JSON, e.message ?: "")
        }
        val map = (root as? Map<String, Any?>)?.toMutableMap() ?: throw BackupException(BackupError.NOT_JSON)
        if (map["format"] != FORMAT) throw BackupException(BackupError.WRONG_FORMAT)

        val declared = (map["appVersion"] as? String)?.takeIf { it.isNotBlank() }
        val fileVersion = declared ?: DEFAULT_VERSION
        if (Versions.compare(fileVersion, currentVersion) > 0) throw BackupException(BackupError.NEWER_VERSION, fileVersion)

        var version = fileVersion
        var steps = 0
        for (u in upgrades.sortedWith { a, b -> Versions.compare(a.toVersion, b.toVersion) }) {
            if (Versions.compare(u.toVersion, version) > 0 && Versions.compare(u.toVersion, currentVersion) <= 0) {
                u.apply(map); version = u.toVersion; steps++
            }
        }

        val sections = LinkedHashMap<String, Map<String, Any?>>()
        (map["sections"] as? Map<*, *>)?.forEach { (k, v) -> if (k is String && v is Map<*, *>) sections[k] = v as Map<String, Any?> }
        if (sections.isEmpty()) throw BackupException(BackupError.EMPTY)
        return ParsedBackup(fileVersion, declared == null, (map["createdAt"] as? Number)?.toLong() ?: 0L, steps, sections)
    }
}

/** Share reminder: due when something is worth saving and no external copy was shared for a week. */
object BackupReminder {
    const val INTERVAL_DAYS = 7
    fun isDue(lastSharedAt: Long, snoozedUntil: Long, finishedGames: Int, now: Long): Boolean =
        finishedGames > 0 && now >= snoozedUntil &&
            (lastSharedAt == 0L || now - lastSharedAt > INTERVAL_DAYS * 86_400_000L)
}
