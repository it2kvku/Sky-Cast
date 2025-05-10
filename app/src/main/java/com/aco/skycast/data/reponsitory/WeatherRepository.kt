package com.aco.skycast.data.repository

import com.aco.skycast.data.api.WeatherResponse
import com.aco.skycast.data.remote.WeatherApi

class WeatherRepository(
    private val api: WeatherApi,
    private val apiKey: String
) {
    suspend fun getWeatherByCity(city: String): WeatherResponse {
        return api.getWeather(
            location = city,
            key = apiKey
        )
    }

    suspend fun getWeatherByCoordinates(lat: Double, lon: Double): WeatherResponse {
        return api.getWeatherByCoordinates(
            latitude = lat,
            longitude = lon,
            apiKey = apiKey
        )
    }

}