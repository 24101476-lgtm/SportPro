package com.esan.sportpro.ui.comunidad

import androidx.lifecycle.viewModelScope
import com.esan.sportpro.core.viewmodel.BaseViewModel
import com.esan.sportpro.data.comunidad.AvisoRepository
import com.esan.sportpro.data.comunidad.ModeracionRepository
import com.esan.sportpro.domain.comunidad.Aviso
import com.esan.sportpro.domain.comunidad.FiltroAvisos
import com.esan.sportpro.domain.comunidad.MotivoReporte
import com.esan.sportpro.domain.comunidad.Reporte
import com.esan.sportpro.domain.comunidad.TipoContenidoReportado
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AvisosUiState(
    val filtro: FiltroAvisos = FiltroAvisos(),
    val avisos: List<Aviso> = emptyList(),
    val isLoading: Boolean = true,
    // TODO(academia): reemplazar por la academia real y su estado de verificación (ver TODO en Aviso.kt).
    val academiaId: String = "academia-demo",
    val academiaNombre: String = "Mi academia",
    val academiaVerificada: Boolean = true,
)

sealed interface AvisosEvent {
    data class Publicado(val avisoId: String) : AvisosEvent
    data class Error(val mensaje: String) : AvisosEvent
    data object Reportado : AvisosEvent
}

/** US-029 — Frames 136 a 140 (Buscador, filtros, aviso nuevo, detalle, estado vacío). */
@HiltViewModel
class AvisosViewModel @Inject constructor(
    private val repository: AvisoRepository,
    private val moderacionRepository: ModeracionRepository,
    private val firebaseAuth: FirebaseAuth,
) : BaseViewModel<AvisosUiState, AvisosEvent>(AvisosUiState()) {

    init {
        observar()
    }

    private fun observar() {
        repository.observarAvisos(currentState.filtro)
            .onEach { lista -> setState { copy(avisos = lista, isLoading = false) } }
            .launchIn(viewModelScope)
    }

    fun aplicarFiltro(nuevo: FiltroAvisos) {
        setState { copy(filtro = nuevo, isLoading = true) }
        observar()
    }

    fun limpiarFiltros() = aplicarFiltro(FiltroAvisos())

    fun publicar(aviso: Aviso) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        val completo = aviso.copy(
            academiaId = currentState.academiaId,
            academiaNombre = currentState.academiaNombre,
            autorUid = uid,
        )
        viewModelScope.launch {
            runCatching { repository.publicar(completo, currentState.academiaVerificada) }
                .onSuccess { sendEvent(AvisosEvent.Publicado(it)) }
                .onFailure { sendEvent(AvisosEvent.Error(it.message ?: "No se pudo publicar el aviso")) }
        }
    }

    fun reportar(avisoId: String, autorUid: String, motivo: MotivoReporte, detalle: String?) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        viewModelScope.launch {
            moderacionRepository.crearReporte(
                Reporte(
                    tipoContenido = TipoContenidoReportado.AVISO,
                    contenidoId = avisoId,
                    motivo = motivo,
                    detalle = detalle,
                    reportadoPorUid = uid,
                    autorContenidoUid = autorUid,
                ),
            ).onSuccess { sendEvent(AvisosEvent.Reportado) }
                .onFailure { sendEvent(AvisosEvent.Error(it.message ?: "No se pudo reportar")) }
        }
    }
}
