/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise.feature.game.ui

import android.graphics.Paint as NativePaint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.aajli.cellwise.core.settings.Settings
import com.aajli.cellwise.feature.game.GameRules
import com.aajli.cellwise.feature.game.GameState

/**
 * Canvas-drawn 9x9 grid. Pure rendering: all game logic stays in GameRules.
 * Look: printed-puzzle serif digits (bold = clues, italic indigo = your pen), rounded highlights, a ring on the selected cell.
 */
@Composable
fun SudokuGrid(
    state: GameState, settings: Settings, onSelect: (Int) -> Unit, modifier: Modifier = Modifier,
    errorCells: Set<Int>? = null,      // when set, exactly these cells are drawn as errors (import screen)
    hintCells: Set<Int> = emptySet(),  // the unit a hint points at
    hintTarget: Int = -1               // the cell a hint points at
) {
    val cs = MaterialTheme.colorScheme
    val select = rememberUpdatedState(onSelect)
    val tfGiven = remember { Typeface.create(Typeface.SERIF, Typeface.BOLD) }
    val tfUser = remember { Typeface.create(Typeface.SERIF, Typeface.ITALIC) }
    val tfNote = remember { Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL) }
    val cGiven = cs.onSurface.toArgb()
    val cUser = cs.primary.toArgb()
    val cError = cs.error.toArgb()
    val cNote = cs.onSurfaceVariant.toArgb()
    val bgSelected = cs.primaryContainer
    val ring = cs.primary
    val bgPeer = cs.primaryContainer.copy(alpha = 0.35f)
    val bgSame = cs.primaryContainer.copy(alpha = 0.85f)
    val bgHint = cs.tertiaryContainer.copy(alpha = 0.6f)
    val bgHintTarget = cs.tertiary.copy(alpha = 0.40f)
    val lineThin = if (settings.highContrast) cs.onSurface.copy(alpha = 0.45f) else cs.outlineVariant.copy(alpha = 0.7f)
    val lineThick = if (settings.highContrast) cs.onSurface else cs.primary.copy(alpha = 0.55f)

    Canvas(
        modifier
            .aspectRatio(1f)
            .pointerInput(Unit) {
                detectTapGestures { o ->
                    val cell = size.width / 9f
                    val c = (o.x / cell).toInt().coerceIn(0, 8)
                    val r = (o.y / cell).toInt().coerceIn(0, 8)
                    select.value(r * 9 + c)
                }
            }
    ) {
        val cell = size.width / 9f
        val inset = 1.5.dp.toPx()
        val radius = CornerRadius(5.dp.toPx())
        val inner = Size(cell - 2 * inset, cell - 2 * inset)
        val sel = state.selected
        val selVal = if (sel >= 0) state.values[sel] else 0

        for (i in 0 until 81) {
            val bg = when {
                i == hintTarget -> bgHintTarget
                i == sel -> bgSelected
                i in hintCells -> bgHint
                settings.highlightSameNumber && selVal != 0 && state.values[i] == selVal -> bgSame
                sel >= 0 && GameRules.sharesUnit(i, sel) -> bgPeer
                else -> null
            }
            if (bg != null) drawRoundRect(bg, Offset((i % 9) * cell + inset, (i / 9) * cell + inset), inner, radius)
        }

        for (k in 1..8) {
            val thick = k % 3 == 0
            val w = if (thick) 2.dp.toPx() else 0.6.dp.toPx()
            val col = if (thick) lineThick else lineThin
            drawLine(col, Offset(k * cell, 0f), Offset(k * cell, size.height), w)
            drawLine(col, Offset(0f, k * cell), Offset(size.width, k * cell), w)
        }

        if (sel >= 0) {
            drawRoundRect(ring, Offset((sel % 9) * cell + inset, (sel / 9) * cell + inset), inner, radius, style = Stroke(2.dp.toPx()))
        }

        drawIntoCanvas { canvas ->
            val nc = canvas.nativeCanvas
            val p = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply { textAlign = NativePaint.Align.CENTER }
            for (i in 0 until 81) {
                val r = i / 9; val c = i % 9
                val v = state.values[i]
                if (v != 0) {
                    val given = state.givens[i] != 0
                    val wrong = if (errorCells != null) i in errorCells
                    else !given && (settings.highlightMistakes || state.showErrors) && v != state.solution[i]
                    p.color = if (wrong) cError else if (given) cGiven else cUser
                    p.typeface = if (given) tfGiven else tfUser
                    p.textSize = cell * (if (settings.largeNumbers) 0.68f else 0.58f)
                    val fm = p.fontMetrics
                    nc.drawText(v.toString(), c * cell + cell / 2, r * cell + cell / 2 - (fm.ascent + fm.descent) / 2, p)
                } else if (state.notes[i] != 0) {
                    p.color = cNote
                    p.typeface = tfNote
                    p.textSize = cell * (if (settings.largeNumbers) 0.29f else 0.25f)
                    val fm = p.fontMetrics
                    val sub = cell / 3
                    for (d in 1..9) if (state.notes[i] and (1 shl d) != 0) {
                        val cx = c * cell + sub * ((d - 1) % 3) + sub / 2
                        val cy = r * cell + sub * ((d - 1) / 3) + sub / 2 - (fm.ascent + fm.descent) / 2
                        nc.drawText(d.toString(), cx, cy, p)
                    }
                }
            }
        }
    }
}
