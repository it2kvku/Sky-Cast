package com.aco.skycast.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aco.skycast.data.api.WeatherDay
import com.aco.skycast.data.model.WeatherUiState
import com.aco.skycast.data.model.WeatherViewModel
import java.text.SimpleDateFormat
import java.util.*

// Color definitions based on requirements
private val BackgroundColor = Color(0xFF7CC1F0)
private val CardBackgroundColor = Color.White // #FFFFFF
private val HumidityIconColor = Color(0xFF90CAF9)
private val WindIconColor = Color(0xFF81D4FA)
private val MetricsTextColor = Color(0xFF666666)
private val WeatherEmojiColor = Color(0xFFFFD54F)
private val TemperatureHighColor = Color(0xFF212121)
private val TemperatureLowColor = Color(0xFF757575)
private val DividerColor = Color.LightGray.copy(alpha = 0.3f)

@Composable
fun SevenDayScreen(
    viewModel: WeatherViewModel,
    latitude: Double,
    longitude: Double
) {
    val weatherState by viewModel.weatherState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.getWeatherByCoordinates(latitude, longitude)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(top = 16.dp)
    ) {
        Text(
            text = "7-Day Forecast",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        when (weatherState) {
            is WeatherUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
            is WeatherUiState.Error -> {
                val errorMessage = (weatherState as WeatherUiState.Error).message
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Unable to load forecast",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            is WeatherUiState.Success -> {
                val weatherData = (weatherState as WeatherUiState.Success).data
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    items(weatherData.days) { day ->
                        ForecastDayCard(day)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ForecastDayCard(day: WeatherDay) {
    // Format date from YYYY-MM-DD to readable format
    val date = try {
        val parts = day.datetime.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val dayOfMonth = parts[2].toInt()
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, dayOfMonth)
        SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(calendar.time)
    } catch (e: Exception) {
        day.datetime
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Weather icon and condition
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(80.dp)
            ) {
                Text(
                    text = getWeatherEmojiFromCondition(day.conditions),
                    fontSize = 38.sp,
                    color = WeatherEmojiColor
                )
                Text(
                    text = day.conditions,
                    style = MaterialTheme.typography.bodySmall,
                    color = MetricsTextColor,
                    maxLines = 2,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Vertical divider
            Divider(
                color = DividerColor,
                modifier = Modifier
                    .height(60.dp)
                    .width(1.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Date and weather details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = date,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TemperatureHighColor
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Precipitation probability
                    Icon(
                        imageVector = Icons.Outlined.WaterDrop,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = HumidityIconColor
                    )
                    Text(
                        text = " ${day.precipprob.toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = MetricsTextColor
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Wind speed
                    Icon(
                        imageVector = Icons.Outlined.Air,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = WindIconColor
                    )
                    Text(
                        text = " ${day.windspeed.toInt()} km/h",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = MetricsTextColor
                    )
                }
            }

            // Temperature range
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${day.tempmax.toInt()}°",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TemperatureHighColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${day.tempmin.toInt()}°",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 16.sp,
                    color = TemperatureLowColor
                )
            }
        }
    }
}

// Renamed to avoid overload resolution ambiguity
private fun getWeatherEmojiFromCondition(condition: String): String {
    return when {
        condition.contains("rain", ignoreCase = true) -> "🌧️"
        condition.contains("cloud", ignoreCase = true) && condition.contains("sun", ignoreCase = true) -> "🌤️"
        condition.contains("cloud", ignoreCase = true) -> "☁️"
        condition.contains("clear", ignoreCase = true) -> "☀️"
        condition.contains("snow", ignoreCase = true) -> "❄️"
        condition.contains("storm", ignoreCase = true) -> "⛈️"
        condition.contains("fog", ignoreCase = true) -> "🌫️"
        else -> "🌤️"
    }
}