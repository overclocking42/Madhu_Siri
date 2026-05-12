package com.example.madhu_siri.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.madhu_siri.data.repository.AppSettingsRepository
import com.example.madhu_siri.ui.screens.DashboardScreen
import com.example.madhu_siri.ui.screens.LoginScreen
import com.example.madhu_siri.ui.screens.MapScreen
import com.example.madhu_siri.ui.screens.ProfileScreen
import com.example.madhu_siri.ui.screens.RegisterScreen
import com.example.madhu_siri.ui.screens.SplashScreen
import com.example.madhu_siri.ui.screens.SprayScreen
import com.example.madhu_siri.ui.screens.TipsScreen
import com.example.madhu_siri.viewmodel.AuthState
import com.example.madhu_siri.viewmodel.AuthViewModel
import com.example.madhu_siri.viewmodel.MainViewModel

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Map : Screen("map")
    data object Spray : Screen("spray")
    data object FarmerDashboard : Screen("farmer_dashboard")
    data object BeekeeperDashboard : Screen("beekeeper_dashboard")
    data object Tips : Screen("tips")
    data object Profile : Screen("profile")
}

@Composable
fun MadhuSiriNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    mainViewModel: MainViewModel,
    settingsRepository: AppSettingsRepository
) {
    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        val destination = when (val state = authState) {
            is AuthState.Authenticated -> {
                if (state.role == "BEEKEEPER") Screen.BeekeeperDashboard.route else Screen.FarmerDashboard.route
            }
            is AuthState.AuthenticatedNoRole -> Screen.Login.route
            is AuthState.Unauthenticated -> Screen.Login.route
            else -> null
        }
        if (destination != null) {
            navController.navigate(destination) {
                popUpTo(Screen.Splash.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            SplashScreen()
        }
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onOpenRegister = { navController.navigate(Screen.Register.route) }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                viewModel = authViewModel,
                settingsRepository = settingsRepository,
                onBackToLogin = { navController.popBackStack() }
            )
        }
        composable(Screen.Map.route) {
            MapScreen(mainViewModel)
        }
        composable(Screen.Spray.route) {
            SprayScreen(mainViewModel)
        }
        composable(Screen.FarmerDashboard.route) {
            DashboardScreen(mainViewModel, role = "FARMER")
        }
        composable(Screen.BeekeeperDashboard.route) {
            DashboardScreen(mainViewModel, role = "BEEKEEPER")
        }
        composable(Screen.Tips.route) {
            TipsScreen()
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                authViewModel = authViewModel,
                mainViewModel = mainViewModel,
                settingsRepository = settingsRepository
            )
        }
    }
}
