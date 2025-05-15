package com.aco.skycast.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.aco.skycast.data.model.AuthViewModel
import com.aco.skycast.data.model.WeatherViewModel
import com.aco.skycast.ui.screens.ChatBotDaily
import com.aco.skycast.ui.screens.SearchScreen
import com.aco.skycast.ui.screens.SevenDayScreen
import com.aco.skycast.ui.screens.SplashScreen
import com.aco.skycast.ui.screens.TomorrowScreen
import com.aco.skycast.ui.screens.UserScreen
import com.aco.skycast.ui.screens.WeatherScreen
import com.aco.skycast.ui.screens.auth.LoginScreen
import com.aco.skycast.ui.screens.auth.SignUpScreen

private const val TRANSITION_DURATION = 300

@Composable
fun AppNavigation(
    navController: NavHostController,
    weatherViewModel: WeatherViewModel,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier,
    showBottomBar: (Boolean) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = "splash",
        modifier = modifier,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(TRANSITION_DURATION)
            ) + fadeIn(animationSpec = tween(TRANSITION_DURATION))
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(TRANSITION_DURATION)
            ) + fadeOut(animationSpec = tween(TRANSITION_DURATION))
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(TRANSITION_DURATION)
            ) + fadeIn(animationSpec = tween(TRANSITION_DURATION))
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(TRANSITION_DURATION)
            ) + fadeOut(animationSpec = tween(TRANSITION_DURATION))
        }
    ) {
        composable("splash") {
            showBottomBar(false)
            SplashScreen(
                onSplashFinished = {
                    val startDestination = if (authViewModel.getCurrentUser() != null) {
                        BottomNavItem.Home.route
                    } else {
                        "login"
                    }
                    navController.navigate(startDestination) {
                        popUpTo("splash") { inclusive = true }
                    }
                    showBottomBar(startDestination != "login")
                }
            )
        }

        composable("login") {
            showBottomBar(false)
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(BottomNavItem.Home.route) {
                        popUpTo("login") { inclusive = true }
                    }
                    showBottomBar(true)
                },
                onSignUpClick = {
                    navController.navigate("signup")
                }
            )
        }

        composable("signup") {
            showBottomBar(false)
            SignUpScreen(
                authViewModel = authViewModel,
                onSignUpSuccess = {
                    navController.navigate(BottomNavItem.Home.route) {
                        popUpTo("login") { inclusive = true }
                    }
                    showBottomBar(true)
                },
                onLoginClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(BottomNavItem.Home.route) {
            showBottomBar(true)
            WeatherScreen(viewModel = weatherViewModel)
        }

        composable(BottomNavItem.Search.route) {
            showBottomBar(true)
            SearchScreen(viewModel = weatherViewModel)
        }

        composable(BottomNavItem.ChatBot.route) {
            showBottomBar(true)
            ChatBotDaily(
                viewModel = weatherViewModel,
                onBackPressed = { navController.popBackStack() }
            )
        }

        composable(BottomNavItem.Tomorrow.route) {
            showBottomBar(true)
            TomorrowScreen(
                viewModel = weatherViewModel,
                latitude = weatherViewModel.latitude,
                longitude = weatherViewModel.longitude
            )
        }

        composable(BottomNavItem.SevenDay.route) {
            showBottomBar(true)
            SevenDayScreen(
                viewModel = weatherViewModel,
                latitude = weatherViewModel.latitude,
                longitude = weatherViewModel.longitude
            )
        }

        composable(BottomNavItem.UserSettings.route) {
            showBottomBar(true)
            UserScreen(
                authViewModel = authViewModel,
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                    showBottomBar(false)
                }
            )
        }
    }
}