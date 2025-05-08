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

class GitHubAiClient(private val token: String) {
    private val TAG = "GitHubAiClient"
    private val ENDPOINT = "https://models.github.ai/inference/chat/completions"
    private val MODEL = "openai/gpt-4.1"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    suspend fun getWeatherInsights(weatherContext: String, userMessage: String): String = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = """
                You are a helpful weather assistant. Use the following weather data to provide insights and answer questions:

                $weatherContext

                Provide concise, accurate and helpful responses based on this weather data.
            """.trimIndent()

            // Create request JSON
            val requestJson = JSONObject().apply {
                put("model", MODEL)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userMessage)
                    })
                })
                put("max_tokens", 500)
            }

            // Create request
            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(ENDPOINT)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            // Execute request
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext "Error ${response.code}: ${responseBody.take(100)}"
            }

            // Parse response
            val jsonResponse = JSONObject(responseBody)
            val choices = jsonResponse.getJSONArray("choices")
            if (choices.length() > 0) {
                val firstChoice = choices.getJSONObject(0)
                val message = firstChoice.getJSONObject("message")
                return@withContext message.getString("content")
            }

            "Sorry, I couldn't generate a response."
        } catch (e: Exception) {
            "I'm having trouble connecting to my knowledge base. Please try again later. Error: ${e.message}"
        }
    }
}