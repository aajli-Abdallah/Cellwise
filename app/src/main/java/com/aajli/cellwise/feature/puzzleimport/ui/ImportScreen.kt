/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise.feature.puzzleimport.ui

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aajli.cellwise.R
import com.aajli.cellwise.core.settings.Settings
import com.aajli.cellwise.engine.Difficulty
import com.aajli.cellwise.feature.game.GameState
import com.aajli.cellwise.feature.game.ui.SudokuGrid
import com.aajli.cellwise.feature.puzzleimport.ImportUi
import com.aajli.cellwise.feature.puzzleimport.PuzzleImport
import com.aajli.cellwise.ui.ScreenHeader

/** Type in a grid seen on paper: tap a cell then a digit (the cursor moves on), or paste/type the whole text. */
@Composable
fun ImportScreen(
    ui: ImportUi, hasActive: Boolean, onBack: () -> Unit, onSelect: (Int) -> Unit, onDigit: (Int) -> Unit,
    onErase: () -> Unit, onClear: () -> Unit, onText: (IntArray) -> Unit, onStart: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val clipboard = LocalClipboardManager.current
    val conflicts = remember(ui.grid) { PuzzleImport.conflicts(ui.grid.toIntArray()) }
    val clues = ui.grid.count { it != 0 }
    val state = remember(ui.grid, ui.selected) {
        GameState(
            id = "import", difficulty = Difficulty.EASY, givens = ui.grid, solution = ui.grid, values = ui.grid,
            notes = List(81) { 0 }, mistakes = 0, createdAt = 0L, selected = ui.selected
        )
    }
    var pasteOpen by remember { mutableStateOf(false) }
    var confirmStart by remember { mutableStateOf(false) }
    val canStart = clues >= PuzzleImport.MIN_CLUES && conflicts.isEmpty() && !ui.busy

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
        ScreenHeader(stringResource(R.string.import_title), onBack)
        Text(stringResource(R.string.import_help), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.import_clues, clues) + if (conflicts.isNotEmpty()) " · " + stringResource(R.string.import_conflicts_note) else "",
            style = MaterialTheme.typography.labelMedium, color = if (conflicts.isNotEmpty()) cs.error else cs.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        SudokuGrid(state, Settings(), onSelect, Modifier.fillMaxWidth(), errorCells = conflicts)
        ui.message?.let { m ->
            Spacer(Modifier.height(8.dp))
            Text(stringResource(m.res, *m.args.toTypedArray()), style = MaterialTheme.typography.bodyMedium, color = cs.error)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onErase, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.import_erase)) }
            OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.import_clear)) }
            OutlinedButton(onClick = { pasteOpen = true }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.import_paste)) }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (d in 1..9) {
                Box9(d, Modifier.weight(1f)) { onDigit(d) }
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { if (hasActive) confirmStart = true else onStart() },
            enabled = canStart, modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text(stringResource(R.string.import_start)) }
        Spacer(Modifier.height(16.dp))
    }

    if (pasteOpen) PasteDialog(initial = clipboard.getText()?.text.orEmpty(), onDismiss = { pasteOpen = false }, onFill = { g -> pasteOpen = false; onText(g) })

    if (confirmStart) {
        AlertDialog(
            onDismissRequest = { confirmStart = false },
            title = { Text(stringResource(R.string.abandon_title)) },
            text = { Text(stringResource(R.string.abandon_message)) },
            confirmButton = { TextButton(onClick = { confirmStart = false; onStart() }) { Text(stringResource(R.string.abandon_confirm)) } },
            dismissButton = { TextButton(onClick = { confirmStart = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
private fun Box9(d: Int, modifier: Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier.height(56.dp).clip(RoundedCornerShape(10.dp)).background(cs.secondaryContainer).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) { Text(d.toString(), style = MaterialTheme.typography.headlineSmall, color = cs.onSecondaryContainer) }
}

@Composable
private fun PasteDialog(initial: String, onDismiss: () -> Unit, onFill: (IntArray) -> Unit) {
    // If the clipboard already holds a puzzle (e.g. a shared Cellwise message), start with it.
    var text by remember { mutableStateOf(if (PuzzleImport.parseText(initial).grid != null) initial else "") }
    val parsed = PuzzleImport.parseText(text)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_paste_title)) },
        text = {
            Column {
                Text(stringResource(R.string.import_paste_help), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text, onValueChange = { text = it }, minLines = 4, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.import_paste_count, parsed.cells), style = MaterialTheme.typography.labelMedium)
            }
        },
        confirmButton = { TextButton(onClick = { parsed.grid?.let(onFill) }, enabled = parsed.grid != null) { Text(stringResource(R.string.import_paste_fill)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
