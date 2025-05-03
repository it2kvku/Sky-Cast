package com.aco.skycast.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.aco.skycast.data.model.AuthUiState
import com.aco.skycast.data.model.AuthViewModel
import com.aco.skycast.data.model.WeatherViewModel
import com.aco.skycast.ui.screens.*
import com.aco.skycast.ui.screens.auth.LoginScreen
import com.aco.skycast.ui.screens.auth.SignUpScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    weatherViewModel: WeatherViewModel,
    authViewModel: AuthViewModel,
    showBottomBar: (Boolean) -> Unit
) {
    val authState by authViewModel.authState.collectAsState()

    // Check if user is logged in and update bottom bar visibility
    LaunchedEffect(authState) {
        when (authState) {
            is AuthUiState.Success -> {
                val user = (authState as AuthUiState.Success).user
                showBottomBar(user != null)

                // If user logs in, navigate to home
                if (user != null && navController.currentDestination?.route !in listOf(
                        BottomNavItem.Home.route,
                        BottomNavItem.Search.route,
                        BottomNavItem.ChatBot.route,
                        BottomNavItem.Tomorrow.route,
                        BottomNavItem.SevenDay.route,
                        BottomNavItem.UserSettings.route
                    )) {
                    navController.navigate(BottomNavItem.Home.route) {
                        popUpTo("login") {
                            inclusive = true
                        }
                    }
                }
            }
            else -> {}
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (authViewModel.getCurrentUser() != null) BottomNavItem.Home.route else "login"
    ) {
        // Auth screens
        composable("login") {
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(BottomNavItem.Home.route) {
                        popUpTo("login") {
                            inclusive = true
                        }
                    }
                },
                onSignUpClick = {
                    navController.navigate("signup")
                }
            )
        }

        composable("signup") {
            SignUpScreen(
                authViewModel = authViewModel,
                onSignUpSuccess = {
                    navController.navigate(BottomNavItem.Home.route) {
                        popUpTo("signup") {
                            inclusive = true
                        }
                    }
                },
                onLoginClick = {
                    navController.navigate("login") {
                        popUpTo("signup") {
                            inclusive = true
                        }
                    }
                }
            )
        }

        // Main screens - accessible only when logged in
        composable(BottomNavItem.Home.route) {
            WeatherScreen(viewModel = weatherViewModel)
        }

        composable(BottomNavItem.Search.route) {
            SearchScreen(viewModel = weatherViewModel)
        }

        composable(BottomNavItem.ChatBot.route) {
            ChatBotDaily(
                viewModel = weatherViewModel,
                onBackPressed = { navController.popBackStack() }
            )
        }

        composable(BottomNavItem.Tomorrow.route) {
            TomorrowScreen(
                viewModel = weatherViewModel,
                latitude = weatherViewModel.latitude,
                longitude = weatherViewModel.longitude
            )
        }

        composable(BottomNavItem.SevenDay.route) {
            SevenDayScreen(
                viewModel = weatherViewModel,
                latitude = weatherViewModel.latitude,
                longitude = weatherViewModel.longitude
            )
        }

        composable(BottomNavItem.UserSettings.route) {
            UserScreen(
                authViewModel = authViewModel,
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}