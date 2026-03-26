package com.skopje.onboard.util

import java.net.HttpURLConnection

/**
 * Fully reads the HTTP response so the connection can be reused and Android does not hang.
 */
fun HttpURLConnection.drainResponse() {
    try {
        val stream = if (responseCode in 200..299) inputStream else errorStream
        stream?.bufferedReader()?.use { it.readText() }
    } catch (_: Exception) {
        // ignore
    }
}
