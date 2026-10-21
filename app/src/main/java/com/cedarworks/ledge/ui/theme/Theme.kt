package com.cedarworks.ledge.ui.theme

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
    primary = LedgeBlue,
    onPrimary = LedgeInk,
    primaryContainer = Color(0xFF3D4F73),
    onPrimaryContainer = Color(0xFFE4EAFF),
    secondary = LedgeGreen,
    onSecondary = Color(0xFF003822),
    secondaryContainer = Color(0xFF1E3D30),
    onSecondaryContainer = Color(0xFFB8F0D0),
    background = NightBackground,
    onBackground = NightOnSurface,
    surface = NightBackground,
    onSurface = NightOnSurface,
    surfaceContainer = NightSurface,
    surfaceContainerHigh = NightSurfaceHigh,
    onSurfaceVariant = NightOnSurfaceVariant,
    outline = Color(0xFF8E8E98),
    error = LedgeRed,
    onError = Color(0xFF3B0808),
    errorContainer = Color(0xFF5C2B28),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val LightColorScheme = lightColorScheme(
    primary = LedgeBlueDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7E2FF),
    onPrimaryContainer = LedgeInk,
    secondary = LedgeGreenDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC8F5DE),
    onSecondaryContainer = Color(0xFF002113),
    background = DayBackground,
    onBackground = DayOnSurface,
    surface = DayBackground,
    onSurface = DayOnSurface,
    surfaceContainer = DaySurface,
    surfaceContainerHigh = DaySurfaceHigh,
    onSurfaceVariant = DayOnSurfaceVariant,
    outline = Color(0xFF757680),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

@Composable
fun LedgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
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
        content = content,
    )
}
