package com.esan.sportpro.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.esan.sportpro.R

/** Títulos grandes: compacta y con personalidad. */
val FuenteTitulos = FontFamily(Font(R.font.bricolage_grotesque_extrabold, FontWeight.ExtraBold))

/** Números: marcadores, fechas, dorsales, estadísticas. */
val FuenteNumeros = FontFamily(Font(R.font.anton_regular, FontWeight.Normal))

/** Texto e interfaz. */
val FuenteTexto = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
)

/** Nota escrita a mano: máximo una por pantalla, en lima y ligeramente rotada. */
val FuenteMano = FontFamily(Font(R.font.caveat_semibold, FontWeight.SemiBold))

private val Titulo = TextStyle(
    fontFamily = FuenteTitulos,
    fontWeight = FontWeight.ExtraBold,
    letterSpacing = (-0.03).em,
)
private val Texto = TextStyle(fontFamily = FuenteTexto)

/** Escala tipográfica completa: todos los textos de la app deben salir de aquí. */
val SportProTypography = Typography(
    displayLarge = Titulo.copy(fontSize = 56.sp, lineHeight = 54.sp),
    displayMedium = Titulo.copy(fontSize = 44.sp, lineHeight = 42.sp),
    displaySmall = Titulo.copy(fontSize = 36.sp, lineHeight = 36.sp),
    headlineLarge = Titulo.copy(fontSize = 32.sp, lineHeight = 32.sp),
    headlineMedium = Titulo.copy(fontSize = 28.sp, lineHeight = 30.sp),
    headlineSmall = Titulo.copy(fontSize = 24.sp, lineHeight = 26.sp),
    titleLarge = Texto.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = Texto.copy(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 24.sp),
    titleSmall = Texto.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge = Texto.copy(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = Texto.copy(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = Texto.copy(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = Texto.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = Texto.copy(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = Texto.copy(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
)

/** Estilos extra fuera de la escala de Material. */
object EstilosSportPro {
    val NumeroGrande = TextStyle(fontFamily = FuenteNumeros, fontSize = 40.sp, lineHeight = 40.sp)
    val NumeroMediano = TextStyle(fontFamily = FuenteNumeros, fontSize = 28.sp, lineHeight = 28.sp)
    /** Color: `SportProTema.colores.textoAcento` sobre el fondo, o `acento` sobre una foto. */
    val NotaMano = TextStyle(
        fontFamily = FuenteMano,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 24.sp,
    )
}
