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
    primary = ProfRoyalBlue,
    onPrimary = Color.White,
    secondary = ProfEmerald,
    onSecondary = Color.White,
    tertiary = ProfTealAccent,
    background = ProfDarkBg,
    onBackground = Color(0xFFF1F5F9),
    surface = ProfDarkSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = ProfDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155)
)

private val LightColorScheme = lightColorScheme(
    primary = ProfNavyPrimary,
    onPrimary = Color.White,
    secondary = ProfRoyalBlue,
    onSecondary = Color.White,
    tertiary = ProfEmerald,
    onTertiary = Color.White,
    background = ProfLightBg,
    onBackground = ProfTextPrimary,
    surface = ProfSurfaceLight,
    onSurface = ProfTextPrimary,
    surfaceVariant = ProfSurfaceVariant,
    onSurfaceVariant = ProfTextSecondary,
    outline = ProfBorderLight
)

@Composable
fun NepScanTheme(
    themeMode: String = "SYSTEM",
    useDynamicColors: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }

    val context = LocalContext.current
    val colorScheme = when {
        useDynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
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
    NepScanTheme(themeMode = if (darkTheme) "DARK" else "LIGHT", content = content)
}
