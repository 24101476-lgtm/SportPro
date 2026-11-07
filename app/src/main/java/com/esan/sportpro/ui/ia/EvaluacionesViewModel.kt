package com.esan.sportpro.ui.ia

import androidx.lifecycle.viewModelScope
import com.esan.sportpro.core.viewmodel.BaseViewModel
import com.esan.sportpro.data.partido.describirErrorFirestore
import com.esan.sportpro.domain.cuentas.PerfilActual
import com.esan.sportpro.domain.cuentas.SesionRepository
import com.esan.sportpro.domain.ia.ConsolidadoEvaluaciones
import com.esan.sportpro.domain.ia.EvaluacionRepository
import com.esan.sportpro.domain.ia.EvaluacionResumen
import com.esan.sportpro.domain.ia.ReglasEvaluacion
import com.esan.sportpro.navigation.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EvaluacionesUiState(
    val cargando: Boolean = true,
    val perfil: PerfilActual? = null,
    val evaluaciones: List<EvaluacionResumen> = emptyList(),
    val error: String? = null,
) {
    /** Solo el administrador y el entrenador consultan evaluaciones (US-025, criterio 7). */
    val tieneAcceso: Boolean
        get() = perfil?.rol == UserRole.ADMINISTRADOR || perfil?.rol == UserRole.ENTRENADOR

    val consolidado: ConsolidadoEvaluaciones get() = ReglasEvaluacion.consolidar(evaluaciones)

    val esAdministrador: Boolean get() = perfil?.rol == UserRole.ADMINISTRADOR
}

/**
 * US-025, criterio 5: consolida las evaluaciones de todos los partidos. El administrador ve las de su
 * academia; el entrenador, las de los resúmenes que él aprobó.
 */
@HiltViewModel
class EvaluacionesViewModel @Inject constructor(
    private val sesionRepository: SesionRepository,
    private val evaluacionRepository: EvaluacionRepository,
) : BaseViewModel<EvaluacionesUiState, Unit>(EvaluacionesUiState()) {

    init {
        viewModelScope.launch {
            val perfil = runCatching { sesionRepository.perfilActual() }.getOrNull()
            setState { copy(perfil = perfil) }
            if (!currentState.tieneAcceso || perfil == null) {
                setState { copy(cargando = false) }
                return@launch
            }
            evaluacionRepository.observarEvaluaciones(perfil.uid, perfil.rol == UserRole.ADMINISTRADOR, perfil.academiaId)
                .catch { e -> setState { copy(cargando = false, error = describirErrorFirestore(e)) } }
                .collect { lista -> setState { copy(evaluaciones = lista, cargando = false, error = null) } }
        }
    }
}
