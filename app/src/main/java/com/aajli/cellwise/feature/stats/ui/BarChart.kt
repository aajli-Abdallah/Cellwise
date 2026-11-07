/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise.feature.stats.ui

import android.graphics.Paint as NativePaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

/** Minimal Canvas bar chart (no chart dependency). values and labels must have the same size. */
@Composable
fun BarChart(values: List<Int>, labels: List<String>, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val barColor = cs.primary
    val axisColor = cs.outlineVariant
    val textColor = cs.onSurfaceVariant.toArgb()
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)

    Canvas(modifier.fillMaxWidth().height(150.dp)) {
        val n = values.size
        if (n == 0) return@Canvas
        val labelH = 20.dp.toPx()
        val topPad = 16.dp.toPx()
        val baseY = size.height - labelH
        val usable = baseY - topPad
        val slot = size.width / n
        val barW = slot * 0.6f

        drawLine(axisColor, Offset(0f, baseY), Offset(size.width, baseY), 1.dp.toPx())
        for (i in 0 until n) {
            val h = usable * values[i] / max
            if (h > 0f) drawRect(barColor, Offset(i * slot + (slot - barW) / 2, baseY - h), Size(barW, h))
        }
        drawIntoCanvas { c ->
            val nc = c.nativeCanvas
            val p = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
                textAlign = NativePaint.Align.CENTER
                textSize = 11.dp.toPx()
                color = textColor
            }
            for (i in 0 until n) {
                val cx = i * slot + slot / 2
                nc.drawText(labels[i], cx, size.height - 4.dp.toPx(), p)
                if (values[i] > 0) nc.drawText(values[i].toString(), cx, baseY - usable * values[i] / max - 4.dp.toPx(), p)
            }
        }
    }
}
