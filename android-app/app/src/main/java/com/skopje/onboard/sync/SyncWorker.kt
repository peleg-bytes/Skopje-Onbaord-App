package com.skopje.onboard.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.skopje.onboard.data.AppDatabase
import com.skopje.onboard.util.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class SyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(applicationContext)
        val prefs = Preferences(applicationContext)
        val baseUrl = prefs.getApiUrl().trimEnd('/')
        val dao = db.surveyDao()
        val unuploaded = dao.getUnuploadedSubmittedSurveys()

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
                }
            } catch (e: Exception) {
                return@withContext Result.retry()
            }
        }
        Result.success()
    }
}
