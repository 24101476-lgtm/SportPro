package com.esan.sportpro.core.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta única de SportPro. Todos los módulos toman sus colores de aquí (directamente o a través
 * de `MaterialTheme.colorScheme`), para que la app se vea como una sola. Los tonos verdes son los
 * del módulo Partido en vivo, que se adoptó como línea gráfica de toda la app.
 */
object PaletaSportPro {
    // Marca
    val VerdeOscuro = Color(0xFF0B3D2E)
    val Verde = Color(0xFF1B7A3E)
    val VerdeClaro = Color(0xFF2E9E5B)
    val VerdeSuave = Color(0xFFDDF2E4)
    val Dorado = Color(0xFFF2A900)
    val DoradoSuave = Color(0xFFFFEDC2)

    // Neutros
    val Fondo = Color(0xFFF4F7F5)
    val Superficie = Color(0xFFFFFFFF)
    val SuperficieVariante = Color(0xFFE8EFEA)
    val Texto = Color(0xFF16201B)
    val TextoSecundario = Color(0xFF55635B)
    val Borde = Color(0xFFC5D1C9)

    // Modo oscuro
    val FondoOscuro = Color(0xFF0E1512)
    val SuperficieOscura = Color(0xFF17211C)
    val SuperficieVarianteOscura = Color(0xFF223029)
    val TextoOscuro = Color(0xFFE3ECE6)
    val TextoSecundarioOscuro = Color(0xFFA9B8AF)

    // Estados (semánticos): úsalos para chips, etiquetas y avisos, no para decorar.
    val Exito = Color(0xFF2E7D32)
    val Advertencia = Color(0xFFEF6C00)
    val Error = Color(0xFFD32F2F)
    val Informacion = Color(0xFF1565C0)
    val Neutro = Color(0xFF607D8B)

    // Acentos del partido (tarjetas, cronología)
    val Amarillo = Color(0xFFFBC02D)
    val Morado = Color(0xFF6A1B9A)
}

// Nombres anteriores, conservados para no romper código existente.
val SportProGreen = PaletaSportPro.Verde
val SportProGreenDark = PaletaSportPro.VerdeOscuro
val SportProAccent = PaletaSportPro.Dorado
val SportProBackground = PaletaSportPro.Fondo
