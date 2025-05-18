package com.aco.skycast.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiAiClient(private val apiKey: String) {
    private val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    suspend fun getWeatherInsights(weatherContext: String, userMessage: String): String = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                You are a helpful weather assistant. Use the following weather data to provide insights and answer questions:

                $weatherContext

                User question: $userMessage
                
                Provide concise, accurate and helpful responses based on this weather data.
            """.trimIndent()

            // Create request JSON according to Gemini API format
            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
            }

            // Create request with API key in URL
            val url = "$BASE_URL?key=$apiKey"
            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            // Execute request
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext "Error ${response.code}: ${responseBody.take(100)}"
            }

            // Parse Gemini response format
            val jsonResponse = JSONObject(responseBody)
            if (jsonResponse.has("candidates")) {
                val candidates = jsonResponse.getJSONArray("candidates")
                if (candidates.length() > 0) {
                    val content = candidates.getJSONObject(0).getJSONObject("content")
                    val parts = content.getJSONArray("parts")
                    if (parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).getString("text")
                    }
                }
            }

            "Sorry, I couldn't generate a response."
        } catch (e: Exception) {
            "I'm having trouble connecting to my knowledge base. Please try again later. Error: ${e.message}"
        }
    }
}