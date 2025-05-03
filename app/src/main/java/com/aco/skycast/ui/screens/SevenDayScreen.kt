package com.aco.skycast.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aco.skycast.data.model.WeatherUiState
import com.aco.skycast.data.model.WeatherViewModel

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

    when (weatherState) {
        is WeatherUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is WeatherUiState.Error -> {
            val errorMessage = (weatherState as WeatherUiState.Error).message
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Error: $errorMessage")
            }
        }
        is WeatherUiState.Success -> {
            val weatherData = (weatherState as WeatherUiState.Success).data
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                items(weatherData.days) { day ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(text = "Date: ${day.datetime}")
                            Text(text = "Max Temp: ${day.tempmax}°C")
                            Text(text = "Min Temp: ${day.tempmin}°C")
                            Text(text = "Conditions: ${day.conditions}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LaunchedEffect(
    key: Any,
    block: suspend () -> Unit
) {
    androidx.compose.runtime.LaunchedEffect(key) {
        block()
    }
}
