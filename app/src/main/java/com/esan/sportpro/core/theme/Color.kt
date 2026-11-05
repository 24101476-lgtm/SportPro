package com.esan.sportpro.core.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta única de SportPro: "fútbol bajo reflectores".
 * Neutros carbón (oscuro) o tiza (claro), verde musgo como marca y un verde lima eléctrico como
 * único acento. El lima se usa poco: la acción principal de cada pantalla y lo seleccionado.
 *
 * Los nombres anteriores (Verde, VerdeOscuro, Fondo, Texto…) se conservan para no romper el código
 * de los módulos. Los verdes de marca son tonos musgo oscuros, así el texto blanco que usan las
 * pantallas encima sigue siendo legible en ambos modos.
 */
object PaletaSportPro {
    // Acento
    val Lima = Color(0xFFD6F55A)
    val LimaSuave = Color(0xFFE8F6B4)

    // Musgo (marca)
    val Musgo = Color(0xFF4B5530)
    val MusgoProfundo = Color(0xFF262B1B)
    val MusgoMedio = Color(0xFF66733A)
    val MusgoClaro = Color(0xFFDDE3C4)

    // Carbón (modo oscuro)
    val Carbon = Color(0xFF121310)
    val CarbonSuperficie = Color(0xFF1C1D19)
    val CarbonAlto = Color(0xFF2A2C25)
    val CarbonLinea = Color(0xFF3A3D33)
    val Tiza = Color(0xFFF2F3EC)
    val Niebla = Color(0xFF9EA195)

    // Tiza (modo claro)
    val TizaFondo = Color(0xFFF2F3EC)
    val Blanco = Color(0xFFFFFFFF)
    val TizaAlta = Color(0xFFE6E8DF)
    val TizaLinea = Color(0xFFD3D6CA)
    val Grafito = Color(0xFF5B5F54)

    // Marca (nombres anteriores)
    val VerdeOscuro = MusgoProfundo
    /**
     * Verde de cancha: las pantallas lo usan como fondo con texto blanco y también como texto o
     * ícono sobre el fondo. Este tono se lee en los tres casos (claro, oscuro y con blanco encima).
     */
    val Verde = Color(0xFF62802B)
    val VerdeClaro = MusgoMedio
    val VerdeSuave = MusgoClaro
    val Dorado = Lima
    val DoradoSuave = LimaSuave

    // Neutros modo claro (nombres anteriores)
    val Fondo = TizaFondo
    val Superficie = Blanco
    val SuperficieVariante = TizaAlta
    val Texto = Carbon
    val TextoSecundario = Grafito
    val Borde = TizaLinea

    // Neutros modo oscuro (nombres anteriores)
    val FondoOscuro = Carbon
    val SuperficieOscura = CarbonSuperficie
    val SuperficieVarianteOscura = CarbonAlto
    val TextoOscuro = Tiza
    val TextoSecundarioOscuro = Niebla

    // Estados (semánticos): úsalos para chips, etiquetas y avisos, no para decorar.
    val Exito = Color(0xFF2E7D32)
    val Advertencia = Color(0xFFEF6C00)
    val Error = Color(0xFFD32F2F)
    val Informacion = Color(0xFF1565C0)
    val Neutro = Color(0xFF607D8B)

    // Estados aclarados para leerse sobre fondo oscuro.
    val ExitoClaro = Color(0xFF8BD17C)
    val AdvertenciaClaro = Color(0xFFF2B544)
    val ErrorClaro = Color(0xFFFF8A7A)
    val InformacionClaro = Color(0xFF8AB8F0)
    val NeutroClaro = Color(0xFFA9B3AD)

    // Acentos del partido (tarjetas, cronología)
    val Amarillo = Color(0xFFFBC02D)
    val Morado = Color(0xFF6A1B9A)
}

// Nombres anteriores, conservados para no romper código existente.
val SportProGreen = PaletaSportPro.Verde
val SportProGreenDark = PaletaSportPro.VerdeOscuro
val SportProAccent = PaletaSportPro.Dorado
val SportProBackground = PaletaSportPro.Fondo
