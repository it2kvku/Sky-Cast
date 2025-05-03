package com.aco.skycast.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aco.skycast.data.model.WeatherUiState
import com.aco.skycast.data.model.WeatherViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.material3.CircularProgressIndicator
import kotlin.text.toInt

// Define a solid color for UI elements
private val BlueSolidColor = Color(0xFF1976D2)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WeatherScreen(viewModel: WeatherViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.weatherState.collectAsState()
    val scrollState = rememberScrollState()

    val locationPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    LaunchedEffect(Unit) {
        if (locationPermissionState.status.isGranted) {
            getCurrentLocation(context) { lat, lon ->
                viewModel.getWeatherByCoordinates(lat, lon)
            }
        } else {
            locationPermissionState.launchPermissionRequest()
            // Default location if permission denied
            viewModel.getWeatherByCoordinates(40.7128, -74.0060) // New York as default
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (uiState) {
            is WeatherUiState.Loading -> CircularProgressIndicator()
            is WeatherUiState.Success -> {
                val weather = (uiState as WeatherUiState.Success).data

                val locationName = extractCityName(weather.resolvedAddress)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    WeatherCard(
                        location = locationName,
                        temperature = "${weather.days.firstOrNull()?.temp?.toInt() ?: 0}°C",
                        condition = weather.days.firstOrNull()?.conditions ?: "Unknown",
                        feelsLike = "${weather.days.firstOrNull()?.feelslike?.toInt() ?: 0}°C",
                        windSpeed = "${weather.days.firstOrNull()?.windspeed?.toInt() ?: 0} km/h",
                        humidity = "${weather.days.firstOrNull()?.humidity?.toInt() ?: 0}%"
                    )

                    ForecastSection(viewModel)

                    TaskSection()
                }
            }
            is WeatherUiState.Error -> {
                Text("Error: ${(uiState as WeatherUiState.Error).message}")
            }
        }
    }
}
internal fun extractCityName(address: String): String {
    // The address format is typically "City, State, Country"
    // or "City, Country" or sometimes has ZIP codes

    // First, try to get the first part before any comma
    val parts = address.split(",")
    if (parts.isNotEmpty()) {
        return parts[0].trim()
    }

    // If there's an issue, return the whole address
    return address
}
// Helper function to get current location
suspend fun getCurrentLocation(context: android.content.Context, onSuccess: (Double, Double) -> Unit) {
    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        try {
            // Switch to IO dispatcher for network/IO operations
            withContext(Dispatchers.IO) {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                val locationTask = fusedLocationClient.lastLocation
                val location = Tasks.await(locationTask)
                if (location != null) {
                    // Switch back to Main dispatcher to update UI
                    withContext(Dispatchers.Main) {
                        onSuccess(location.latitude, location.longitude)
                    }
                } else {
                    // Use default location if location is null
                    withContext(Dispatchers.Main) {
                        onSuccess(40.7128, -74.0060) // New York as default
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    } else {
        // Use default location if permission not granted
        onSuccess(40.7128, -74.0060) // New York as default
    }


}
@Composable
fun ForecastSection(viewModel: WeatherViewModel) {
    val uiState by viewModel.weatherState.collectAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Today's Forecast",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (uiState) {
                is WeatherUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
                is WeatherUiState.Error -> {
                    Text(
                        text = "Failed to load forecast",
                        color = Color.Red
                    )
                }
                is WeatherUiState.Success -> {
                    val weather = (uiState as WeatherUiState.Success).data
                    val today = weather.days.firstOrNull()

                    if (today?.hours != null && today.hours.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Filter for specific times of the day (9AM, 12PM, 3PM, 6PM)
                            val timePoints = listOf("09:00:00", "12:00:00", "15:00:00", "18:00:00")

                            for (timePoint in timePoints) {
                                val hourData = today.hours.find { it.datetime.endsWith(timePoint) == true }
                                if (hourData != null) {
                                    val time = timePoint.substring(0, 2).toInt()
                                    val timeLabel = when {
                                        time == 0 -> "12 AM"
                                        time < 12 -> "$time AM"
                                        time == 12 -> "12 PM"
                                        else -> "${time - 12} PM"
                                    }

                                    ForecastItem(
                                        time = timeLabel,
                                        icon = getWeatherEmoji(hourData.conditions),
                                        temp = "${hourData.temp.toInt()}°"
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Hourly forecast not available",
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}
// Helper function to map weather conditions to emojis
fun getWeatherEmoji(condition: String?): String {
    return when {
        condition == null -> "❓"
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

@Composable
fun ForecastItem(time: String, icon: String, temp: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = time, color = Color.Gray, fontSize = 14.sp)
        Text(text = icon, fontSize = 24.sp)
        Text(text = temp, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun TaskSection() {
    // Implementing the previously marked TODO function
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Weather Recommendations",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = { /* Add task */ },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Placeholder recommendations
            TaskItem("Good day for outdoor activities", isCompleted = false)
            TaskItem("Moderate UV index - use sunscreen", isCompleted = true)
        }
    }
}

@Composable
fun TaskItem(text: String, isCompleted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isCompleted,
            onCheckedChange = { /* Handle check change */

                // Handle the checkbox state change


            }
        )

        Text(
            text = text,
            modifier = Modifier.padding(start = 8.dp),
            color = if (isCompleted) Color.Gray else Color.Black,
            fontSize = 16.sp
        )
    }
}

@Composable
fun WeatherCard(
    location: String,
    temperature: String,
    condition: String,
    feelsLike: String? = null,
    windSpeed: String? = null,
    humidity: String? = null
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = BlueSolidColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = location,
                color = Color.White,
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = temperature,
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = condition,
                color = Color.White,
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                WeatherDetail("Feels like", feelsLike ?: "N/A")
                WeatherDetail("Wind", windSpeed ?: "N/A")
                WeatherDetail("Humidity", humidity ?: "N/A")
            }
        }
    }
}

@Composable
fun WeatherDetail(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp
        )

        Text(
            text = value,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
