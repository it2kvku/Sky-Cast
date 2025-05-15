package com.aco.skycast.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aco.skycast.data.api.WeatherResponse
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeatherData(weatherEntity: WeatherEntity)

    @Query("SELECT * FROM weather_data WHERE locationKey = :locationKey")
    suspend fun getWeatherData(locationKey: String): WeatherEntity?

    @Query("SELECT * FROM weather_data WHERE locationKey = :locationKey")
    fun getWeatherDataFlow(locationKey: String): Flow<WeatherEntity?>

    @Query("SELECT * FROM weather_data WHERE lastUpdated > :timestamp")
    suspend fun getRecentWeatherData(timestamp: Long): List<WeatherEntity>
}