package com.esan.sportpro.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = PaletaSportPro.Verde,
    onPrimary = Color.White,
    primaryContainer = PaletaSportPro.VerdeSuave,
    onPrimaryContainer = PaletaSportPro.VerdeOscuro,
    secondary = PaletaSportPro.Dorado,
    onSecondary = PaletaSportPro.Texto,
    secondaryContainer = PaletaSportPro.DoradoSuave,
    onSecondaryContainer = Color(0xFF3D2C00),
    tertiary = PaletaSportPro.Informacion,
    onTertiary = Color.White,
    background = PaletaSportPro.Fondo,
    onBackground = PaletaSportPro.Texto,
    surface = PaletaSportPro.Superficie,
    onSurface = PaletaSportPro.Texto,
    surfaceVariant = PaletaSportPro.SuperficieVariante,
    onSurfaceVariant = PaletaSportPro.TextoSecundario,
    surfaceContainer = PaletaSportPro.SuperficieVariante,
    surfaceContainerLow = PaletaSportPro.Fondo,
    surfaceContainerHigh = PaletaSportPro.SuperficieVariante,
    outline = PaletaSportPro.Borde,
    outlineVariant = PaletaSportPro.SuperficieVariante,
    error = PaletaSportPro.Error,
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = PaletaSportPro.VerdeClaro,
    onPrimary = Color.White,
    primaryContainer = PaletaSportPro.VerdeOscuro,
    onPrimaryContainer = PaletaSportPro.VerdeSuave,
    secondary = PaletaSportPro.Dorado,
    onSecondary = PaletaSportPro.Texto,
    secondaryContainer = Color(0xFF4A3800),
    onSecondaryContainer = PaletaSportPro.DoradoSuave,
    tertiary = Color(0xFF90CAF9),
    background = PaletaSportPro.FondoOscuro,
    onBackground = PaletaSportPro.TextoOscuro,
    surface = PaletaSportPro.SuperficieOscura,
    onSurface = PaletaSportPro.TextoOscuro,
    surfaceVariant = PaletaSportPro.SuperficieVarianteOscura,
    onSurfaceVariant = PaletaSportPro.TextoSecundarioOscuro,
    surfaceContainer = PaletaSportPro.SuperficieVarianteOscura,
    surfaceContainerLow = PaletaSportPro.FondoOscuro,
    surfaceContainerHigh = PaletaSportPro.SuperficieVarianteOscura,
    outline = Color(0xFF52625A),
    outlineVariant = PaletaSportPro.SuperficieVarianteOscura,
    error = Color(0xFFEF9A9A),
)

/** Formas redondeadas compartidas: tarjetas 20 dp, diálogos 28 dp, campos y botones 14 dp. */
val FormasSportPro = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
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
        shapes = FormasSportPro,
        content = content,
    )
}
