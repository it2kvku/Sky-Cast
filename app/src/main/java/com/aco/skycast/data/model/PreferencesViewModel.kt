package com.aco.skycast.data.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aco.skycast.data.repository.PreferencesRepository
import com.aco.skycast.utils.NotificationHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class PreferencesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PreferencesRepository(application)
    private val notificationHelper = NotificationHelper(application.applicationContext)
    val severeAlerts: StateFlow<Boolean> = repository.severeAlerts
        .stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            true // Default value until actual value is loaded
        )

    val dailyForecast: StateFlow<Boolean> = repository.dailyForecast
        .stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            true
        )

    val precipitationAlerts: StateFlow<Boolean> = repository.precipitationAlerts
        .stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            false
        )

    fun updateSevereAlerts(enabled: Boolean) {
        repository.updateSevereAlerts(enabled)
    }

    fun updateDailyForecast(enabled: Boolean) {
        repository.updateDailyForecast(enabled)
    }

    fun updatePrecipitationAlerts(enabled: Boolean) {
        repository.updatePrecipitationAlerts(enabled)
    }
    fun testSevereWeatherAlert() {
        if (severeAlerts.value) {
            notificationHelper.sendSevereWeatherAlert()
        }
    }

    fun testDailyForecastAlert() {
        if (dailyForecast.value) {
            notificationHelper.sendDailyForecastAlert()
        }
    }

    fun testPrecipitationAlert() {
        if (precipitationAlerts.value) {
            notificationHelper.sendPrecipitationAlert()
        }
    }
}