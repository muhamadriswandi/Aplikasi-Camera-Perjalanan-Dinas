package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SurveyPrimaryAmber,
    onPrimary = Color.Black,
    primaryContainer = SurveyPrimaryAmberVariant,
    onPrimaryContainer = Color.White,
    secondary = SurveyAccentTeal,
    onSecondary = Color.Black,
    tertiary = SurveyGpsGreen,
    background = SurveyNavyDark,
    surface = SurveySurfaceDark,
    surfaceVariant = SurveySurfaceVariantDark,
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF94A3B8)
)

private val LightColorScheme = lightColorScheme(
    primary = SurveyPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF78350F),
    secondary = SurveySecondaryLight,
    onSecondary = Color.White,
    tertiary = Color(0xFF059669),
    background = SurveySurfaceLight,
    surface = Color.White,
    surfaceVariant = SurveySurfaceVariantLight,
    onBackground = SurveyTextLight,
    onSurface = SurveyTextLight,
    onSurfaceVariant = Color(0xFF475569)
)

@Composable
fun GeoCameraTheme(
    darkTheme: Boolean = true, // Surveyor apps default to dark theme for camera visibility & battery life
    dynamicColor: Boolean = false, // Keep high-contrast surveyor colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    GeoCameraTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
