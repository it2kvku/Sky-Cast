package com.aco.skycast.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Refresh
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

                    TaskSection(viewModel)
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
    val horizontalScrollState = rememberScrollState()

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
                    text = "Today's Forecast",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = WeatherUtils.TemperatureHighColor
                )

                Text(
                    text = "Hourly",
                    fontSize = 14.sp,
                    color = WeatherUtils.BlueSolidColor,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (uiState) {
                is WeatherUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = WeatherUtils.BlueSolidColor
                        )
                    }
                }
                is WeatherUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Failed to load forecast",
                                color = Color.Red
                            )
                            Text(
                                text = "Pull to refresh",
                                color = WeatherUtils.MetricsTextColor,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
                is WeatherUiState.Success -> {
                    val weather = (uiState as WeatherUiState.Success).data
                    val today = weather.days.firstOrNull()

                    if (today?.hours != null && today.hours.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(horizontalScrollState)
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Display hourly forecasts for the current day
                            today.hours.forEachIndexed { index, hour ->
                                if (index % 2 == 0) { // Show every 2 hours to save space
                                    val time = hour.datetime.substring(0, 5) // Format: "HH:MM"
                                    ForecastItem(
                                        time = time,
                                        icon = WeatherUtils.getWeatherEmoji(hour.conditions),
                                        temp = "${hour.temp.toInt()}°",
                                        condition = hour.conditions
                                    )
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Hourly forecast not available",
                                    color = WeatherUtils.MetricsTextColor
                                )
                                Text(
                                    text = "Check your API settings",
                                    color = WeatherUtils.MetricsTextColor.copy(alpha = 0.7f),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ForecastItem(
    time: String,
    icon: String,
    temp: String,
    condition: String
) {
    Card(
        modifier = Modifier
            .width(80.dp)
            .padding(bottom = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = WeatherUtils.LightBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
        ) {
            Text(
                text = time,
                color = WeatherUtils.MetricsTextColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = icon,
                fontSize = 32.sp,
                color = WeatherUtils.WeatherEmojiColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = temp,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = WeatherUtils.TemperatureHighColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = condition.split(",").firstOrNull() ?: condition,
                color = WeatherUtils.MetricsTextColor,
                fontSize = 10.sp,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TaskSection(viewModel: WeatherViewModel) {
    val recommendations by viewModel.recommendations.collectAsState()

    // Request recommendations if empty
    LaunchedEffect(Unit) {
        if (recommendations.isEmpty()) {
            viewModel.getWeatherRecommendations()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = WeatherUtils.CardBackgroundColor.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Weather Recommendations",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = WeatherUtils.BlueSolidColor
                )

                IconButton(
                    onClick = { viewModel.getWeatherRecommendations() },
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = WeatherUtils.LightBackground,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh recommendations",
                        tint = WeatherUtils.BlueSolidColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Divider(
                color = WeatherUtils.DividerColor,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (recommendations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = WeatherUtils.BlueSolidColor,
                        strokeWidth = 3.dp
                    )
                }
            } else {
                recommendations.forEachIndexed { index, recommendation ->
                    EnhancedTaskItem(
                        text = recommendation.text,
                        isCompleted = recommendation.isCompleted,
                        onCheckedChange = { viewModel.toggleRecommendation(recommendation.id) }
                    )

                    if (index < recommendations.size - 1) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedTaskItem(
    text: String,
    isCompleted: Boolean,
    onCheckedChange: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isCompleted)
            WeatherUtils.LightBackground.copy(alpha = 0.6f)
        else
            WeatherUtils.LightBackground,
        tonalElevation = if (isCompleted) 0.dp else 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isCompleted,
                onCheckedChange = { onCheckedChange() },
                modifier = Modifier.size(24.dp),
                colors = CheckboxDefaults.colors(
                    checkedColor = WeatherUtils.BlueSolidColor,
                    uncheckedColor = WeatherUtils.BlueSolidColor.copy(alpha = 0.5f),
                    checkmarkColor = Color.White
                )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = text,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                color = if (isCompleted)
                    WeatherUtils.MetricsTextColor
                else
                    WeatherUtils.TemperatureHighColor,
                textDecoration = if (isCompleted)
                    androidx.compose.ui.text.style.TextDecoration.LineThrough
                else
                    null,
                fontWeight = if (isCompleted)
                    FontWeight.Normal
                else
                    FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
        }
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