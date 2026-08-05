package com.glassesscan.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = iOSBlue,
    onPrimary = White,
    secondary = iOSGray,
    onSecondary = White,
    background = Black,
    onBackground = White,
    surface = DarkGray,
    onSurface = White,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = iOSGray,
    error = Color(0xFFFF453A), // iOS Red
    onError = White
)

private val LightColorScheme = lightColorScheme(
    primary = iOSBlue,
    onPrimary = White,
    secondary = iOSGray,
    onSecondary = White,
    background = iOSBackground,
    onBackground = Black,
    surface = iOSSystemBackground,
    onSurface = Black,
    surfaceVariant = iOSLightGray,
    onSurfaceVariant = iOSGray,
    error = Color(0xFFFF3B30), // iOS Red
    onError = White
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp)
)

@Composable
fun GlassesScanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
