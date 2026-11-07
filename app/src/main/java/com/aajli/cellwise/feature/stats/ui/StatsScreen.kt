/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise.feature.stats.ui

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aajli.cellwise.R
import com.aajli.cellwise.core.formatTime
import com.aajli.cellwise.feature.daily.DailyInfo
import com.aajli.cellwise.feature.stats.GameRecord
import com.aajli.cellwise.feature.stats.LevelStats
import com.aajli.cellwise.feature.stats.StatsSummary
import com.aajli.cellwise.ui.ScreenHeader
import com.aajli.cellwise.ui.labelRes

private const val DASH = "–"

@Composable
fun StatsScreen(stats: StatsSummary, daily: DailyInfo, onBack: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(stringResource(R.string.stats_title), onBack)
        if (!stats.hasData) {
            Text(stringResource(R.string.stats_empty), Modifier.padding(24.dp), color = cs.onSurfaceVariant)
        } else {
            LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp)) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Tile(stringResource(R.string.stat_won), stats.overall.won.toString())
                        Tile(stringResource(R.string.stat_winrate), stats.overall.winRatePercent?.let { stringResource(R.string.stat_percent, it) } ?: DASH)
                        Tile(stringResource(R.string.stat_streak), stats.currentStreak.toString())
                        Tile(stringResource(R.string.stat_best_streak), stats.bestStreak.toString())
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(stringResource(R.string.daily_title), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Tile(stringResource(R.string.stat_streak), daily.streak.toString())
                        Tile(stringResource(R.string.stat_best_streak), daily.best.toString())
                        Tile(stringResource(R.string.daily_solved), daily.totalWon.toString())
                    }
                    Spacer(Modifier.height(20.dp))
                    LevelRow(stringResource(R.string.col_level), stringResource(R.string.col_won), stringResource(R.string.col_best),
                        stringResource(R.string.col_avg), stringResource(R.string.col_mistakes), header = true)
                    stats.perLevel.forEach { (d, s) -> LevelRow(stringResource(d.labelRes()), s) }
                    LevelRow(stringResource(R.string.col_total), stats.overall)
                    Spacer(Modifier.height(20.dp))
                    Text(stringResource(R.string.chart_title), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    BarChart(stats.winsPerDay.map { it.wins }, stats.winsPerDay.map { it.date.dayOfMonth.toString() })
                    Spacer(Modifier.height(20.dp))
                    Text(stringResource(R.string.history_title), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                }
                items(stats.history, key = { it.id }) { HistoryRow(it) }
            }
        }
    }
}

@Composable
private fun RowScope.Tile(label: String, value: String) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(cs.surfaceVariant.copy(alpha = 0.5f)).padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = cs.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
    }
}

@Composable
private fun LevelRow(c1: String, c2: String, c3: String, c4: String, c5: String, header: Boolean) {
    val style = if (header) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium
    val color = if (header) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(c1, Modifier.weight(1.3f), style = style, color = color)
        Text(c2, Modifier.weight(1f), style = style, color = color)
        Text(c3, Modifier.weight(1f), style = style, color = color)
        Text(c4, Modifier.weight(1f), style = style, color = color)
        Text(c5, Modifier.weight(1f), style = style, color = color)
    }
}

@Composable
private fun LevelRow(name: String, s: LevelStats) {
    LevelRow(
        name, "${s.won}/${s.played}", s.bestSec?.let { formatTime(it) } ?: DASH,
        s.avgSec?.let { formatTime(it) } ?: DASH, s.avgMistakes?.let { "%.1f".format(it) } ?: DASH, header = false
    )
}

@Composable
private fun HistoryRow(r: GameRecord) {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    val won = r.status == "WON"
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.history_line, stringResource(r.difficulty.labelRes()), stringResource(if (won) R.string.status_won else R.string.status_abandoned)),
                style = MaterialTheme.typography.bodyMedium, color = if (won) cs.primary else cs.onSurfaceVariant
            )
            Text(
                stringResource(R.string.history_detail, formatTime(r.elapsedSec), r.mistakes) +
                    (if (r.hints > 0) stringResource(R.string.history_hints, r.hints) else "") +
                    (if (r.daily) stringResource(R.string.history_daily) else ""),
                style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant
            )
        }
        Text(
            DateUtils.formatDateTime(ctx, r.endedAt, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_ALL),
            style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant
        )
    }
}
