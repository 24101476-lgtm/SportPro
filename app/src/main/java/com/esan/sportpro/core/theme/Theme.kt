package com.esan.sportpro.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = SportProGreen,
    secondary = SportProAccent,
    background = SportProBackground,
)

private val DarkColors = darkColorScheme(
    primary = SportProGreen,
    secondary = SportProAccent,
    background = SportProGreenDark,
)

@Composable
fun SportProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = SportProTypography,
        content = content,
    )
}
