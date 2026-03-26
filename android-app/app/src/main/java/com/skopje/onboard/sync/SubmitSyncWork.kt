package com.skopje.onboard.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

private const val AFTER_SUBMIT_WORK = "skopje_sync_after_submit"

/**
 * Retries upload when the device has a network connection (Wi‑Fi or mobile data).
 * [ExistingWorkPolicy.KEEP] avoids cancelling an in-flight sync when the user submits several surveys quickly;
 * one run of [SyncWorker] uploads the full pending queue.
 */
fun enqueueSubmitFollowUpSync(context: Context) {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
    val request = OneTimeWorkRequestBuilder<SyncWorker>()
        .setConstraints(constraints)
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
        .build()
    WorkManager.getInstance(context).enqueueUniqueWork(
        AFTER_SUBMIT_WORK,
        ExistingWorkPolicy.KEEP,
        request,
    )
}
