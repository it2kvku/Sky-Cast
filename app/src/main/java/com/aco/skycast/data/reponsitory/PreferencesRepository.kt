package com.aco.skycast.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesRepository(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("weather_preferences", Context.MODE_PRIVATE)

    // Preference keys
    companion object {
        const val KEY_SEVERE_ALERTS = "severe_alerts"
        const val KEY_DAILY_FORECAST = "daily_forecast"
        const val KEY_PRECIPITATION = "precipitation_alerts"
    }

    // StateFlows for each preference
    private val _severeAlerts = MutableStateFlow(
        sharedPreferences.getBoolean(KEY_SEVERE_ALERTS, true)
    )
    val severeAlerts: Flow<Boolean> = _severeAlerts.asStateFlow()

    private val _dailyForecast = MutableStateFlow(
        sharedPreferences.getBoolean(KEY_DAILY_FORECAST, true)
    )
    val dailyForecast: Flow<Boolean> = _dailyForecast.asStateFlow()

    private val _precipitationAlerts = MutableStateFlow(
        sharedPreferences.getBoolean(KEY_PRECIPITATION, false)
    )
    val precipitationAlerts: Flow<Boolean> = _precipitationAlerts.asStateFlow()

    fun updateSevereAlerts(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_SEVERE_ALERTS, enabled).apply()
        _severeAlerts.value = enabled
    }

    fun updateDailyForecast(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_DAILY_FORECAST, enabled).apply()
        _dailyForecast.value = enabled
    }

    fun updatePrecipitationAlerts(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_PRECIPITATION, enabled).apply()
        _precipitationAlerts.value = enabled
    }

}