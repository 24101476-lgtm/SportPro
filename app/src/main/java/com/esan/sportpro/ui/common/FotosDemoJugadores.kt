package com.esan.sportpro.ui.common

import androidx.annotation.DrawableRes
import com.esan.sportpro.R
import java.text.Normalizer

/**
 * Fotos de demostración de los jugadores del plantel de prueba. Se usan solo cuando el jugador
 * todavía no tiene una foto subida (`fotoUrl` vacío), igual que los escudos de demo: en cuanto
 * se sube una foto real desde "Editar ficha", esa tiene prioridad.
 */
object FotosDemoJugadores {

    /** Palabras que debe contener el nombre (sin tildes) → foto. */
    private val fotos: List<Pair<List<String>, Int>> = listOf(
        listOf("adrian", "castillo") to R.drawable.jugador_demo_adrian_castillo_perez,
        listOf("mateo", "flores") to R.drawable.jugador_demo_mateo_flores_castro,
        listOf("hugo", "flores") to R.drawable.jugador_demo_hugo_flores_chavesta,
        listOf("nicolas", "herrera") to R.drawable.jugador_demo_nicolas_herrera_diaz,
        listOf("joaquin", "medina") to R.drawable.jugador_demo_joaquin_medina_salazar,
        listOf("sebastian", "mendoza") to R.drawable.jugador_demo_sebastian_mendoza_ruiz,
        listOf("alejandro", "navarro") to R.drawable.jugador_demo_alejandro_navarro_cruz,
        listOf("diego", "ramirez") to R.drawable.jugador_demo_diego_ramirez_torres,
        listOf("bruno", "saravia") to R.drawable.jugador_demo_bruno_saravia_rojas,
    )

    @DrawableRes
    fun fotoDe(nombre: String?): Int? {
        if (nombre.isNullOrBlank()) return null
        val palabras = normalizar(nombre).split(" ").toSet()
        return fotos.firstOrNull { (claves, _) -> claves.all { it in palabras } }?.second
    }

    /**
     * Modelo para Coil: la foto subida si existe; si no, la de demostración; si tampoco, `null`
     * (y la pantalla muestra las iniciales).
     */
    fun modelo(fotoUrl: String?, nombre: String?): Any? =
        fotoUrl?.takeIf { it.isNotBlank() } ?: fotoDe(nombre)

    private fun normalizar(texto: String): String =
        Normalizer.normalize(texto.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^a-z ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
}
