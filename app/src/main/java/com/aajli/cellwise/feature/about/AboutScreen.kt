/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise.feature.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aajli.cellwise.BuildConfig
import com.aajli.cellwise.R
import com.aajli.cellwise.core.CoreInfo
import com.aajli.cellwise.ui.CellwiseLogo
import com.aajli.cellwise.ui.ScreenHeader

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val uri = LocalUriHandler.current
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader(stringResource(R.string.about_title), onBack)
        CellwiseLogo(88.dp, Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(8.dp))
        Column(Modifier.padding(horizontal = 24.dp)) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, color = cs.primary)
            Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            Text(CoreInfo.COPYRIGHT, style = MaterialTheme.typography.bodyLarge)
            TextButton(onClick = { uri.openUri("mailto:" + CoreInfo.EMAIL) }) { Text(stringResource(R.string.about_email) + ": " + CoreInfo.EMAIL) }
            TextButton(onClick = { uri.openUri(CoreInfo.GITHUB) }) { Text(stringResource(R.string.about_github) + ": github.com/aajli-Abdallah") }
            TextButton(onClick = { uri.openUri(CoreInfo.WEBSITE) }) { Text(stringResource(R.string.about_website) + ": aajli.com") }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.about_licenses_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.about_licenses_body), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        }
    }
}
