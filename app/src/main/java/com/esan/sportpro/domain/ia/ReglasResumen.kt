package com.esan.sportpro.domain.ia

import com.esan.sportpro.domain.partido.EstadoPartido
import com.esan.sportpro.domain.partido.EventoPartido
import com.esan.sportpro.domain.partido.Partido
import com.esan.sportpro.domain.partido.ReglasPartido

/**
 * Reglas del resumen narrativo (US-023): cuándo se puede generar, verificación automática de lo
 * que escribió el modelo contra los eventos registrados y armado de los tres bloques.
 * Funciones puras, sin Firebase ni Android, para probarlas con pruebas unitarias.
 */
object ReglasResumen {

    const val MIN_EVENTOS = 3
    const val MAX_PALABRAS = 400

    /** Cambia cuando se modifica la instrucción del servidor; se guarda con cada versión. */
    const val VERSION_PROMPT = "v1"

    const val MOTIVO_SIN_ACTA = "Cierra el acta del partido para generar el resumen"
    const val MOTIVO_POCOS_EVENTOS = "No hay suficientes eventos registrados para generar el resumen"
    const val MOTIVO_SIN_CONEXION = "Necesitas conexión para generar el resumen"

    /** Mensaje que impide generar el resumen, o null si se puede (criterios 1 y 8). */
    fun motivoNoGenerar(partido: Partido, eventos: List<EventoPartido>, conectado: Boolean): String? = when {
        partido.estado != EstadoPartido.ACTA_CERRADA -> MOTIVO_SIN_ACTA
        ReglasPartido.vigentes(eventos).size < MIN_EVENTOS -> MOTIVO_POCOS_EVENTOS
        !conectado -> MOTIVO_SIN_CONEXION
        else -> null
    }

    fun contarPalabras(texto: String): Int = texto.split(Regex("\\s+")).count { it.isNotBlank() }

    fun dentroDelLimite(bloques: BloquesResumen): Boolean =
        contarPalabras(bloques.hechos) + contarPalabras(bloques.desarrollo) <= MAX_PALABRAS

    /** Hechos como lista y desarrollo como párrafos; la interpretación queda vacía para el entrenador. */
    fun armarBloques(salida: SalidaIa): BloquesResumen = BloquesResumen(
        hechos = salida.hechos.joinToString("\n") { "• ${it.texto.trim()}" },
        desarrollo = salida.desarrollo.joinToString("\n\n") { it.texto.trim() },
    )

    /** Cambia cada alias (`J09`) por el nombre de pila y el dorsal del jugador: "Luis (9)". */
    fun reidentificar(texto: String, aliases: Map<String, AliasJugador>): String {
        if (aliases.isEmpty()) return texto
        val patron = Regex("\\b(" + aliases.keys.joinToString("|") { Regex.escape(it) } + ")\\b")
        return patron.replace(texto) { aliases.getValue(it.value).nombreVisible }
    }

    /** Fragmento del texto; [noVerificado] indica que se muestra resaltado con "Dato no verificado". */
    data class Segmento(val texto: String, val noVerificado: Boolean)

    /** Parte [texto] en fragmentos normales y fragmentos que coinciden con alguna mención no verificada. */
    fun segmentar(texto: String, menciones: List<MencionNoVerificada>): List<Segmento> {
        val rangos = mutableListOf<IntRange>()
        menciones.map { it.texto }.filter { it.isNotBlank() }.distinct().forEach { mencion ->
            var desde = 0
            while (true) {
                val inicio = texto.indexOf(mencion, desde)
                if (inicio < 0) break
                rangos += inicio until inicio + mencion.length
                desde = inicio + mencion.length
            }
        }
        val resultado = mutableListOf<Segmento>()
        var cursor = 0
        rangos.sortedWith(compareBy({ it.first }, { -it.last })).forEach { rango ->
            if (rango.first < cursor) return@forEach
            if (rango.first > cursor) resultado += Segmento(texto.substring(cursor, rango.first), false)
            resultado += Segmento(texto.substring(rango.first, rango.last + 1), true)
            cursor = rango.last + 1
        }
        if (cursor < texto.length) resultado += Segmento(texto.substring(cursor), false)
        return resultado
    }

    // ------------------------------------------------------------------ Verificación (criterio 7)

    private val ALIAS = Regex("\\b(J\\d{2}|JX\\d+)\\b")
    private val MINUTO_ANADIDO = Regex("(?<!\\d)(\\d{1,3})\\+(\\d{1,2})\\s*['’′]")
    private val MINUTO_APOSTROFE = Regex("(?<!\\d)(\\d{1,3})\\s*['’′]")
    private val MINUTO_PALABRA = Regex("(?i)\\bmin(?:uto|utos|\\.)?\\s+(\\d{1,3})\\b")
    private val MARCADOR = Regex("(?<![\\d+])(\\d{1,2})\\s*[-–]\\s*(\\d{1,2})(?![\\d'’′])")
    private val NOMBRE_PROPIO = Regex("\\p{Lu}\\p{Ll}{2,}")
    private val INICIO_DE_ORACION = setOf('.', '!', '?', ':', ';', '¡', '¿', '(', '"', '«', '-', '–', '•', '*')

    /**
     * Compara el texto con los eventos registrados y devuelve todo lo que no se puede respaldar:
     * alias de jugadores inexistentes, minutos sin evento, marcadores que nunca ocurrieron,
     * nombres propios fuera de los equipos y hechos sin una cita de evento válida.
     */
    fun verificar(salida: SalidaIa, insumo: InsumoResumen): List<MencionNoVerificada> {
        val resultado = linkedMapOf<String, MencionNoVerificada>()
        fun marcar(texto: String, motivo: String) {
            if (texto.isNotBlank()) resultado.putIfAbsent(texto, MencionNoVerificada(texto, motivo))
        }

        val idsValidos = insumo.eventos.map { it.id }.toSet()
        salida.hechos.forEach { hecho ->
            if (hecho.eventoIds.isEmpty() || hecho.eventoIds.any { it !in idsValidos }) {
                marcar(hecho.texto.trim(), "El hecho no cita un evento registrado")
            }
        }

        val minutos = insumo.eventos.map { it.minuto }.toSet()
        val marcadores = marcadoresPosibles(insumo)
        val vocabulario = vocabularioPermitido(insumo)
        (salida.hechos + salida.desarrollo).map { it.texto }.forEach { texto ->
            verificarTexto(texto, insumo, minutos, marcadores, vocabulario, ::marcar)
        }
        return resultado.values.toList()
    }

    private fun verificarTexto(
        texto: String,
        insumo: InsumoResumen,
        minutos: Set<Int>,
        marcadores: Set<String>,
        vocabulario: Set<String>,
        marcar: (String, String) -> Unit,
    ) {
        ALIAS.findAll(texto).forEach { m ->
            if (m.value !in insumo.aliases) marcar(m.value, "Jugador que no está en la alineación")
        }

        val ocupados = mutableListOf<IntRange>()
        fun libre(rango: IntRange) = ocupados.none { it.first <= rango.last && rango.first <= it.last }
        MINUTO_ANADIDO.findAll(texto).forEach { m ->
            ocupados += m.range
            val minuto = m.groupValues[1].toInt() + m.groupValues[2].toInt()
            if (minuto !in minutos) marcar(m.value, "Minuto sin un evento registrado")
        }
        (MINUTO_APOSTROFE.findAll(texto) + MINUTO_PALABRA.findAll(texto)).forEach { m ->
            if (!libre(m.range)) return@forEach
            val minuto = m.groupValues[1].toInt()
            if (minuto !in minutos) marcar(m.value, "Minuto sin un evento registrado")
        }

        MARCADOR.findAll(texto).forEach { m ->
            if ("${m.groupValues[1].toInt()}-${m.groupValues[2].toInt()}" !in marcadores) {
                marcar(m.value, "Marcador que no ocurrió en el partido")
            }
        }

        NOMBRE_PROPIO.findAll(texto).forEach { m ->
            if (!esInicioDeOracion(texto, m.range.first) && m.value.lowercase() !in vocabulario) {
                marcar(m.value, "Nombre que no figura en los datos del partido")
            }
        }
    }

    /** Marcadores `propio-rival` por los que pasó el partido, del 0-0 al final. */
    private fun marcadoresPosibles(insumo: InsumoResumen): Set<String> {
        var propio = 0
        var rival = 0
        val resultado = mutableSetOf("0-0")
        insumo.eventos.filter { it.sumaMarcador }.forEach { e ->
            when (e.equipo) {
                "PROPIO" -> propio++
                "RIVAL" -> rival++
            }
            resultado += "$propio-$rival"
        }
        resultado += "${insumo.partido.marcadorPropio}-${insumo.partido.marcadorRival}"
        return resultado
    }

    /** Palabras en mayúscula que sí pueden aparecer: equipos, categoría y tipos de evento. */
    private fun vocabularioPermitido(insumo: InsumoResumen): Set<String> {
        val fuentes = listOfNotNull(insumo.partido.equipo, insumo.partido.rival, insumo.partido.categoria) +
            insumo.eventos.map { it.tipo }
        return fuentes.flatMap { NOMBRE_PROPIO.findAll(it).map { m -> m.value.lowercase() }.toList() }.toSet()
    }

    private fun esInicioDeOracion(texto: String, posicion: Int): Boolean {
        var i = posicion - 1
        while (i >= 0 && (texto[i] == ' ' || texto[i] == '\t')) i--
        return i < 0 || texto[i] == '\n' || texto[i] in INICIO_DE_ORACION
    }
}
