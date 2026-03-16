package com.skopje.onboard.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skopje.onboard.R
import com.skopje.onboard.util.GpsStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountingScreen(
    passengerCount: Int,
    stationName: String,
    surveyorId: String,
    gpsStatus: GpsStatus,
    serverOnline: Boolean,
    onAdd: (Int) -> Unit,
    onReset: () -> Unit,
    onSubmit: () -> Unit,
    onVibrate: () -> Unit,
) {
    val view = LocalView.current

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(stationName, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                Text(surveyorId, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                Text(
                    when (gpsStatus) {
                        GpsStatus.ACQUIRING -> stringResource(R.string.gps_acquiring)
                        GpsStatus.LOCKED -> stringResource(R.string.gps_locked)
                        GpsStatus.UNAVAILABLE -> stringResource(R.string.gps_unavailable)
                    },
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                )
                Text(
                    if (serverOnline) stringResource(R.string.server_online) else stringResource(R.string.server_offline),
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$passengerCount",
                fontSize = 96.sp,
                textAlign = TextAlign.Center,
            )
            Text(stringResource(R.string.count), style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                CounterButton(label = "+5") {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onAdd(5)
                }
                CounterButton(label = "+1") {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onAdd(1)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                CounterButton(label = "-1", enabled = passengerCount > 0) {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onAdd(-1)
                }
                CounterButton(label = "-5", enabled = passengerCount >= 5) {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onAdd(-5)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Button(
                    onClick = onReset,
                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.secondary),
                ) {
                    Text(stringResource(R.string.reset_counter))
                }
                Button(onClick = onSubmit) {
                    Text(stringResource(R.string.done_submit))
                }
            }
        }
    }
}

@Composable
private fun CounterButton(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .height(72.dp)
            .padding(8.dp),
    ) {
        Text(label, fontSize = 24.sp)
    }
}
