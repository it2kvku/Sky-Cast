package com.aco.skycast.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.aco.skycast.data.model.WeatherUiState
import com.aco.skycast.data.model.WeatherViewModel
import com.aco.skycast.utils.LocationUtils
import com.aco.skycast.utils.WeatherUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
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
        viewModel.getCityFromIp()
        if (locationPermissionState.status.isGranted) {
            getCurrentLocation(context) { lat, lon ->
                // Launch a coroutine to call the suspend function
                CoroutineScope(Dispatchers.Main).launch {
                    // Get location name before fetching weather
                    val locationName = LocationUtils.getLocationNameFromCoordinates(context, lat, lon)
                    // Update to use the existing ipCityState
                    viewModel.updateIpCity(locationName)
                    viewModel.getWeatherByCoordinates(lat, lon)
                }
            }
        } else {
            locationPermissionState.launchPermissionRequest()
            // Default location if permission denied
            viewModel.getWeatherByCoordinates(40.7128, -74.0060) // New York as default
        }
    }

    val ipCityState by viewModel.ipCity.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WeatherUtils.BackgroundColor),
        contentAlignment = Alignment.Center
    ) {
        when (uiState) {
            is WeatherUiState.Loading -> CircularProgressIndicator(color = Color.White)
            is WeatherUiState.Success -> {
                val weather = (uiState as WeatherUiState.Success).data

                // Ensure non-nullability with ?: operator
                val locationName: String = if (!ipCityState.isNullOrBlank()) {
                    ipCityState ?: "Current Location"
                } else {
                    if (weather.address != null) {
                        weather.address
                    } else if (weather.resolvedAddress != null && !weather.resolvedAddress.matches(Regex(".*\\d+\\.\\d+.*"))) {
                        weather.resolvedAddress
                    } else {
                        "Current Location"
                    }
                }
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Unable to load weather data",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = (uiState as WeatherUiState.Error).message,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// Helper function to get current location
suspend fun getCurrentLocation(context: android.content.Context, onSuccess: (Double, Double) -> Unit) {
    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        try {
            withContext(Dispatchers.IO) {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                val locationTask = fusedLocationClient.lastLocation
                val location = Tasks.await(locationTask)
                if (location != null) {
                    withContext(Dispatchers.Main) {
                        onSuccess(location.latitude, location.longitude)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onSuccess(40.7128, -74.0060) // New York as default
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    } else {
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
        colors = CardDefaults.cardColors(containerColor = WeatherUtils.CardBackgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Today's Forecast",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = WeatherUtils.TemperatureHighColor
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
                                        icon = WeatherUtils.getWeatherEmoji(hourData.conditions),
                                        temp = "${hourData.temp.toInt()}°"
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Hourly forecast not available",
                            color = WeatherUtils.MetricsTextColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ForecastItem(time: String, icon: String, temp: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Text(
            text = time,
            color = WeatherUtils.MetricsTextColor,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = icon,
            fontSize = 32.sp,
            color = WeatherUtils.WeatherEmojiColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = temp,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = WeatherUtils.TemperatureHighColor
        )
    }
}

@Composable
fun TaskSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WeatherUtils.CardBackgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                    fontWeight = FontWeight.Bold,
                    color = WeatherUtils.TemperatureHighColor
                )

                IconButton(
                    onClick = { /* Add task */ },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        tint = WeatherUtils.BlueSolidColor
                    )
                }
            }

            Divider(
                color = WeatherUtils.DividerColor,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            TaskItem("Good day for outdoor activities", isCompleted = false)
            TaskItem("Moderate UV index - use sunscreen", isCompleted = true)
            TaskItem("High humidity - stay hydrated", isCompleted = false)
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
            onCheckedChange = { /* Handle check change */ },
            colors = CheckboxDefaults.colors(
                checkedColor = WeatherUtils.BlueSolidColor,
                uncheckedColor = WeatherUtils.MetricsTextColor
            )
        )

        Text(
            text = text,
            modifier = Modifier.padding(start = 8.dp),
            color = if (isCompleted) WeatherUtils.MetricsTextColor else WeatherUtils.TemperatureHighColor,
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
            containerColor = WeatherUtils.BlueSolidColor
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
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = WeatherUtils.getWeatherEmoji(condition),
                fontSize = 64.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

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

            Spacer(modifier = Modifier.height(24.dp))

            Divider(color = Color.White.copy(alpha = 0.2f))

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                WeatherDetail(
                    icon = Icons.Outlined.WaterDrop,
                    value = humidity ?: "N/A",
                    label = "Humidity"
                )

                WeatherDetail(
                    icon = Icons.Outlined.Air,
                    value = windSpeed ?: "N/A",
                    label = "Wind"
                )

                WeatherDetail(
                    icon = null,
                    value = feelsLike ?: "N/A",
                    label = "Feels like"
                )
            }
        }
    }
}

@Composable
fun WeatherDetail(icon: androidx.compose.ui.graphics.vector.ImageVector?, value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        Text(
            text = value,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp
        )
    }
}