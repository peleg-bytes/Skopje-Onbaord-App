package com.skopje.onboard.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.skopje.onboard.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    language: String,
    theme: String,
    onLanguageChange: (String) -> Unit,
    onThemeChange: (String) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
        ) {
            Text(stringResource(R.string.language), style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            listOf(
                "mk" to stringResource(R.string.language_option_mk),
                "en" to stringResource(R.string.language_option_en),
            ).forEach { (value, label) ->
                Row(Modifier.fillMaxWidth().clickable { onLanguageChange(value) }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = language == value, onClick = { onLanguageChange(value) })
                    Text(label, modifier = Modifier.padding(start = 8.dp))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            Text(stringResource(R.string.theme), style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            listOf(
                "light" to stringResource(R.string.light_mode),
                "dark" to stringResource(R.string.dark_mode),
                "system" to stringResource(R.string.theme_system),
            ).forEach { (value, label) ->
                Row(Modifier.fillMaxWidth().clickable { onThemeChange(value) }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = theme == value, onClick = { onThemeChange(value) })
                    Text(label, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}
