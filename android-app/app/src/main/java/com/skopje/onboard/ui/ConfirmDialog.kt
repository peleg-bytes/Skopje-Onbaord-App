package com.skopje.onboard.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.skopje.onboard.R

@Composable
fun ResetConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reset_counter)) },
        text = { Text(stringResource(R.string.reset_confirm)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.reset))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
fun SubmitConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isSubmitting: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text(stringResource(R.string.done_submit)) },
        text = { Text(stringResource(R.string.submit_confirm)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isSubmitting,
            ) {
                Text(if (isSubmitting) stringResource(R.string.submitting) else stringResource(R.string.submit))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
fun ExitSurveyConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.exit_survey)) },
        text = { Text(stringResource(R.string.exit_survey_confirm)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.exit_survey), color = androidx.compose.material3.MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
