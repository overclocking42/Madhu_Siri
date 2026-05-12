package com.example.madhu_siri.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.madhu_siri.data.model.AppThemePreference

private val LightColorScheme = lightColorScheme(
    primary = HoneyGold,
    onPrimary = PollenCream,
    primaryContainer = NectarMint,
    onPrimaryContainer = DuskForest,
    secondary = Wildflower,
    onSecondary = PollenCream,
    secondaryContainer = Color(0xFFE4F0D8),
    onSecondaryContainer = DuskForest,
    tertiary = BeeBrown,
    background = PollenCream,
    onBackground = DuskForest,
    surface = PollenCream,
    onSurface = DuskForest,
    surfaceContainer = Color(0xFFF6EFD9),
    surfaceContainerHigh = Color(0xFFF0E7CD),
    onSurfaceVariant = Color(0xFF5C5A4E),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFF2BC4B),
    onPrimary = DarkPollen,
    primaryContainer = Color(0xFF5B3A00),
    onPrimaryContainer = Color(0xFFFFE9B5),
    secondary = Color(0xFFB7E18B),
    onSecondary = Color(0xFF1A240F),
    secondaryContainer = Color(0xFF243A1F),
    onSecondaryContainer = Color(0xFFE3F3D3),
    tertiary = Color(0xFFFFC857),
    tertiaryContainer = Color(0xFF8C5E00),
    onTertiaryContainer = Color(0xFFFFF0C8),
    background = Color(0xFF121512),
    onBackground = Color(0xFFF7F1E2),
    surface = Color(0xFF181C18),
    onSurface = Color(0xFFF7F1E2),
    surfaceContainer = Color(0xFF232822),
    surfaceContainerHigh = Color(0xFF2D342C),
    onSurfaceVariant = Color(0xFFDEE4D6),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun Madhu_SiriTheme(
    themePreference: AppThemePreference = AppThemePreference.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themePreference) {
        AppThemePreference.SYSTEM -> isSystemInDarkTheme()
        AppThemePreference.LIGHT -> false
        AppThemePreference.DARK -> true
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
