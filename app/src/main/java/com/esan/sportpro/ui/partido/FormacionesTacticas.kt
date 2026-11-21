package com.esan.sportpro.ui.partido

import androidx.compose.ui.graphics.Color
import com.esan.sportpro.domain.comunidad.PosicionJugador
import com.esan.sportpro.domain.comunidad.PosicionJugador.ARQUERO
import com.esan.sportpro.domain.comunidad.PosicionJugador.DEFENSA_CENTRAL
import com.esan.sportpro.domain.comunidad.PosicionJugador.DELANTERO
import com.esan.sportpro.domain.comunidad.PosicionJugador.EXTREMO
import com.esan.sportpro.domain.comunidad.PosicionJugador.LATERAL_DERECHO
import com.esan.sportpro.domain.comunidad.PosicionJugador.LATERAL_IZQUIERDO
import com.esan.sportpro.domain.comunidad.PosicionJugador.VOLANTE_DEFENSIVO
import com.esan.sportpro.domain.comunidad.PosicionJugador.VOLANTE_MIXTO
import com.esan.sportpro.domain.comunidad.PosicionJugador.VOLANTE_OFENSIVO

/**
 * Formaciones de la pizarra táctica (cancha vertical, arco propio abajo).
 *
 * Cada casilla sabe qué posiciones del catálogo prefiere; así, al elegir una formación,
 * el arquero va al arco, los defensas atrás y los delanteros arriba, en lugar de
 * repartirse según el orden de la lista.
 */
const val FORMACION_PERSONALIZADA = "Personalizada"

val FORMACIONES_DISPONIBLES = listOf("4-3-3", "4-4-2", "4-2-3-1", "3-5-2", "3-4-3")

private class Casilla(
    val x: Float,
    val y: Float,
    val preferidas: List<PosicionJugador>,
)

private fun c(x: Float, y: Float, vararg preferidas: PosicionJugador) =
    Casilla(x, y, preferidas.toList())

private val POR = c(0.50f, 0.91f, ARQUERO)

private val CASILLAS: Map<String, List<Casilla>> = mapOf(
    "4-3-3" to listOf(
        POR,
        c(0.14f, 0.70f, LATERAL_IZQUIERDO, LATERAL_DERECHO),
        c(0.38f, 0.73f, DEFENSA_CENTRAL),
        c(0.62f, 0.73f, DEFENSA_CENTRAL),
        c(0.86f, 0.70f, LATERAL_DERECHO, LATERAL_IZQUIERDO),
        c(0.24f, 0.46f, VOLANTE_MIXTO, VOLANTE_OFENSIVO),
        c(0.50f, 0.54f, VOLANTE_DEFENSIVO, VOLANTE_MIXTO),
        c(0.76f, 0.46f, VOLANTE_MIXTO, VOLANTE_OFENSIVO),
        c(0.16f, 0.22f, EXTREMO, DELANTERO),
        c(0.50f, 0.15f, DELANTERO, EXTREMO),
        c(0.84f, 0.22f, EXTREMO, DELANTERO),
    ),
    "4-4-2" to listOf(
        POR,
        c(0.14f, 0.70f, LATERAL_IZQUIERDO, LATERAL_DERECHO),
        c(0.38f, 0.73f, DEFENSA_CENTRAL),
        c(0.62f, 0.73f, DEFENSA_CENTRAL),
        c(0.86f, 0.70f, LATERAL_DERECHO, LATERAL_IZQUIERDO),
        c(0.14f, 0.44f, EXTREMO, VOLANTE_OFENSIVO, VOLANTE_MIXTO),
        c(0.38f, 0.50f, VOLANTE_MIXTO, VOLANTE_DEFENSIVO),
        c(0.62f, 0.50f, VOLANTE_DEFENSIVO, VOLANTE_MIXTO),
        c(0.86f, 0.44f, EXTREMO, VOLANTE_OFENSIVO, VOLANTE_MIXTO),
        c(0.34f, 0.18f, DELANTERO, EXTREMO),
        c(0.66f, 0.18f, DELANTERO, EXTREMO),
    ),
    "4-2-3-1" to listOf(
        POR,
        c(0.14f, 0.70f, LATERAL_IZQUIERDO, LATERAL_DERECHO),
        c(0.38f, 0.73f, DEFENSA_CENTRAL),
        c(0.62f, 0.73f, DEFENSA_CENTRAL),
        c(0.86f, 0.70f, LATERAL_DERECHO, LATERAL_IZQUIERDO),
        c(0.34f, 0.54f, VOLANTE_DEFENSIVO, VOLANTE_MIXTO),
        c(0.66f, 0.54f, VOLANTE_MIXTO, VOLANTE_DEFENSIVO),
        c(0.15f, 0.33f, EXTREMO, VOLANTE_OFENSIVO),
        c(0.50f, 0.36f, VOLANTE_OFENSIVO, VOLANTE_MIXTO),
        c(0.85f, 0.33f, EXTREMO, VOLANTE_OFENSIVO),
        c(0.50f, 0.13f, DELANTERO, EXTREMO),
    ),
    "3-5-2" to listOf(
        POR,
        c(0.24f, 0.70f, DEFENSA_CENTRAL, LATERAL_IZQUIERDO),
        c(0.50f, 0.73f, DEFENSA_CENTRAL),
        c(0.76f, 0.70f, DEFENSA_CENTRAL, LATERAL_DERECHO),
        c(0.11f, 0.44f, LATERAL_IZQUIERDO, EXTREMO, VOLANTE_MIXTO),
        c(0.31f, 0.50f, VOLANTE_MIXTO, VOLANTE_OFENSIVO),
        c(0.50f, 0.53f, VOLANTE_DEFENSIVO, VOLANTE_MIXTO),
        c(0.69f, 0.50f, VOLANTE_OFENSIVO, VOLANTE_MIXTO),
        c(0.89f, 0.44f, LATERAL_DERECHO, EXTREMO, VOLANTE_MIXTO),
        c(0.34f, 0.18f, DELANTERO, EXTREMO),
        c(0.66f, 0.18f, DELANTERO, EXTREMO),
    ),
    "3-4-3" to listOf(
        POR,
        c(0.24f, 0.70f, DEFENSA_CENTRAL, LATERAL_IZQUIERDO),
        c(0.50f, 0.73f, DEFENSA_CENTRAL),
        c(0.76f, 0.70f, DEFENSA_CENTRAL, LATERAL_DERECHO),
        c(0.13f, 0.47f, LATERAL_IZQUIERDO, VOLANTE_MIXTO, EXTREMO),
        c(0.38f, 0.52f, VOLANTE_DEFENSIVO, VOLANTE_MIXTO),
        c(0.62f, 0.52f, VOLANTE_MIXTO, VOLANTE_OFENSIVO),
        c(0.87f, 0.47f, LATERAL_DERECHO, VOLANTE_MIXTO, EXTREMO),
        c(0.16f, 0.21f, EXTREMO, DELANTERO),
        c(0.50f, 0.15f, DELANTERO, EXTREMO),
        c(0.84f, 0.21f, EXTREMO, DELANTERO),
    ),
)

/** Línea del campo a la que pertenece cada posición. */
enum class LineaCampo(val color: Color) {
    ARCO(Color(0xFFF59E0B)),
    DEFENSA(Color(0xFF2F7DE1)),
    MEDIO(Color(0xFF22A060)),
    ATAQUE(Color(0xFFE5484D)),
}

fun lineaDe(posicion: PosicionJugador): LineaCampo = when (posicion) {
    ARQUERO -> LineaCampo.ARCO
    DEFENSA_CENTRAL, LATERAL_DERECHO, LATERAL_IZQUIERDO -> LineaCampo.DEFENSA
    VOLANTE_DEFENSIVO, VOLANTE_MIXTO, VOLANTE_OFENSIVO -> LineaCampo.MEDIO
    EXTREMO, DELANTERO -> LineaCampo.ATAQUE
}

/** Abreviatura corta que se ve en la ficha ("Volante mixto" → "MC"). */
fun abreviaturaPosicion(etiqueta: String?): String =
    when (PosicionJugador.desdeEtiqueta(etiqueta) ?: PosicionJugador.desde(etiqueta)) {
        ARQUERO -> "POR"
        DEFENSA_CENTRAL -> "DFC"
        LATERAL_DERECHO -> "LD"
        LATERAL_IZQUIERDO -> "LI"
        VOLANTE_DEFENSIVO -> "MCD"
        VOLANTE_MIXTO -> "MC"
        VOLANTE_OFENSIVO -> "MCO"
        EXTREMO -> "EXT"
        DELANTERO -> "DC"
        null -> "—"
    }

fun colorPosicion(etiqueta: String?): Color =
    (PosicionJugador.desdeEtiqueta(etiqueta) ?: PosicionJugador.desde(etiqueta))
        ?.let { lineaDe(it).color }
        ?: Color(0xFF6B7280)

/** Qué tan bien encaja un jugador en una casilla: mayor es mejor. */
private fun puntaje(casilla: Casilla, etiqueta: String?): Int {
    val posicion = PosicionJugador.desdeEtiqueta(etiqueta) ?: PosicionJugador.desde(etiqueta)
        ?: return 1
    val indice = casilla.preferidas.indexOf(posicion)
    if (indice >= 0) return 20 - indice * 3

    val lineaCasilla = lineaDe(casilla.preferidas.first())
    val lineaJugador = lineaDe(posicion)
    return when {
        lineaCasilla == lineaJugador -> 8
        // Un arquero en el campo, o un jugador de campo en el arco, es la peor opción.
        lineaCasilla == LineaCampo.ARCO || lineaJugador == LineaCampo.ARCO -> 0
        kotlin.math.abs(lineaCasilla.ordinal - lineaJugador.ordinal) == 1 -> 4
        else -> 2
    }
}

/**
 * Ubica a los titulares en las casillas de [formacion] según su posición.
 * Si la formación no existe (p. ej. "Personalizada") devuelve la lista tal cual.
 */
fun distribuirEnFormacion(
    formacion: String,
    jugadores: List<JugadorCanchaUi>,
): List<JugadorCanchaUi> {
    val casillas = CASILLAS[formacion] ?: return jugadores
    val titulares = jugadores.take(casillas.size)

    // Asignación voraz por mejor encaje: con 11×11 pares es instantánea.
    val pares = buildList {
        casillas.forEachIndexed { ci, casilla ->
            titulares.forEachIndexed { ji, jugador ->
                add(Triple(ci, ji, puntaje(casilla, jugador.posicion)))
            }
        }
    }.sortedWith(compareByDescending<Triple<Int, Int, Int>> { it.third }.thenBy { it.first }.thenBy { it.second })

    val casillaDeJugador = HashMap<Int, Int>()
    val casillasUsadas = HashSet<Int>()
    for ((ci, ji, _) in pares) {
        if (ji in casillaDeJugador || ci in casillasUsadas) continue
        casillaDeJugador[ji] = ci
        casillasUsadas += ci
    }

    return titulares.mapIndexed { ji, jugador ->
        val casilla = casillas[casillaDeJugador.getValue(ji)]
        jugador.copy(x = casilla.x, y = casilla.y)
    } + jugadores.drop(casillas.size)
}
