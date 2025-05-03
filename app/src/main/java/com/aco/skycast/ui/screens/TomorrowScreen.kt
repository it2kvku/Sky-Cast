package com.aco.skycast.ui.screens

import android.location.Geocoder
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aco.skycast.data.api.WeatherDay
import com.aco.skycast.data.api.WeatherHour
import com.aco.skycast.data.model.WeatherUiState
import com.aco.skycast.data.model.WeatherViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun TomorrowScreen(
    viewModel: WeatherViewModel,
    latitude: Double,
    longitude: Double
) {
    val scrollState = rememberScrollState()
    val uiState by viewModel.weatherState.collectAsState()
    val context = LocalContext.current
    var cityName by remember { mutableStateOf("") }

    // Fetch city name from coordinates
    LaunchedEffect(latitude, longitude) {
        if (latitude != 0.0 && longitude != 0.0) {
            cityName = getCityNameFromCoordinates(context, latitude, longitude)
            viewModel.getWeatherByCoordinates(latitude, longitude)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (uiState) {
            is WeatherUiState.Loading -> CircularProgressIndicator()
            is WeatherUiState.Success -> {
                val weatherData = (uiState as WeatherUiState.Success).data

                // Get tomorrow's date in the format used by the API (YYYY-MM-DD)
                val tomorrow = LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)

                // Find tomorrow's forecast in the days list
                val tomorrowForecast = weatherData.days.find { it.datetime == tomorrow }

                if (tomorrowForecast != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TomorrowHeader(
                            date = tomorrow,
                            location = cityName
                        )

                        TomorrowSummaryCard(tomorrowForecast)

                        // Only call HourlyForecastSection if hours list is not null
                        tomorrowForecast.hours?.let { hours ->
                            if (hours.isNotEmpty()) {
                                HourlyForecastSection(hours)
                            }
                        }

                        DetailedForecastCard(tomorrowForecast)
                    }
                } else {
                    Text("Tomorrow's forecast not available")
                }
            }
            is WeatherUiState.Error -> {
                Text("Error: ${(uiState as WeatherUiState.Error).message}")
            }
        }
    }
}

// Function to get city name from coordinates using Geocoder
suspend fun getCityNameFromCoordinates(context: android.content.Context, latitude: Double, longitude: Double): String {
    return withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            
            // For Android API 33+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                var result = "Unknown Location"
                geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                    if (addresses.isNotEmpty()) {
                        val address = addresses[0]
                        result = address.locality ?: address.subAdminArea ?: address.adminArea ?: "Unknown Location"
                    }
                }
                return@withContext result
            } else {
                // For older Android versions
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    return@withContext address.locality ?: address.subAdminArea ?: address.adminArea ?: "Unknown Location"
                }
                return@withContext "Unknown Location"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext "Unknown Location"
        }
    }
}

@Composable
fun TomorrowHeader(date: String, location: String) {
    val formattedDate = try {
        val parsedDate = LocalDate.parse(date)
        parsedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))
    } catch (e: Exception) {
        date
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Tomorrow's Weather",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = formattedDate,
            fontSize = 18.sp,
            color = Color.Gray
        )

        Text(
            text = location,
            fontSize = 16.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun TomorrowSummaryCard(forecast: WeatherDay) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1976D2)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = forecast.conditions,
                color = Color.White,
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "High",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${forecast.tempmax.toInt()}°C",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Low",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${forecast.tempmin.toInt()}°C",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = forecast.description,
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
fun HourlyForecastSection(hours: List<WeatherHour>) {
    // Skip rendering if hours list is empty
    if (hours.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Hourly Forecast",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Only show a subset of hours to keep it manageable (every 3 hours)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Show forecast at 8AM, 12PM, 4PM, 8PM
                val selectedHours = listOf("08:00:00", "12:00:00", "16:00:00", "20:00:00")

                for (timePoint in selectedHours) {
                    val hourData = hours.find { it.datetime.endsWith(timePoint) }
                    hourData?.let {
                        val time = it.datetime.split(":")[0].toInt()
                        val timeLabel = when {
                            time == 0 -> "12 AM"
                            time < 12 -> "$time AM"
                            time == 12 -> "12 PM"
                            else -> "${time - 12} PM"
                        }

                        HourlyForecastItem(
                            time = timeLabel,
                            icon = getWeatherEmoji(it.icon),
                            temp = "${it.temp.toInt()}°"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HourlyForecastItem(time: String, icon: String, temp: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(text = time, color = Color.Gray, fontSize = 14.sp)
        Text(text = icon, fontSize = 24.sp)
        Text(text = temp, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun DetailedForecastCard(forecast: WeatherDay) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Weather Details",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Divider()

            ForecastDetailRow("Humidity", "${forecast.humidity.toInt()}%")
            ForecastDetailRow("Wind Speed", "${forecast.windspeed} km/h")
            ForecastDetailRow("Precipitation", "${forecast.precip} mm")
            ForecastDetailRow("Precipitation Chance", "${forecast.precipprob}%")
            ForecastDetailRow("UV Index", forecast.uvindex.toString())
            ForecastDetailRow("Sunrise", forecast.sunrise)
            ForecastDetailRow("Sunset", forecast.sunset)
        }
    }
}

@Composable
fun ForecastDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray)
        Text(text = value, fontWeight = FontWeight.Medium)
    }
}

// Helper function to map weather condition icons to emoji
fun getWeatherEmoji(icon: String): String {
    return when (icon) {
        "clear-day", "clear-night" -> "☀️"
        "partly-cloudy-day", "partly-cloudy-night" -> "🌤️"
        "cloudy" -> "☁️"
        "rain" -> "🌧️"
        "snow", "snow-showers-day", "snow-showers-night" -> "❄️"
        "thunder-rain", "thunder-showers-day", "thunder-showers-night" -> "⛈️"
        "fog" -> "🌫️"
        "wind" -> "🌬️"
        else -> "🌡️"
    }
}
