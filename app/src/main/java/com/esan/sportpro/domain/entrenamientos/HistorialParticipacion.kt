package com.esan.sportpro.domain.entrenamientos

import com.esan.sportpro.domain.academia.Equipo
import com.esan.sportpro.domain.cuentas.PerfilActual
import com.esan.sportpro.domain.jugadores.Jugador
import com.esan.sportpro.domain.jugadores.Vinculacion
import com.esan.sportpro.domain.partido.EstadoPartido
import com.esan.sportpro.domain.partido.EstadoRespuestaConvocatoria
import com.esan.sportpro.domain.partido.EventoPartido
import com.esan.sportpro.domain.partido.JugadorConvocado
import com.esan.sportpro.domain.partido.Partido
import com.esan.sportpro.domain.partido.ReglasActa
import com.esan.sportpro.domain.partido.RespuestaConvocatoria
import com.esan.sportpro.navigation.UserRole
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.Normalizer
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Tipo de actividad del historial (US-012, criterio 3). */
enum class TipoActividad(val etiqueta: String) {
    ENTRENAMIENTO("Entrenamiento"),
    PARTIDO("Partido"),
}

/**
 * Una sesión del historial de un jugador: fecha, equipo, estado de asistencia y minutos de
 * retraso cuando corresponde (criterio 2).
 */
data class RegistroParticipacion(
    val id: String,
    val tipo: TipoActividad,
    val fecha: LocalDate,
    val hora: String,
    val equipoNombre: String,
    val descripcion: String,
    val estado: EstadoAsistencia,
    val minutosRetraso: Int? = null,
    /** Solo partidos: minutos que jugó según la alineación y los cambios (0 = suplente sin ingresar). */
    val minutosJugados: Int? = null,
)

/** Filtros de la vista: rango de fechas (inclusive) y tipo de actividad; `null` = sin filtro. */
data class FiltroHistorial(
    val desde: LocalDate? = null,
    val hasta: LocalDate? = null,
    val tipo: TipoActividad? = null,
)

/** Totales del periodo filtrado (criterio 1). */
data class ResumenHistorial(
    val convocadas: Int = 0,
    val asistencias: Int = 0,
    val tardanzas: Int = 0,
    val justificadas: Int = 0,
    val injustificadas: Int = 0,
) {
    /** Asistencias más tardanzas. */
    val participaciones: Int get() = asistencias + tardanzas

    /** Criterio 6: (asistencias + tardanzas) / convocadas, con un decimal. */
    val porcentaje: Double get() = ReglasHistorial.porcentaje(participaciones, convocadas)
}

/** Barra del gráfico mensual (criterio 7). */
data class AsistenciaMensual(
    val mes: YearMonth,
    val convocadas: Int,
    val participaciones: Int,
    /** "Oct", o "Dic 25" si el mes es de otro año. */
    val etiqueta: String = ReglasHistorial.etiquetaMes(mes),
) {
    val porcentaje: Double get() = ReglasHistorial.porcentaje(participaciones, convocadas)
}

/** Reglas del historial de participación (US-012). Sin dependencias de Android ni Firebase. */
object ReglasHistorial {
    const val MENSAJE_VACIO = "No hay registros en el periodo seleccionado"
    const val MENSAJE_RANGO_INVALIDO = "La fecha inicial no puede ser posterior a la final"
    const val MESES_GRAFICO = 6

    private val ZONA_PERU: ZoneId = ZoneId.of("America/Lima")
    private val FORMATO_HORA: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private val MESES = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Set", "Oct", "Nov", "Dic")

    /**
     * Sesiones de entrenamiento convocadas del jugador: solo las Realizadas de su equipo en las que
     * figura con un estado. Las programadas, canceladas y las anteriores a su ingreso no cuentan.
     */
    fun desdeEntrenamientos(jugadorId: String, entrenamientos: List<Entrenamiento>): List<RegistroParticipacion> =
        entrenamientos
            .filter { it.realizada }
            .mapNotNull { sesion ->
                val estado = EstadoAsistencia.desde(sesion.asistencia[jugadorId]) ?: return@mapNotNull null
                val fecha = ValidacionEntrenamiento.parsearFecha(sesion.fecha) ?: return@mapNotNull null
                RegistroParticipacion(
                    id = sesion.id,
                    tipo = TipoActividad.ENTRENAMIENTO,
                    fecha = fecha,
                    hora = sesion.hora,
                    equipoNombre = sesion.equipoNombre,
                    descripcion = sesion.objetivo,
                    estado = estado,
                    minutosRetraso = if (estado == EstadoAsistencia.TARDE) {
                        sesion.asistenciaDetalle[jugadorId]?.minutosRetraso
                    } else {
                        null
                    },
                )
            }

    /** Un partido entra al historial cuando terminó: finalizado o con el acta cerrada. */
    fun partidoCuenta(partido: Partido): Boolean =
        partido.estado == EstadoPartido.FINALIZADO || partido.estado == EstadoPartido.ACTA_CERRADA

    /**
     * Registro de un partido terminado para el jugador, o `null` si no le corresponde:
     * - En la alineación final (`convocados`): Presente, con los minutos que jugó (0 si fue suplente
     *   y no ingresó); los minutos salen de [ReglasActa.minutosJugados], igual que en el acta.
     * - Convocado que respondió "No disponible": falta justificada, con su motivo.
     * - Convocado que no respondió y no estuvo en la alineación: falta injustificada.
     * - Convocado que confirmó pero el entrenador no incluyó en la alineación: no se cuenta,
     *   porque no depende del jugador.
     */
    fun desdePartido(
        jugadorId: String,
        partido: Partido,
        convocados: List<JugadorConvocado>,
        respuestas: List<RespuestaConvocatoria>,
        eventos: List<EventoPartido>,
        zona: ZoneId = ZONA_PERU,
    ): RegistroParticipacion? {
        if (!partidoCuenta(partido)) return null
        val instante = partido.fechaMs?.let { Instant.ofEpochMilli(it).atZone(zona) } ?: return null
        val respuesta = respuestas.firstOrNull { it.jugadorId == jugadorId }
        val enAlineacion = convocados.any { it.id == jugadorId }

        val marcador = "${partido.golesEquipo}-${partido.golesRival}"
        val rival = "vs ${partido.nombreRival.ifBlank { "Rival" }} · $marcador"

        val (estado, minutos, detalle) = when {
            enAlineacion -> {
                val minutos = ReglasActa.minutosJugados(partido, eventos, convocados)
                    .firstOrNull { it.jugadorId == jugadorId }?.minutos ?: 0
                Triple(
                    EstadoAsistencia.PRESENTE,
                    minutos,
                    if (minutos > 0) "Jugó $minutos min" else "Suplente, no ingresó",
                )
            }
            respuesta?.estado == EstadoRespuestaConvocatoria.NO_DISPONIBLE -> Triple(
                EstadoAsistencia.JUSTIFICADO,
                null,
                respuesta.motivo?.trim()?.takeIf { it.isNotEmpty() }?.let { "No disponible: $it" } ?: "No disponible",
            )
            respuesta?.estado == EstadoRespuestaConvocatoria.SIN_RESPONDER ->
                Triple(EstadoAsistencia.AUSENTE, null, "No respondió la convocatoria")
            else -> return null
        }

        return RegistroParticipacion(
            id = partido.id,
            tipo = TipoActividad.PARTIDO,
            fecha = instante.toLocalDate(),
            hora = partido.hora?.takeIf { it.isNotBlank() } ?: instante.format(FORMATO_HORA),
            equipoNombre = listOfNotNull(partido.nombreEquipo.ifBlank { null }, partido.categoria?.ifBlank { null })
                .joinToString(" · "),
            descripcion = "$rival · $detalle",
            estado = estado,
            minutosJugados = minutos,
        )
    }

    /** Criterio 2: de la más reciente a la más antigua. */
    fun ordenar(registros: List<RegistroParticipacion>): List<RegistroParticipacion> =
        registros.sortedWith(compareByDescending<RegistroParticipacion> { it.fecha }.thenByDescending { it.hora })

    fun validarFiltro(filtro: FiltroHistorial): String? {
        val desde = filtro.desde ?: return null
        val hasta = filtro.hasta ?: return null
        return if (desde.isAfter(hasta)) MENSAJE_RANGO_INVALIDO else null
    }

    /** Criterio 3: rango de fechas inclusive y tipo de actividad. */
    fun filtrar(registros: List<RegistroParticipacion>, filtro: FiltroHistorial): List<RegistroParticipacion> =
        registros.filter { registro ->
            (filtro.tipo == null || registro.tipo == filtro.tipo) &&
                (filtro.desde == null || !registro.fecha.isBefore(filtro.desde)) &&
                (filtro.hasta == null || !registro.fecha.isAfter(filtro.hasta))
        }

    fun resumen(registros: List<RegistroParticipacion>): ResumenHistorial = ResumenHistorial(
        convocadas = registros.size,
        asistencias = registros.count { it.estado == EstadoAsistencia.PRESENTE },
        tardanzas = registros.count { it.estado == EstadoAsistencia.TARDE },
        justificadas = registros.count { it.estado == EstadoAsistencia.JUSTIFICADO },
        injustificadas = registros.count { it.estado == EstadoAsistencia.AUSENTE },
    )

    /** Porcentaje con un decimal; 0.0 si no hay sesiones. */
    fun porcentaje(participaciones: Int, total: Int): Double =
        if (total == 0) 0.0 else BigDecimal(participaciones * 100.0 / total).setScale(1, RoundingMode.HALF_UP).toDouble()

    /**
     * Criterio 7: los últimos seis meses (incluido el actual), del más antiguo al más reciente.
     * Los meses sin sesiones aparecen con 0 convocadas.
     */
    fun mensual(registros: List<RegistroParticipacion>, hoy: LocalDate): List<AsistenciaMensual> {
        val actual = YearMonth.from(hoy)
        val porMes = registros.groupBy { YearMonth.from(it.fecha) }
        return (MESES_GRAFICO - 1 downTo 0).map { atras ->
            val mes = actual.minusMonths(atras.toLong())
            val delMes = porMes[mes].orEmpty()
            AsistenciaMensual(
                mes = mes,
                convocadas = delMes.size,
                participaciones = delMes.count { it.estado.participo },
                etiqueta = etiquetaMes(mes, actual.year),
            )
        }
    }

    /** "Oct", o "Oct 25" si el mes no es del año en curso. */
    fun etiquetaMes(mes: YearMonth, anioActual: Int = mes.year): String {
        val nombre = MESES[mes.monthValue - 1]
        return if (mes.year == anioActual) nombre else "$nombre ${mes.year % 100}"
    }

    /**
     * Criterio 5: el jugador ve solo su historial; el padre, el de sus hijos con vínculo aprobado;
     * el entrenador, el de los equipos que dirige; el administrador, el de toda la academia.
     *
     * Un equipo sin entrenador asignado ([Equipo.entrenadorNombre] vacío) queda visible para todos
     * los entrenadores de la academia, para no dejar a sus jugadores sin seguimiento.
     */
    fun puedeVer(
        perfil: PerfilActual,
        jugador: Jugador,
        equipo: Equipo?,
        vinculaciones: List<Vinculacion> = emptyList(),
    ): Boolean = when (perfil.rol) {
        UserRole.ADMINISTRADOR -> jugador.academiaId.isBlank() || jugador.academiaId == perfil.academiaId
        UserRole.ENTRENADOR -> equipo != null && equipo.id == jugador.equipoId &&
            (equipo.entrenadorNombre.isBlank() || normalizar(equipo.entrenadorNombre) == normalizar(perfil.nombre))
        UserRole.PADRE_DE_FAMILIA -> vinculaciones.any {
            it.jugadorId == jugador.id && it.padreUid == perfil.uid && it.aprobada
        }
        UserRole.JUGADOR -> jugador.usuarioUid.isNotBlank() && jugador.usuarioUid == perfil.uid
    }

    private fun normalizar(texto: String): String =
        Normalizer.normalize(texto.trim().lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("\\s+"), " ")
}
