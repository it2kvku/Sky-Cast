package com.aco.skycast.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.aco.skycast.ui.components.WeatherLottieAnimation
import com.aco.skycast.utils.WeatherUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TomorrowScreen(
    viewModel: WeatherViewModel,
    latitude: Double,
    longitude: Double
) {
    val scrollState = rememberScrollState()
    val uiState by viewModel.weatherState.collectAsState()
    val ipCityState by viewModel.ipCity.collectAsState()
    val context = LocalContext.current

    // Fetch weather data
    LaunchedEffect(latitude, longitude) {
        if (latitude != 0.0 && longitude != 0.0) {
            viewModel.getWeatherByCoordinates(latitude, longitude)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WeatherUtils.BackgroundColor),
        contentAlignment = Alignment.Center
    ) {
        when (uiState) {
            is WeatherUiState.Loading -> CircularProgressIndicator(color = Color.White)
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
                            location = ipCityState ?: weatherData.address ?: "Current Location"
                        )

                        TomorrowSummaryCard(tomorrowForecast)

                        // Hourly forecast section removed

                        DetailedForecastCard(tomorrowForecast)
                    }
                } else {
                    Text(
                        "Tomorrow's forecast not available",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            is WeatherUiState.Error -> {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Unable to load forecast",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = (uiState as WeatherUiState.Error).message,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
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

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Text(
            text = "Tomorrow's Weather",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = formattedDate,
            fontSize = 18.sp,
            color = Color.White.copy(alpha = 0.9f)
        )

        Text(
            text = location,
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun TomorrowSummaryCard(forecast: WeatherDay) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WeatherUtils.BlueSolidColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Replace static emoji with Lottie animation
            WeatherLottieAnimation(
                weatherCondition = forecast.conditions
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = forecast.conditions,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
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
                        fontSize = 16.sp
                    )
                    Text(
                        text = "${forecast.tempmax.toInt()}°",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Low",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp
                    )
                    Text(
                        text = "${forecast.tempmin.toInt()}°",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Divider(color = Color.White.copy(alpha = 0.2f))

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
fun DetailedForecastCard(forecast: WeatherDay) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WeatherUtils.CardBackgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Weather Details",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = WeatherUtils.TemperatureHighColor
            )

            Divider(color = WeatherUtils.DividerColor)

            ForecastDetailRow("Humidity", "${forecast.humidity.toInt()}%", Icons.Outlined.WaterDrop, WeatherUtils.HumidityIconColor)
            ForecastDetailRow("Wind Speed", "${forecast.windspeed.toInt()} km/h", Icons.Outlined.Air, WeatherUtils.WindIconColor)
            ForecastDetailRow("Precipitation", "${forecast.precip} mm", Icons.Outlined.Opacity, WeatherUtils.HumidityIconColor)
            ForecastDetailRow("Precipitation Chance", "${forecast.precipprob.toInt()}%", Icons.Outlined.Watch, WeatherUtils.MetricsTextColor)
            ForecastDetailRow("UV Index", forecast.uvindex.toString(), Icons.Outlined.WbSunny, WeatherUtils.WeatherEmojiColor)

            Divider(color = WeatherUtils.DividerColor)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ForecastDetailColumn("Sunrise", forecast.sunrise, Icons.Outlined.WbSunny)
                ForecastDetailColumn("Sunset", forecast.sunset, Icons.Outlined.Nightlight)
            }
        }
    }
}

@Composable
fun ForecastDetailRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = label,
            color = WeatherUtils.MetricsTextColor,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = value,
            fontWeight = FontWeight.Medium,
            color = WeatherUtils.TemperatureHighColor
        )
    }
}

@Composable
fun ForecastDetailColumn(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = WeatherUtils.WeatherEmojiColor,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            color = WeatherUtils.MetricsTextColor,
            fontSize = 14.sp
        )

        Text(
            text = value,
            fontWeight = FontWeight.Medium,
            color = WeatherUtils.TemperatureHighColor
        )
    }
}