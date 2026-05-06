package com.example.madhu_siri.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.madhu_siri.ui.screens.*
import com.example.madhu_siri.viewmodel.AuthState
import com.example.madhu_siri.viewmodel.AuthViewModel
import com.example.madhu_siri.viewmodel.MainViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object RoleSelection : Screen("role_selection")
    object Map : Screen("map")
    object Spray : Screen("spray")
    object Dashboard : Screen("dashboard")
    object Tips : Screen("tips")
}

@Composable
fun MadhuSiriNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    mainViewModel: MainViewModel
) {
    val authState by authViewModel.authState.collectAsState()

    val startDestination = when (authState) {
        is AuthState.Authenticated -> {
            val role = (authState as AuthState.Authenticated).role
            if (role == "BEEKEEPER") Screen.Map.route else Screen.Spray.route
        }
        is AuthState.AuthenticatedNoRole -> Screen.RoleSelection.route
        else -> Screen.Login.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(authViewModel)
        }
        composable(Screen.RoleSelection.route) {
            RoleSelectionScreen(authViewModel)
        }
        composable(Screen.Map.route) {
            MapScreen(mainViewModel)
        }
        composable(Screen.Spray.route) {
            SprayScreen(mainViewModel)
        }
        composable(Screen.Dashboard.route) {
            val role = (authState as? AuthState.Authenticated)?.role ?: ""
            DashboardScreen(mainViewModel, role)
        }
        composable(Screen.Tips.route) {
            TipsScreen()
        }
    }
}
