package com.aco.skycast

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aco.skycast.data.model.AuthViewModel
import com.aco.skycast.data.model.WeatherViewModel
import com.aco.skycast.data.model.WeatherViewModelFactory
import com.aco.skycast.data.remote.WeatherApi
import com.aco.skycast.data.repository.WeatherRepository
import com.aco.skycast.ui.navigation.AppNavigation
import com.aco.skycast.ui.navigation.BottomNavItem
import com.aco.skycast.ui.screens.*
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
        // In your Application class or MainActivity onCreate
        FirebaseApp.initializeApp(this)
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Setup Retrofit and API
        val retrofit = Retrofit.Builder()
            .baseUrl("https://weather.visualcrossing.com/VisualCrossingWebServices/rest/services/timeline/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val weatherApi = retrofit.create(WeatherApi::class.java)

        val weatherRepository = WeatherRepository(weatherApi, "SK2GNSWPUS93N52XLXLUKY37L") // Replace with your actual API key


        // Initialize ViewModel with Factory
        val factory = WeatherViewModelFactory(weatherRepository)
        weatherViewModel = ViewModelProvider(this, factory)[WeatherViewModel::class.java]

        setContent {
            SkyCastTheme {

                val weatherViewModel = viewModel<WeatherViewModel>()
                val authViewModel = viewModel<AuthViewModel>()

                // In your MainActivity.kt, replace the Scaffold section with this code:
                val navController = rememberNavController()
                var showBottomBar by remember { mutableStateOf(authViewModel.getCurrentUser() != null) }

                Scaffold(
                    bottomBar = {
                        // Only show bottom bar if user is logged in
                        if (showBottomBar) {
                            NavigationBar {
                                val navBackStackEntry by navController.currentBackStackEntryAsState()
                                val currentDestination = navBackStackEntry?.destination

                                BottomNavItem.values().forEach { screen ->
                                    NavigationBarItem(
                                        icon = { Icon(painterResource(id = screen.icon), contentDescription = null) },
                                        label = { Text(stringResource(id = screen.title)) },
                                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                        onClick = {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
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

@Composable
fun MainScreen(weatherViewModel: WeatherViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                BottomNavItem.values().forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(painterResource(id = screen.icon), contentDescription = null) },
                        label = { Text(stringResource(id = screen.title)) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = BottomNavItem.Home.route,
                modifier = Modifier.padding(paddingValues)
            ) {
                composable(BottomNavItem.Home.route) {
                    WeatherScreen(viewModel = weatherViewModel)
                }
                composable(BottomNavItem.Search.route) {
                    SearchScreen(viewModel = weatherViewModel )
                }
                composable(BottomNavItem.ChatBot.route) {
                    // Placeholder for ChatBotDaily - implement this screen
                   ChatBotDaily(
                        viewModel = weatherViewModel,
                        onBackPressed = { navController.popBackStack() })
                }
                composable(BottomNavItem.Tomorrow.route) {
                    // Placeholder for TomorrowScreen - implement this screen
                    TomorrowScreen(
                        viewModel = weatherViewModel,
                        //get tomorrow forecast for a curent location
                        latitude = weatherViewModel.latitude,
                        longitude = weatherViewModel.longitude
)

                }
                composable(BottomNavItem.SevenDay.route) {
                    SevenDayScreen(
                        viewModel = weatherViewModel,
                        //get 7 day forecast for a curent location
                        latitude = weatherViewModel.latitude,
                        longitude = weatherViewModel.longitude
                    )
                }
                composable(BottomNavItem.UserSettings.route) {
                    // Placeholder for UserScreen - implement this screen
                    val authViewModel = viewModel<AuthViewModel>()
                    UserScreen(authViewModel) {
                        authViewModel.signOut()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            }
        }
    }
}