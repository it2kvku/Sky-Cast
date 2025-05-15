package com.aco.skycast.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.aco.skycast.data.api.WeatherDay
import com.aco.skycast.data.api.WeatherHour
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.Instant

@Entity(tableName = "weather_data")
data class WeatherEntity(
    @PrimaryKey
    val locationKey: String, // city name or lat,lng
    val queryCost: Int,
    val latitude: Double,
    val longitude: Double,
    val resolvedAddress: String,
    val address: String,
    val timezone: String,
    val tzoffset: Double,
    @TypeConverters(WeatherTypeConverters::class)
    val days: List<WeatherDay>,
    val lastUpdated: Long = System.currentTimeMillis()
)

class WeatherTypeConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromWeatherDayList(value: List<WeatherDay>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toWeatherDayList(value: String): List<WeatherDay> {
        val listType = object : TypeToken<List<WeatherDay>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromWeatherHourList(value: List<WeatherHour>?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toWeatherHourList(value: String): List<WeatherHour>? {
        val listType = object : TypeToken<List<WeatherHour>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String>? {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }
}