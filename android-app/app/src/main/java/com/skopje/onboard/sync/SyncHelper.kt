package com.skopje.onboard.sync

import android.content.Context
import com.skopje.onboard.data.AppDatabase
import com.skopje.onboard.util.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object SyncHelper {

    sealed class SyncResult {
        data object Success : SyncResult()
        data object Failed : SyncResult()
    }

    /**
     * Uploads all unuploaded surveys to the backend. Runs on IO dispatcher.
     * Returns Success if at least one survey was uploaded, Failed if none (e.g. offline).
     */
    suspend fun syncNow(context: Context): SyncResult = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        val dao = db.surveyDao()
        val baseUrl = AppConfig.API_BASE_URL.trimEnd('/')
        val unuploaded = dao.getUnuploadedSubmittedSurveys()
        var anyUploaded = false

        for (survey in unuploaded) {
            try {
                val url = URL("$baseUrl/api/submit-survey")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

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

                if (conn.responseCode in 200..299) {
                    dao.markUploaded(survey.id)
                    anyUploaded = true
                }
            } catch (e: Exception) {
                // Will retry via WorkManager
            }
        }
        if (anyUploaded) SyncResult.Success else SyncResult.Failed
    }
}
