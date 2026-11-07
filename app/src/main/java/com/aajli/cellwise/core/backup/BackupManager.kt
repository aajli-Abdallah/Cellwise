/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise.core.backup

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.aajli.cellwise.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.time.LocalDate

class LocalBackup(val name: String, val modifiedAt: Long, val sizeBytes: Long)

class ImportPreview(
    val sourceLabel: String,
    val parsed: ParsedBackup,
    val unknownSections: List<String>,
    val merge: List<SectionPlan>,
    val replace: List<SectionPlan>
)

/**
 * Creates, stores, shares, reads and previews backups. Knows nothing about features: it loops over [sections].
 * Local automatic backups: filesDir/backups/auto-YYYY-MM-DD.json (newest 7 kept).
 * Files to share: cacheDir/exports/ (exposed through FileProvider, see res/xml/file_paths.xml).
 */
class BackupManager(
    private val context: Context,
    private val sections: List<BackupSection>,
    private val hasData: suspend () -> Boolean
) {
    private val prefs = context.getSharedPreferences("cellwise_backup", Context.MODE_PRIVATE)
    private val autoDir get() = File(context.filesDir, "backups")
    private val exportDir get() = File(context.cacheDir, "exports")

    var lastSharedAt: Long
        get() = prefs.getLong("last_shared_at", 0L)
        set(v) { prefs.edit().putLong("last_shared_at", v).apply() }

    var snoozedUntil: Long
        get() = prefs.getLong("snoozed_until", 0L)
        set(v) { prefs.edit().putLong("snoozed_until", v).apply() }

    suspend fun createJson(now: Long = System.currentTimeMillis()): String = withContext(Dispatchers.IO) {
        val bodies = LinkedHashMap<String, Map<String, Any?>>()
        for (s in sections) bodies[s.key] = s.export()
        BackupCodec.build(BuildConfig.VERSION_NAME, now, bodies)
    }

    /** Writes a backup to a file the user picked with the system "save as" dialog (e.g. in Downloads). */
    suspend fun writeTo(uri: Uri) {
        val json = createJson()
        withContext(Dispatchers.IO) {
            try {
                val out = context.contentResolver.openOutputStream(uri, "wt") ?: throw BackupException(BackupError.WRITE_FAILED)
                out.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            } catch (e: BackupException) {
                throw e
            } catch (e: Exception) {
                throw BackupException(BackupError.WRITE_FAILED, e.message ?: "")
            }
        }
    }

    suspend fun writeShareFile(): Uri = withContext(Dispatchers.IO) {
        val json = createJson()
        exportDir.mkdirs()
        exportDir.listFiles()?.forEach { it.delete() }
        val f = File(exportDir, "Cellwise-backup-v${BuildConfig.VERSION_NAME}-${LocalDate.now()}.json")
        f.writeText(json)
        FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", f)
    }

    /** One automatic backup per day, only when there is data. Written to a temp file, then renamed. */
    suspend fun autoBackupIfDue() {
        withContext(Dispatchers.IO) {
            autoDir.mkdirs()
            val f = File(autoDir, "auto-${LocalDate.now()}.json")
            if (f.exists() || !hasData()) return@withContext
            val tmp = File(autoDir, f.name + ".tmp")
            tmp.writeText(createJson())
            if (!tmp.renameTo(f)) tmp.delete()
            autoFiles().drop(KEEP_AUTO).forEach { it.delete() }
        }
    }

    private fun autoFiles(): List<File> =
        autoDir.listFiles { x -> x.name.startsWith("auto-") && x.name.endsWith(".json") }
            ?.sortedByDescending { it.name } ?: emptyList()

    fun listLocal(): List<LocalBackup> = autoFiles().map { LocalBackup(it.name, it.lastModified(), it.length()) }

    suspend fun readText(uri: Uri): String = withContext(Dispatchers.IO) {
        try {
            val input = context.contentResolver.openInputStream(uri) ?: throw BackupException(BackupError.READ_FAILED)
            input.use { readCapped(it) }
        } catch (e: BackupException) {
            throw e
        } catch (e: Exception) {
            throw BackupException(BackupError.READ_FAILED, e.message ?: "")
        }
    }

    suspend fun readLocal(name: String): String = withContext(Dispatchers.IO) {
        if (name.contains('/') || name.contains("..")) throw BackupException(BackupError.READ_FAILED)
        try {
            File(autoDir, name).inputStream().use { readCapped(it) }
        } catch (e: BackupException) {
            throw e
        } catch (e: Exception) {
            throw BackupException(BackupError.READ_FAILED, e.message ?: "")
        }
    }

    private fun readCapped(input: InputStream): String {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8192)
        var total = 0
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            total += n
            if (total > MAX_BYTES) throw BackupException(BackupError.TOO_LARGE)
            out.write(buf, 0, n)
        }
        return out.toString("UTF-8")
    }

    suspend fun preview(text: String, label: String): ImportPreview = withContext(Dispatchers.IO) {
        val parsed = BackupCodec.parse(text, BuildConfig.VERSION_NAME)
        val known = sections.map { it.key }.toSet()
        val merge = ArrayList<SectionPlan>()
        val replace = ArrayList<SectionPlan>()
        for (s in sections) {
            val body = parsed.sections[s.key] ?: continue
            merge += s.plan(body, ImportMode.MERGE)
            replace += s.plan(body, ImportMode.REPLACE)
        }
        if (merge.isEmpty()) throw BackupException(BackupError.EMPTY)
        ImportPreview(label, parsed, parsed.sections.keys.filter { it !in known }, merge, replace)
    }

    suspend fun apply(preview: ImportPreview, mode: ImportMode) {
        for (s in sections) {
            val body = preview.parsed.sections[s.key] ?: continue
            s.apply(body, mode)
        }
    }

    private companion object {
        const val KEEP_AUTO = 7
        const val MAX_BYTES = 20 * 1024 * 1024
    }
}
