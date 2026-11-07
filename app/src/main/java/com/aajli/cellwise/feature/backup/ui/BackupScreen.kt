/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise.feature.backup.ui

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aajli.cellwise.R
import com.aajli.cellwise.core.backup.ImportMode
import com.aajli.cellwise.core.backup.ImportPreview
import com.aajli.cellwise.core.backup.SectionPlan
import com.aajli.cellwise.feature.backup.BackupUi
import com.aajli.cellwise.ui.ScreenHeader

private const val DATE_FLAGS = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_ALL

@Composable
fun BackupScreen(
    ui: BackupUi, onBack: () -> Unit, onShare: () -> Unit, onSaveFile: () -> Unit, onPickFile: () -> Unit,
    onRestoreLocal: (String) -> Unit, onCancelPreview: () -> Unit, onImport: (ImportMode) -> Unit,
    onDismissMessage: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader(stringResource(R.string.backup_title), onBack)
        if (ui.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            ui.message?.let { m ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(cs.secondaryContainer).padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(m.res, *m.args.toTypedArray()), Modifier.weight(1f), color = cs.onSecondaryContainer)
                    TextButton(onClick = onDismissMessage) { Text(stringResource(R.string.backup_ok)) }
                }
                Spacer(Modifier.height(16.dp))
            }
            ui.preview?.let { p ->
                PreviewCard(p, ui.busy, onCancelPreview, onImport)
                Spacer(Modifier.height(16.dp))
            }

            Text(stringResource(R.string.backup_info), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onShare, enabled = !ui.busy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(stringResource(R.string.backup_share))
            }
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(onClick = onSaveFile, enabled = !ui.busy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(stringResource(R.string.backup_save))
            }
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(onClick = onPickFile, enabled = !ui.busy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(stringResource(R.string.backup_import))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (ui.lastSharedAt > 0) stringResource(R.string.backup_last_shared, DateUtils.formatDateTime(ctx, ui.lastSharedAt, DATE_FLAGS))
                else stringResource(R.string.backup_never_shared),
                style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.backup_auto_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            if (ui.local.isEmpty()) {
                Text(stringResource(R.string.backup_auto_empty), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            } else {
                ui.local.forEach { b ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(DateUtils.formatDateTime(ctx, b.modifiedAt, DATE_FLAGS), style = MaterialTheme.typography.bodyMedium)
                            Text("${b.name} · ${(b.sizeBytes + 1023) / 1024} KB", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                        }
                        TextButton(onClick = { onRestoreLocal(b.name) }, enabled = !ui.busy) { Text(stringResource(R.string.backup_restore)) }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PreviewCard(p: ImportPreview, busy: Boolean, onCancel: () -> Unit, onImport: (ImportMode) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    var mode by remember(p) { mutableStateOf(ImportMode.MERGE) }
    var confirm by remember(p) { mutableStateOf(false) }
    val plans = if (mode == ImportMode.MERGE) p.merge else p.replace
    val removedGames = p.replace.firstOrNull { it.key == "games" }?.removed ?: 0

    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(cs.surfaceVariant.copy(alpha = 0.5f)).padding(16.dp)) {
        Text(stringResource(R.string.preview_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.preview_source, p.sourceLabel), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        var fileLine = stringResource(
            R.string.preview_file, p.parsed.fileVersion,
            if (p.parsed.createdAt > 0) DateUtils.formatDateTime(ctx, p.parsed.createdAt, DATE_FLAGS) else "–"
        )
        if (p.parsed.versionAssumed) fileLine += " " + stringResource(R.string.preview_assumed)
        if (p.parsed.upgradedSteps > 0) fileLine += " " + stringResource(R.string.preview_upgraded)
        Text(fileLine, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        if (p.unknownSections.isNotEmpty()) {
            Text(stringResource(R.string.preview_unknown, p.unknownSections.joinToString()), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        ModeRow(mode == ImportMode.MERGE, stringResource(R.string.mode_merge), stringResource(R.string.mode_merge_desc)) { mode = ImportMode.MERGE }
        ModeRow(mode == ImportMode.REPLACE, stringResource(R.string.mode_replace), stringResource(R.string.mode_replace_desc)) { mode = ImportMode.REPLACE }
        Spacer(Modifier.height(8.dp))
        plans.forEach { Text(planText(it), style = MaterialTheme.typography.bodyMedium) }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel, enabled = !busy) { Text(stringResource(R.string.cancel)) }
            Button(onClick = { if (mode == ImportMode.REPLACE) confirm = true else onImport(ImportMode.MERGE) }, enabled = !busy) {
                Text(stringResource(R.string.preview_import))
            }
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.replace_title)) },
            text = { Text(stringResource(R.string.replace_message, removedGames)) },
            confirmButton = { TextButton(onClick = { confirm = false; onImport(ImportMode.REPLACE) }) { Text(stringResource(R.string.replace_confirm)) } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
private fun ModeRow(selected: Boolean, label: String, desc: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.padding(start = 12.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun planText(pl: SectionPlan): String {
    val name = when (pl.key) {
        "games" -> stringResource(R.string.section_games)
        "settings" -> stringResource(R.string.section_settings)
        else -> pl.key
    }
    if (pl.replaceOnly) return stringResource(R.string.plan_replace_only, name)
    var t = stringResource(R.string.plan_line, name, pl.added, pl.updated, pl.unchanged)
    if (pl.removed > 0) t += stringResource(R.string.plan_removed, pl.removed)
    if (pl.invalid > 0) t += stringResource(R.string.plan_invalid, pl.invalid)
    return t
}
