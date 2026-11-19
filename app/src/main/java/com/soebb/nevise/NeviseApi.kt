package com.soebb.nevise

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Client for the Nevise Gradio Space.
 * Gradio expects input values in the "data" array.
 */
object NeviseApi {
    private const val BASE_URL = "https://soheybsamadi-nevise-api.hf.space"
    private const val CONNECT_TIMEOUT_MS = 30_000
    private const val READ_TIMEOUT_MS = 180_000

    suspend fun correctText(text: String): String = withContext(Dispatchers.IO) {
        val eventId = submitText(text)
        readResult(eventId)
    }

    private fun submitText(text: String): String {
        val connection = (URL("$BASE_URL/gradio_api/call/correct_text").openConnection()
                as HttpURLConnection)

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")

            // Gradio call endpoints expect: {"data": ["input text"]}
            val body = JSONObject()
                .put("data", JSONArray().put(text))
                .toString()

            connection.outputStream.use { output ->
                output.write(body.toByteArray(Charsets.UTF_8))
            }

            val status = connection.responseCode
            val response = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()

            if (status !in 200..299) {
                throw IllegalStateException("ارسال متن ناموفق بود (HTTP $status): $response")
            }

            return JSONObject(response).getString("event_id")
        } finally {
            connection.disconnect()
        }
    }

    private fun readResult(eventId: String): String {
        val connection = (URL("$BASE_URL/gradio_api/call/correct_text/$eventId").openConnection()
                as HttpURLConnection)

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.setRequestProperty("Accept", "text/event-stream")

            val status = connection.responseCode
            if (status !in 200..299) {
                val error = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    .orEmpty()
                throw IllegalStateException("دریافت نتیجه ناموفق بود (HTTP $status): $error")
            }

            connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                var eventName = ""
                val data = StringBuilder()

                while (true) {
                    val line = reader.readLine() ?: break

                    when {
                        line.startsWith("event:") -> eventName = line.substringAfter("event:").trim()
                        line.startsWith("data:") -> {
                            if (data.isNotEmpty()) data.append('\n')
                            data.append(line.substringAfter("data:").trim())
                        }
                        line.isEmpty() -> {
                            if (eventName == "complete") {
                                val outputs = JSONArray(data.toString())
                                return outputs.getString(0)
                            }
                            if (eventName == "error") {
                                throw IllegalStateException("مدل نتوانست متن را پردازش کند: $data")
                            }
                            eventName = ""
                            data.setLength(0)
                        }
                    }
                }
            }

            throw IllegalStateException("پاسخ نهایی از سرور دریافت نشد.")
        } finally {
            connection.disconnect()
        }
    }
}
