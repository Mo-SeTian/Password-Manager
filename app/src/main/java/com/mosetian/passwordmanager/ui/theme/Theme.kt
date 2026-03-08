package com.mosetian.passwordmanager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val DarkScheme = darkColorScheme(
    primary = ElectricBlue,
    secondary = EmeraldAccent,
    tertiary = VioletAccent,
    background = AmoledBackground,
    surface = DeepSurface,
    surfaceContainer = SurfaceRaised,
    surfaceContainerHigh = SurfaceRaised,
    surfaceVariant = SurfaceRail,
    onPrimary = Color.Black,
    onBackground = SoftText,
    onSurface = SoftText,
    onSurfaceVariant = MutedText,
    primaryContainer = ElectricBlue.copy(alpha = 0.20f),
    secondaryContainer = EmeraldAccent.copy(alpha = 0.18f)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(18.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun PasswordManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else DarkScheme,
        shapes = AppShapes,
        typography = Typography(),
        content = content
    )
}
