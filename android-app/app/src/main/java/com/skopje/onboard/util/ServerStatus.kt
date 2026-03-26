package com.skopje.onboard.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

suspend fun checkServerOnline(): Boolean = withContext(Dispatchers.IO) {
    val baseUrl = AppConfig.API_BASE_URL
    try {
        val url = URL("$baseUrl/api/health")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        val ok = conn.responseCode == 200
        conn.drainResponse()
        conn.disconnect()
        ok
    } catch (e: Exception) {
        false
    }
}
