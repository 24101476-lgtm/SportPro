package com.esan.sportpro.ui.mensualidades

import androidx.lifecycle.viewModelScope
import com.esan.sportpro.core.viewmodel.BaseViewModel
import com.esan.sportpro.data.mensualidades.MensualidadRepository
import com.esan.sportpro.data.partido.describirErrorFirestore
import com.esan.sportpro.domain.academia.EquipoRepository
import com.esan.sportpro.domain.cuentas.PerfilActual
import com.esan.sportpro.domain.cuentas.SesionRepository
import com.esan.sportpro.domain.jugadores.JugadorRepository
import com.esan.sportpro.domain.mensualidades.EstadoMensualidad
import com.esan.sportpro.domain.mensualidades.Mensualidad
import com.esan.sportpro.navigation.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class MensualidadesGrillaUiState(
    val mes: Int = LocalDate.now().monthValue,
    val anio: Int = LocalDate.now().year,
    val equipoFiltro: String? = null,
    val estadoFiltro: EstadoMensualidad? = null,
    /**
     * US-008, criterio 7: el rol determina qué ve la grilla. ADMINISTRADOR ve montos completos;
     * ENTRENADOR solo el indicador de estado sin montos.
     */
    val rolActual: UserRole = UserRole.ADMINISTRADOR,
    /** Todas las filas del mes (sin filtros); los filtros se aplican en memoria. */
    val todas: List<MensualidadConEstadoEfectivo> = emptyList(),
    /** Equipos con jugadores activos, para el filtro por equipo de la grilla. */
    val equiposDisponibles: List<EquipoOpcion> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    /** Día de corte configurado por la academia (US-004); por defecto el 10. */
    val diaCorte: Int = EstadoMensualidad.DIA_CORTE,
    /** Entrenador sin ningún equipo asignado a su cuenta: no ve mensualidades. */
    val sinEquiposAsignados: Boolean = false,
    /** Equipos que figuran con el nombre del entrenador pero no con su cuenta (equipos antiguos). */
    val equiposSoloPorNombre: List<String> = emptyList(),
) {
    val esAdministrador: Boolean get() = rolActual == UserRole.ADMINISTRADOR

    /** Filas del equipo elegido (base del resumen: el resumen no depende del filtro de estado). */
    private val delEquipo: List<MensualidadConEstadoEfectivo>
        get() = todas.filter { equipoFiltro == null || it.mensualidad.equipoId == equipoFiltro }

    val mensualidades: List<MensualidadConEstadoEfectivo>
        get() = delEquipo.filter { estadoFiltro == null || it.estadoEfectivo == estadoFiltro }

    val resumen: ResumenMes
        get() {
            val base = delEquipo
            val porEstado = base.groupingBy { it.estadoEfectivo }.eachCount()
            return ResumenMes(
                total = base.size,
                porEstado = porEstado,
                recaudado = base.filter { it.estadoEfectivo == EstadoMensualidad.PAGADO }
                    .sumOf { it.mensualidad.montoReferencial },
            )
        }

    val esMesActual: Boolean
        get() = LocalDate.now().let { it.monthValue == mes && it.year == anio }
}

/** Totales del mes para la cabecera de la grilla. */
data class ResumenMes(
    val total: Int = 0,
    val porEstado: Map<EstadoMensualidad, Int> = emptyMap(),
    val recaudado: Double = 0.0,
) {
    fun cantidad(estado: EstadoMensualidad) = porEstado[estado] ?: 0

    /** Pagados + exonerados: los que ya no deben nada este mes. */
    val alDia: Int get() = cantidad(EstadoMensualidad.PAGADO) + cantidad(EstadoMensualidad.EXONERADO)
    val progreso: Float get() = if (total == 0) 0f else alDia.toFloat() / total
}

/** Opción del filtro por equipo. */
data class EquipoOpcion(val id: String, val nombre: String)

/** Mensualidad + su estado efectivo ya resuelto (Vencido calculado, nunca persistido). */
data class MensualidadConEstadoEfectivo(
    val mensualidad: Mensualidad,
    val estadoEfectivo: EstadoMensualidad,
    /** true si el jugador activo aún no tiene ningún pago registrado en el mes (se muestra como Pendiente/Vencido). */
    val sinRegistro: Boolean = false,
    /** Foto de la ficha del jugador; vacía si no tiene. */
    val fotoUrl: String = "",
)

sealed interface MensualidadesGrillaEvent {
    data class AbrirHistorial(val jugadorId: String, val jugadorNombre: String, val fotoUrl: String) : MensualidadesGrillaEvent

    /** Registrar un pago; con [jugadorId] el formulario abre con ese jugador ya elegido. */
    data class AbrirRegistrar(val jugadorId: String?, val mes: Int, val anio: Int) : MensualidadesGrillaEvent
}

/** US-008 — Frame 37_Mensualidades_Grilla. */
@HiltViewModel
class MensualidadesGrillaViewModel @Inject constructor(
    private val repository: MensualidadRepository,
    private val sesionRepository: SesionRepository,
    private val jugadorRepository: JugadorRepository,
    private val equipoRepository: EquipoRepository,
    private val academiaRepository: com.esan.sportpro.domain.academia.AcademiaRepository,
) : BaseViewModel<MensualidadesGrillaUiState, MensualidadesGrillaEvent>(MensualidadesGrillaUiState()) {

    private var academiaIdActual = PerfilActual.ACADEMIA_POR_DEFECTO
    private var perfil: PerfilActual? = null
    private var observacionJob: Job? = null

    init {
        viewModelScope.launch {
            // Rol y academia reales: el entrenador ve solo el estado, sin montos (criterio 7).
            sesionRepository.perfilActual()?.let { p ->
                perfil = p
                academiaIdActual = p.academiaId
                setState { copy(rolActual = p.rol) }
            }
            // US-004: el día de corte lo configura la academia; al cambiar se recalculan los estados.
            launch {
                academiaRepository.observarConfiguracion(academiaIdActual)
                    .map { it.diaCorte }
                    .distinctUntilChanged()
                    .collect { dia ->
                        val cambio = dia != currentState.diaCorte
                        setState { copy(diaCorte = dia) }
                        if (cambio) observarMes()
                    }
            }
            observarMes()
        }
    }

    /**
     * Equipos cuyas mensualidades puede ver el usuario: `null` = toda la academia (administrador).
     * El entrenador solo ve los equipos asignados a su cuenta, igual que en las reglas de Firestore.
     */
    private fun equiposVisibles() = perfil?.takeIf { it.rol == UserRole.ENTRENADOR }?.let { p ->
        equipoRepository.observarEquipos(academiaIdActual).map { equipos ->
            val activos = equipos.filter { it.activo }
            val propios = activos.filter { it.entrenadorUid == p.uid }
            val soloNombre = activos.filter { it.entrenadorUid.isBlank() && it.dirigidoPor(p.uid, p.nombre) }
            setState {
                copy(
                    sinEquiposAsignados = propios.isEmpty(),
                    equiposSoloPorNombre = soloNombre.map { it.nombre },
                )
            }
            propios.map { it.id }.sorted()
        }.distinctUntilChanged()
    } ?: flowOf(null)

    /** Solo se vuelve a consultar Firestore al cambiar de mes; los filtros no reinician la consulta. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observarMes() {
        observacionJob?.cancel()
        val mes = currentState.mes
        val anio = currentState.anio
        val diaCorte = currentState.diaCorte
        observacionJob = viewModelScope.launch {
            // Se combinan los jugadores activos con los pagos vigentes del mes: así aparecen todos los
            // jugadores inscritos (con su equipo) y los que no tienen registro salen como Pendiente,
            // pasando a Vencido automáticamente al cumplirse el día de corte.
            equiposVisibles().flatMapLatest { equipoIds ->
              combine(
                jugadorRepository.observarJugadores(academiaIdActual),
                if (equipoIds == null) {
                    repository.observarGrilla(academiaId = academiaIdActual, mes = mes, anio = anio)
                } else {
                    repository.observarGrillaDeEquipos(academiaIdActual, equipoIds, mes, anio)
                },
              ) { todosLosJugadores, pagos ->
                // El entrenador solo ve a los jugadores de sus equipos.
                val jugadores = if (equipoIds == null) todosLosJugadores
                else todosLosJugadores.filter { it.equipoId in equipoIds }
                val activos = jugadores.filter { it.activo }
                val fotoPorJugador = jugadores.associate { it.id to it.fotoUrl }
                val pagoPorJugador = pagos.associateBy { it.jugadorId }

                val equipos = activos.filter { it.equipoId.isNotBlank() }
                    .map { EquipoOpcion(it.equipoId, it.equipoNombre.ifBlank { "Equipo" }) }
                    .distinctBy { it.id }
                    .sortedBy { it.nombre.lowercase() }

                val deActivos = activos.map { jugador ->
                    val pago = pagoPorJugador[jugador.id]
                    if (pago != null) {
                        // Se refrescan nombre y equipo con la ficha actual del jugador.
                        val m = pago.copy(
                            jugadorNombre = jugador.nombreCompleto.ifBlank { pago.jugadorNombre },
                            equipoId = jugador.equipoId.ifBlank { pago.equipoId },
                            equipoNombre = jugador.equipoNombre.ifBlank { pago.equipoNombre },
                        )
                        MensualidadConEstadoEfectivo(
                            mensualidad = m,
                            estadoEfectivo = EstadoMensualidad.efectivo(m.estadoRegistrado, mes, anio, diaCorte = diaCorte),
                            fotoUrl = jugador.fotoUrl,
                        )
                    } else {
                        val vacio = Mensualidad(
                            id = "sin-registro-${jugador.id}",
                            academiaId = academiaIdActual,
                            equipoId = jugador.equipoId,
                            equipoNombre = jugador.equipoNombre,
                            jugadorId = jugador.id,
                            jugadorNombre = jugador.nombreCompleto,
                            mes = mes,
                            anio = anio,
                            estadoRegistrado = EstadoMensualidad.PENDIENTE,
                        )
                        MensualidadConEstadoEfectivo(
                            mensualidad = vacio,
                            estadoEfectivo = EstadoMensualidad.efectivo(EstadoMensualidad.PENDIENTE, mes, anio, diaCorte = diaCorte),
                            sinRegistro = true,
                            fotoUrl = jugador.fotoUrl,
                        )
                    }
                }
                // Pagos de jugadores que ya no están activos se conservan (el historial no se pierde).
                val idsActivos = activos.map { it.id }.toSet()
                val deInactivos = pagos.filter { it.jugadorId !in idsActivos }.map { m ->
                    MensualidadConEstadoEfectivo(
                        mensualidad = m,
                        estadoEfectivo = EstadoMensualidad.efectivo(m.estadoRegistrado, mes, anio, diaCorte = diaCorte),
                        fotoUrl = fotoPorJugador[m.jugadorId].orEmpty(),
                    )
                }
                // Primero lo que requiere acción (vencidos, pendientes), luego los que están al día.
                val filas = (deActivos + deInactivos).sortedWith(
                    compareBy<MensualidadConEstadoEfectivo> { prioridad(it.estadoEfectivo) }
                        .thenBy { it.mensualidad.jugadorNombre.lowercase() },
                )
                equipos to filas
              }
            }.catch { e ->
                setState { copy(isLoading = false, errorMessage = describirErrorFirestore(e)) }
            }.collectLatest { (equipos, filas) ->
                setState {
                    copy(
                        todas = filas,
                        equiposDisponibles = equipos,
                        // Si el equipo filtrado ya no existe, se vuelve a "Todos".
                        equipoFiltro = equipoFiltro?.takeIf { f -> equipos.any { it.id == f } },
                        isLoading = false,
                        errorMessage = null,
                    )
                }
            }
        }
    }

    private fun prioridad(estado: EstadoMensualidad) = when (estado) {
        EstadoMensualidad.VENCIDO -> 0
        EstadoMensualidad.PENDIENTE -> 1
        EstadoMensualidad.PAGADO -> 2
        EstadoMensualidad.EXONERADO -> 3
    }

    fun onMesAnterior() = cambiarMes(-1)

    fun onMesSiguiente() = cambiarMes(1)

    private fun cambiarMes(delta: Long) {
        val nuevo = java.time.YearMonth.of(currentState.anio, currentState.mes).plusMonths(delta)
        setState { copy(mes = nuevo.monthValue, anio = nuevo.year, isLoading = true, todas = emptyList()) }
        observarMes()
    }

    fun onFiltroEquipoChanged(equipoId: String?) = setState { copy(equipoFiltro = equipoId) }

    /** Tocar el estado ya elegido lo quita. */
    fun onFiltroEstadoChanged(estado: EstadoMensualidad?) =
        setState { copy(estadoFiltro = if (estadoFiltro == estado) null else estado) }

    fun onJugadorSeleccionado(fila: MensualidadConEstadoEfectivo) = sendEvent(
        MensualidadesGrillaEvent.AbrirHistorial(fila.mensualidad.jugadorId, fila.mensualidad.jugadorNombre, fila.fotoUrl),
    )

    fun onNuevoRegistro() = sendEvent(
        MensualidadesGrillaEvent.AbrirRegistrar(jugadorId = null, mes = currentState.mes, anio = currentState.anio),
    )

    /** Registro rápido desde la fila: abre el formulario con el jugador y el mes de la grilla. */
    fun onRegistrarDe(fila: MensualidadConEstadoEfectivo) = sendEvent(
        MensualidadesGrillaEvent.AbrirRegistrar(
            jugadorId = fila.mensualidad.jugadorId,
            mes = currentState.mes,
            anio = currentState.anio,
        ),
    )
}
