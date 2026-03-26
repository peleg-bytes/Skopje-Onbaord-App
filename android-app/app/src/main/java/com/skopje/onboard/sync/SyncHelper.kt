package com.skopje.onboard.sync

import android.content.Context
import com.skopje.onboard.data.AppDatabase
import com.skopje.onboard.util.AppConfig
import com.skopje.onboard.util.drainResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import com.skopje.onboard.data.Survey
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object SyncHelper {

    /** Only one bulk upload runs at a time (periodic work, startup, etc.); avoids duplicate work and DB contention. */
    private val bulkSyncMutex = Mutex()

    sealed class SyncResult {
        data object Success : SyncResult()
        data object Failed : SyncResult()
    }

    /**
     * Single fast upload attempt (short timeouts). Marks [Survey.uploadedStatus] on HTTP success.
     * Not guarded by [bulkSyncMutex] so the submit flow never waits on a long background queue sync.
     */
    suspend fun tryUploadSurveyQuick(context: Context, survey: Survey): Boolean =
        withContext(Dispatchers.IO) {
            uploadSurveyWithTimeouts(context, survey, connectMs = 3000, readMs = 4000)
        }

    /**
     * Uploads **every** submitted-but-not-uploaded survey in one pass (sequential POSTs).
     * Runs on [Dispatchers.IO] only; safe to call from WorkManager or ViewModel without blocking the UI thread.
     * Concurrent callers are serialized so two workers do not run the same queue in parallel.
     */
    suspend fun syncNow(context: Context): SyncResult = bulkSyncMutex.withLock {
        withContext(Dispatchers.IO) {
            val dao = AppDatabase.getInstance(context).surveyDao()
            var anyUploaded = false
            // Re-fetch so surveys submitted while this job runs are uploaded in the same pass.
            // Stop if nothing changed (all remaining failed offline) to avoid spinning forever.
            while (true) {
                val batch = dao.getUnuploadedSubmittedSurveys()
                if (batch.isEmpty()) break
                val idsBefore = batch.map { it.id }.toSet()
                for (survey in batch) {
                    if (uploadSurveyWithTimeouts(context, survey, connectMs = 6000, readMs = 10000)) {
                        anyUploaded = true
                    }
                }
                val after = dao.getUnuploadedSubmittedSurveys()
                if (after.isEmpty()) break
                val idsAfter = after.map { it.id }.toSet()
                if (idsAfter == idsBefore) break
            }
            if (anyUploaded) SyncResult.Success else SyncResult.Failed
        }
    }

    private suspend fun uploadSurveyWithTimeouts(
        context: Context,
        survey: Survey,
        connectMs: Int,
        readMs: Int,
    ): Boolean {
        val dao = AppDatabase.getInstance(context).surveyDao()
        val baseUrl = AppConfig.API_BASE_URL
        var conn: HttpURLConnection? = null
        return try {
            val url = URL("$baseUrl/api/submit-survey")
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = connectMs
            conn.readTimeout = readMs

            val body = JSONObject().apply {
                put("surveyorId", survey.surveyorId)
                put("stationName", survey.stationName)
                put("startTime", survey.startTime)
                put("submitTime", survey.submitTime)
                put("latitude", survey.latitude ?: JSONObject.NULL)
                put("longitude", survey.longitude ?: JSONObject.NULL)
                put("passengerCount", survey.passengerCount)
            }
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            conn.drainResponse()
            if (code in 200..299) {
                dao.markUploaded(survey.id)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        } finally {
            conn?.disconnect()
        }
    }
}
