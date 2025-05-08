package com.aco.skycast.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.aco.skycast.BuildConfig
import com.aco.skycast.data.api.GitHubAiClient
import com.aco.skycast.data.model.WeatherUiState
import com.aco.skycast.data.model.WeatherViewModel
import com.aco.skycast.data.reponsitory.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
data class ChatMessage(
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun ChatBotDaily(
    viewModel: WeatherViewModel,
    onBackPressed: () -> Unit
) {
    val weatherState by viewModel.weatherState.collectAsState()
    val scope = rememberCoroutineScope()
    // Don't hardcode tokens in source code
    val token = BuildConfig.GITHUB_TOKEN
    val gitHubAiClient = remember { GitHubAiClient(token) }
    val context = LocalContext.current
    val chatRepository = remember { ChatRepository(context) }

    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var messageInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scrollState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    // Load previous messages
    LaunchedEffect(Unit) {
        val savedMessages = chatRepository.loadMessages()
        if (savedMessages.isNotEmpty()) {
            messages = savedMessages
        } else {
            messages = listOf(ChatMessage(
                content = "Hello! I'm your weather assistant. How can I help you with today's weather?",
                isFromUser = false
            ))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp)
    ) {
        // Top app bar with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackPressed) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Weather Assistant",
                style = MaterialTheme.typography.titleLarge
            )
        }

        // Messages area
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = scrollState,
            reverseLayout = false,
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages) { message ->
                MessageBubble(message)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Weather data status
        when (weatherState) {
            is WeatherUiState.Success -> {
                // Weather data is available and can be used for chat context
            }
            is WeatherUiState.Loading -> {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }
            is WeatherUiState.Error -> {
                Text(
                    text = "Couldn't load weather data: ${(weatherState as WeatherUiState.Error).message}",
                    color = Color.Red,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        // Input area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageInput,
                onValueChange = { messageInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (messageInput.isNotBlank()) {
                            sendMessage(
                                messageInput,
                                messages,
                                { messages = it },
                                weatherState,
                                gitHubAiClient,
                                { isLoading = it },
                                scope,
                                focusManager,
                                chatRepository
                            )
                            messageInput = ""
                        }
                    }
                ),
                singleLine = true,
                shape = RoundedCornerShape(24.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (messageInput.isNotBlank()) {
                        sendMessage(
                            messageInput,
                            messages,
                            { messages = it },
                            weatherState,
                            gitHubAiClient,
                            { isLoading = it },
                            scope,
                            focusManager,
                            chatRepository
                        )
                        messageInput = ""
                    }
                },
                enabled = !isLoading && messageInput.isNotBlank(),
                contentPadding = PaddingValues(12.dp),
                shape = CircleShape
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Send, contentDescription = "Send")
                }
            }
        }
    }

    // Scroll to bottom when new message is added
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scrollState.animateScrollToItem(messages.size - 1)
        }
    }
}

private fun getWeatherContext(weatherState: WeatherUiState): String {
    return when (weatherState) {
        is WeatherUiState.Success -> {
            val weather = weatherState.data
            val currentDay = weather.days.firstOrNull()
            """
            Current location: ${weather.resolvedAddress}
            Date: ${currentDay?.datetime ?: "Unknown"}
            Temperature: ${currentDay?.temp ?: "Unknown"}°C
            Feels like: ${currentDay?.feelslike ?: "Unknown"}°C
            Conditions: ${currentDay?.conditions ?: "Unknown"}
            Humidity: ${currentDay?.humidity ?: "Unknown"}%
            Wind: ${currentDay?.windspeed ?: "Unknown"} km/h
            Precipitation: ${currentDay?.precip ?: "Unknown"} mm
            UV Index: ${currentDay?.uvindex ?: "Unknown"}
            Sunrise: ${currentDay?.sunrise ?: "Unknown"}
            Sunset: ${currentDay?.sunset ?: "Unknown"}
            """
        }
        is WeatherUiState.Loading -> "Loading weather data..."
        is WeatherUiState.Error -> "Weather data not available: ${(weatherState as WeatherUiState.Error).message}"
    }
}

private fun sendMessage(
    message: String,
    currentMessages: List<ChatMessage>,
    updateMessages: (List<ChatMessage>) -> Unit,
    weatherState: WeatherUiState,
    gitHubAiClient: GitHubAiClient,
    setLoading: (Boolean) -> Unit,
    scope: CoroutineScope,
    focusManager: androidx.compose.ui.focus.FocusManager,
    chatRepository: ChatRepository
) {
    // Add user message
    val userMessage = ChatMessage(content = message, isFromUser = true)
    val updatedMessages = currentMessages + userMessage
    updateMessages(updatedMessages)

    // Save messages right after adding user message
    chatRepository.saveMessages(updatedMessages)

    // Clear focus from text field
    focusManager.clearFocus()

    // Get AI response
    scope.launch {
        setLoading(true)
        try {
            val weatherContext = getWeatherContext(weatherState)

            // Use the GitHub AI service instead of local fallback
            val response = gitHubAiClient.getWeatherInsights(weatherContext, message)

            // Add AI response
            val aiMessage = ChatMessage(content = response, isFromUser = false)
            val finalMessages = updatedMessages + aiMessage
            updateMessages(finalMessages)
            chatRepository.saveMessages(finalMessages)
        } catch (e: Exception) {
            // Add error message
            val errorMessage = ChatMessage(
                content = "Sorry, I couldn't process your request: ${e.message}",
                isFromUser = false
            )
            val finalMessages = updatedMessages + errorMessage
            updateMessages(finalMessages)
            chatRepository.saveMessages(finalMessages)
        } finally {
            setLoading(false)
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val backgroundColor = if (message.isFromUser) Color(0xFF1976D2) else Color.White
    val textColor = if (message.isFromUser) Color.White else Color.Black
    val alignment = if (message.isFromUser) Alignment.CenterEnd else Alignment.CenterStart
    val shape = if (message.isFromUser)
        RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)
    else
        RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Column(horizontalAlignment = if (message.isFromUser) Alignment.End else Alignment.Start) {
            Card(
                shape = shape,
                colors = CardDefaults.cardColors(containerColor = backgroundColor),
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Text(
                    text = message.content,
                    color = textColor,
                    modifier = Modifier.padding(12.dp)
                )
            }

            // Add timestamp
            Text(
                text = formatTimestamp(message.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 2.dp)
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}