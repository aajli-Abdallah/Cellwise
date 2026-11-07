/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import com.aajli.cellwise.ui.theme.Spark
import kotlin.math.cos
import kotlin.math.sin

/** True when animations should be off (setting or system animation scale 0). Provided by MainActivity. */
val LocalReduceMotion = compositionLocalOf { false }

/** The app icon as a live logo: eight indigo cells around the amber "wise" cell, which breathes. */
@Composable
fun CellwiseLogo(logoSize: Dp, modifier: Modifier = Modifier, animated: Boolean = true) {
    val cs = MaterialTheme.colorScheme
    val pulse = if (animated && !LocalReduceMotion.current) {
        val t = rememberInfiniteTransition(label = "logo")
        t.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(animation = tween(1500, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
            label = "pulse"
        ).value
    } else 0f
    val corner = cs.primary
    val edge = cs.primary.copy(alpha = 0.42f)

    Canvas(modifier.size(logoSize)) {
        val u = size.width / 3f
        val gap = u * 0.16f
        val cell = u - gap
        val radius = CornerRadius(cell * 0.26f)
        for (i in 0..2) for (j in 0..2) {
            if (i == 1 && j == 1) continue
            drawRoundRect(
                if ((i + j) % 2 == 0) corner else edge,
                Offset(j * u + gap / 2, i * u + gap / 2), Size(cell, cell), radius
            )
        }
        val c = 1.5f * u
        val s = cell * (1f + 0.10f * pulse)
        drawCircle(Spark.copy(alpha = 0.30f * pulse), radius = u * 0.95f, center = Offset(c, c))
        drawRoundRect(Spark, Offset(c - s / 2, c - s / 2), Size(s, s), radius)
        val star = Path().apply {
            for (k in 0 until 8) {
                val a = Math.toRadians(-90.0 + 45.0 * k)
                val r = if (k % 2 == 0) s * 0.36f else s * 0.12f
                val x = c + (r * cos(a)).toFloat()
                val y = c + (r * sin(a)).toFloat()
                if (k == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        drawPath(star, Color.White)
    }
}
