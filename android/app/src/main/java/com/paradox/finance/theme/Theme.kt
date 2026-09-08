package com.paradox.finance.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ParadoxIndigo,
    onPrimary = ParadoxZinc100,
    primaryContainer = ParadoxIndigoDark,
    onPrimaryContainer = ParadoxZinc100,
    secondary = ParadoxEmerald,
    onSecondary = ParadoxZinc100,
    background = ParadoxZinc950,
    onBackground = ParadoxZinc100,
    surface = ParadoxZinc900,
    onSurface = ParadoxZinc100,
    surfaceVariant = ParadoxZinc800,
    onSurfaceVariant = ParadoxZinc400,
    outline = ParadoxZinc700,
    error = ParadoxRose,
    onError = ParadoxZinc100
)

@Composable
fun ParadoxTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = ParadoxZinc950.toArgb()
            window.navigationBarColor = ParadoxZinc950.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
