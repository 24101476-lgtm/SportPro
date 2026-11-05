package com.esan.sportpro.domain.partido

/** Minutos que jugó un convocado del equipo propio (US-022, criterio 3). */
data class MinutosJugador(
    val jugadorId: String,
    val nombre: String,
    val dorsal: Int?,
    val titular: Boolean,
    val minutos: Int,
) {
    val etiqueta: String
        get() = if (dorsal != null) "$dorsal · $nombre" else nombre
}

/** Resumen del acta de un partido (US-022). Se calcula siempre a partir de los eventos vigentes. */
data class ActaPartido(
    val partidoId: String,
    val equipo: String,
    val rival: String,
    val categoria: String?,
    val marcadorPropio: Int,
    val marcadorRival: Int,
    val eventosVigentes: Int,
    val eventosAnulados: Int,
    val eventosIncompletos: Int,
    val minutosPorJugador: List<MinutosJugador>,
    val operadores: List<String>,
    val inicioRealMs: Long?,
    val finRealMs: Long?,
    val versionesCatalogo: List<Int>,
)

/** Reglas del cierre, la reapertura y el contenido del acta. Funciones puras, sin Firebase ni Android. */
object ReglasActa {

    const val ROL_ADMINISTRADOR = "ADMINISTRADOR"

    /** Un acta cerrada solo puede reabrirse durante estas horas posteriores al cierre (criterio 5). */
    const val VENTANA_REAPERTURA_HORAS = 48L

    private const val MS_POR_HORA = 3_600_000L

    fun construir(
        partido: Partido,
        eventos: List<EventoPartido>,
        convocados: List<JugadorConvocado>,
        catalogo: List<TipoEvento>,
    ): ActaPartido {
        val vigentes = ReglasPartido.vigentes(eventos)
        val marcador = ReglasPartido.marcador(eventos, catalogo)
        val inicio = vigentes.firstOrNull { it.tipoId == TiposEvento.INICIO_PARTIDO }
        val fin = vigentes.lastOrNull { it.tipoId == TiposEvento.FIN_PARTIDO }

        return ActaPartido(
            partidoId = partido.id,
            equipo = partido.nombreEquipo,
            rival = partido.nombreRival,
            categoria = partido.categoria,
            marcadorPropio = marcador.propio,
            marcadorRival = marcador.rival,
            eventosVigentes = vigentes.size,
            eventosAnulados = eventos.count { it.estado == EstadoEvento.ANULADO },
            eventosIncompletos = ReglasPartido.incompletos(eventos).size,
            minutosPorJugador = minutosJugados(partido, eventos, convocados),
            operadores = vigentes
                .mapNotNull { evento -> evento.operadorNombre?.trim()?.takeIf { it.isNotEmpty() } }
                .distinct(),
            inicioRealMs = partido.inicioPrimerTiempoMs ?: inicio?.registradoEnMs,
            finRealMs = fin?.registradoEnMs,
            versionesCatalogo = vigentes.map { it.versionCatalogo }.distinct().sorted(),
        )
    }

    /**
     * Minutos jugados por cada convocado a partir de la alineación titular y de los cambios y rojas
     * vigentes del equipo propio. El minuto de los eventos del 2.º tiempo vuelve a empezar en la
     * duración del tiempo, así que se le suma el tiempo añadido del 1.º para medir el tiempo real.
     * Un suplente que no ingresó figura con 0.
     */
    fun minutosJugados(
        partido: Partido,
        eventos: List<EventoPartido>,
        convocados: List<JugadorConvocado>,
    ): List<MinutosJugador> {
        val vigentes = ReglasPartido.vigentes(eventos)
        val duracion = partido.duracionTiempoMin.coerceAtLeast(1)
        val adicionPrimerTiempo = vigentes
            .firstOrNull { it.tipoId == TiposEvento.FIN_PRIMER_TIEMPO }
            ?.let { (it.minuto - duracion).coerceAtLeast(0) }
            ?: 0

        fun instante(evento: EventoPartido): Int =
            if (ReglasPartido.periodoDe(evento, eventos) == 2) evento.minuto + adicionPrimerTiempo else evento.minuto

        val finPartido = vigentes
            .lastOrNull { it.tipoId == TiposEvento.FIN_PARTIDO }
            ?.let(::instante)
            ?: (2 * duracion + adicionPrimerTiempo)

        fun jugadorReal(id: String?): String? = id?.takeUnless { JugadorEspecial.esEspecial(it) }

        val entradas = mutableMapOf<String, Int>()
        val salidas = mutableMapOf<String, Int>()
        convocados.filter { it.titular }.forEach { entradas[it.id] = 0 }

        vigentes.filter { it.equipo == EquipoEvento.PROPIO }.forEach { evento ->
            when (evento.tipoId) {
                TiposEvento.CAMBIO -> {
                    jugadorReal(evento.jugadorId)?.let { salidas.putIfAbsent(it, instante(evento)) }
                    jugadorReal(evento.jugadorSecundarioId)?.let { entradas.putIfAbsent(it, instante(evento)) }
                }
                TiposEvento.TARJETA_ROJA ->
                    jugadorReal(evento.jugadorId)?.let { salidas.putIfAbsent(it, instante(evento)) }
            }
        }

        return convocados
            .sortedWith(compareByDescending<JugadorConvocado> { it.titular }.thenBy { it.dorsal ?: Int.MAX_VALUE }.thenBy { it.nombre })
            .map { convocado ->
                val desde = entradas[convocado.id]
                val minutos = if (desde == null) 0 else ((salidas[convocado.id] ?: finPartido) - desde).coerceAtLeast(0)
                MinutosJugador(convocado.id, convocado.nombre, convocado.dorsal, convocado.titular, minutos)
            }
    }

    /** Límite (en ms) hasta el que el administrador puede reabrir el acta, o null si no hay marca de cierre. */
    fun limiteReaperturaMs(partido: Partido): Long? =
        partido.actaCerradaEnMs?.plus(VENTANA_REAPERTURA_HORAS * MS_POR_HORA)

    /** Solo el administrador, con el acta cerrada y dentro de las 48 h siguientes al cierre. */
    fun puedeReabrir(rol: String?, partido: Partido, ahoraMs: Long): Boolean {
        if (rol != ROL_ADMINISTRADOR || partido.estado != EstadoPartido.ACTA_CERRADA) return false
        val limite = limiteReaperturaMs(partido) ?: return false
        return ahoraMs <= limite
    }

    /** Texto plano del acta para el selector nativo de Android (criterio 7). */
    fun textoCompartible(acta: ActaPartido, formatoFechaHora: (Long) -> String): String = buildString {
        appendLine("ACTA DEL PARTIDO - SportPro")
        appendLine("${acta.equipo} vs ${acta.rival}" + (acta.categoria?.let { " ($it)" } ?: ""))
        appendLine("Resultado final: ${acta.equipo} ${acta.marcadorPropio} - ${acta.marcadorRival} ${acta.rival}")
        appendLine("Inicio: ${acta.inicioRealMs?.let(formatoFechaHora) ?: "sin registro"}")
        appendLine("Fin: ${acta.finRealMs?.let(formatoFechaHora) ?: "sin registro"}")
        appendLine("Operador: ${acta.operadores.joinToString(", ").ifEmpty { "no identificado" }}")
        appendLine("Catálogo de eventos: ${acta.versionesCatalogo.joinToString(", ") { "v$it" }.ifEmpty { "sin registro" }}")
        appendLine(
            "Eventos vigentes: ${acta.eventosVigentes} · anulados: ${acta.eventosAnulados} · " +
                "incompletos: ${acta.eventosIncompletos}",
        )
        appendLine()
        appendLine("Minutos jugados:")
        acta.minutosPorJugador.forEach { appendLine("- ${it.etiqueta}${if (it.titular) " (titular)" else ""}: ${it.minutos}'") }
    }.trimEnd()
}
