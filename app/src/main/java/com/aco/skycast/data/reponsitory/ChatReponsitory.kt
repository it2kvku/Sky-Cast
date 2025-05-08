package com.aco.skycast.data.reponsitory

import android.content.Context
import com.aco.skycast.ui.screens.ChatMessage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ChatRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("chat_history", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveMessages(messages: List<ChatMessage>) {
        val json = gson.toJson(messages)
        prefs.edit().putString("messages", json).apply()
    }

    fun loadMessages(): List<ChatMessage> {
        val json = prefs.getString("messages", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<ChatMessage>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearHistory() {
        prefs.edit().remove("messages").apply()
    }
}