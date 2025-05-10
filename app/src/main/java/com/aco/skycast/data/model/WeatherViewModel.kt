package com.aco.skycast.data.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aco.skycast.BuildConfig
import com.aco.skycast.data.api.GitHubAiClient
import com.aco.skycast.data.api.WeatherResponse
import com.aco.skycast.data.repository.IpLocationRepository
import com.aco.skycast.data.repository.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import com.aco.skycast.data.model.CitySearchData

class WeatherViewModel(
    private val repository: WeatherRepository
) : ViewModel() {

    private val _weatherState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val weatherState: StateFlow<WeatherUiState> = _weatherState

    private val ipLocationRepository = IpLocationRepository()
    private val _cityFromIp = MutableStateFlow<String?>(null)

    private val _recommendations = MutableStateFlow<List<WeatherRecommendation>>(emptyList())
    val recommendations = _recommendations.asStateFlow()

    private val _searchResults = MutableStateFlow<List<CitySearchData>>(emptyList())
    val searchResults: StateFlow<List<CitySearchData>> = _searchResults

    private val _isSearchLoading = MutableStateFlow(false)
    val isSearchLoading: StateFlow<Boolean> = _isSearchLoading

    fun searchCities(query: String) {
        viewModelScope.launch {
            _isSearchLoading.value = true
            try {
                // Get the weather response for the city
                val weatherResponse = repository.getWeatherByCity(query)

                // Convert WeatherResponse to CitySearchData
                val cityName = weatherResponse.resolvedAddress.split(",").firstOrNull()?.trim() ?: query
                val country = weatherResponse.resolvedAddress.split(",").lastOrNull()?.trim() ?: ""
                val temperature = weatherResponse.days.firstOrNull()?.temp ?: 0.0

                // Extract latitude and longitude
                val lat = weatherResponse.latitude
                val lon = weatherResponse.longitude

                // Create a CitySearchData object with all required parameters
                val citySearchData = CitySearchData(
                    name = cityName,
                    country = country,
                    lat = lat,
                    lon = lon,
                    temperature = temperature
                )

                // Update the search results
                _searchResults.value = listOf(citySearchData)
            } catch (e: Exception) {
                _searchResults.value = emptyList()
            } finally {
                _isSearchLoading.value = false
            }
        }
    }
    data class WeatherRecommendation(
        val text: String,
        val isCompleted: Boolean = false,
        val id: String = UUID.randomUUID().toString()
    )

    fun toggleRecommendation(id: String) {
        _recommendations.update { list ->
            list.map {
                if (it.id == id) it.copy(isCompleted = !it.isCompleted) else it
            }
        }
    }

    fun getWeatherRecommendations() {
        viewModelScope.launch {
            val weatherState = _weatherState.value
            if (weatherState is WeatherUiState.Success) {
                val weather = weatherState.data
                val currentDay = weather.days.firstOrNull()

                // Create weather context similar to ChatBotDaily
                val weatherContext = """
                Current location: ${weather.resolvedAddress}
                Temperature: ${currentDay?.temp ?: "Unknown"}°C
                Conditions: ${currentDay?.conditions ?: "Unknown"}
                Humidity: ${currentDay?.humidity ?: "Unknown"}%
                Wind: ${currentDay?.windspeed ?: "Unknown"} km/h
            """.trimIndent()

                try {
                    val gitHubAiClient = GitHubAiClient(BuildConfig.GITHUB_TOKEN)
                    val response = gitHubAiClient.getWeatherInsights(
                        weatherContext,
                        "Give me 3 practical weather recommendations for today based on these conditions. Format as a simple list."
                    )

                    // Parse response into recommendations
                    val recommendations = parseRecommendations(response)
                    _recommendations.value = recommendations
                } catch (e: Exception) {
                    // Fallback to default recommendations if AI fails
                    _recommendations.value = listOf(
                        WeatherRecommendation("Check weather updates regularly"),
                        WeatherRecommendation("Prepare for ${currentDay?.conditions ?: "current"} conditions"),
                        WeatherRecommendation("Stay hydrated")
                    )
                }
            }
        }
    }

    private fun parseRecommendations(aiResponse: String): List<WeatherRecommendation> {
        // Simple parsing: split by newlines and filter empty lines
        return aiResponse
            .split("\n")
            .map { line -> line.trim() }
            .filter { it.isNotEmpty() }
            .map { line ->
                // Remove list markers like "1.", "•", "-" if present
                val cleanedLine = line.replace(Regex("^[\\d•\\-\\*]+\\.?\\s*"), "")
                WeatherRecommendation(cleanedLine)
            }
            .take(5) // Limit to 5 recommendations
    }
    private val _ipCity = MutableStateFlow<String>("")
    val ipCity = _ipCity.asStateFlow()
    var latitude: Double = 0.0
        private set
    var longitude: Double = 0.0
        private set
    fun updateIpCity(city: String) {
        _ipCity.value = city
    }
    fun getCityFromIp() {
        viewModelScope.launch {
            ipLocationRepository.getIpLocation()
                .onSuccess { location ->
                    _cityFromIp.value = location.city
                }
                .onFailure { /* Handle error */ }
        }
    }
    fun fetchWeather(city: String) {
        viewModelScope.launch {
            _weatherState.value = WeatherUiState.Loading
            try {
                val response = repository.getWeatherByCity(city)
                _weatherState.value = WeatherUiState.Success(response)
                latitude = response.latitude
                longitude = response.longitude
            } catch (e: Exception) {
                _weatherState.value = WeatherUiState.Error(e.message ?: "Unknown error")
                e.printStackTrace()
            }
        }
    }

    fun getWeatherByCoordinates(lat: Double, lon: Double) {
        viewModelScope.launch {
            _weatherState.value = WeatherUiState.Loading
            try {
                val response = repository.getWeatherByCoordinates(lat, lon)
                _weatherState.value = WeatherUiState.Success(response)
                latitude = lat
                longitude = lon
            } catch (e: Exception) {
                _weatherState.value = WeatherUiState.Error(e.message ?: "Unknown error")
                e.printStackTrace()
            }
        }
    }
}

sealed class WeatherUiState {
    object Loading : WeatherUiState()
    data class Success(val data: WeatherResponse) : WeatherUiState()
    data class Error(val message: String) : WeatherUiState()
}
