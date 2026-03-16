package com.skopje.onboard.ui

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    onExit: () -> Unit,
) {
    val view = LocalView.current
    val context = LocalContext.current
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun doVibrate() {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(30)
            }
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.station_name_label, stationName),
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(R.string.surveyor_id_label, surveyorId),
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    )
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
                TextButton(
                    onClick = {
                        doVibrate()
                        onExit()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.exit_survey))
                }
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CounterButton(label = "+5", isPositive = true, sizeLarge = false) {
                        doVibrate()
                        onAdd(5)
                    }
                    CounterButton(label = "-5", isPositive = false, sizeLarge = false, enabled = passengerCount >= 5) {
                        doVibrate()
                        onAdd(-5)
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CounterButton(label = "+1", isPositive = true, sizeLarge = true) {
                        doVibrate()
                        onAdd(1)
                    }
                    CounterButton(label = "-1", isPositive = false, sizeLarge = true, enabled = passengerCount > 0) {
                        doVibrate()
                        onAdd(-1)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = {
                        doVibrate()
                        onReset()
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.secondary),
                ) {
                    Text(stringResource(R.string.reset), maxLines = 1)
                }
                Button(
                    onClick = {
                        doVibrate()
                        onSubmit()
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                ) {
                    Text(stringResource(R.string.submit), maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun CounterButton(
    label: String,
    isPositive: Boolean,
    sizeLarge: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val containerColor = if (isPositive) Color(0xFF66BB6A) else Color(0xFFE57373)
    val height = if (sizeLarge) 84.dp else 60.dp
    val fontSize = if (sizeLarge) 28.sp else 20.sp
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .height(height)
            .padding(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            disabledContainerColor = containerColor.copy(alpha = 0.4f),
        ),
    ) {
        Text(label, fontSize = fontSize)
    }
}
