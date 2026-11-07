/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise.core.backup

enum class ImportMode { MERGE, REPLACE }

/**
 * What an import would do to one section. [replaceOnly] = the section is ignored in Merge mode.
 * [invalid] = items in the file that were skipped because they failed validation.
 */
data class SectionPlan(
    val key: String,
    val added: Int,
    val updated: Int,
    val unchanged: Int,
    val removed: Int = 0,
    val invalid: Int = 0,
    val replaceOnly: Boolean = false
)

/**
 * A feature registers one section. The backup code never changes when a feature is added:
 * it only loops over the registered sections. Each section owns its merge rule.
 */
interface BackupSection {
    val key: String
    suspend fun export(): Map<String, Any?>
    suspend fun plan(body: Map<String, Any?>, mode: ImportMode): SectionPlan
    suspend fun apply(body: Map<String, Any?>, mode: ImportMode)
}
