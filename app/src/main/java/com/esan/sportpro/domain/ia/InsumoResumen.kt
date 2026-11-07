package com.esan.sportpro.domain.ia

import com.esan.sportpro.domain.partido.EventoPartido
import com.esan.sportpro.domain.partido.JugadorConvocado
import com.esan.sportpro.domain.partido.JugadorEspecial
import com.esan.sportpro.domain.partido.Partido
import com.esan.sportpro.domain.partido.ReglasPartido
import com.esan.sportpro.domain.partido.TipoEvento

/** Jugador propio con su alias; la tabla alias → nombre solo existe en el dispositivo del entrenador. */
data class AliasJugador(
    val alias: String,
    val jugadorId: String,
    val nombre: String,
    val dorsal: Int?,
) {
    /** Nombre de pila y dorsal, lo único con lo que se identifica a un jugador en el resumen. */
    val nombreVisible: String
        get() {
            val pila = nombre.trim().substringBefore(' ').ifBlank { "Jugador" }
            return if (dorsal != null) "$pila ($dorsal)" else pila
        }
}

data class DatosPartidoInsumo(
    val categoria: String?,
    val equipo: String,
    val rival: String,
    val duracionTiempoMin: Int,
    val marcadorPropio: Int,
    val marcadorRival: Int,
)

data class JugadorInsumo(
    val alias: String,
    val dorsal: Int?,
    val titular: Boolean,
    val posicion: String?,
)

data class EventoInsumo(
    val id: String,
    val minuto: Int,
    val periodo: Int,
    val tipo: String,
    val sumaMarcador: Boolean,
    /** PROPIO, RIVAL o NINGUNO (eventos del partido como inicio o fin). */
    val equipo: String,
    /** Alias del jugador, [JugadorEspecial.RIVAL] o [JugadorEspecial.NO_IDENTIFICADO]. */
    val jugador: String?,
    val jugadorSecundario: String?,
    val resultadoPenal: String?,
    val observacion: String?,
)

/**
 * Lo único que sale hacia el proveedor de IA (US-023, criterios 2 y 3): eventos vigentes,
 * alineación, marcador y nombres de los equipos. Los jugadores van como alias; [aliases] se queda
 * en el dispositivo para reponer los nombres de pila al mostrar el texto.
 */
data class InsumoResumen(
    val partidoId: String,
    val partido: DatosPartidoInsumo,
    val alineacion: List<JugadorInsumo>,
    val eventos: List<EventoInsumo>,
    val incompletos: List<String>,
    val aliases: Map<String, AliasJugador>,
)

/** Arma el [InsumoResumen] a partir de los eventos vigentes; función pura, sin Firebase ni Android. */
object ConstructorInsumo {

    const val MAX_OBSERVACION = 140
    private const val DATO_OMITIDO = "[dato omitido]"

    /** Siete o más cifras seguidas, con a lo sumo un separador entre ellas (teléfono, DNI). */
    private val TELEFONO_O_DOCUMENTO = Regex("""(?:\d[ .\-]?){7,}""")
    private val CORREO = Regex("""\S+@\S+""")

    fun construir(
        partido: Partido,
        eventos: List<EventoPartido>,
        convocados: List<JugadorConvocado>,
        catalogo: List<TipoEvento>,
    ): InsumoResumen {
        val porId = asignarAlias(convocados)
        val aliases = porId.values.associateBy { it.alias }
        val vigentes = ReglasPartido.vigentes(eventos)
        val marcador = ReglasPartido.marcador(eventos, catalogo)
        val suman = catalogo.filter { it.sumaMarcador }.map { it.id }.toSet().ifEmpty { setOf("gol") }

        fun aliasDe(id: String?): String? = when {
            id == null -> null
            JugadorEspecial.esEspecial(id) -> id
            else -> porId[id]?.alias ?: JugadorEspecial.NO_IDENTIFICADO
        }

        return InsumoResumen(
            partidoId = partido.id,
            partido = DatosPartidoInsumo(
                categoria = partido.categoria,
                equipo = partido.nombreEquipo,
                rival = partido.nombreRival,
                duracionTiempoMin = partido.duracionTiempoMin,
                marcadorPropio = marcador.propio,
                marcadorRival = marcador.rival,
            ),
            alineacion = convocados
                .sortedWith(compareByDescending<JugadorConvocado> { it.titular }.thenBy { it.dorsal ?: Int.MAX_VALUE })
                .mapNotNull { c ->
                    porId[c.id]?.let { JugadorInsumo(it.alias, c.dorsal, c.titular, c.posicion) }
                },
            eventos = vigentes.map { e ->
                EventoInsumo(
                    id = e.id,
                    minuto = e.minuto,
                    periodo = ReglasPartido.periodoDe(e, eventos),
                    tipo = e.tipoNombre,
                    sumaMarcador = e.tipoId in suman,
                    equipo = e.equipo?.name ?: "NINGUNO",
                    jugador = aliasDe(e.jugadorId),
                    jugadorSecundario = aliasDe(e.jugadorSecundarioId),
                    resultadoPenal = e.resultadoPenal?.name,
                    observacion = sanear(e.observacion, porId.values),
                )
            },
            incompletos = vigentes.filter { it.incompleto }.map { it.id },
            aliases = aliases,
        )
    }

    /**
     * Alias estable por partido: `J` + dorsal de dos cifras (J09). Si no hay dorsal, o se repite,
     * se usa `JX1`, `JX2`… Así el proveedor nunca conoce nombres ni apellidos.
     */
    fun asignarAlias(convocados: List<JugadorConvocado>): Map<String, AliasJugador> {
        val usados = mutableSetOf<String>()
        var extra = 0
        val resultado = linkedMapOf<String, AliasJugador>()
        convocados
            .sortedWith(compareBy<JugadorConvocado> { it.dorsal ?: Int.MAX_VALUE }.thenBy { it.id })
            .forEach { c ->
                val base = c.dorsal?.takeIf { it in 0..99 }?.let { "J%02d".format(it) }
                val alias = if (base != null && usados.add(base)) {
                    base
                } else {
                    var candidato: String
                    do {
                        extra++
                        candidato = "JX$extra"
                    } while (!usados.add(candidato))
                    candidato
                }
                resultado[c.id] = AliasJugador(alias, c.id, c.nombre, c.dorsal)
            }
        return resultado
    }

    /**
     * La observación es texto libre del operador: se reemplazan los nombres de los convocados por su
     * alias, se ocultan teléfonos, documentos y correos, y se recorta (criterio 2).
     */
    fun sanear(observacion: String?, jugadores: Collection<AliasJugador>): String? {
        var texto = observacion?.trim().orEmpty()
        if (texto.isEmpty()) return null
        jugadores.forEach { j ->
            j.nombre.split(Regex("\\s+")).filter { it.length >= 3 }.forEach { parte ->
                texto = texto.replace(Regex("(?i)\\b${Regex.escape(parte)}\\b"), j.alias)
            }
        }
        texto = texto.replace(CORREO, DATO_OMITIDO).replace(TELEFONO_O_DOCUMENTO, DATO_OMITIDO)
        return texto.take(MAX_OBSERVACION).ifBlank { null }
    }
}
