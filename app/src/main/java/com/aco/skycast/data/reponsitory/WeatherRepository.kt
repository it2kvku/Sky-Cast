package com.aco.skycast.data.repository

import android.content.Context
import com.aco.skycast.data.api.WeatherResponse
import com.aco.skycast.data.database.WeatherDatabase
import com.aco.skycast.data.database.WeatherEntity
import com.aco.skycast.data.remote.WeatherApi
import java.util.concurrent.TimeUnit

open class WeatherRepository(
    private val api: WeatherApi,
    private val apiKey: String,
    private val context: Context
) {
    private val weatherDao = WeatherDatabase.getDatabase(context).weatherDao()

    open suspend fun getWeatherByCity(city: String): WeatherResponse {
        val locationKey = city.trim().lowercase()
        val cachedData = weatherDao.getWeatherData(locationKey)

        if (cachedData != null && isCacheValid(cachedData.lastUpdated)) {
            return mapEntityToResponse(cachedData)
        }

        // Cache is invalid or not present, fetch from API
        val apiResponse = api.getWeather(
            location = city,
            key = apiKey
        )

        // Save to database
        weatherDao.insertWeatherData(mapResponseToEntity(apiResponse, locationKey))

        return apiResponse
    }

    open suspend fun getWeatherByCoordinates(lat: Double, lon: Double): WeatherResponse {
        val locationKey = "$lat,$lon"
        val cachedData = weatherDao.getWeatherData(locationKey)

        if (cachedData != null && isCacheValid(cachedData.lastUpdated)) {
            return mapEntityToResponse(cachedData)
        }

        // Cache is invalid or not present, fetch from API
        val apiResponse = api.getWeatherByCoordinates(
            latitude = lat,
            longitude = lon,
            apiKey = apiKey
        )

        // Save to database
        weatherDao.insertWeatherData(mapResponseToEntity(apiResponse, locationKey))

        return apiResponse
    }

    private fun isCacheValid(timestamp: Long): Boolean {
        val currentTime = System.currentTimeMillis()
        val cacheAge = currentTime - timestamp
        val oneDayInMillis = TimeUnit.DAYS.toMillis(1)
        return cacheAge < oneDayInMillis
    }

    private fun mapEntityToResponse(entity: WeatherEntity): WeatherResponse {
        return WeatherResponse(
            queryCost = entity.queryCost,
            latitude = entity.latitude,
            longitude = entity.longitude,
            resolvedAddress = entity.resolvedAddress,
            address = entity.address,
            timezone = entity.timezone,
            tzoffset = entity.tzoffset,
            days = entity.days
        )
    }

    private fun mapResponseToEntity(response: WeatherResponse, locationKey: String): WeatherEntity {
        return WeatherEntity(
            locationKey = locationKey,
            queryCost = response.queryCost,
            latitude = response.latitude,
            longitude = response.longitude,
            resolvedAddress = response.resolvedAddress,
            address = response.address,
            timezone = response.timezone,
            tzoffset = response.tzoffset,
            days = response.days
        )
    }
}