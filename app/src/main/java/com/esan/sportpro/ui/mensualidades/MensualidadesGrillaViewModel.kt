package com.esan.sportpro.ui.mensualidades

import com.esan.sportpro.data.partido.describirErrorFirestore
import kotlinx.coroutines.flow.catch
import com.esan.sportpro.domain.cuentas.PerfilActual
import com.esan.sportpro.domain.cuentas.SesionRepository
import androidx.lifecycle.viewModelScope
import com.esan.sportpro.core.viewmodel.BaseViewModel
import com.esan.sportpro.data.mensualidades.MensualidadRepository
import com.esan.sportpro.domain.jugadores.JugadorRepository
import com.esan.sportpro.domain.mensualidades.EstadoMensualidad
import com.esan.sportpro.domain.mensualidades.Mensualidad
import com.esan.sportpro.navigation.UserRole
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class MensualidadesGrillaUiState(
    val mes: Int = LocalDate.now().monthValue,
    val anio: Int = LocalDate.now().year,
    val equipoFiltro: String? = null,
    val estadoFiltro: EstadoMensualidad? = null,
    /** Día de corte configurado por la academia (criterio de aceptación 5); TODO: leer de la academia real. */
    val diaCorteMes: Int = 10,
    /**
     * US-008, criterio 7: el rol determina qué ve la grilla. ADMINISTRADOR ve montos completos;
     * ENTRENADOR solo el indicador de estado sin montos. TODO(cuentas): reemplazar por el rol real
     * del usuario autenticado cuando el módulo de perfiles esté disponible.
     */
    val rolActual: UserRole = UserRole.ADMINISTRADOR,
    val mensualidades: List<MensualidadConEstadoEfectivo> = emptyList(),
    /** Equipos con jugadores activos, para el filtro por equipo de la grilla. */
    val equiposDisponibles: List<EquipoOpcion> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

/** Opción del filtro por equipo. */
data class EquipoOpcion(val id: String, val nombre: String)

/** Mensualidad + su estado efectivo ya resuelto (Vencido calculado, nunca persistido). */
data class MensualidadConEstadoEfectivo(
    val mensualidad: Mensualidad,
    val estadoEfectivo: EstadoMensualidad,
    /** true si el jugador activo aún no tiene ningún pago registrado en el mes (se muestra como Pendiente/Vencido). */
    val sinRegistro: Boolean = false,
)

sealed interface MensualidadesGrillaEvent {
    data class AbrirHistorial(val jugadorId: String) : MensualidadesGrillaEvent
    data object AbrirRegistrar : MensualidadesGrillaEvent
}

/** US-008 — Frame 37_Mensualidades_Grilla. */
@HiltViewModel
class MensualidadesGrillaViewModel @Inject constructor(
    private val repository: MensualidadRepository,
    private val firebaseAuth: FirebaseAuth,
    private val sesionRepository: SesionRepository,
    private val jugadorRepository: JugadorRepository,
) : BaseViewModel<MensualidadesGrillaUiState, MensualidadesGrillaEvent>(MensualidadesGrillaUiState()) {

    private var academiaIdActual = PerfilActual.ACADEMIA_POR_DEFECTO
    private var observacionJob: Job? = null

    init {
        viewModelScope.launch {
            // Rol y academia reales: el entrenador ve solo el estado, sin montos (criterio 7).
            sesionRepository.perfilActual()?.let { perfil ->
                academiaIdActual = perfil.academiaId
                setState { copy(rolActual = perfil.rol) }
            }
            observarGrilla()
        }
    }

    private fun observarGrilla() {
        observacionJob?.cancel()
        observacionJob = viewModelScope.launch {
            val estado = currentState
            val corte = LocalDate.of(estado.anio, estado.mes, 1)
                .withDayOfMonth(
                    minOf(estado.diaCorteMes, YearMonth.of(estado.anio, estado.mes).lengthOfMonth()),
                )
            // Se combinan los jugadores activos con los pagos vigentes del mes: así aparecen todos los
            // jugadores inscritos (con su equipo) y los que no tienen registro salen como Pendiente,
            // pasando a Vencido automáticamente al cumplirse el día de corte.
            combine(
                jugadorRepository.observarJugadores(academiaIdActual),
                repository.observarGrilla(
                    academiaId = academiaIdActual,
                    mes = estado.mes,
                    anio = estado.anio,
                ),
            ) { jugadores, pagos ->
                val pagoPorJugador = pagos.associateBy { it.jugadorId }
                val equipos = jugadores.filter { it.activo && it.equipoId.isNotBlank() }
                    .map { EquipoOpcion(it.equipoId, it.equipoNombre.ifBlank { "Equipo" }) }
                    .distinctBy { it.id }
                    .sortedBy { it.nombre.lowercase() }
                setState {
                    // Si el equipo filtrado ya no existe, se vuelve a "Todos".
                    copy(
                        equiposDisponibles = equipos,
                        equipoFiltro = equipoFiltro?.takeIf { f -> equipos.any { it.id == f } },
                    )
                }
                val deJugadoresActivos = jugadores.filter { it.activo }.map { jugador ->
                    val pago = pagoPorJugador[jugador.id]
                    if (pago != null) {
                        // Se refrescan nombre y equipo con la ficha actual del jugador.
                        val m = pago.copy(
                            jugadorNombre = jugador.nombreCompleto.ifBlank { pago.jugadorNombre },
                            equipoId = jugador.equipoId.ifBlank { pago.equipoId },
                            equipoNombre = jugador.equipoNombre.ifBlank { pago.equipoNombre },
                        )
                        MensualidadConEstadoEfectivo(m, EstadoMensualidad.calcular(m.estadoRegistrado, corte))
                    } else {
                        val vacio = Mensualidad(
                            id = "sin-registro-${jugador.id}",
                            academiaId = academiaIdActual,
                            equipoId = jugador.equipoId,
                            equipoNombre = jugador.equipoNombre,
                            jugadorId = jugador.id,
                            jugadorNombre = jugador.nombreCompleto,
                            mes = estado.mes,
                            anio = estado.anio,
                            estadoRegistrado = EstadoMensualidad.PENDIENTE,
                        )
                        MensualidadConEstadoEfectivo(
                            mensualidad = vacio,
                            estadoEfectivo = EstadoMensualidad.calcular(EstadoMensualidad.PENDIENTE, corte),
                            sinRegistro = true,
                        )
                    }
                }
                // Pagos de jugadores que ya no están activos se conservan (el historial no se pierde).
                val idsActivos = jugadores.filter { it.activo }.map { it.id }.toSet()
                val deInactivos = pagos.filter { it.jugadorId !in idsActivos }.map { m ->
                    MensualidadConEstadoEfectivo(m, EstadoMensualidad.calcular(m.estadoRegistrado, corte))
                }
                (deJugadoresActivos + deInactivos)
                    .filter { estado.equipoFiltro == null || it.mensualidad.equipoId == estado.equipoFiltro }
                    .filter { estado.estadoFiltro == null || it.estadoEfectivo == estado.estadoFiltro }
                    .sortedWith(
                        compareBy<MensualidadConEstadoEfectivo> { it.mensualidad.equipoNombre.lowercase() }
                            .thenBy { it.mensualidad.jugadorNombre.lowercase() },
                    )
            }.catch { e ->
                setState { copy(isLoading = false, errorMessage = describirErrorFirestore(e)) }
            }.collectLatest { resueltas ->
                setState { copy(mensualidades = resueltas, isLoading = false, errorMessage = null) }
            }
        }
    }

    fun onMesAnioChanged(mes: Int, anio: Int) {
        setState { copy(mes = mes, anio = anio, isLoading = true) }
        observarGrilla()
    }

    fun onFiltroEquipoChanged(equipoId: String?) {
        setState { copy(equipoFiltro = equipoId, isLoading = true) }
        observarGrilla()
    }

    fun onFiltroEstadoChanged(estado: EstadoMensualidad?) {
        setState { copy(estadoFiltro = estado, isLoading = true) }
        observarGrilla()
    }

    fun onJugadorSeleccionado(jugadorId: String) = sendEvent(MensualidadesGrillaEvent.AbrirHistorial(jugadorId))

    fun onNuevoRegistro() = sendEvent(MensualidadesGrillaEvent.AbrirRegistrar)
}
