/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aajli.cellwise.R

@Composable
fun ScreenHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onBack) { Text("← " + stringResource(R.string.back)) }
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 8.dp))
    }
}
