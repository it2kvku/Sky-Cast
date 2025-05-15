package com.aco.skycast.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aco.skycast.BuildConfig
import com.aco.skycast.data.api.GitHubAiClient
import com.aco.skycast.data.model.WeatherUiState
import com.aco.skycast.data.model.WeatherViewModel
import com.aco.skycast.data.reponsitory.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.focus.FocusManager

data class ChatMessage(
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatBotDaily(
    viewModel: WeatherViewModel,
    onBackPressed: () -> Unit
) {
    val weatherState by viewModel.weatherState.collectAsState()
    val scope = rememberCoroutineScope()
    val token = BuildConfig.GITHUB_TOKEN
    val gitHubAiClient = remember { GitHubAiClient(token) }
    val context = LocalContext.current
    val chatRepository = remember { ChatRepository(context) }

    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var messageInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scrollState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    // Quick reply suggestions
    val quickReplies = remember {
        listOf(
            "What's the weather like today?",
            "Should I bring an umbrella?",
            "What should I wear today?"
        )
    }

    // Load previous messages
    LaunchedEffect(Unit) {
        val savedMessages = chatRepository.loadMessages()
        if (savedMessages.isNotEmpty()) {
            messages = savedMessages
        } else {
            messages = listOf(
                ChatMessage(
                    content = "Hello! I'm your weather assistant. How can I help you with today's weather?",
                    isFromUser = false
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.CloudSync,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Weather Assistant",
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (weatherState is WeatherUiState.Success) {
                                val city = (weatherState as WeatherUiState.Success).data.resolvedAddress
                                    .split(",").firstOrNull()?.trim() ?: ""
                                Text(
                                    text = city,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Weather status card (if loading or error)
            when (weatherState) {
                is WeatherUiState.Loading -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                is WeatherUiState.Error -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = "Unable to load weather data. Chat functionality may be limited.",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                else -> { /* No indicator needed */ }
            }

            // Messages area
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = scrollState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages.indices.toList()) { index ->
                    AnimatedMessageBubble(message = messages[index], index = index)
                }

                // Typing indicator when loading
                if (isLoading) {
                    item {
                        TypingIndicator()
                    }
                }
            }

            // Quick reply chips (show only for new users or empty chat)
            if (messages.size <= 2) {
                QuickReplyChips(
                    suggestions = quickReplies,
                    onSuggestionSelected = { suggestion ->
                        messageInput = suggestion
                        sendChatMessage(
                            message = messageInput,
                            currentMessages = messages,
                            updateMessages = { messages = it },
                            weatherState = weatherState,
                            gitHubAiClient = gitHubAiClient,
                            setLoading = { isLoading = it },
                            scope = scope,
                            focusManager = focusManager,
                            chatRepository = chatRepository,
                            viewModel = viewModel  // Add this parameter
                        )
                        messageInput = ""
                    }
                )
            }

            // Input area
            ChatInputField(
                value = messageInput,
                onValueChange = { messageInput = it },
                onSend = {
                    if (messageInput.isNotBlank()) {
                        sendChatMessage(
                            message = messageInput,
                            currentMessages = messages,
                            updateMessages = { messages = it },
                            weatherState = weatherState,
                            gitHubAiClient = gitHubAiClient,
                            setLoading = { isLoading = it },
                            scope = scope,
                            focusManager = focusManager,
                            chatRepository = chatRepository,
                            viewModel = viewModel  // Add this parameter
                        )
                        messageInput = ""
                    }
                },
                isLoading = isLoading
            )
        }
    }

    // Scroll to bottom when new message is added
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            scrollState.animateScrollToItem(messages.size - 1)
        }
    }
}

private fun sendChatMessage(
    message: String,
    currentMessages: List<ChatMessage>,
    updateMessages: (List<ChatMessage>) -> Unit,
    weatherState: WeatherUiState,
    gitHubAiClient: GitHubAiClient,
    setLoading: (Boolean) -> Unit,
    scope: CoroutineScope,
    focusManager: FocusManager,
    chatRepository: ChatRepository,
    viewModel: WeatherViewModel
) {
    // Add user message
    val userMessage = ChatMessage(content = message, isFromUser = true)
    val updatedMessages = currentMessages + userMessage
    updateMessages(updatedMessages)

    // Clear focus from input field
    focusManager.clearFocus()

    // Set loading state
    setLoading(true)

    scope.launch {
        try {
            // Get current city directly from viewModel without using collectAsState
            val ipCity = viewModel.ipCity.value

            // Get weather data to include in prompt with improved location information
            val weatherInfo = when (weatherState) {
                is WeatherUiState.Success -> {
                    val data = weatherState.data

                    // Use ipCity if available, otherwise fall back to resolvedAddress
                    val locationName = if (!ipCity.isNullOrBlank()) {
                        ipCity
                    } else {
                        data.resolvedAddress.split(",").firstOrNull()?.trim() ?: data.resolvedAddress
                    }

                    "Current weather in $locationName:\n" +
                            "Temperature: ${data.days.firstOrNull()?.temp ?: "N/A"}°C\n" +
                            "Conditions: ${data.days.firstOrNull()?.conditions ?: "Unknown"}\n" +
                            "Humidity: ${data.days.firstOrNull()?.humidity ?: "N/A"}%\n" +
                            "Wind: ${data.days.firstOrNull()?.windspeed ?: "N/A"} km/h\n" +
                            "Description: ${data.days.firstOrNull()?.description ?: "No description available"}"
                }
                else -> "Weather data is not available."
            }

            // Get response from AI
            val response = gitHubAiClient.getWeatherInsights(weatherInfo, message)

            val botMessage = ChatMessage(
                content = response.trim(),
                isFromUser = false
            )

            val newMessages = updatedMessages + botMessage
            updateMessages(newMessages)

            chatRepository.saveMessages(newMessages)
        } catch (e: Exception) {
            val errorMessage = ChatMessage(
                content = "Sorry, I couldn't process your request. ${e.message ?: "Unknown error"}",
                isFromUser = false
            )
            updateMessages(updatedMessages + errorMessage)
        } finally {
            setLoading(false)
        }
    }
}
@Composable
fun AnimatedMessageBubble(message: ChatMessage, index: Int) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(300)) +
                slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(300)
                )
    ) {
        MessageBubble(message)
    }
}

@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.padding(end = 50.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                var dotCount by remember { mutableStateOf(1) }

                LaunchedEffect(Unit) {
                    while(true) {
                        delay(500)
                        dotCount = (dotCount % 3) + 1
                    }
                }

                Text(
                    text = ".".repeat(dotCount),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun QuickReplyChips(
    suggestions: List<String>,
    onSuggestionSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.forEach { suggestion ->
            SuggestionChip(
                onClick = { onSuggestionSelected(suggestion) },
                label = {
                    Text(
                        text = suggestion,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}

@Composable
fun ChatInputField(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent
                ),
                trailingIcon = {
                    IconButton(onClick = { /* Voice input functionality */ }) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Voice input",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            FloatingActionButton(
                onClick = onSend,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(48.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send message"
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message.isFromUser

    val backgroundColor = if (isUser)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.surfaceVariant

    val textColor = if (isUser)
        MaterialTheme.colorScheme.onPrimary
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    val shape = if (isUser)
        RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)
    else
        RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
            Row(verticalAlignment = Alignment.Bottom) {
                if (!isUser) {
                    // Bot avatar
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.CloudSync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

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

                if (isUser) {
                    Spacer(modifier = Modifier.width(8.dp))
                    // User avatar
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "U",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            // Timestamp
            Text(
                text = formatTimestamp(message.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(
                    start = if (!isUser) 40.dp else 0.dp,
                    end = if (isUser) 40.dp else 0.dp,
                    top = 4.dp
                )
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000} min ago"
        else -> {
            val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            sdf.format(java.util.Date(timestamp))
        }
    }
}