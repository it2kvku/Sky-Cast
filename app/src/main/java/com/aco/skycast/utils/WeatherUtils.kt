package com.aco.skycast.utils

import androidx.compose.ui.graphics.Color

object WeatherUtils {
    // UI Colors based on requirements
    val BackgroundColor = Color(0xFF7CC1F0)
    val CardBackgroundColor = Color.White // #FFFFFF
    val HumidityIconColor = Color(0xFF90CAF9)
    val WindIconColor = Color(0xFF81D4FA)
    val MetricsTextColor = Color(0xFF666666)
    val WeatherEmojiColor = Color(0xFFFFD54F)
    val TemperatureHighColor = Color(0xFF212121)
    val TemperatureLowColor = Color(0xFF757575)
    val DividerColor = Color.LightGray.copy(alpha = 0.3f)
    val BlueSolidColor = Color(0xFF1976D2)
    val LightBackground = Color(0xFFF5F9FF)

    // Weather emoji mapping
    fun getWeatherEmoji(condition: String?): String {
        return when {
            condition == null -> "❓"
            condition.contains("rain", ignoreCase = true) -> "🌧️"
            condition.contains("cloud", ignoreCase = true) && condition.contains("sun", ignoreCase = true) -> "🌤️"
            condition.contains("cloud", ignoreCase = true) -> "☁️"
            condition.contains("clear", ignoreCase = true) -> "☀️"
            condition.contains("snow", ignoreCase = true) -> "❄️"
            condition.contains("storm", ignoreCase = true) -> "⛈️"
            condition.contains("fog", ignoreCase = true) -> "🌫️"
            else -> "🌤️"
        }
    }
}