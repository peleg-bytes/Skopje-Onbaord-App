package com.skopje.onboard.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

suspend fun checkServerOnline(baseUrl: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val url = URL("$baseUrl/api/health")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        conn.responseCode == 200
    } catch (e: Exception) {
        false
    }
}
