/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.aajli.cellwise.R
import com.aajli.cellwise.core.settings.Settings
import com.aajli.cellwise.ui.ScreenHeader
import com.aajli.cellwise.ui.theme.PALETTE_HUES

private val PALETTE_NAMES = listOf(R.string.palette_indigo, R.string.palette_teal, R.string.palette_rose, R.string.palette_forest)

@Composable
fun SettingsScreen(settings: Settings, onChange: ((Settings) -> Settings) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader(stringResource(R.string.settings_title), onBack)

        Section(stringResource(R.string.settings_section_look))
        Label(stringResource(R.string.setting_theme))
        ChoiceRow(
            listOf(stringResource(R.string.theme_system), stringResource(R.string.theme_light), stringResource(R.string.theme_dark)),
            settings.themeMode
        ) { v -> onChange { it.copy(themeMode = v) } }
        Label(stringResource(R.string.setting_palette))
        PaletteRow(settings.palette) { v -> onChange { it.copy(palette = v) } }
        SwitchRow(stringResource(R.string.setting_dynamic), stringResource(R.string.setting_dynamic_desc), settings.dynamicColors) { v ->
            onChange { it.copy(dynamicColors = v) }
        }

        Section(stringResource(R.string.settings_section_game))
        SwitchRow(stringResource(R.string.setting_mistakes), stringResource(R.string.setting_mistakes_desc), settings.highlightMistakes) { v ->
            onChange { it.copy(highlightMistakes = v) }
        }
        SwitchRow(stringResource(R.string.setting_same), null, settings.highlightSameNumber) { v ->
            onChange { it.copy(highlightSameNumber = v) }
        }
        SwitchRow(stringResource(R.string.setting_awake), null, settings.keepScreenOn) { v ->
            onChange { it.copy(keepScreenOn = v) }
        }
        SwitchRow(stringResource(R.string.setting_haptics), stringResource(R.string.setting_haptics_desc), settings.haptics) { v ->
            onChange { it.copy(haptics = v) }
        }
        SwitchRow(stringResource(R.string.setting_left), stringResource(R.string.setting_left_desc), settings.leftHanded) { v ->
            onChange { it.copy(leftHanded = v) }
        }

        Section(stringResource(R.string.settings_section_access))
        SwitchRow(stringResource(R.string.setting_contrast), stringResource(R.string.setting_contrast_desc), settings.highContrast) { v ->
            onChange { it.copy(highContrast = v) }
        }
        SwitchRow(stringResource(R.string.setting_large), stringResource(R.string.setting_large_desc), settings.largeNumbers) { v ->
            onChange { it.copy(largeNumbers = v) }
        }
        SwitchRow(stringResource(R.string.setting_motion), stringResource(R.string.setting_motion_desc), settings.reduceMotion) { v ->
            onChange { it.copy(reduceMotion = v) }
        }
        Box(Modifier.height(24.dp))
    }
}

@Composable
private fun Section(title: String) {
    Text(
        title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary,
        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 20.dp, top = 8.dp))
}

@Composable
private fun ChoiceRow(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { i, l ->
            val sel = i == selected
            Box(
                Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (sel) cs.primaryContainer else cs.surfaceVariant.copy(alpha = 0.55f))
                    .then(if (sel) Modifier.border(2.dp, cs.primary, RoundedCornerShape(12.dp)) else Modifier)
                    .clickable(role = Role.RadioButton) { onSelect(i) },
                contentAlignment = Alignment.Center
            ) {
                Text(l, style = MaterialTheme.typography.labelLarge, color = if (sel) cs.onPrimaryContainer else cs.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PaletteRow(selected: Int, onSelect: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        PALETTE_NAMES.forEachIndexed { i, nameRes ->
            val swatch = if (i == 0) Color(0xFF4338CA) else Color.hsl(PALETTE_HUES[i], 0.55f, 0.40f)
            Column(
                Modifier.clip(RoundedCornerShape(12.dp)).clickable(role = Role.RadioButton) { onSelect(i) }.padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(swatch)
                        .then(if (i == selected) Modifier.border(3.dp, cs.onSurface, CircleShape) else Modifier)
                )
                Text(stringResource(nameRes), style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, desc: String?, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (desc != null) Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}
