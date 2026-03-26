package com.skopje.onboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skopje.onboard.R
import com.skopje.onboard.data.Survey
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmittedSurveysScreen(
    surveys: List<Survey>,
    isSyncInProgress: Boolean,
    onBack: () -> Unit,
    onSyncNow: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.submitted_surveys_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OutlinedButton(
                onClick = onSyncNow,
                enabled = !isSyncInProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(stringResource(R.string.submitted_surveys_sync_now))
            }
            if (surveys.isEmpty()) {
                Text(
                    stringResource(R.string.submitted_surveys_empty),
                    modifier = Modifier.padding(24.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(surveys, key = { it.id }) { survey ->
                        SubmittedSurveyCard(survey = survey)
                    }
                }
            }
        }
    }
}

@Composable
private fun SubmittedSurveyCard(survey: Survey) {
    val loc = when {
        survey.latitude != null && survey.longitude != null ->
            String.format(Locale.US, "%.5f, %.5f", survey.latitude, survey.longitude)
        else -> stringResource(R.string.submitted_surveys_no_location)
    }
    val (bannerColor, onBannerColor) = if (survey.uploadedStatus) {
        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = bannerColor,
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        stringResource(R.string.submitted_surveys_server_status),
                        style = MaterialTheme.typography.labelMedium,
                        color = onBannerColor.copy(alpha = 0.85f),
                    )
                    Text(
                        if (survey.uploadedStatus) {
                            stringResource(R.string.survey_status_on_server)
                        } else {
                            stringResource(R.string.survey_status_pending_upload)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = onBannerColor,
                    )
                }
            }
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    stringResource(R.string.submitted_surveys_section_details),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    stringResource(R.string.submitted_surveys_id_format, survey.id),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(R.string.station_name_label, survey.stationName),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    stringResource(R.string.surveyor_id_label, survey.surveyorId),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(
                        R.string.submitted_surveys_times_format,
                        survey.startTime,
                        survey.submitTime,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(R.string.submitted_surveys_count_format, survey.passengerCount),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    stringResource(R.string.submitted_surveys_location_format, loc),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
