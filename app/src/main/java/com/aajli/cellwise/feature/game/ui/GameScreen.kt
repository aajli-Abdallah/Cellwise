/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise.feature.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.aajli.cellwise.R
import com.aajli.cellwise.core.formatTime
import com.aajli.cellwise.core.settings.Settings
import com.aajli.cellwise.engine.Hint
import com.aajli.cellwise.engine.Technique
import com.aajli.cellwise.engine.Units
import com.aajli.cellwise.feature.game.GameState
import com.aajli.cellwise.ui.labelRes

fun techniqueName(t: Technique): Int = when (t) {
    Technique.NAKED_SINGLE -> R.string.tech_naked_single
    Technique.HIDDEN_SINGLE -> R.string.tech_hidden_single
    Technique.POINTING -> R.string.tech_pointing
    Technique.BOX_LINE -> R.string.tech_box_line
    Technique.NAKED_PAIR -> R.string.tech_naked_pair
    Technique.HIDDEN_PAIR -> R.string.tech_hidden_pair
    Technique.X_WING -> R.string.tech_x_wing
    Technique.NAKED_TRIPLE -> R.string.tech_naked_triple
    Technique.HIDDEN_TRIPLE -> R.string.tech_hidden_triple
    Technique.XY_WING -> R.string.tech_xy_wing
    Technique.SWORDFISH -> R.string.tech_swordfish
    Technique.GUESS -> R.string.tech_guess
}

/** Lays children out right-to-left (used for the left-handed layout). Text content keeps its own direction. */
@Composable
private fun Mirrored(mirror: Boolean, content: @Composable () -> Unit) {
    if (mirror) CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { content() } else content()
}

@Composable
fun GameScreen(
    state: GameState, elapsed: Long, settings: Settings,
    onBack: () -> Unit, onSelect: (Int) -> Unit, onDigit: (Int) -> Unit,
    onErase: () -> Unit, onUndo: () -> Unit, onToggleNotes: () -> Unit,
    onHint: () -> Unit, onCheck: () -> Unit, onAutoNotes: () -> Unit, onReveal: () -> Unit, onDismissPanel: () -> Unit,
    onNewSame: () -> Unit, onHome: () -> Unit, onShareCode: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val tick = { if (settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    var lastMistakes by remember { mutableStateOf(state.mistakes) }
    LaunchedEffect(state.mistakes) {
        if (state.mistakes > lastMistakes && settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        lastMistakes = state.mistakes
    }
    var wasWon by remember { mutableStateOf(state.won) }
    LaunchedEffect(state.won) {
        if (state.won && !wasWon && settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        wasWon = state.won
    }

    val hintCells = state.hint?.unit?.takeIf { it >= 0 }?.let { Units.ALL[it].toSet() } ?: emptySet()
    val sel = state.selected
    val shareDesc = stringResource(R.string.share_puzzle)
    val gridDesc = if (sel < 0) stringResource(R.string.a11y_grid) else {
        val v = state.values[sel]
        val valueText = when {
            v == 0 -> stringResource(R.string.a11y_empty)
            state.givens[sel] != 0 -> stringResource(R.string.a11y_clue, v)
            else -> stringResource(R.string.a11y_value, v)
        }
        stringResource(R.string.a11y_grid_selected, sel / 9 + 1, sel % 9 + 1, valueText)
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Mirrored(settings.leftHanded) {
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) {
                    Text(if (settings.leftHanded) "→" else "←", style = MaterialTheme.typography.titleLarge)
                }
                Column(Modifier.weight(1f)) {
                    Text(stringResource(state.difficulty.labelRes()), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.game_mistakes, state.mistakes), style = MaterialTheme.typography.labelMedium,
                        color = if (state.mistakes > 0) cs.error else cs.onSurfaceVariant
                    )
                }
                TextButton(onClick = onShareCode, modifier = Modifier.semantics { contentDescription = shareDesc }) { Text("📤") }
                Text(
                    formatTime(elapsed), style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Monospace,
                    color = cs.onPrimaryContainer,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(cs.primaryContainer).padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(cs.surface)
                .border(if (settings.highContrast) 2.5.dp else 1.5.dp, if (settings.highContrast) cs.onSurface else cs.outlineVariant, RoundedCornerShape(16.dp))
                .semantics { contentDescription = gridDesc }
        ) {
            SudokuGrid(
                state, settings, { tick(); onSelect(it) }, Modifier.fillMaxWidth(),
                hintCells = hintCells, hintTarget = state.hint?.cell ?: -1
            )
        }
        Spacer(Modifier.weight(1f))
        if (state.hint != null || state.checkWrong >= 0) {
            InfoPanel(state, onReveal, onDismissPanel)
        } else {
            Mirrored(settings.leftHanded) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ToolButton("↶", stringResource(R.string.tool_undo), onClick = onUndo)
                    ToolButton("⌫", stringResource(R.string.tool_erase), onClick = onErase)
                    ToolButton("✎", stringResource(R.string.tool_notes), active = state.notesMode, onClick = onToggleNotes)
                    ToolButton("💡", stringResource(R.string.tool_hint), onClick = onHint)
                    ToolButton("✔", stringResource(R.string.tool_check), onClick = onCheck)
                    ToolButton("✨", stringResource(R.string.tool_auto), onClick = onAutoNotes)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        DigitPad(state) { tick(); onDigit(it) }
        Spacer(Modifier.height(12.dp))
    }

    if (state.won) {
        AlertDialog(
            onDismissRequest = onHome,
            title = { Text(stringResource(if (state.dailyDay != 0) R.string.win_title_daily else R.string.win_title)) },
            text = { Text(stringResource(R.string.win_message, formatTime(elapsed), state.mistakes, state.hints)) },
            confirmButton = { TextButton(onClick = onNewSame) { Text(stringResource(R.string.win_new)) } },
            dismissButton = { TextButton(onClick = onHome) { Text(stringResource(R.string.win_home)) } }
        )
    }
}

@Composable
private fun RowScope.ToolButton(icon: String, label: String, active: Boolean = false, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val fg = if (active) cs.onPrimary else cs.onSurfaceVariant
    Column(
        Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(10.dp))
            .background(if (active) cs.primary else cs.surfaceVariant.copy(alpha = 0.6f))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(icon, style = MaterialTheme.typography.titleMedium, color = fg)
        Text(label, style = MaterialTheme.typography.labelSmall, color = fg)
    }
}

/** Replaces the tool row while a hint or a check result is shown. Typing a digit closes it too. */
@Composable
private fun InfoPanel(state: GameState, onReveal: () -> Unit, onClose: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val h = state.hint
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(cs.tertiaryContainer)
            .padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            if (h != null) {
                Text(stringResource(techniqueName(h.technique)), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = cs.onTertiaryContainer)
                Text(hintBody(h), style = MaterialTheme.typography.bodySmall, color = cs.onTertiaryContainer)
            } else {
                Text(
                    if (state.checkWrong == 0) stringResource(R.string.check_none) else stringResource(R.string.check_some, state.checkWrong),
                    style = MaterialTheme.typography.bodyMedium, color = cs.onTertiaryContainer
                )
            }
        }
        if (h != null) TextButton(onClick = onReveal) { Text(stringResource(R.string.hint_reveal)) }
        TextButton(onClick = onClose) { Text(stringResource(R.string.hint_close)) }
    }
}

@Composable
private fun unitLabel(u: Int): String = when {
    u < 9 -> stringResource(R.string.unit_row, u + 1)
    u < 18 -> stringResource(R.string.unit_col, u - 8)
    else -> stringResource(R.string.unit_box, u - 17)
}

@Composable
private fun hintBody(h: Hint): String {
    val cell = "R${h.cell / 9 + 1}C${h.cell % 9 + 1}"
    var t = when (h.technique) {
        Technique.HIDDEN_SINGLE -> stringResource(R.string.hint_hidden_single_body, unitLabel(h.unit))
        Technique.GUESS -> stringResource(R.string.hint_guess_body, cell)
        else -> stringResource(R.string.hint_naked_single_body, cell)
    }
    h.via?.let { t += " " + stringResource(R.string.hint_via, stringResource(techniqueName(it))) }
    return t
}

@Composable
private fun DigitPad(state: GameState, onDigit: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (d in 1..9) {
            val left = 9 - state.placed(d)
            val enabled = left > 0 && !state.won
            val desc = stringResource(R.string.a11y_digit, d, left)
            Column(
                Modifier
                    .weight(1f)
                    .height(64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(cs.primaryContainer.copy(alpha = if (enabled) 1f else 0.4f))
                    .clickable(enabled = enabled, role = Role.Button) { onDigit(d) }
                    .semantics(mergeDescendants = true) { contentDescription = desc },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(d.toString(), style = MaterialTheme.typography.headlineSmall, color = cs.onPrimaryContainer)
                Text(left.toString(), style = MaterialTheme.typography.labelSmall, color = cs.onPrimaryContainer.copy(alpha = 0.7f))
            }
        }
    }
}
