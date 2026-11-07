/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Brand amber: the "wise" centre cell of the icon. Used for sparks, progress and highlights. */
val Spark = Color(0xFFFFB020)

// Midnight indigo + amber. Light = paper, dark = night flight.
private val Light = lightColorScheme(
    primary = Color(0xFF4338CA), onPrimary = Color.White,
    primaryContainer = Color(0xFFE3E4FF), onPrimaryContainer = Color(0xFF14125E),
    secondary = Color(0xFF5B5A8C), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6E4F7), onSecondaryContainer = Color(0xFF1C1B3A),
    tertiary = Color(0xFFB45309), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE8BF), onTertiaryContainer = Color(0xFF3B1D00),
    background = Color(0xFFFBF9FF), onBackground = Color(0xFF1B1A2B),
    surface = Color.White, onSurface = Color(0xFF1B1A2B),
    surfaceVariant = Color(0xFFE9E7F5), onSurfaceVariant = Color(0xFF494763),
    outline = Color(0xFF7B7993), outlineVariant = Color(0xFFCFCDE3),
    error = Color(0xFFD1293D), onError = Color.White
)
private val Dark = darkColorScheme(
    primary = Color(0xFFB4B8FF), onPrimary = Color(0xFF1A1773),
    primaryContainer = Color(0xFF332F91), onPrimaryContainer = Color(0xFFE3E4FF),
    secondary = Color(0xFFC3C2EE), onSecondary = Color(0xFF2B2A52),
    secondaryContainer = Color(0xFF3A3961), onSecondaryContainer = Color(0xFFE6E4F7),
    tertiary = Color(0xFFFFC857), onTertiary = Color(0xFF452B00),
    tertiaryContainer = Color(0xFF5E3C00), onTertiaryContainer = Color(0xFFFFE8BF),
    background = Color(0xFF100F22), onBackground = Color(0xFFE8E6F8),
    surface = Color(0xFF1A1933), onSurface = Color(0xFFE8E6F8),
    surfaceVariant = Color(0xFF2C2B48), onSurfaceVariant = Color(0xFFC6C4DD),
    outline = Color(0xFF8F8DA8), outlineVariant = Color(0xFF46456A),
    error = Color(0xFFFF8896), onError = Color(0xFF50000E)
)

// Serif display and digits give the "printed puzzle" feel; body text stays sans-serif for readability.
private val base = Typography()
private val AppTypography = base.copy(
    displayMedium = base.displayMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp), medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp), extraLarge = RoundedCornerShape(28.dp)
)

/** Hue (degrees) of each accent palette. Index 0 (Indigo) uses the hand-tuned schemes above; the amber accent is shared. */
val PALETTE_HUES = floatArrayOf(245f, 175f, 340f, 140f)

fun isDark(themeMode: Int, systemDark: Boolean): Boolean = when (themeMode) {
    1 -> false
    2 -> true
    else -> systemDark
}

/** A full scheme built from one hue with HSL tones; neutrals are tinted with the hue, tertiary/error stay brand. */
private fun generated(h: Float, dark: Boolean): ColorScheme = if (!dark) lightColorScheme(
    primary = Color.hsl(h, 0.55f, 0.40f), onPrimary = Color.White,
    primaryContainer = Color.hsl(h, 0.90f, 0.92f), onPrimaryContainer = Color.hsl(h, 0.65f, 0.14f),
    secondary = Color.hsl(h, 0.20f, 0.42f), onSecondary = Color.White,
    secondaryContainer = Color.hsl(h, 0.40f, 0.91f), onSecondaryContainer = Color.hsl(h, 0.40f, 0.14f),
    tertiary = Light.tertiary, onTertiary = Light.onTertiary,
    tertiaryContainer = Light.tertiaryContainer, onTertiaryContainer = Light.onTertiaryContainer,
    background = Color.hsl(h, 0.60f, 0.985f), onBackground = Color.hsl(h, 0.30f, 0.12f),
    surface = Color.White, onSurface = Color.hsl(h, 0.30f, 0.12f),
    surfaceVariant = Color.hsl(h, 0.35f, 0.93f), onSurfaceVariant = Color.hsl(h, 0.15f, 0.32f),
    outline = Color.hsl(h, 0.10f, 0.50f), outlineVariant = Color.hsl(h, 0.25f, 0.84f),
    error = Light.error, onError = Light.onError
) else darkColorScheme(
    primary = Color.hsl(h, 1.0f, 0.82f), onPrimary = Color.hsl(h, 0.70f, 0.20f),
    primaryContainer = Color.hsl(h, 0.55f, 0.30f), onPrimaryContainer = Color.hsl(h, 0.90f, 0.92f),
    secondary = Color.hsl(h, 0.45f, 0.82f), onSecondary = Color.hsl(h, 0.35f, 0.20f),
    secondaryContainer = Color.hsl(h, 0.25f, 0.28f), onSecondaryContainer = Color.hsl(h, 0.40f, 0.91f),
    tertiary = Dark.tertiary, onTertiary = Dark.onTertiary,
    tertiaryContainer = Dark.tertiaryContainer, onTertiaryContainer = Dark.onTertiaryContainer,
    background = Color.hsl(h, 0.40f, 0.09f), onBackground = Color.hsl(h, 0.30f, 0.92f),
    surface = Color.hsl(h, 0.30f, 0.14f), onSurface = Color.hsl(h, 0.30f, 0.92f),
    surfaceVariant = Color.hsl(h, 0.22f, 0.22f), onSurfaceVariant = Color.hsl(h, 0.20f, 0.82f),
    outline = Color.hsl(h, 0.12f, 0.60f), outlineVariant = Color.hsl(h, 0.20f, 0.32f),
    error = Dark.error, onError = Dark.onError
)

/**
 * Theme mode (system / light / dark) and accent palette; wallpaper colors (Android 12+) override the palette when [dynamic] is on.
 */
@Composable
fun CellwiseTheme(dynamic: Boolean = false, themeMode: Int = 0, palette: Int = 0, content: @Composable () -> Unit) {
    val dark = isDark(themeMode, isSystemInDarkTheme())
    val ctx = LocalContext.current
    val scheme = when {
        dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        palette in 1 until PALETTE_HUES.size -> generated(PALETTE_HUES[palette], dark)
        dark -> Dark
        else -> Light
    }
    MaterialTheme(colorScheme = scheme, typography = AppTypography, shapes = AppShapes, content = content)
}
