/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise.core.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Number of accent palettes (index 0 = Indigo). Must match ui/theme PALETTE_HUES. */
const val PALETTE_COUNT = 4

data class Settings(
    val highlightMistakes: Boolean = true,
    val keepScreenOn: Boolean = true,
    val highlightSameNumber: Boolean = true,
    val dynamicColors: Boolean = false,   // false = Cellwise palettes, true = wallpaper colors (Android 12+)
    val themeMode: Int = 0,               // 0 = follow system, 1 = light, 2 = dark
    val palette: Int = 0,                 // 0..PALETTE_COUNT-1
    val haptics: Boolean = true,
    val leftHanded: Boolean = false,      // mirrors the top bar and the tool row on the game screen
    val highContrast: Boolean = false,
    val largeNumbers: Boolean = false,
    val reduceMotion: Boolean = false
)

/** SharedPreferences-backed settings exposed as a StateFlow. Included in backups by SettingsBackupSection. */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("cellwise_settings", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<Settings> = _state.asStateFlow()

    private fun read() = Settings(
        highlightMistakes = prefs.getBoolean("highlight_mistakes", true),
        keepScreenOn = prefs.getBoolean("keep_screen_on", true),
        highlightSameNumber = prefs.getBoolean("highlight_same", true),
        dynamicColors = prefs.getBoolean("dynamic_colors", false),
        themeMode = prefs.getInt("theme_mode", 0).coerceIn(0, 2),
        palette = prefs.getInt("palette", 0).coerceIn(0, PALETTE_COUNT - 1),
        haptics = prefs.getBoolean("haptics", true),
        leftHanded = prefs.getBoolean("left_handed", false),
        highContrast = prefs.getBoolean("high_contrast", false),
        largeNumbers = prefs.getBoolean("large_numbers", false),
        reduceMotion = prefs.getBoolean("reduce_motion", false)
    )

    fun update(transform: (Settings) -> Settings) {
        val n = transform(_state.value)
        prefs.edit()
            .putBoolean("highlight_mistakes", n.highlightMistakes)
            .putBoolean("keep_screen_on", n.keepScreenOn)
            .putBoolean("highlight_same", n.highlightSameNumber)
            .putBoolean("dynamic_colors", n.dynamicColors)
            .putInt("theme_mode", n.themeMode)
            .putInt("palette", n.palette)
            .putBoolean("haptics", n.haptics)
            .putBoolean("left_handed", n.leftHanded)
            .putBoolean("high_contrast", n.highContrast)
            .putBoolean("large_numbers", n.largeNumbers)
            .putBoolean("reduce_motion", n.reduceMotion)
            .apply()
        _state.value = n
    }
}
