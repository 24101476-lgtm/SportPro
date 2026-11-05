package com.esan.sportpro.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/*
 * En modo oscuro el primario es el lima (se lee bien como texto sobre carbón).
 * En modo claro el primario es el musgo (el lima no se lee como texto sobre blanco);
 * ahí el lima solo aparece como relleno, a través de [ColoresSportPro.acento].
 */

private val ColoresClaros = lightColorScheme(
    primary = PaletaSportPro.Musgo,
    onPrimary = Color.White,
    primaryContainer = PaletaSportPro.MusgoClaro,
    onPrimaryContainer = PaletaSportPro.MusgoProfundo,
    inversePrimary = PaletaSportPro.Lima,
    secondary = PaletaSportPro.MusgoMedio,
    onSecondary = Color.White,
    secondaryContainer = PaletaSportPro.LimaSuave,
    onSecondaryContainer = PaletaSportPro.MusgoProfundo,
    tertiary = PaletaSportPro.Informacion,
    onTertiary = Color.White,
    background = PaletaSportPro.TizaFondo,
    onBackground = PaletaSportPro.Carbon,
    surface = PaletaSportPro.Blanco,
    onSurface = PaletaSportPro.Carbon,
    surfaceVariant = PaletaSportPro.TizaAlta,
    onSurfaceVariant = PaletaSportPro.Grafito,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = PaletaSportPro.Blanco,
    surfaceContainerLow = PaletaSportPro.TizaFondo,
    surfaceContainer = PaletaSportPro.Blanco,
    surfaceContainerHigh = PaletaSportPro.TizaAlta,
    surfaceContainerHighest = PaletaSportPro.TizaAlta,
    inverseSurface = PaletaSportPro.Carbon,
    inverseOnSurface = PaletaSportPro.Tiza,
    outline = PaletaSportPro.TizaLinea,
    outlineVariant = PaletaSportPro.TizaAlta,
    error = PaletaSportPro.Error,
    onError = Color.White,
    errorContainer = Color(0xFFFCE3DF),
    onErrorContainer = Color(0xFF7A1C12),
    scrim = Color(0x99000000),
)

private val ColoresOscuros = darkColorScheme(
    primary = PaletaSportPro.Lima,
    onPrimary = PaletaSportPro.Carbon,
    primaryContainer = PaletaSportPro.Musgo,
    onPrimaryContainer = PaletaSportPro.LimaSuave,
    inversePrimary = PaletaSportPro.Musgo,
    secondary = PaletaSportPro.LimaSuave,
    onSecondary = PaletaSportPro.Carbon,
    secondaryContainer = Color(0xFF3A4222),
    onSecondaryContainer = PaletaSportPro.LimaSuave,
    tertiary = PaletaSportPro.InformacionClaro,
    onTertiary = PaletaSportPro.Carbon,
    background = PaletaSportPro.Carbon,
    onBackground = PaletaSportPro.Tiza,
    surface = PaletaSportPro.CarbonSuperficie,
    onSurface = PaletaSportPro.Tiza,
    surfaceVariant = PaletaSportPro.CarbonAlto,
    onSurfaceVariant = PaletaSportPro.Niebla,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = PaletaSportPro.Carbon,
    surfaceContainerLow = PaletaSportPro.Carbon,
    surfaceContainer = PaletaSportPro.CarbonSuperficie,
    surfaceContainerHigh = PaletaSportPro.CarbonAlto,
    surfaceContainerHighest = PaletaSportPro.CarbonAlto,
    inverseSurface = PaletaSportPro.Tiza,
    inverseOnSurface = PaletaSportPro.Carbon,
    outline = PaletaSportPro.CarbonLinea,
    outlineVariant = PaletaSportPro.CarbonAlto,
    error = PaletaSportPro.ErrorClaro,
    onError = PaletaSportPro.Carbon,
    errorContainer = Color(0xFF4A1F1A),
    onErrorContainer = Color(0xFFFFD9D3),
    scrim = Color(0xCC000000),
)

/** Colores propios de SportPro que Material no cubre. Se leen con `SportProTema.colores`. */
@Immutable
data class ColoresSportPro(
    val esOscuro: Boolean,
    /** Relleno de la acción principal y de lo seleccionado. Siempre con [sobreAcento] encima. */
    val acento: Color,
    val sobreAcento: Color,
    /** Relleno suave para una tarjeta destacada. Siempre con [sobreSuave] encima. */
    val suave: Color,
    val sobreSuave: Color,
    /** Texto o ícono de acento que debe leerse sobre el fondo de la pantalla. */
    val textoAcento: Color,
    /** Indicador de la pestaña activa en la barra inferior y su ícono. */
    val indicadorNav: Color,
    val iconoNavActivo: Color,
    /** Estados (chips) legibles en el modo actual. */
    val exito: Color,
    val advertencia: Color,
    val error: Color,
    val informacion: Color,
    val neutro: Color,
)

private val ExtraClaros = ColoresSportPro(
    esOscuro = false,
    acento = PaletaSportPro.Lima,
    sobreAcento = PaletaSportPro.Carbon,
    suave = PaletaSportPro.LimaSuave,
    sobreSuave = PaletaSportPro.Carbon,
    textoAcento = PaletaSportPro.Musgo,
    indicadorNav = PaletaSportPro.Lima,
    iconoNavActivo = PaletaSportPro.Carbon,
    exito = PaletaSportPro.Exito,
    advertencia = Color(0xFFB45309),
    error = PaletaSportPro.Error,
    informacion = PaletaSportPro.Informacion,
    neutro = PaletaSportPro.Neutro,
)

private val ExtraOscuros = ColoresSportPro(
    esOscuro = true,
    acento = PaletaSportPro.Lima,
    sobreAcento = PaletaSportPro.Carbon,
    suave = PaletaSportPro.LimaSuave,
    sobreSuave = PaletaSportPro.Carbon,
    textoAcento = PaletaSportPro.Lima,
    indicadorNav = PaletaSportPro.Musgo,
    iconoNavActivo = PaletaSportPro.Lima,
    exito = PaletaSportPro.ExitoClaro,
    advertencia = PaletaSportPro.AdvertenciaClaro,
    error = PaletaSportPro.ErrorClaro,
    informacion = PaletaSportPro.InformacionClaro,
    neutro = PaletaSportPro.NeutroClaro,
)

val LocalColoresSportPro = staticCompositionLocalOf { ExtraClaros }

/** Acceso a los colores propios: `SportProTema.colores.acento`. */
object SportProTema {
    val colores: ColoresSportPro
        @Composable get() = LocalColoresSportPro.current
}

/**
 * Radios por jerarquía: chips pequeños 12 dp, tiles y campos 16 dp, tarjetas 24 dp,
 * encabezados 28 dp, hojas y diálogos 32 dp. Botones principales: píldora (CircleShape).
 */
val FormasSportPro = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** Claro u oscuro según la apariencia elegida en la app ([PreferenciaTema]); por defecto, la del teléfono. */
@Composable
fun SportProTheme(
    darkTheme: Boolean = PreferenciaTema.esOscuro(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalColoresSportPro provides if (darkTheme) ExtraOscuros else ExtraClaros) {
        MaterialTheme(
            colorScheme = if (darkTheme) ColoresOscuros else ColoresClaros,
            typography = SportProTypography,
            shapes = FormasSportPro,
            content = content,
        )
    }
}
