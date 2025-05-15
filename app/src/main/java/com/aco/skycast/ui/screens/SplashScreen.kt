package com.aco.skycast.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aco.skycast.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    var offsetY by remember { mutableStateOf(0f) }
    var showLogo by remember { mutableStateOf(false) }
    val dragThreshold = 300f

    // Animation states
    val alphaAnim = animateFloatAsState(
        targetValue = if (showLogo) 1f else 0f,
        animationSpec = tween(1000)
    )

    LaunchedEffect(key1 = true) {
        delay(500)
        showLogo = true
        delay(3000) // Auto-proceed after 3 seconds
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE3F2FD))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (offsetY < -dragThreshold) {
                            onSplashFinished()
                        }
                        offsetY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        // Only track vertical drag up (negative Y values)
                        if (dragAmount.y < 0) {
                            offsetY += dragAmount.y
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Weather icon animation
        WeatherAnimation(modifier = Modifier.fillMaxSize())

        // Logo and text with fade-in effect
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(alphaAnim.value)
        ) {
            Text(
                text = "SKY CAST",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0D47A1)
            )

            Text(
                text = "Kéo lên để tiếp tục",
                fontSize = 16.sp,
                color = Color(0xFF1976D2),
                modifier = Modifier.padding(top = 24.dp)
            )
        }
    }
}

@Composable
fun WeatherAnimation(modifier: Modifier = Modifier) {
    // Simple animated weather icon as a fallback for Lottie
    val infiniteTransition = rememberInfiniteTransition(label = "weather-anim")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Use a standard weather icon from material icons or your drawable resources
        Icon(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "Weather",
            tint = Color(0xFF1976D2).copy(alpha = 0.3f),
            modifier = Modifier
                .size(200.dp)
                .scale(scale)
        )
    }
}