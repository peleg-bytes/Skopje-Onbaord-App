package com.skopje.onboard.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.skopje.onboard.R

@Composable
fun ResumeDialog(
    onResume: () -> Unit,
    onDiscard: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { },
        title = { Text(stringResource(R.string.resume_previous)) },
        text = { Text(stringResource(R.string.resume_previous)) },
        confirmButton = {
            TextButton(onClick = onResume) {
                Text(stringResource(R.string.resume))
            }
        },
        dismissButton = {
            TextButton(onClick = onDiscard) {
                Text(stringResource(R.string.discard))
            }
        },
    )
}
