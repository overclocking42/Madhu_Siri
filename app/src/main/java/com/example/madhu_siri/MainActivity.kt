package com.example.madhu_siri

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.madhu_siri.data.repository.FirebaseRepository
import com.example.madhu_siri.ui.MadhuSiriNavGraph
import com.example.madhu_siri.ui.Screen
import com.example.madhu_siri.ui.theme.Madhu_SiriTheme
import com.example.madhu_siri.viewmodel.AuthState
import com.example.madhu_siri.viewmodel.AuthViewModel
import com.example.madhu_siri.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val repository = FirebaseRepository()
        val authViewModel = AuthViewModel(repository)
        val mainViewModel = MainViewModel(repository)

        setContent {
            Madhu_SiriTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val authState by authViewModel.authState.collectAsState()
                val context = LocalContext.current

                // Start local notification observer for beekeepers
                LaunchedEffect(authState) {
                    if (authState is AuthState.Authenticated) {
                        val role = (authState as AuthState.Authenticated).role
                        mainViewModel.startNotificationObserver(context, role)
                    }
                }

                Scaffold(
                    bottomBar = {
                        if (currentRoute != Screen.Login.route && currentRoute != Screen.RoleSelection.route) {
                            NavigationBar {
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Map, contentDescription = "Map") },
                                    label = { Text("Map") },
                                    selected = currentRoute == Screen.Map.route,
                                    onClick = { navController.navigate(Screen.Map.route) }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Warning, contentDescription = "Alert") },
                                    label = { Text("Spray") },
                                    selected = currentRoute == Screen.Spray.route,
                                    onClick = { navController.navigate(Screen.Spray.route) }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                                    label = { Text("Home") },
                                    selected = currentRoute == Screen.Dashboard.route,
                                    onClick = { navController.navigate(Screen.Dashboard.route) }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Info, contentDescription = "Tips") },
                                    label = { Text("Tips") },
                                    selected = currentRoute == Screen.Tips.route,
                                    onClick = { navController.navigate(Screen.Tips.route) }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Surface(modifier = Modifier.padding(innerPadding)) {
                        MadhuSiriNavGraph(
                            navController = navController,
                            authViewModel = authViewModel,
                            mainViewModel = mainViewModel
                        )
                    }
                }
            }
        }
    }
}
