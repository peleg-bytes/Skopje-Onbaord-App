package com.skopje.onboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.skopje.onboard.data.Survey
import com.skopje.onboard.R

private val ContinueSurveyGreen = Color(0xFF2E7D32)
private val OnContinueSurveyGreen = Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartSurveyScreen(
    stationName: String,
    surveyorId: String,
    isSyncInProgress: Boolean,
    startSurveyEnabled: Boolean,
    homeDraft: Survey?,
    onStationNameChange: (String) -> Unit,
    onSurveyorIdChange: (String) -> Unit,
    onContinueSurvey: () -> Unit,
    onStartSurvey: () -> Unit,
    onSyncNow: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSubmittedSurveys: () -> Unit,
) {
    val canContinue = startSurveyEnabled && (homeDraft?.isContinuableDraft() == true)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
                actions = {
                    TextButton(
                        onClick = onOpenSubmittedSurveys,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Text(
                            stringResource(R.string.submissions_button),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            FilledTonalButton(
                onClick = onSyncNow,
                enabled = !isSyncInProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (isSyncInProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .height(22.dp)
                            .width(22.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                } else {
                    Icon(Icons.Filled.Sync, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(stringResource(R.string.start_screen_sync_now))
            }
            Text(
                stringResource(R.string.start_screen_sync_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp),
            )
            Spacer(modifier = Modifier.height(28.dp))
            OutlinedTextField(
                value = stationName,
                onValueChange = onStationNameChange,
                label = { Text(stringResource(R.string.station_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = surveyorId,
                onValueChange = onSurveyorIdChange,
                label = { Text(stringResource(R.string.surveyor_id)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onContinueSurvey,
                enabled = canContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ContinueSurveyGreen,
                    contentColor = OnContinueSurveyGreen,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = if (canContinue) 4.dp else 0.dp,
                    pressedElevation = if (canContinue) 6.dp else 0.dp,
                    disabledElevation = 0.dp,
                ),
            ) {
                Text(
                    stringResource(R.string.continue_survey),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onStartSurvey,
                enabled = startSurveyEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                if (!startSurveyEnabled) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .height(22.dp)
                            .width(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(stringResource(R.string.start_survey), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
