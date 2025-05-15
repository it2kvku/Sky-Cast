package com.aco.skycast

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.aco.skycast.data.api.WeatherResponse
import com.aco.skycast.data.model.AuthViewModel
import com.aco.skycast.data.model.WeatherViewModel
import com.aco.skycast.data.model.WeatherViewModelFactory
import com.aco.skycast.data.remote.WeatherApi
import com.aco.skycast.data.repository.WeatherRepository
import com.aco.skycast.ui.navigation.AppNavigation
import com.aco.skycast.ui.navigation.BottomNavBar
import com.aco.skycast.ui.theme.SkyCastTheme
import com.aco.skycast.utils.NotificationHelper
import com.google.firebase.FirebaseApp
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.Manifest

class MainActivity : ComponentActivity() {
    private lateinit var weatherViewModel: WeatherViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        // Request notification permission on app start
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NotificationHelper.PERMISSION_REQUEST_CODE
                )
            }
        }

        FirebaseApp.initializeApp(this)
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Setup Retrofit and API
        val retrofit = Retrofit.Builder()
            .baseUrl("https://weather.visualcrossing.com/VisualCrossingWebServices/rest/services/timeline/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val weatherApi = retrofit.create(WeatherApi::class.java)
        val apiKey = BuildConfig.WEATHER_API_KEY

        // Create a temporary repository that bypasses Room
        val weatherRepository = object : WeatherRepository(weatherApi, apiKey, applicationContext) {
            // Cache the API and key in local variables for our overrides
            private val localApi = weatherApi
            private val localApiKey = apiKey

            override suspend fun getWeatherByCity(city: String): WeatherResponse {
                return localApi.getWeather(
                    location = city,
                    key = localApiKey
                )
            }

            override suspend fun getWeatherByCoordinates(lat: Double, lon: Double): WeatherResponse {
                return localApi.getWeatherByCoordinates(
                    latitude = lat,
                    longitude = lon,
                    apiKey = localApiKey
                )
            }
        }

        val factory = WeatherViewModelFactory(weatherRepository)
        weatherViewModel = ViewModelProvider(this, factory)[WeatherViewModel::class.java]

        setContent {
            SkyCastTheme {
                val authViewModel = viewModel<AuthViewModel>()
                val navController = rememberNavController()
                var showBottomBar by remember { mutableStateOf(authViewModel.getCurrentUser() != null) }

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            BottomNavBar(navController = navController)
                        }
                    }
                ) { paddingValues ->
                    Box(modifier = Modifier.padding(paddingValues)) {
                        AppNavigation(
                            navController = navController,
                            weatherViewModel = weatherViewModel,
                            authViewModel = authViewModel,
                            showBottomBar = { isVisible -> showBottomBar = isVisible }
                        )
                    }
                }
            }
        }
    }
}