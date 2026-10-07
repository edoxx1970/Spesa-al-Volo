package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BrandGoGreenDark,
    onPrimary = Color(0xFF05241A),
    primaryContainer = BrandGoGreenDarkSoft,
    onPrimaryContainer = BrandGoGreenDark,
    secondary = BrandGoGreenDark,
    onSecondary = Color(0xFF05241A),
    background = BgDark,
    onBackground = InkDark,
    surface = PaperDark,
    onSurface = InkDark,
    surfaceVariant = SoftDark,
    onSurfaceVariant = MutedDark,
    outline = LineDark,
    error = BrandLaserRedDark,
    onError = Color(0xFF2A0805),
    errorContainer = BrandLaserRedDarkSoft,
    onErrorContainer = BrandLaserRedDark
)

private val LightColorScheme = lightColorScheme(
    primary = BrandGoGreenLight,
    onPrimary = Color.White,
    primaryContainer = BrandGoGreenLightSoft,
    onPrimaryContainer = BrandGoGreenLight,
    secondary = BrandGoGreenLight,
    onSecondary = Color.White,
    background = BgLight,
    onBackground = InkLight,
    surface = PaperLight,
    onSurface = InkLight,
    surfaceVariant = SoftLight,
    onSurfaceVariant = MutedLight,
    outline = LineLight,
    error = BrandLaserRedLight,
    onError = Color.White,
    errorContainer = BrandLaserRedLightSoft,
    onErrorContainer = BrandLaserRedLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
