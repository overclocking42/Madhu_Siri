package com.example.madhu_siri

import android.Manifest
import android.os.Bundle
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.madhu_siri.data.repository.AppSettingsRepository
import com.example.madhu_siri.data.model.UserRole
import com.example.madhu_siri.data.repository.FirebaseRepository
import com.example.madhu_siri.ui.MadhuSiriNavGraph
import com.example.madhu_siri.ui.Screen
import com.example.madhu_siri.ui.localization.LocalAppText
import com.example.madhu_siri.ui.localization.ProvideAppText
import com.example.madhu_siri.ui.localization.TextKey
import com.example.madhu_siri.ui.theme.Madhu_SiriTheme
import com.example.madhu_siri.viewmodel.AuthState
import com.example.madhu_siri.viewmodel.AuthViewModel
import com.example.madhu_siri.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val repository by lazy { FirebaseRepository() }
    private val settingsRepository by lazy { AppSettingsRepository(this) }
    private val viewModelFactory by lazy { AppViewModelFactory(repository, settingsRepository) }
    private val authViewModel by viewModels<AuthViewModel> { viewModelFactory }
    private val mainViewModel by viewModels<MainViewModel> { viewModelFactory }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themePreference by settingsRepository.themePreference.collectAsState()
            val language by settingsRepository.language.collectAsState()
            Madhu_SiriTheme(themePreference = themePreference) {
                ProvideAppText(language) {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    val currentRoute = currentDestination?.route
                    val authState by authViewModel.authState.collectAsState()
                    val role = (authState as? AuthState.Authenticated)?.role
                    val context = LocalContext.current
                    val notificationPermissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { }

                    LaunchedEffect(authState, role) {
                        val userId = when (authState) {
                            is AuthState.Authenticated,
                            is AuthState.AuthenticatedNoRole -> mainViewModel.getCurrentUserId()
                            else -> null
                        }
                        mainViewModel.onSessionChanged(userId)
                        if (role != null) {
                            if (
                                role == UserRole.BEEKEEPER.name &&
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            mainViewModel.startNotificationObserver(context, role)
                        }
                    }

                    val colorScheme = androidx.compose.material3.MaterialTheme.colorScheme
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        colorScheme.primaryContainer.copy(alpha = 0.28f),
                                        colorScheme.secondaryContainer.copy(alpha = 0.18f),
                                        colorScheme.background
                                    )
                                )
                            )
                    ) {
                        Scaffold(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                            topBar = {
                                if (role != null && currentRoute !in authRoutes) {
                                    CenterAlignedTopAppBar(
                                        title = { Text(routeTitle(currentRoute, role)) },
                                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                            containerColor = colorScheme.surface.copy(alpha = 0.88f),
                                            titleContentColor = colorScheme.onSurface
                                        ),
                                        actions = {
                                            IconButton(onClick = { navController.navigate(Screen.Profile.route) }) {
                                                Icon(Icons.Default.AccountCircle, contentDescription = LocalAppText.current(TextKey.TITLE_PROFILE))
                                            }
                                        }
                                    )
                                }
                            },
                            bottomBar = {
                                if (role != null && currentRoute in bottomBarRoutes(role)) {
                                    NavigationBar(containerColor = colorScheme.surface.copy(alpha = 0.92f)) {
                                        bottomNavItems(role).forEach { item ->
                                            NavigationBarItem(
                                                icon = { Icon(item.icon, contentDescription = item.label) },
                                                label = { Text(item.label) },
                                                selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                                                onClick = {
                                                    navController.navigate(item.route) {
                                                        popUpTo(navController.graph.startDestinationId) {
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
                        ) { innerPadding ->
                            Surface(modifier = Modifier.padding(innerPadding), color = androidx.compose.ui.graphics.Color.Transparent) {
                                MadhuSiriNavGraph(
                                    navController = navController,
                                    authViewModel = authViewModel,
                                    mainViewModel = mainViewModel,
                                    settingsRepository = settingsRepository
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private val authRoutes = setOf(Screen.Splash.route, Screen.Login.route, Screen.Register.route)

private data class BottomNavItem(
    val label: String,
    val route: String,
    val icon: ImageVector
)

@Composable
private fun bottomNavItems(role: String): List<BottomNavItem> {
    val tr = LocalAppText.current
    return if (role == UserRole.BEEKEEPER.name) {
        listOf(
            BottomNavItem(tr(TextKey.NAV_DASHBOARD), Screen.BeekeeperDashboard.route, Icons.Default.Dashboard),
            BottomNavItem(tr(TextKey.NAV_MAP), Screen.Map.route, Icons.Default.Map),
            BottomNavItem(tr(TextKey.NAV_TIPS), Screen.Tips.route, Icons.Default.Grass)
        )
    } else {
        listOf(
            BottomNavItem(tr(TextKey.NAV_DASHBOARD), Screen.FarmerDashboard.route, Icons.Default.Dashboard),
            BottomNavItem(tr(TextKey.NAV_SPRAY), Screen.Spray.route, Icons.Default.Warning),
            BottomNavItem(tr(TextKey.NAV_MAP), Screen.Map.route, Icons.Default.Map),
            BottomNavItem(tr(TextKey.NAV_TIPS), Screen.Tips.route, Icons.Default.Grass)
        )
    }
}

@Composable
private fun bottomBarRoutes(role: String): Set<String> {
    return bottomNavItems(role).map { it.route }.toSet() + Screen.Profile.route
}

@Composable
private fun routeTitle(route: String?, role: String): String {
    val tr = LocalAppText.current
    return when (route) {
        Screen.Map.route -> tr(TextKey.TITLE_HIVE_MAP)
        Screen.Spray.route -> tr(TextKey.TITLE_SPRAY_ALERT)
        Screen.Profile.route -> tr(TextKey.TITLE_PROFILE)
        Screen.Tips.route -> tr(TextKey.TITLE_BEE_FRIENDLY_TIPS)
        Screen.BeekeeperDashboard.route -> tr(TextKey.TITLE_BEEKEEPER_DASHBOARD)
        Screen.FarmerDashboard.route -> tr(TextKey.TITLE_FARMER_DASHBOARD)
        else -> if (role == UserRole.BEEKEEPER.name) tr(TextKey.TITLE_BEEKEEPER_DASHBOARD) else tr(TextKey.TITLE_FARMER_DASHBOARD)
    }
}
