package com.esan.sportpro.ui.comunidad

import com.esan.sportpro.data.partido.describirErrorFirestore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import com.esan.sportpro.domain.cuentas.PerfilActual
import com.esan.sportpro.domain.cuentas.SesionRepository
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
    /** Academia del perfil autenticado. La verificación de la academia sigue pendiente (ver Aviso.kt). */
    val academiaId: String = PerfilActual.ACADEMIA_POR_DEFECTO,
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
    private val sesionRepository: SesionRepository,
) : BaseViewModel<AvisosUiState, AvisosEvent>(AvisosUiState()) {

    private var trabajoAvisos: Job? = null

    init {
        viewModelScope.launch {
            sesionRepository.perfilActual()?.let { perfil -> setState { copy(academiaId = perfil.academiaId) } }
        }
        observar()
    }

    private fun observar() {
        trabajoAvisos?.cancel()
        trabajoAvisos = repository.observarAvisos(currentState.filtro)
            .onEach { lista -> setState { copy(avisos = lista, isLoading = false) } }
            .catch { e -> setState { copy(isLoading = false) }; sendEvent(AvisosEvent.Error(describirErrorFirestore(e))) }
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

    /**
     * Carga convocatorias de ejemplo para poder demostrar el módulo. Quedan marcadas en
     * "requisitos" como datos de ejemplo; un administrador puede ocultarlas con el flujo de moderación.
     */
    fun cargarEjemplos() {
        val uid = firebaseAuth.currentUser?.uid ?: return
        val dia = 24L * 60 * 60 * 1000
        val ahora = System.currentTimeMillis()
        fun ejemplo(titulo: String, categoria: String, posiciones: List<String>, dias: Int, hora: String, distrito: String, referencia: String) =
            Aviso(
                titulo = titulo,
                academiaId = currentState.academiaId,
                academiaNombre = currentState.academiaNombre,
                categoria = categoria,
                posicionesBuscadas = posiciones,
                fechaPrueba = java.util.Date(ahora + dias * dia),
                hora = hora,
                ubicacionDistrito = distrito,
                ubicacionReferencia = referencia,
                requisitos = "Traer DNI, botines y ropa deportiva. (Datos de ejemplo)",
                contactoInstitucional = "contacto@academia.pe",
                autorUid = uid,
            )
        val ejemplos = listOf(
            ejemplo("Prueba de arqueros Sub-15", "Sub-15", listOf("ARQUERO"), 4, "9:00", "San Miguel", "Cancha municipal, puerta 2"),
            ejemplo("Convocatoria delanteros y extremos Sub-13", "Sub-13", listOf("DELANTERO", "EXTREMO"), 9, "15:30", "Surco", "Complejo deportivo Los Pinos"),
            ejemplo("Captación de volantes Sub-17", "Sub-17", listOf("VOLANTE_MIXTO", "VOLANTE_OFENSIVO"), 14, "10:00", "La Molina", "Losa deportiva del parque central"),
        )
        viewModelScope.launch {
            ejemplos.forEach { aviso ->
                runCatching { repository.publicar(aviso, currentState.academiaVerificada) }
                    .onFailure { sendEvent(AvisosEvent.Error(it.message ?: "No se pudo cargar el ejemplo")) }
            }
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
