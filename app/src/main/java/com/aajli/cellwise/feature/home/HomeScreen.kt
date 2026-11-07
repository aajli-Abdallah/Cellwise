/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise.feature.home

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.aajli.cellwise.R
import com.aajli.cellwise.core.CoreInfo
import com.aajli.cellwise.core.formatTime
import com.aajli.cellwise.engine.Difficulty
import com.aajli.cellwise.feature.daily.DailyInfo
import com.aajli.cellwise.feature.game.GameState
import com.aajli.cellwise.ui.CellwiseLogo
import com.aajli.cellwise.ui.descRes
import com.aajli.cellwise.ui.labelRes
import com.aajli.cellwise.ui.theme.Spark
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DayStyle
import java.util.Locale

@Composable
fun HomeScreen(
    active: GameState?, elapsed: Long, generating: Boolean, reminderDue: Boolean,
    onContinue: () -> Unit, onNew: (Difficulty) -> Unit, onStats: () -> Unit, onBackup: () -> Unit,
    onSettings: () -> Unit, onAbout: () -> Unit, onSnooze: () -> Unit, onImport: () -> Unit,
    daily: DailyInfo, onDaily: () -> Unit
) {
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }  // action waiting for "abandon current game?" confirmation
    val cs = MaterialTheme.colorScheme

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (reminderDue) {
            Column(Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(16.dp)).background(cs.tertiaryContainer).padding(16.dp)) {
                Text(stringResource(R.string.reminder_text), style = MaterialTheme.typography.bodyMedium, color = cs.onTertiaryContainer)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onSnooze) { Text(stringResource(R.string.reminder_later)) }
                    Button(onClick = onBackup) { Text(stringResource(R.string.reminder_now)) }
                }
            }
            Spacer(Modifier.height(8.dp))
        } else {
            Spacer(Modifier.height(32.dp))
        }
        CellwiseLogo(96.dp)
        Spacer(Modifier.height(8.dp))
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = cs.onSurface)) { append("Cell") }
                withStyle(SpanStyle(color = cs.tertiary)) { append("wise") }
            },
            style = MaterialTheme.typography.displayMedium
        )
        Text(stringResource(R.string.home_tagline), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))

        if (active != null) {
            HeroCard(active, elapsed, !generating, onContinue)
            Spacer(Modifier.height(12.dp))
        }
        if (daily.today != 0) {
            val dailyInProgress = active != null && active.dailyDay == daily.today
            DailyCard(daily, dailyInProgress, !generating) {
                when {
                    dailyInProgress -> onContinue()
                    active != null -> pending = onDaily
                    else -> onDaily()
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.home_new_game), style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
        Spacer(Modifier.height(8.dp))
        Difficulty.values().forEach { d ->
            LevelCard(d, !generating) { if (active != null) pending = { onNew(d) } else onNew(d) }
            Spacer(Modifier.height(8.dp))
        }
        if (generating) {
            CircularProgressIndicator()
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tile("📰", R.string.tile_import, onImport)
            Tile("📊", R.string.home_stats, onStats)
            Tile("💾", R.string.home_backup, onBackup)
            Tile("⚙️", R.string.home_settings, onSettings)
        }
        TextButton(onClick = onAbout) { Text(stringResource(R.string.home_about)) }
        Text(CoreInfo.COPYRIGHT, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
    }

    pending?.let { action ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(stringResource(R.string.abandon_title)) },
            text = { Text(stringResource(R.string.abandon_message)) },
            confirmButton = { TextButton(onClick = { pending = null; action() }) { Text(stringResource(R.string.abandon_confirm)) } },
            dismissButton = { TextButton(onClick = { pending = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

/** Game in progress: level, time and how much of the grid is filled. */
@Composable
private fun HeroCard(active: GameState, elapsed: Long, enabled: Boolean, onClick: () -> Unit) {
    val empty = active.givens.count { it == 0 }
    val filled = (0 until 81).count { active.givens[it] == 0 && active.values[it] != 0 }
    val progress = if (empty == 0) 1f else filled.toFloat() / empty
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF3730A3), Color(0xFF6D28D9))))
            .clickable(enabled = enabled, onClick = onClick).padding(20.dp)
    ) {
        Text(stringResource(R.string.hero_continue), style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.75f))
        Text(
            stringResource(active.difficulty.labelRes()) + " · " + formatTime(elapsed),
            style = MaterialTheme.typography.headlineMedium, color = Color.White
        )
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.25f))) {
            Box(Modifier.fillMaxWidth(progress).height(8.dp).background(Spark))
        }
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.hero_progress, (progress * 100).toInt()), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
    }
}

@Composable
private fun LevelCard(d: Difficulty, enabled: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(cs.surfaceVariant.copy(alpha = 0.55f))
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DotMeter(d.ordinal + 1)
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(stringResource(d.labelRes()), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(d.descRes()), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        }
        Text("›", style = MaterialTheme.typography.headlineSmall, color = cs.primary)
    }
}

/** Three dots, [level] of them lit in amber. */
@Composable
private fun DotMeter(level: Int) {
    val on = MaterialTheme.colorScheme.tertiary
    val off = MaterialTheme.colorScheme.outlineVariant
    Canvas(Modifier.size(width = 52.dp, height = 10.dp)) {
        val d = size.height
        for (i in 0..3) drawCircle(if (i < level) on else off, radius = d / 2, center = Offset(d / 2 + i * (d + 4.dp.toPx()), d / 2))
    }
}

@Composable
private fun RowScope.Tile(emoji: String, labelRes: Int, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(cs.surfaceVariant.copy(alpha = 0.55f))
            .clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, style = MaterialTheme.typography.titleLarge)
        Text(stringResource(labelRes), style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, maxLines = 1)
    }
}

/** Today's puzzle: level, streak, last 7 days. Same puzzle on every phone (see docs/DAILY.md). */
@Composable
private fun DailyCard(daily: DailyInfo, inProgress: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val date = remember(daily.today) { LocalDate.ofEpochDay(daily.today.toLong()) }
    val white = Color.White
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF92400E), Color(0xFFD97706))))
            .clickable(enabled = enabled && !daily.doneToday, onClick = onClick).padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.daily_title), style = MaterialTheme.typography.labelLarge, color = white.copy(alpha = 0.85f))
                Text(date.format(DateTimeFormatter.ofPattern("EEE d MMM")), style = MaterialTheme.typography.headlineSmall, color = white)
                Text(stringResource(daily.level.labelRes()), style = MaterialTheme.typography.bodySmall, color = white.copy(alpha = 0.85f))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("🔥 ${daily.streak}", style = MaterialTheme.typography.headlineSmall, color = white)
                Text(stringResource(R.string.daily_streak), style = MaterialTheme.typography.labelSmall, color = white.copy(alpha = 0.85f))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            daily.week.forEachIndexed { i, won ->
                val d = date.minusDays((6 - i).toLong())
                Box(
                    Modifier.size(28.dp).clip(CircleShape)
                        .background(if (won) Spark else white.copy(alpha = 0.22f))
                        .then(if (i == 6) Modifier.border(2.dp, white, CircleShape) else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        d.dayOfWeek.getDisplayName(DayStyle.NARROW, Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall, color = if (won) Color(0xFF3B1D00) else white
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(if (daily.doneToday) R.string.daily_done else if (inProgress) R.string.daily_continue else R.string.daily_play),
            style = MaterialTheme.typography.titleMedium, color = white
        )
    }
}
