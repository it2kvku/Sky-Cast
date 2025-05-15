package com.aco.skycast.ui.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*
import com.aco.skycast.R
import kotlinx.coroutines.delay

@Composable
fun WeatherLottieAnimation(
    weatherCondition: String,
    modifier: Modifier = Modifier
) {
    // Background gradient colors based on weather
    val (gradientColors, targetGradientColors) = getWeatherGradient(weatherCondition)

    // Animate gradient transition using transition animation
    val transition = rememberInfiniteTransition(label = "GradientTransition")

    val animatedColors = List<Color>(gradientColors.size) { index ->
        val startColor = gradientColors[index]
        val endColor = targetGradientColors[index]

        val animatedColor by transition.animateColor(
            initialValue = startColor,
            targetValue = endColor,
            animationSpec = infiniteRepeatable<Color>(
                animation = tween<Color>(3000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ColorAnimation$index"
        )
        animatedColor
    }

    // Map weather conditions to Lottie animation resources
    val animationResId = when {
        weatherCondition.contains("rain", ignoreCase = true) -> R.raw.rain
        weatherCondition.contains("snow", ignoreCase = true) -> R.raw.snow
        weatherCondition.contains("cloud", ignoreCase = true) &&
                weatherCondition.contains("sun", ignoreCase = true) -> R.raw.partly_cloudy
        weatherCondition.contains("cloud", ignoreCase = true) -> R.raw.cloudy
        weatherCondition.contains("clear", ignoreCase = true) -> R.raw.sunny
        weatherCondition.contains("storm", ignoreCase = true) -> R.raw.storm
        weatherCondition.contains("fog", ignoreCase = true) -> R.raw.fog
        else -> R.raw.sunny
    }

    // Background with animated gradient
    Box(
        modifier = modifier
            .size(64.dp)  // Modified to match the 64.sp size of the weather icon
            .background(
                brush = Brush.verticalGradient(
                    colors = animatedColors,
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY,
                    tileMode = TileMode.Clamp
                ),
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        // Lottie animation
        val composition by rememberLottieComposition(
            spec = LottieCompositionSpec.RawRes(animationResId)
        )
        val progress by animateLottieCompositionAsState(
            composition = composition,
            iterations = LottieConstants.IterateForever
        )

        LottieAnimation(
            modifier = Modifier.fillMaxSize(),
            composition = composition,
            progress = { progress }
        )
    }
}

@Composable
private fun getWeatherGradient(weatherCondition: String): Pair<List<Color>, List<Color>> {
    return when {
        weatherCondition.contains("rain", ignoreCase = true) -> Pair(
            listOf(Color(0xFF2C3E50), Color(0xFF4CA1AF)),
            listOf(Color(0xFF1F2B38), Color(0xFF2E5D6C))
        )
        weatherCondition.contains("snow", ignoreCase = true) -> Pair(
            listOf(Color(0xFF83a4d4), Color(0xFFb6fbff)),
            listOf(Color(0xFF6E8DBA), Color(0xFF9BE8F0))
        )
        weatherCondition.contains("cloud", ignoreCase = true) -> Pair(
            listOf(Color(0xFF2980B9), Color(0xFF6DD5FA)),
            listOf(Color(0xFF2471A3), Color(0xFF5BC0DE))
        )
        weatherCondition.contains("clear", ignoreCase = true) -> Pair(
            listOf(Color(0xFF56CCF2), Color(0xFF2F80ED)),
            listOf(Color(0xFF4FC3EF), Color(0xFF1A73E8))
        )
        weatherCondition.contains("storm", ignoreCase = true) -> Pair(
            listOf(Color(0xFF373B44), Color(0xFF4286f4)),
            listOf(Color(0xFF2A2D33), Color(0xFF3A77DB))
        )
        weatherCondition.contains("fog", ignoreCase = true) -> Pair(
            listOf(Color(0xFFD3D3D3), Color(0xFF808080)),
            listOf(Color(0xFFC6C6C6), Color(0xFF707070))
        )
        else -> Pair(
            listOf(Color(0xFF2193b0), Color(0xFF6dd5ed)),
            listOf(Color(0xFF1C839E), Color(0xFF5BC0D8))
        )
    }
}