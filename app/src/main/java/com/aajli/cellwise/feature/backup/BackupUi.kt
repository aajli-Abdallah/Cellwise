/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise.feature.backup

import com.aajli.cellwise.core.UiMsg
import com.aajli.cellwise.core.backup.ImportPreview
import com.aajli.cellwise.core.backup.LocalBackup

data class BackupUi(
    val busy: Boolean = false,
    val preview: ImportPreview? = null,
    val message: UiMsg? = null,
    val local: List<LocalBackup> = emptyList(),
    val lastSharedAt: Long = 0L
)
