package com.paradox.finance.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = ParadoxIndigo,
    onPrimary = ParadoxZinc100,
    secondary = ParadoxIndigoLight,
    onSecondary = ParadoxZinc100,
    tertiary = ParadoxEmerald,
    background = ParadoxZinc950,
    onBackground = ParadoxZinc100,
    surface = ParadoxZinc900,
    onSurface = ParadoxZinc100,
    surfaceVariant = ParadoxZinc800,
    onSurfaceVariant = ParadoxZinc400
)

@Composable
fun ParadoxTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
