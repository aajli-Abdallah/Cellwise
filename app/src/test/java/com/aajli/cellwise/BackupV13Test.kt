package com.aajli.cellwise

import com.aajli.cellwise.core.backup.*
import com.aajli.cellwise.feature.game.backup.GameBackupMapper
import com.aajli.cellwise.feature.game.data.GameEntity
import org.junit.Assert.*
import org.junit.Test

class BackupV13Test {
    private val sol = "534678912672195348198342567859761423426853791713924856961537284287419635345286179"
    private val puz = "530070000600195000098000060800060003400803001700020006060000280000419005000080079"
    private val notes = List(81) { "0" }.joinToString(",")

    @Test fun newFieldsRoundTrip() {
        val e = GameEntity("a", "HARD", puz, sol, sol, notes, 1, 5, "WON", 1, 2, 2, hints = 3, imported = 1, rating = 6)
        assertEquals(e, GameBackupMapper.fromMap(GameBackupMapper.toMap(e)))
    }

    @Test fun oldBackupIsUpgraded() {
        val oldItem = GameBackupMapper.toMap(GameEntity("a", "EASY", puz, sol, sol, notes, 0, 5, "WON", 1, 2, 2)) - "hints" - "imported" - "rating"
        val file = BackupCodec.build("1.2.0", 1L, mapOf("games" to mapOf("schema" to 1, "items" to listOf(oldItem))))
        val p = BackupCodec.parse(file, "1.3.0")
        assertEquals(1, p.upgradedSteps); assertEquals("1.2.0", p.fileVersion)
        @Suppress("UNCHECKED_CAST")
        val item = (p.sections["games"]!!["items"] as List<Map<String, Any?>>)[0]
        assertEquals(0L, item["hints"]); assertEquals(0L, item["rating"])
        assertEquals("a", GameBackupMapper.fromMap(item)!!.id)
        // a 1.3.0 file needs no upgrade; a file without the fields is still readable by the mapper
        assertEquals(0, BackupCodec.parse(BackupCodec.build("1.3.0", 1L, mapOf("games" to mapOf("items" to emptyList<Any?>()))), "1.3.0").upgradedSteps)
        assertEquals("a", GameBackupMapper.fromMap(oldItem)!!.id)
    }
}
