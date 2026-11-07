/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise

import com.aajli.cellwise.core.backup.*
import com.aajli.cellwise.feature.game.backup.GameBackupMapper
import com.aajli.cellwise.feature.game.backup.GameMerge
import com.aajli.cellwise.feature.game.data.GameEntity
import org.junit.Assert.*
import org.junit.Test

class BackupTest {
    private fun fails(code: BackupError, f: () -> Unit) {
        try { f() } catch (e: BackupException) { assertEquals(code, e.error); return }
        throw AssertionError("expected $code")
    }

    @Test fun jsonRoundTripAndEscapes() {
        val v = linkedMapOf("a" to 1L, "b" to listOf(true, null, 2.5, "x\"y\\z\n\u0001é"), "c" to linkedMapOf<String, Any?>(), "d" to emptyList<Any?>())
        for (pretty in listOf(true, false)) assertEquals(v, MiniJson.parse(MiniJson.write(v, pretty)))
        assertEquals("é\u00e9", MiniJson.parse("\"\\u00e9\\u00E9\""))
        assertEquals(-12L, MiniJson.parse(" -12 "))
    }

    @Test fun jsonRejectsBadInput() {
        for (bad in listOf("", "{", "{\"a\":}", "[1,]", "{\"a\":1} x", "\"abc", "[1 2]", "nul", "{'a':1}", "\"\\x\"")) {
            try { MiniJson.parse(bad); throw AssertionError("accepted: $bad") } catch (e: MiniJson.JsonException) { }
        }
    }

    @Test fun versionsCompare() {
        assertTrue(Versions.compare("1.4.0", "1.4.01") < 0)
        assertTrue(Versions.compare("1.10.0", "1.9.9") > 0)
        assertEquals(0, Versions.compare("1.2", "1.2.0"))
    }

    private val body = mapOf("games" to mapOf<String, Any?>("schema" to 1L, "items" to emptyList<Any?>()))

    @Test fun codecRoundTripAndGuards() {
        val p = BackupCodec.parse(BackupCodec.build("1.2.0", 5L, body), "1.2.0")
        assertEquals("1.2.0", p.fileVersion); assertEquals(5L, p.createdAt); assertTrue(!p.versionAssumed); assertEquals(0, p.upgradedSteps)
        assertTrue(p.sections.containsKey("games"))
        fails(BackupError.NEWER_VERSION) { BackupCodec.parse(BackupCodec.build("1.3.0", 5L, body), "1.2.0") }
        fails(BackupError.WRONG_FORMAT) { BackupCodec.parse("{\"format\":\"x\"}", "1.2.0") }
        fails(BackupError.NOT_JSON) { BackupCodec.parse("hello", "1.2.0") }
        fails(BackupError.NOT_JSON) { BackupCodec.parse("[1]", "1.2.0") }
        fails(BackupError.EMPTY) { BackupCodec.parse("{\"format\":\"cellwise-backup\",\"sections\":{}}", "1.2.0") }
    }

    @Test fun missingVersionUsesDefault() {
        val p = BackupCodec.parse("{\"format\":\"cellwise-backup\",\"sections\":{\"games\":{}}}", "1.2.0")
        assertTrue(p.versionAssumed); assertEquals(BackupCodec.DEFAULT_VERSION, p.fileVersion)
    }

    @Test fun upgradesRunInOrderOnlyWhenNeeded() {
        val log = ArrayList<String>()
        val ups = listOf(
            BackupUpgrade("1.4.0") { log += "1.4.0" },
            BackupUpgrade("1.3.0") { log += "1.3.0" },
            BackupUpgrade("1.2.0") { log += "1.2.0" },     // file already 1.2.0: skipped
            BackupUpgrade("1.9.0") { log += "1.9.0" }      // newer than the app: skipped
        )
        val p = BackupCodec.parse(BackupCodec.build("1.2.0", 1L, body), "1.4.0", ups)
        assertEquals(listOf("1.3.0", "1.4.0"), log); assertEquals(2, p.upgradedSteps)
    }

    @Test fun reminder() {
        val d = 86_400_000L
        assertTrue(BackupReminder.isDue(0, 0, 1, 10 * d))
        assertTrue(!BackupReminder.isDue(0, 0, 0, 10 * d))
        assertTrue(!BackupReminder.isDue(5 * d, 0, 3, 10 * d))
        assertTrue(BackupReminder.isDue(2 * d, 0, 3, 10 * d))
        assertTrue(!BackupReminder.isDue(0, 20 * d, 3, 10 * d))
    }

    // ---- games ----
    private val sol = "534678912672195348198342567859761423426853791713924856961537284287419635345286179"
    private val puz = "530070000600195000098000060800060003400803001700020006060000280000419005000080079"
    private val notes = List(81) { "0" }.joinToString(",")
    private fun g(id: String, upd: Long, status: String = "WON", mistakes: Int = 0, values: String = sol) =
        GameEntity(id, "EASY", puz, sol, values, notes, mistakes, 100, status, 1, upd, if (status == "WON") upd else null)

    @Test fun mapperRoundTripAndValidation() {
        val e = g("a", 5)
        assertEquals(e, GameBackupMapper.fromMap(GameBackupMapper.toMap(e)))
        val viaJson = MiniJson.parse(MiniJson.write(GameBackupMapper.toMap(e))) as Map<String, Any?>
        assertEquals(e, GameBackupMapper.fromMap(viaJson))
        val m = GameBackupMapper.toMap(e)
        assertEquals(null, GameBackupMapper.fromMap(m + ("id" to "")))
        assertEquals(null, GameBackupMapper.fromMap(m + ("status" to "X")))
        assertEquals(null, GameBackupMapper.fromMap(m + ("puzzle" to "123")))
        assertEquals(null, GameBackupMapper.fromMap(m + ("currentValues" to "9" + sol.drop(1)))) // changes a given
        assertEquals(null, GameBackupMapper.fromMap(m - "mistakes"))
        assertEquals(null, GameBackupMapper.fromMap(m + ("notes" to "1,2")))
    }

    @Test fun mergeNewestWins() {
        val local = listOf(g("a", 10, mistakes = 1), g("b", 10), g("c", 10))
        val file = listOf(g("a", 20, mistakes = 5), g("b", 5, mistakes = 9), g("c", 10), g("d", 1))
        val p = GameMerge.plan(local, file, ImportMode.MERGE)
        assertEquals(1, p.added); assertEquals(1, p.updated); assertEquals(2, p.unchanged); assertEquals(0, p.removed)
        assertEquals(setOf("a", "d"), p.toWrite.map { it.id }.toSet())
        assertEquals(5, p.toWrite.first { it.id == "a" }.mistakes)
    }

    @Test fun replaceRemovesAndOverwrites() {
        val local = listOf(g("a", 10), g("x", 10))
        val file = listOf(g("a", 5, mistakes = 3), g("d", 1))
        val p = GameMerge.plan(local, file, ImportMode.REPLACE)
        assertEquals(1, p.added); assertEquals(1, p.updated); assertEquals(0, p.unchanged); assertEquals(1, p.removed)
        assertEquals(3, p.toWrite.first { it.id == "a" }.mistakes)
    }

    @Test fun onlyNewestGameStaysInProgress() {
        val p = GameMerge.plan(listOf(g("a", 10, "IN_PROGRESS")), listOf(g("b", 20, "IN_PROGRESS")), ImportMode.MERGE)
        val a = p.toWrite.first { it.id == "a" }
        assertEquals("ABANDONED", a.status)
        assertEquals("IN_PROGRESS", p.toWrite.first { it.id == "b" }.status)
        val r = GameMerge.plan(listOf(g("a", 10, "IN_PROGRESS")), listOf(g("b", 20, "IN_PROGRESS")), ImportMode.REPLACE)
        assertEquals(1, r.removed); assertEquals(null, r.toWrite.firstOrNull { it.status == "ABANDONED" })
    }
}
