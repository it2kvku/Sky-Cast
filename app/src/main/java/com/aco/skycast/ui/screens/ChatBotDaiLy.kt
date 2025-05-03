package com.aco.skycast.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aco.skycast.data.model.WeatherViewModel
import kotlinx.coroutines.launch

// Define a solid color to use where Color is expected
private val BlueSolidColor = Color(0xFF1976D2)

data class ChatMessage(
    val content: String,
    val isFromUser: Boolean,
    val timestamp: String = getCurrentTime()
)

private fun getCurrentTime(): String {
    val calendar = java.util.Calendar.getInstance()
    val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
    val minute = calendar.get(java.util.Calendar.MINUTE)
    return String.format("%02d:%02d", hour, minute)
}

@Composable
fun ChatBotDaily(viewModel: WeatherViewModel = viewModel(), onBackPressed: () -> Boolean) {
    val chatMessages = remember { mutableStateListOf(
        ChatMessage("Hello! I'm your weather assistant. How can I help you today?", false),
        ChatMessage("Would you like to know today's forecast or get recommendations for activities?", false)
    )}

    var userInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Chat header
        ChatHeader()

        // Messages area
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            reverseLayout = false
        ) {
            items(chatMessages) { message ->
                ChatMessageItem(message)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Quick suggestions
        SuggestionChips(
            suggestions = listOf("Today's weather", "Weather alerts", "Activity suggestions"),
            onSuggestionClick = { suggestion ->
                chatMessages.add(ChatMessage(suggestion, true))
                coroutineScope.launch {
                    // Simulate response delay
                    kotlinx.coroutines.delay(500)

                    val response = when (suggestion) {
                        "Today's weather" -> "Today will be partly cloudy with temperatures around 24°C. There's a 10% chance of rain in the afternoon."
                        "Weather alerts" -> "There are no active weather alerts for your area at the moment."
                        "Activity suggestions" -> "Based on today's weather forecast, it's a great day for outdoor activities like hiking or gardening."
                        else -> "I'm not sure how to respond to that. Could you rephrase your question?"
                    }
                    chatMessages.add(ChatMessage(response, false))
                    listState.animateScrollToItem(chatMessages.size - 1)
                }
            }
        )

        // Input area
        ChatInputField(
            value = userInput,
            onValueChange = { userInput = it },
            onSendClick = {
                if (userInput.isNotBlank()) {
                    chatMessages.add(ChatMessage(userInput, true))
                    val userQuestion = userInput
                    userInput = ""

                    coroutineScope.launch {
                        listState.animateScrollToItem(chatMessages.size - 1)

                        // Simulate response delay
                        kotlinx.coroutines.delay(800)

                        val response = generateResponse(userQuestion)
                        chatMessages.add(ChatMessage(response, false))
                        listState.animateScrollToItem(chatMessages.size - 1)
                    }
                }
            }
        )
    }
}

// Generates a contextual response based on user input
private fun generateResponse(userInput: String): String {
    val input = userInput.lowercase()

    return when {
        input.contains("rain") || input.contains("raining") ->
            "Based on the forecast, there's a low chance of rain today. You should be fine without an umbrella."

        input.contains("temperature") || input.contains("hot") || input.contains("cold") ->
            "The current temperature is 24°C (75°F). The high today will be 26°C with a low of 18°C tonight."

        input.contains("forecast") || input.contains("weather") ->
            "Today's forecast shows partly cloudy skies with a high of 26°C. Tomorrow will be sunny with similar temperatures."

        input.contains("hello") || input.contains("hi") ->
            "Hello! How can I help with your weather questions today?"

        input.contains("thank") ->
            "You're welcome! Is there anything else you'd like to know about the weather?"

        input.contains("activity") || input.contains("do today") || input.contains("recommend") ->
            "Based on today's pleasant weather, I recommend outdoor activities like hiking, cycling, or having a picnic in the park."

        else -> "I'm not sure I understand your question. Could you rephrase it or ask about today's weather, rain chances, or activity recommendations?"
    }
}

@Composable
fun ChatHeader() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = BlueSolidColor, // Changed from BlueGradient to a solid color
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Weather Assistant",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Ask me about weather and activity recommendations",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isFromUser) {
            // Bot icon
            Surface(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape),
                color = BlueSolidColor // Changed from BlueGradient to a solid color
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "🤖",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (message.isFromUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                color = if (message.isFromUser) BlueSolidColor else Color.LightGray.copy(alpha = 0.2f), // Changed from BlueGradient to a solid color
                shape = RoundedCornerShape(
                    topStart = if (message.isFromUser) 16.dp else 4.dp,
                    topEnd = if (message.isFromUser) 4.dp else 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                )
            ) {
                Text(
                    text = message.content,
                    modifier = Modifier.padding(12.dp),
                    color = if (message.isFromUser) Color.White else Color.Black
                )
            }

            Text(
                text = message.timestamp,
                fontSize = 10.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (message.isFromUser) {
            Spacer(modifier = Modifier.width(8.dp))
            // User icon
            Surface(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape),
                color = Color.LightGray
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "👤",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ChatInputField(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text("Type a message...") },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp)),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.LightGray.copy(alpha = 0.1f),
                    unfocusedContainerColor = Color.LightGray.copy(alpha = 0.1f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSendClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(BlueSolidColor), // Changed from BlueGradient to a solid color
                enabled = value.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun SuggestionChips(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.forEach { suggestion ->
            SuggestionChip(
                onClick = { onSuggestionClick(suggestion) },
                label = { Text(suggestion) },
                modifier = Modifier.weight(1f, false),
                shape = RoundedCornerShape(16.dp),
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = Color.LightGray.copy(alpha = 0.2f)
                )
            )
        }
    }
}