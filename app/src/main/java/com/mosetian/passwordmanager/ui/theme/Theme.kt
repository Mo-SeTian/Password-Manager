package com.mosetian.passwordmanager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DarkScheme = darkColorScheme(
    primary = ElectricBlue,
    secondary = EmeraldAccent,
    tertiary = VioletAccent,
    background = AmoledBackground,
    surface = DeepSurface,
    surfaceContainer = SurfaceRaised,
    surfaceContainerHigh = SurfaceHigher,
    surfaceVariant = SurfaceGlass,
    outlineVariant = OutlineSoft,
    onPrimary = Color.Black,
    onBackground = SoftText,
    onSurface = SoftText,
    onSurfaceVariant = MutedText,
    primaryContainer = ElectricBlue.copy(alpha = 0.20f),
    secondaryContainer = EmeraldAccent.copy(alpha = 0.18f),
    tertiaryContainer = VioletAccent.copy(alpha = 0.18f)
)

private val LightScheme = lightColorScheme(
    primary = ElectricBlue,
    secondary = EmeraldAccent,
    tertiary = VioletAccent,
    background = LightBackground,
    surface = LightSurface,
    surfaceContainer = LightSurfaceRaised,
    surfaceContainerHigh = LightSurfaceHigher,
    surfaceVariant = LightSurfaceGlass,
    outlineVariant = LightOutlineSoft,
    onPrimary = Color.Black,
    onBackground = LightSoftText,
    onSurface = LightSoftText,
    onSurfaceVariant = LightMutedText,
    primaryContainer = ElectricBlue.copy(alpha = 0.20f),
    secondaryContainer = EmeraldAccent.copy(alpha = 0.18f),
    tertiaryContainer = VioletAccent.copy(alpha = 0.18f)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(18.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

private val AppTypography = Typography(
    headlineSmall = TextStyle(
        fontSize = 28.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Normal
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Medium
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium
    )
)

@Composable
fun PasswordManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        shapes = AppShapes,
        typography = AppTypography,
        content = content
    )
}
