/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise.core.settings

import com.aajli.cellwise.core.backup.BackupSection
import com.aajli.cellwise.core.backup.ImportMode
import com.aajli.cellwise.core.backup.SectionPlan

/**
 * Settings are applied only in Replace mode; Merge leaves this phone's settings untouched.
 * Keys missing from the file (older backups) keep this phone's current value.
 */
class SettingsBackupSection(private val store: SettingsStore) : BackupSection {
    override val key = "settings"

    override suspend fun export(): Map<String, Any?> {
        val s = store.state.value
        return mapOf(
            "schema" to 1,
            "values" to mapOf(
                "highlightMistakes" to s.highlightMistakes, "keepScreenOn" to s.keepScreenOn,
                "highlightSameNumber" to s.highlightSameNumber, "dynamicColors" to s.dynamicColors,
                "themeMode" to s.themeMode, "palette" to s.palette, "haptics" to s.haptics,
                "leftHanded" to s.leftHanded, "highContrast" to s.highContrast,
                "largeNumbers" to s.largeNumbers, "reduceMotion" to s.reduceMotion
            )
        )
    }

    private fun parse(body: Map<String, Any?>): Settings? {
        val v = body["values"] as? Map<*, *> ?: return null
        val c = store.state.value
        fun b(k: String, d: Boolean) = v[k] as? Boolean ?: d
        fun i(k: String, d: Int, max: Int) = ((v[k] as? Number)?.toInt() ?: d).coerceIn(0, max)
        return Settings(
            highlightMistakes = b("highlightMistakes", c.highlightMistakes),
            keepScreenOn = b("keepScreenOn", c.keepScreenOn),
            highlightSameNumber = b("highlightSameNumber", c.highlightSameNumber),
            dynamicColors = b("dynamicColors", c.dynamicColors),
            themeMode = i("themeMode", c.themeMode, 2),
            palette = i("palette", c.palette, PALETTE_COUNT - 1),
            haptics = b("haptics", c.haptics),
            leftHanded = b("leftHanded", c.leftHanded),
            highContrast = b("highContrast", c.highContrast),
            largeNumbers = b("largeNumbers", c.largeNumbers),
            reduceMotion = b("reduceMotion", c.reduceMotion)
        )
    }

    override suspend fun plan(body: Map<String, Any?>, mode: ImportMode): SectionPlan {
        if (mode == ImportMode.MERGE) return SectionPlan(key, 0, 0, 0, replaceOnly = true)
        val p = parse(body) ?: return SectionPlan(key, 0, 0, 0, invalid = 1)
        return if (p == store.state.value) SectionPlan(key, 0, 0, 1) else SectionPlan(key, 0, 1, 0)
    }

    override suspend fun apply(body: Map<String, Any?>, mode: ImportMode) {
        if (mode != ImportMode.REPLACE) return
        parse(body)?.let { p -> store.update { p } }
    }
}
