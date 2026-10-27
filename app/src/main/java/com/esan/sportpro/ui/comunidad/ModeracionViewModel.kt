package com.esan.sportpro.ui.comunidad

import kotlinx.coroutines.flow.catch
import com.esan.sportpro.domain.cuentas.SesionRepository
import androidx.lifecycle.viewModelScope
import com.esan.sportpro.core.viewmodel.BaseViewModel
import com.esan.sportpro.data.comunidad.ModeracionRepository
import com.esan.sportpro.domain.comunidad.AccionModeracion
import com.esan.sportpro.domain.comunidad.Bloqueo
import com.esan.sportpro.domain.comunidad.DuracionSuspension
import com.esan.sportpro.domain.comunidad.Reporte
import com.esan.sportpro.domain.comunidad.Suspension
import com.esan.sportpro.navigation.UserRole
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ModeracionUiState(
    // Solo el ADMINISTRADOR modera (criterio: "Como administrador deseo revisar..."). Hasta leer
    // el perfil real se asume el rol de menor privilegio.
    val rolActual: UserRole = UserRole.JUGADOR,
    val bandeja: List<Reporte> = emptyList(),
    val isLoadingBandeja: Boolean = true,
    val reporteSeleccionado: Reporte? = null,
    val bloqueados: List<Bloqueo> = emptyList(),
    val suspensionActual: Suspension? = null,
) {
    val puedeModerar: Boolean get() = rolActual == UserRole.ADMINISTRADOR
    val estaSuspendido: Boolean get() = suspensionActual != null
}

sealed interface ModeracionEvent {
    data object ReporteResuelto : ModeracionEvent
    data class Error(val mensaje: String) : ModeracionEvent
}

/** US-030 — Frames 141 a 145 (Reportar, bandeja, resolver, usuario suspendido, usuarios bloqueados). */
@HiltViewModel
class ModeracionViewModel @Inject constructor(
    private val repository: ModeracionRepository,
    private val firebaseAuth: FirebaseAuth,
    private val sesionRepository: SesionRepository,
) : BaseViewModel<ModeracionUiState, ModeracionEvent>(ModeracionUiState()) {

    init {
        viewModelScope.launch {
            sesionRepository.perfilActual()?.let { perfil -> setState { copy(rolActual = perfil.rol) } }
            if (currentState.puedeModerar) {
                repository.observarBandeja()
                    .onEach { lista -> setState { copy(bandeja = lista, isLoadingBandeja = false) } }
                    .catch { setState { copy(isLoadingBandeja = false) } }
                    .launchIn(viewModelScope)
            }
        }
        firebaseAuth.currentUser?.uid?.let { uid ->
            repository.observarSuspensionActiva(uid)
                .onEach { suspension -> setState { copy(suspensionActual = suspension) } }
                .catch { }
                .launchIn(viewModelScope)
            repository.observarBloqueadosPor(uid)
                .onEach { lista -> setState { copy(bloqueados = lista) } }
                .catch { }
                .launchIn(viewModelScope)
        }
    }

    fun seleccionarReporte(reporte: Reporte) {
        setState { copy(reporteSeleccionado = reporte) }
    }

    fun cerrarReporte() {
        setState { copy(reporteSeleccionado = null) }
    }

    fun resolver(accion: AccionModeracion, motivoResolucion: String, duracion: DuracionSuspension?) {
        val reporte = currentState.reporteSeleccionado ?: return
        val moderadorUid = firebaseAuth.currentUser?.uid ?: return
        viewModelScope.launch {
            runCatching { repository.resolver(reporte, accion, motivoResolucion, moderadorUid, duracion) }
                .onSuccess {
                    setState { copy(reporteSeleccionado = null) }
                    sendEvent(ModeracionEvent.ReporteResuelto)
                }
                .onFailure { sendEvent(ModeracionEvent.Error(it.message ?: "No se pudo resolver el reporte")) }
        }
    }

    fun desbloquear(uidBloqueado: String) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        viewModelScope.launch { repository.desbloquear(uid, uidBloqueado) }
    }
}
