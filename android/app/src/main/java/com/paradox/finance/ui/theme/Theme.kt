package com.paradox.finance.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ElectricEmerald,
    onPrimary = OnPrimary,
    primaryContainer = EmeraldContainer,
    secondary = SoftIndigo,
    onSecondary = PitchBlack,
    secondaryContainer = IndigoContainer,
    tertiary = LuminousCyan,
    onTertiary = PitchBlack,
    tertiaryContainer = CyanContainer,
    background = PitchBlack,
    onBackground = OnSurface,
    surface = ObsidianCanvas,
    onSurface = OnSurface,
    surfaceVariant = SurfaceContainerHigh,
    onSurfaceVariant = OnSurfaceVariant,
    error = CoralError,
    onError = PitchBlack,
    errorContainer = ErrorContainer,
    outline = MutedOutline,
    outlineVariant = OutlineVariant
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
            window.statusBarColor = PitchBlack.toArgb()
            window.navigationBarColor = PitchBlack.toArgb()
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
