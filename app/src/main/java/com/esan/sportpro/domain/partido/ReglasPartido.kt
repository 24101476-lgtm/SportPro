package com.esan.sportpro.domain.partido

/**
 * Reglas de negocio del registro del partido. Son funciones puras (sin Firebase ni Android)
 * para que puedan probarse con pruebas unitarias y reutilizarse en la vista del espectador,
 * las estadísticas y el resumen narrativo.
 */
object ReglasPartido {

    const val MINUTO_MIN = 0
    const val MINUTO_MAX = 130
    const val MOTIVO_MIN = 5
    const val MOTIVO_MAX = 200

    data class Marcador(val propio: Int, val rival: Int)

    /** Eventos que cuentan para marcador, estadísticas y resumen: solo la versión vigente. */
    fun vigentes(eventos: List<EventoPartido>): List<EventoPartido> =
        eventos.filter { it.estado == EstadoEvento.VIGENTE }.ordenados()

    /** Cronología visible: vigentes y anulados (tachados). Las versiones reemplazadas solo van al historial. */
    fun cronologia(eventos: List<EventoPartido>): List<EventoPartido> =
        eventos.filter { it.estado != EstadoEvento.REEMPLAZADO }.ordenados()

    /**
     * Orden de la cronología: primero por tiempo (1.º / 2.º), luego por minuto y, a igual minuto,
     * el inicio de un tiempo va primero y su final al último. La hora real de registro desempata.
     * Así el tiempo añadido del 1.er tiempo (p. ej. 45+15' guardado como 60) queda antes del
     * inicio del 2.º tiempo (45').
     */
    fun List<EventoPartido>.ordenados(): List<EventoPartido> {
        val todos = this
        return sortedWith(
            compareBy<EventoPartido> { periodoDe(it, todos) }
                .thenBy { it.minuto }
                .thenBy { rangoControl(it.tipoId) }
                .thenBy { it.registradoEnMs ?: Long.MAX_VALUE },
        )
    }

    private fun rangoControl(tipoId: String): Int = when (tipoId) {
        TiposEvento.INICIO_PARTIDO, TiposEvento.INICIO_SEGUNDO_TIEMPO -> 0
        TiposEvento.FIN_PRIMER_TIEMPO, TiposEvento.FIN_PARTIDO -> 2
        else -> 1
    }

    /**
     * Tiempo (1 o 2) de un evento. Usa el campo guardado y, en eventos antiguos que no lo tienen,
     * lo deduce: los eventos de control tienen tiempo fijo y el resto se compara con la hora en
     * que se registró el inicio del 2.º tiempo.
     */
    fun periodoDe(evento: EventoPartido, eventos: List<EventoPartido>): Int {
        evento.periodo?.let { return it }
        when (evento.tipoId) {
            TiposEvento.INICIO_PARTIDO, TiposEvento.FIN_PRIMER_TIEMPO -> return 1
            TiposEvento.INICIO_SEGUNDO_TIEMPO, TiposEvento.FIN_PARTIDO -> return 2
        }
        val inicio2 = eventos
            .filter { it.tipoId == TiposEvento.INICIO_SEGUNDO_TIEMPO && it.estado != EstadoEvento.ANULADO }
            .minOfOrNull { it.registradoEnMs ?: Long.MAX_VALUE }
            ?: return 1
        val registrado = evento.registradoEnMs ?: return 2
        return if (registrado >= inicio2) 2 else 1
    }

    /**
     * Tiempo en el que cae un evento nuevo según el estado del partido: el 2.º tiempo empieza con
     * su propio evento de inicio; un fin de partido sin 2.º tiempo (p. ej. suspendido) también es 2.
     */
    fun periodoParaRegistro(tipoId: String, partido: Partido?, eventos: List<EventoPartido>): Int = when {
        tipoId == TiposEvento.INICIO_SEGUNDO_TIEMPO || tipoId == TiposEvento.FIN_PARTIDO -> 2
        tipoId == TiposEvento.INICIO_PARTIDO || tipoId == TiposEvento.FIN_PRIMER_TIEMPO -> 1
        partido?.inicioSegundoTiempoMs != null || existeVigente(TiposEvento.INICIO_SEGUNDO_TIEMPO, eventos) -> 2
        else -> 1
    }

    /**
     * Minuto tal como lo anota el árbitro: el tiempo añadido se escribe como "45+3'" o "90+2'".
     * [duracion] es la duración de cada tiempo del partido.
     */
    fun textoMinuto(minuto: Int, periodo: Int, duracion: Int): String {
        val fin = if (periodo == 1) duracion else 2 * duracion
        return if (minuto > fin) "$fin+${minuto - fin}'" else "$minuto'"
    }

    /**
     * "Registro fuera de orden" (US-019): el minuto es anterior al del último evento vigente del
     * mismo tiempo. El 2.º tiempo vuelve a empezar en 45', así que no se compara con el 1.º.
     */
    fun esFueraDeOrden(minuto: Int, periodo: Int, eventos: List<EventoPartido>): Boolean {
        val maximo = vigentes(eventos).filter { periodoDe(it, eventos) == periodo }.maxOfOrNull { it.minuto }
        return maximo != null && minuto < maximo
    }

    /** Valida que el minuto tenga sentido para el tiempo en curso. */
    fun validarMinutoPeriodo(minuto: Int, periodo: Int, duracion: Int): String? = when {
        periodo == 1 && minuto > duracion + ADICION_MAX ->
            "En el 1.er tiempo el minuto va de 0 a ${duracion + ADICION_MAX} (incluye el tiempo añadido)"
        periodo == 2 && minuto < duracion ->
            "En el 2.º tiempo el minuto empieza en $duracion'"
        else -> null
    }

    fun marcador(eventos: List<EventoPartido>, catalogo: List<TipoEvento>): Marcador {
        val sumaMarcador = catalogo.filter { it.sumaMarcador }.map { it.id }.toSet()
            .ifEmpty { setOf(TiposEvento.GOL) }
        val goles = vigentes(eventos).filter { it.tipoId in sumaMarcador }
        return Marcador(
            propio = goles.count { it.equipo == EquipoEvento.PROPIO },
            rival = goles.count { it.equipo == EquipoEvento.RIVAL },
        )
    }

    /** Ids de los jugadores propios que están en cancha tras aplicar cambios y expulsiones. */
    fun enCancha(convocados: List<JugadorConvocado>, eventos: List<EventoPartido>): Set<String> {
        val cancha = convocados.filter { it.titular }.map { it.id }.toMutableSet()
        vigentes(eventos).filter { it.equipo == EquipoEvento.PROPIO }.forEach { e ->
            when (e.tipoId) {
                TiposEvento.CAMBIO -> {
                    e.jugadorId?.let { cancha.remove(it) }
                    e.jugadorSecundarioId?.takeUnless { JugadorEspecial.esEspecial(it) }?.let { cancha.add(it) }
                }
                TiposEvento.TARJETA_ROJA -> e.jugadorId?.let { cancha.remove(it) }
            }
        }
        return cancha
    }

    /** Suplentes que todavía no han ingresado ni han sido expulsados. */
    fun suplentesDisponibles(convocados: List<JugadorConvocado>, eventos: List<EventoPartido>): List<JugadorConvocado> {
        val vig = vigentes(eventos).filter { it.equipo == EquipoEvento.PROPIO }
        val usados = vig.filter { it.tipoId == TiposEvento.CAMBIO }.mapNotNull { it.jugadorSecundarioId }.toSet()
        val expulsados = vig.filter { it.tipoId == TiposEvento.TARJETA_ROJA }.mapNotNull { it.jugadorId }.toSet()
        return convocados.filter { !it.titular && it.id !in usados && it.id !in expulsados }
    }

    fun cambiosRealizados(eventos: List<EventoPartido>): Int =
        vigentes(eventos).count { it.tipoId == TiposEvento.CAMBIO && it.equipo == EquipoEvento.PROPIO }

    fun amarillasDe(jugadorId: String, eventos: List<EventoPartido>): Int =
        vigentes(eventos).count { it.tipoId == TiposEvento.TARJETA_AMARILLA && it.jugadorId == jugadorId }

    fun incompletos(eventos: List<EventoPartido>): List<EventoPartido> =
        vigentes(eventos).filter { it.incompleto }

    fun pendientesDeSincronizar(eventos: List<EventoPartido>): Int =
        eventos.count { it.pendienteSincronizar }


    fun existeVigente(tipoId: String, eventos: List<EventoPartido>): Boolean =
        vigentes(eventos).any { it.tipoId == tipoId }

    /**
     * Posibles duplicados: mismo tipo, mismo minuto, mismo equipo y mismo jugador.
     * Suelen aparecer cuando dos operadores registran sin conexión y luego sincronizan.
     */
    fun duplicados(eventos: List<EventoPartido>): List<List<EventoPartido>> =
        vigentes(eventos)
            .groupBy { listOf(it.tipoId, it.minuto.toString(), it.equipo?.name, it.jugadorId) }
            .values
            .filter { it.size > 1 }

    /** Un evento queda Incompleto si le falta identificar al jugador requerido. */
    fun esIncompleto(b: BorradorEvento): Boolean {
        fun faltante(id: String?) = id == null || id == JugadorEspecial.NO_IDENTIFICADO
        return when {
            b.tipo.requiere(Campos.JUGADOR_SALE) || b.tipo.requiere(Campos.JUGADOR_ENTRA) ->
                faltante(b.jugadorId) || faltante(b.secundarioId)
            b.tipo.requiere(Campos.JUGADOR) -> faltante(b.jugadorId)
            else -> false
        }
    }

    /** Minutos máximos de tiempo añadido que se cuentan al final de cada tiempo. */
    const val ADICION_MAX = 15

    /** Estado del reloj del partido calculado a partir de las marcas de tiempo del servidor. */
    data class Reloj(
        /** Segundos de juego acumulados (el 2.º tiempo empieza en duración × 60). */
        val segundosJuego: Long,
        /** Minuto que se precarga en los eventos (incluye el tiempo añadido, p. ej. 47). */
        val minuto: Int,
        /** Minutos de tiempo añadido del tiempo en curso (0 si no aplica). */
        val adicion: Int,
        /** true cuando el 2.º tiempo agotó el tiempo añadido máximo: se cierra automáticamente. */
        val tiempoCumplido: Boolean,
        /** Avance del partido entre 0 y 1, para la barra de progreso. */
        val progreso: Float,
        /** Segundos transcurridos en el descanso (0 si no hay descanso en curso). */
        val segundosDescanso: Long = 0,
    ) {
        /** true cuando ya pasó la duración reglamentaria del tiempo en curso. */
        val enTiempoAnadido: Boolean get() = adicion > 0
    }

    /**
     * Calcula el reloj a partir de las marcas de tiempo del servidor, de modo que el
     * cronómetro sigue avanzando aunque la app esté en segundo plano o se haya cerrado.
     * - Cada tiempo corre hasta su duración (45' / 90') y luego cuenta tiempo añadido.
     *   Lo termina el operador con "Fin de primer tiempo" / "Fin de partido", como el árbitro.
     * - Descanso: el reloj de juego se detiene y se muestra cuánto lleva el descanso.
     * - Si nadie registra el final, el 2.º tiempo se cierra solo al agotar [ADICION_MAX].
     */
    fun reloj(partido: Partido, ahoraMs: Long): Reloj {
        val dur = partido.duracionTiempoMin.coerceAtLeast(1)
        val durSeg = dur * 60L
        val total = 2 * durSeg
        val maxAdicion = ADICION_MAX * 60L
        val inicio1 = partido.inicioPrimerTiempoMs
        return when {
            partido.estado == EstadoPartido.PROGRAMADO || inicio1 == null -> Reloj(0, 0, 0, false, 0f)
            partido.estado != EstadoPartido.EN_CURSO -> Reloj(total, 2 * dur, 0, true, 1f)
            partido.enDescanso -> Reloj(
                segundosJuego = durSeg,
                minuto = dur,
                adicion = 0,
                tiempoCumplido = false,
                progreso = 0.5f,
                segundosDescanso = partido.finPrimerTiempoMs?.let { segundosEntre(it, ahoraMs) } ?: 0,
            )
            partido.inicioSegundoTiempoMs != null -> {
                val seg = segundosEntre(partido.inicioSegundoTiempoMs, ahoraMs).coerceAtMost(durSeg + maxAdicion)
                val juego = durSeg + seg
                Reloj(
                    segundosJuego = juego,
                    minuto = (juego / 60).toInt(),
                    adicion = if (seg > durSeg) ((seg - durSeg) / 60).toInt() else 0,
                    tiempoCumplido = seg >= durSeg + maxAdicion,
                    progreso = (juego.coerceAtMost(total).toFloat() / total).coerceIn(0f, 1f),
                )
            }
            else -> {
                val seg = segundosEntre(inicio1, ahoraMs).coerceAtMost(durSeg + maxAdicion)
                Reloj(
                    segundosJuego = seg,
                    minuto = (seg / 60).toInt(),
                    adicion = if (seg > durSeg) ((seg - durSeg) / 60).toInt() else 0,
                    tiempoCumplido = false,
                    progreso = (seg.coerceAtMost(durSeg).toFloat() / total).coerceIn(0f, 0.5f),
                )
            }
        }
    }

    fun minutoActual(partido: Partido, ahoraMs: Long): Int =
        reloj(partido, ahoraMs).minuto.coerceIn(MINUTO_MIN, MINUTO_MAX)

    private fun segundosEntre(desdeMs: Long, hastaMs: Long): Long =
        ((hastaMs - desdeMs) / 1000L).coerceAtLeast(0)

    /**
     * Valida el borrador antes de guardar. Devuelve el mensaje de error o null si es válido.
     * [eventos] debe excluir el evento que se está corrigiendo, si aplica.
     */
    fun validar(
        b: BorradorEvento,
        convocados: List<JugadorConvocado>,
        eventos: List<EventoPartido>,
    ): String? {
        if (b.minuto !in MINUTO_MIN..MINUTO_MAX) return "Minuto inválido"
        if (b.tipo.requiere(Campos.EQUIPO) && b.equipo == null) return "Selecciona el equipo"
        if (b.tipo.requiere(Campos.JUGADOR) && b.jugadorId == null) return "Selecciona un jugador"
        if (b.tipo.requiere(Campos.JUGADOR_SALE) && b.jugadorId == null) return "Selecciona el jugador que sale"
        if (b.tipo.requiere(Campos.JUGADOR_ENTRA) && b.secundarioId == null) return "Selecciona el jugador que entra"
        if (b.tipo.requiere(Campos.RESULTADO) && b.resultadoPenal == null) {
            return "Indica si el penal fue convertido o fallado"
        }
        if (b.tipo.id == TiposEvento.CAMBIO && b.equipo == EquipoEvento.PROPIO) {
            val sale = b.jugadorId
            val entra = b.secundarioId
            if (sale != null && !JugadorEspecial.esEspecial(sale) && sale !in enCancha(convocados, eventos)) {
                return "El jugador que sale no está en cancha"
            }
            if (entra != null && !JugadorEspecial.esEspecial(entra) &&
                suplentesDisponibles(convocados, eventos).none { it.id == entra }
            ) {
                return "El jugador que entra debe ser un suplente no utilizado"
            }
            if (sale != null && sale == entra && !JugadorEspecial.esEspecial(sale)) {
                return "El jugador que sale y el que entra no pueden ser el mismo"
            }
        }
        return null
    }

    fun validarMotivo(motivo: String): String? {
        val largo = motivo.trim().length
        return if (largo < MOTIVO_MIN || largo > MOTIVO_MAX) {
            "El motivo debe tener entre $MOTIVO_MIN y $MOTIVO_MAX caracteres"
        } else {
            null
        }
    }

    /** Campos que cambian entre la versión vigente y la corrección propuesta. */
    fun camposModificados(original: EventoPartido, b: BorradorEvento): List<String> = buildList {
        if (original.minuto != b.minuto) add("minuto")
        if (original.equipo != b.equipo) add("equipo")
        if (original.jugadorId != b.jugadorId) add("jugador")
        if (original.jugadorSecundarioId != b.secundarioId) add("jugadorSecundario")
        if (original.resultadoPenal != b.resultadoPenal) add("resultado")
        if ((original.observacion ?: "") != (b.observacion ?: "")) add("observacion")
    }
}
