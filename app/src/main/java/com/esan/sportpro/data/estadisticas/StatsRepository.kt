package com.esan.sportpro.data.estadisticas

import com.esan.sportpro.domain.estadisticas.ColumnaEstadisticaJugador
import com.esan.sportpro.domain.estadisticas.EstadisticaEquipo
import com.esan.sportpro.domain.estadisticas.EstadisticaJugador
import com.esan.sportpro.domain.estadisticas.FiltroEstadisticas
import com.esan.sportpro.domain.estadisticas.GolesPorPartidoPunto
import com.esan.sportpro.domain.estadisticas.TipoPartido
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * US-026 — calcula estadísticas por jugador y por equipo a partir de partidos finalizados.
 *
 * Los módulos de Partido (eventos de juego) y Entrenamientos (asistencia) son responsabilidad de
 * otros integrantes del equipo y, al momento de construir este módulo, todavía no exponen un
 * repositorio propio (ver `data/partido/` y `data/entrenamientos/`, ambos solo con `.gitkeep`).
 * Para no bloquear US-026 se consultan directamente las colecciones de Firestore con el esquema
 * acordado en el diseño del backlog:
 *
 * - `partidos`: equipoId, temporada, tipo (AMISTOSO|LIGA|TORNEO), estado (FINALIZADO|EN_CURSO|
 *   PROGRAMADO), fecha (Timestamp), rivalNombre, golesEquipo, golesRival.
 * - `eventos`: partidoId, equipoId, jugadorId (nulo = "Jugador no identificado"), tipo (GOL|
 *   ASISTENCIA|TARJETA_AMARILLA|TARJETA_ROJA|FALTA), minutosJugados, vigente (los anulados tienen
 *   vigente = false y se excluyen del cálculo, criterio de aceptación 1).
 * - `asistencias_entrenamiento`: equipoId, jugadorId, presente (Boolean), fecha.
 * - `jugadores`: nombre, equipoId.
 *
 * TODO(Jugadores / Partido / Entrenamientos): cuando esos módulos publiquen sus propios
 * repositorios, reemplazar las lecturas directas de abajo por llamadas a ellos — los nombres de
 * colección y campos deben mantenerse o actualizarse en ambos lados a la vez.
 */
@Singleton
class StatsRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) {

    private data class PartidoFinalizado(
        val id: String,
        val fecha: LocalDate,
        val rivalNombre: String,
        val golesEquipo: Int,
        val golesRival: Int,
    )

    private data class EventoPartido(
        val partidoId: String,
        val jugadorId: String?,
        val tipo: String,
        val minutosJugados: Int,
    )

    suspend fun obtenerTemporadasDisponibles(equipoId: String): List<String> = runCatching {
        firestore.collection("partidos")
            .whereEqualTo("equipoId", equipoId)
            .get()
            .await()
            .documents
            .mapNotNull { it.getString("temporada") }
            .distinct()
            .sortedDescending()
    }.getOrDefault(emptyList())

    suspend fun obtenerEstadisticaEquipo(equipoId: String, filtro: FiltroEstadisticas): EstadisticaEquipo {
        val partidos = partidosFinalizados(equipoId, filtro)
        if (partidos.isEmpty()) return EstadisticaEquipo()

        var ganados = 0
        var empatados = 0
        var perdidos = 0
        var golesFavor = 0
        var golesContra = 0
        partidos.forEach { p ->
            golesFavor += p.golesEquipo
            golesContra += p.golesRival
            when {
                p.golesEquipo > p.golesRival -> ganados++
                p.golesEquipo < p.golesRival -> perdidos++
                else -> empatados++
            }
        }
        return EstadisticaEquipo(
            partidosJugados = partidos.size,
            ganados = ganados,
            empatados = empatados,
            perdidos = perdidos,
            golesFavor = golesFavor,
            golesContra = golesContra,
        )
    }

    suspend fun obtenerGolesUltimosPartidos(equipoId: String, limite: Int = 10): List<GolesPorPartidoPunto> {
        val partidos = partidosFinalizados(equipoId, FiltroEstadisticas())
            .sortedByDescending { it.fecha }
            .take(limite)
            .sortedBy { it.fecha }
        return partidos.map {
            GolesPorPartidoPunto(
                partidoId = it.id,
                fecha = it.fecha,
                rivalNombre = it.rivalNombre,
                golesEquipo = it.golesEquipo,
                golesRival = it.golesRival,
            )
        }
    }

    /**
     * Devuelve la tabla de jugadores del equipo. Si [jugadorIdsPermitidos] no es nulo, solo se
     * incluyen esos jugadores (US-026, criterio 8: un padre solo ve a su(s) hijo(s) vinculado(s)).
     * La fila "Sin asignar" (criterio 6) siempre se agrega al final cuando existen eventos sin
     * jugador identificado, sin importar el filtro de jugadores permitidos.
     */
    suspend fun obtenerEstadisticasJugadores(
        equipoId: String,
        filtro: FiltroEstadisticas,
        jugadorIdsPermitidos: List<String>? = null,
    ): List<EstadisticaJugador> {
        val partidos = partidosFinalizados(equipoId, filtro)
        if (partidos.isEmpty()) return emptyList()
        val partidoIds = partidos.map { it.id }
        val eventos = eventosVigentesDe(partidoIds)
        val nombresPorJugador = nombresDeJugadores(equipoId)
        val asistenciaPorJugador = asistenciaPorJugador(equipoId, filtro)

        val eventosPorJugador = eventos.groupBy { it.jugadorId }
        val jugadorIds = (eventosPorJugador.keys.filterNotNull() + nombresPorJugador.keys)
            .distinct()
            .let { ids -> jugadorIdsPermitidos?.let { permitidos -> ids.filter { it in permitidos } } ?: ids }

        val filas = jugadorIds.map { jugadorId ->
            construirEstadistica(
                jugadorId = jugadorId,
                jugadorNombre = nombresPorJugador[jugadorId] ?: "Jugador sin nombre",
                eventos = eventosPorJugador[jugadorId].orEmpty(),
                partidosDelJugador = eventosPorJugador[jugadorId].orEmpty().map { it.partidoId }.distinct().size,
                porcentajeAsistencia = asistenciaPorJugador[jugadorId] ?: 0.0,
            )
        }

        val sinAsignar = eventosPorJugador[null].orEmpty()
        val filaSinAsignar = if (sinAsignar.isNotEmpty()) {
            listOf(
                construirEstadistica(
                    jugadorId = "sin-asignar",
                    jugadorNombre = "Sin asignar",
                    eventos = sinAsignar,
                    partidosDelJugador = sinAsignar.map { it.partidoId }.distinct().size,
                    porcentajeAsistencia = 0.0,
                ).copy(sinAsignar = true),
            )
        } else emptyList()

        return filas + filaSinAsignar
    }

    suspend fun obtenerEstadisticaJugador(
        equipoId: String,
        jugadorId: String,
        filtro: FiltroEstadisticas,
    ): EstadisticaJugador? = obtenerEstadisticasJugadores(equipoId, filtro).firstOrNull { it.jugadorId == jugadorId }

    fun ordenarJugadores(
        lista: List<EstadisticaJugador>,
        columna: ColumnaEstadisticaJugador,
        ascendente: Boolean,
    ): List<EstadisticaJugador> {
        val comparador = when (columna) {
            ColumnaEstadisticaJugador.JUGADOR -> compareBy<EstadisticaJugador> { it.jugadorNombre.lowercase() }
            ColumnaEstadisticaJugador.PARTIDOS -> compareBy { it.partidosJugados }
            ColumnaEstadisticaJugador.MINUTOS -> compareBy { it.minutosJugados }
            ColumnaEstadisticaJugador.GOLES -> compareBy { it.goles }
            ColumnaEstadisticaJugador.ASISTENCIAS -> compareBy { it.asistencias }
            ColumnaEstadisticaJugador.TARJETAS_AMARILLAS -> compareBy { it.tarjetasAmarillas }
            ColumnaEstadisticaJugador.TARJETAS_ROJAS -> compareBy { it.tarjetasRojas }
            ColumnaEstadisticaJugador.FALTAS -> compareBy { it.faltas }
            ColumnaEstadisticaJugador.ASISTENCIA_ENTRENAMIENTOS -> compareBy { it.porcentajeAsistenciaEntrenamientos }
        }
        // La fila "Sin asignar" siempre queda al final, sin importar el orden elegido (criterio 6).
        val (normales, sinAsignar) = lista.partition { !it.sinAsignar }
        val ordenadas = if (ascendente) normales.sortedWith(comparador) else normales.sortedWith(comparador.reversed())
        return ordenadas + sinAsignar
    }

    private fun construirEstadistica(
        jugadorId: String,
        jugadorNombre: String,
        eventos: List<EventoPartido>,
        partidosDelJugador: Int,
        porcentajeAsistencia: Double,
    ): EstadisticaJugador = EstadisticaJugador(
        jugadorId = jugadorId,
        jugadorNombre = jugadorNombre,
        partidosJugados = partidosDelJugador,
        minutosJugados = eventos.sumOf { it.minutosJugados },
        goles = eventos.count { it.tipo == "GOL" },
        asistencias = eventos.count { it.tipo == "ASISTENCIA" },
        tarjetasAmarillas = eventos.count { it.tipo == "TARJETA_AMARILLA" },
        tarjetasRojas = eventos.count { it.tipo == "TARJETA_ROJA" },
        faltas = eventos.count { it.tipo == "FALTA" },
        porcentajeAsistenciaEntrenamientos = porcentajeAsistencia,
    )

    private suspend fun partidosFinalizados(equipoId: String, filtro: FiltroEstadisticas): List<PartidoFinalizado> {
        var query: Query = firestore.collection("partidos")
            .whereEqualTo("equipoId", equipoId)
            .whereEqualTo("estado", "FINALIZADO")
        filtro.temporada?.let { query = query.whereEqualTo("temporada", it) }
        filtro.tipoPartido?.let { query = query.whereEqualTo("tipo", it.name) }

        return runCatching {
            query.get().await().documents.mapNotNull { doc ->
                val timestamp = doc.getTimestamp("fecha") ?: return@mapNotNull null
                val fecha = timestamp.toDate().toLocalDate()
                if (filtro.fechaInicio != null && fecha.isBefore(filtro.fechaInicio)) return@mapNotNull null
                if (filtro.fechaFin != null && fecha.isAfter(filtro.fechaFin)) return@mapNotNull null
                PartidoFinalizado(
                    id = doc.id,
                    fecha = fecha,
                    rivalNombre = doc.getString("rivalNombre") ?: "Rival",
                    golesEquipo = (doc.getLong("golesEquipo") ?: 0L).toInt(),
                    golesRival = (doc.getLong("golesRival") ?: 0L).toInt(),
                )
            }
        }.getOrDefault(emptyList())
    }

    /** Firestore limita `whereIn` a 30 valores, por eso se consulta en bloques (chunks). */
    private suspend fun eventosVigentesDe(partidoIds: List<String>): List<EventoPartido> {
        if (partidoIds.isEmpty()) return emptyList()
        val resultados = mutableListOf<EventoPartido>()
        partidoIds.chunked(30).forEach { bloque ->
            val docs = runCatching {
                firestore.collection("eventos")
                    .whereIn("partidoId", bloque)
                    .whereEqualTo("vigente", true)
                    .get()
                    .await()
                    .documents
            }.getOrDefault(emptyList())
            docs.forEach { doc ->
                resultados += EventoPartido(
                    partidoId = doc.getString("partidoId") ?: return@forEach,
                    jugadorId = doc.getString("jugadorId"),
                    tipo = doc.getString("tipo") ?: return@forEach,
                    minutosJugados = (doc.getLong("minutosJugados") ?: 0L).toInt(),
                )
            }
        }
        return resultados
    }

    private suspend fun nombresDeJugadores(equipoId: String): Map<String, String> = runCatching {
        firestore.collection("jugadores")
            .whereEqualTo("equipoId", equipoId)
            .get()
            .await()
            .documents
            .associate { it.id to (it.getString("nombre") ?: "Jugador sin nombre") }
    }.getOrDefault(emptyMap())

    private suspend fun asistenciaPorJugador(equipoId: String, filtro: FiltroEstadisticas): Map<String, Double> {
        val docs = runCatching {
            firestore.collection("asistencias_entrenamiento")
                .whereEqualTo("equipoId", equipoId)
                .get()
                .await()
                .documents
        }.getOrDefault(emptyList())

        val filtrados = docs.filter { doc ->
            val fecha = doc.getTimestamp("fecha")?.toDate()?.toLocalDate() ?: return@filter true
            (filtro.fechaInicio == null || !fecha.isBefore(filtro.fechaInicio)) &&
                (filtro.fechaFin == null || !fecha.isAfter(filtro.fechaFin))
        }

        return filtrados
            .groupBy { it.getString("jugadorId") }
            .filterKeys { it != null }
            .mapKeys { it.key!! }
            .mapValues { (_, registros) ->
                val presentes = registros.count { it.getBoolean("presente") == true }
                if (registros.isEmpty()) 0.0 else (presentes.toDouble() / registros.size) * 100.0
            }
    }

    private fun Date.toLocalDate(): LocalDate = this.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
}
