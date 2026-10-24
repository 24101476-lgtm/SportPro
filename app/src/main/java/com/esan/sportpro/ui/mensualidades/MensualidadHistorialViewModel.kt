package com.esan.sportpro.ui.mensualidades

import androidx.lifecycle.viewModelScope
import com.esan.sportpro.core.viewmodel.BaseViewModel
import com.esan.sportpro.data.mensualidades.MensualidadRepository
import com.esan.sportpro.domain.mensualidades.Mensualidad
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MensualidadHistorialUiState(
    val jugadorId: String = "",
    val jugadorNombre: String = "",
    val registros: List<Mensualidad> = emptyList(),
    val isLoading: Boolean = true,
)

/**
 * US-008 — Frame 39_Mensualidad_Historial. Muestra **todos** los registros (vigentes y anulados)
 * de un jugador, más reciente primero, de forma que la corrección de un pago quede trazable
 * (criterio de aceptación 6: ningún registro se elimina).
 *
 * El módulo navega internamente por estado (ver [MensualidadesEntryScreen]) en vez de usar
 * argumentos de Navigation-Compose, así que [cargar] se invoca explícitamente desde la pantalla.
 */
@HiltViewModel
class MensualidadHistorialViewModel @Inject constructor(
    private val repository: MensualidadRepository,
) : BaseViewModel<MensualidadHistorialUiState, Unit>(MensualidadHistorialUiState()) {

    private var observacionJob: Job? = null

    fun cargar(jugadorId: String, jugadorNombre: String) {
        if (currentState.jugadorId == jugadorId && !currentState.isLoading) return
        setState { copy(jugadorId = jugadorId, jugadorNombre = jugadorNombre, isLoading = true) }
        observacionJob?.cancel()
        observacionJob = viewModelScope.launch {
            repository.observarHistorialDeJugador(jugadorId).collect { registros ->
                setState { copy(registros = registros, isLoading = false) }
            }
        }
    }
}
