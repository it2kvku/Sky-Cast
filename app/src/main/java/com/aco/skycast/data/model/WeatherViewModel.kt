package com.aco.skycast.data.model


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aco.skycast.data.api.WeatherResponse
import com.aco.skycast.data.repository.IpLocationRepository
import com.aco.skycast.data.repository.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WeatherViewModel(
    private val repository: WeatherRepository
) : ViewModel() {

    private val _weatherState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val weatherState: StateFlow<WeatherUiState> = _weatherState

    private val ipLocationRepository = IpLocationRepository()
    private val _cityFromIp = MutableStateFlow<String?>(null)


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