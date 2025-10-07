package com.p4handheld.ui.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = OnBluePrimary,
    primaryContainer = BluePrimaryContainer,
    onPrimaryContainer = OnBluePrimaryContainer,

    secondary = TealSecondary,
    onSecondary = OnTealSecondary,
    secondaryContainer = TealSecondaryContainer,
    onSecondaryContainer = OnTealSecondaryContainer,

    tertiary = OrangeTertiary,
    onTertiary = OnOrangeTertiary,
    tertiaryContainer = OrangeTertiaryContainer,
    onTertiaryContainer = OnOrangeTertiaryContainer,

    background = WhiteBackground,
    onBackground = OnGrayBackground,
    surface = GraySurface,
    onSurface = OnGraySurface,

    error = RedError,
    onError = OnRedError,
    errorContainer = RedErrorContainer,
    onErrorContainer = OnRedErrorContainer,

    outline = OutlineOnGraySurface,
)

val DarkColorScheme = darkColorScheme(
    primary = BluePrimaryContainer,
    onPrimary = OnBluePrimaryContainer,
    primaryContainer = BluePrimary,
    onPrimaryContainer = OnBluePrimary,

    secondary = TealSecondaryContainer,
    onSecondary = OnTealSecondaryContainer,
    secondaryContainer = TealSecondary,
    onSecondaryContainer = OnTealSecondary,

    tertiary = OrangeTertiaryContainer,
    onTertiary = OnOrangeTertiaryContainer,
    tertiaryContainer = OrangeTertiary,
    onTertiaryContainer = OnOrangeTertiary,

    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE0E0E0),

    error = RedErrorContainer,
    onError = OnRedErrorContainer,
    errorContainer = RedError,
    onErrorContainer = OnRedError,

    outline = OutlineOnGraySurface
)

@Composable
fun HandheldP4WTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}