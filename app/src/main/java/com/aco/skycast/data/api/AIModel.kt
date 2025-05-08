package com.aco.skycast.data.api

import com.azure.ai.openai.models.ChatCompletionsOptions
import com.azure.ai.openai.models.ChatMessage
import com.azure.ai.openai.models.ChatRole

object AIModel {
    fun systemMessage(prompt: String = ""): ChatMessage {
        val message = ChatMessage(ChatRole.SYSTEM)
        message.content = prompt
        return message
    }

    /** User message */
    fun userMessage(content: String): ChatMessage {
        val message = ChatMessage(ChatRole.USER)
        message.content = content
        return message
    }

    /** Wrap into options */
    fun makeOptions(messages: List<ChatMessage>): ChatCompletionsOptions {
        return ChatCompletionsOptions(messages)
            .setModel("openai/gpt-4o")
    }
}