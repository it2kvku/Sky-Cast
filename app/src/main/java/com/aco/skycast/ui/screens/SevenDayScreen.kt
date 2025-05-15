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
import com.aco.skycast.utils.WeatherUtils
import java.text.SimpleDateFormat
import java.util.*
import com.aco.skycast.ui.components.WeatherLottieAnimation
import com.airbnb.lottie.compose.*
import com.aco.skycast.R
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TileMode


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
            .background(WeatherUtils.BackgroundColor)
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
        colors = CardDefaults.cardColors(containerColor = WeatherUtils.CardBackgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Weather animation and condition
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(80.dp)
            ) {
                // Use sunny animation for overcast conditions in Seven Day Screen
                if (day.conditions.contains("overcast", ignoreCase = true)) {
                    // Custom Lottie animation for overcast in Seven Day Screen
                    val composition by rememberLottieComposition(
                        spec = LottieCompositionSpec.RawRes(R.raw.sunny)
                    )
                    val progress by animateLottieCompositionAsState(
                        composition = composition,
                        iterations = LottieConstants.IterateForever
                    )

                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFF56CCF2), Color(0xFF2F80ED)),
                                    startY = 0f,
                                    endY = Float.POSITIVE_INFINITY,
                                    tileMode = TileMode.Clamp
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        LottieAnimation(
                            modifier = Modifier.fillMaxSize(),
                            composition = composition,
                            progress = { progress }
                        )
                    }
                } else {
                    // Use standard animation for other conditions
                    WeatherLottieAnimation(
                        weatherCondition = day.conditions
                    )
                }

                Text(
                    text = day.conditions,
                    style = MaterialTheme.typography.bodySmall,
                    color = WeatherUtils.MetricsTextColor,
                    maxLines = 2,
                    textAlign = TextAlign.Center
                )
            }

            // Rest of the card content remains the same
            Spacer(modifier = Modifier.width(16.dp))

            // Vertical divider
            Divider(
                color = WeatherUtils.DividerColor,
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
                    color = WeatherUtils.TemperatureHighColor
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Precipitation probability
                    Icon(
                        imageVector = Icons.Outlined.WaterDrop,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = WeatherUtils.HumidityIconColor
                    )
                    Text(
                        text = " ${day.precipprob.toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = WeatherUtils.MetricsTextColor
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Wind speed
                    Icon(
                        imageVector = Icons.Outlined.Air,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = WeatherUtils.WindIconColor
                    )
                    Text(
                        text = " ${day.windspeed.toInt()} km/h",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = WeatherUtils.MetricsTextColor
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
                    color = WeatherUtils.TemperatureHighColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${day.tempmin.toInt()}°",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 16.sp,
                    color = WeatherUtils.TemperatureLowColor
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