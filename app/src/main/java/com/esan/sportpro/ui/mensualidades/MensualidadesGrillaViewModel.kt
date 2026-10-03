package com.esan.sportpro.ui.mensualidades

import com.esan.sportpro.core.viewmodel.BaseViewModel
import com.esan.sportpro.data.mensualidades.MensualidadRepository
import com.esan.sportpro.domain.mensualidades.EstadoMensualidad
import com.esan.sportpro.domain.mensualidades.Mensualidad
import com.esan.sportpro.navigation.UserRole
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
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
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

/** Mensualidad + su estado efectivo ya resuelto (Vencido calculado, nunca persistido). */
data class MensualidadConEstadoEfectivo(
    val mensualidad: Mensualidad,
    val estadoEfectivo: EstadoMensualidad,
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
) : BaseViewModel<MensualidadesGrillaUiState, MensualidadesGrillaEvent>(MensualidadesGrillaUiState()) {

    // TODO(academia): reemplazar por el academiaId real del usuario autenticado.
    private val academiaIdActual = "academia-demo"
    private var observacionJob: Job? = null

    init {
        observarGrilla()
    }

    private fun observarGrilla() {
        observacionJob?.cancel()
        observacionJob = viewModelScope.launch {
            val estado = currentState
            val corte = LocalDate.of(estado.anio, estado.mes, 1)
                .withDayOfMonth(
                    minOf(estado.diaCorteMes, YearMonth.of(estado.anio, estado.mes).lengthOfMonth()),
                )
            repository.observarGrilla(
                academiaId = academiaIdActual,
                mes = estado.mes,
                anio = estado.anio,
                equipoId = estado.equipoFiltro,
                estado = estado.estadoFiltro,
            ).collectLatest { lista ->
                val resueltas = lista.map { m ->
                    MensualidadConEstadoEfectivo(
                        mensualidad = m,
                        estadoEfectivo = EstadoMensualidad.calcular(m.estadoRegistrado, corte),
                    )
                }
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
