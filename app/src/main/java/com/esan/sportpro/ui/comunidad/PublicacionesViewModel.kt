package com.esan.sportpro.ui.comunidad

import android.net.Uri
import com.esan.sportpro.core.viewmodel.BaseViewModel
import com.esan.sportpro.data.comunidad.ModeracionRepository
import com.esan.sportpro.data.comunidad.PublicacionRepository
import com.esan.sportpro.domain.comunidad.Comentario
import com.esan.sportpro.domain.comunidad.MotivoReporte
import com.esan.sportpro.domain.comunidad.Publicacion
import com.esan.sportpro.domain.comunidad.Reporte
import com.esan.sportpro.domain.comunidad.TipoContenidoReportado
import com.esan.sportpro.domain.comunidad.TipoReaccion
import com.esan.sportpro.domain.comunidad.VisibilidadPublicacion
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PublicacionesUiState(
    // TODO(academia): reemplazar por la academia real del usuario autenticado.
    val academiaId: String = "academia-demo",
    // TODO(US-003/cuentas): calcular desde la fecha de nacimiento del perfil real.
    val autorEsMenorDeEdad: Boolean = false,
    val primeraPagina: List<Publicacion> = emptyList(),
    val paginasAdicionales: List<Publicacion> = emptyList(),
    val cargandoMas: Boolean = false,
    val sinMasPaginas: Boolean = false,
    val isLoading: Boolean = true,
    val publicacionAbierta: Publicacion? = null,
    val comentarios: List<Comentario> = emptyList(),
) {
    val publicaciones: List<Publicacion>
        get() = (primeraPagina + paginasAdicionales).distinctBy { it.id }
}

sealed interface PublicacionesEvent {
    data class RequiereConfirmacionTelefono(val texto: String, val imagenes: List<Uri>, val visibilidad: VisibilidadPublicacion) : PublicacionesEvent
    data class Publicada(val imagenesFallidas: Int, val quedoEnRevision: Boolean) : PublicacionesEvent
    data class Error(val mensaje: String) : PublicacionesEvent
    data object Reportada : PublicacionesEvent
}

/** US-028 — Frames 131 a 135 (Muro, publicación nueva, detalle/comentarios, en revisión, alerta de contacto). */
@HiltViewModel
class PublicacionesViewModel @Inject constructor(
    private val repository: PublicacionRepository,
    private val moderacionRepository: ModeracionRepository,
    private val firebaseAuth: FirebaseAuth,
) : BaseViewModel<PublicacionesUiState, PublicacionesEvent>(PublicacionesUiState()) {

    private var uidsBloqueados: List<String> = emptyList()
    private var trabajoComentarios: Job? = null

    init {
        viewModelScope.launch {
            val uid = firebaseAuth.currentUser?.uid
            uidsBloqueados = uid?.let { moderacionRepository.obtenerUidsOcultosParaMuro(it) }.orEmpty()
            repository.observarPrimeraPagina(currentState.academiaId, uidsBloqueados)
                .onEach { lista -> setState { copy(primeraPagina = lista, isLoading = false) } }
                .launchIn(viewModelScope)
        }
    }

    /** Gesto de arrastre hacia abajo (criterio 5): la primera página ya es realtime, solo se descartan las adicionales. */
    fun refrescar() {
        setState { copy(paginasAdicionales = emptyList(), sinMasPaginas = false) }
    }

    fun cargarMas() {
        val estado = currentState
        val ultima = estado.publicaciones.lastOrNull() ?: return
        if (estado.cargandoMas || estado.sinMasPaginas) return
        setState { copy(cargandoMas = true) }
        viewModelScope.launch {
            val siguientes = repository.cargarSiguientePagina(estado.academiaId, ultima, uidsBloqueados)
            setState {
                copy(
                    paginasAdicionales = paginasAdicionales + siguientes,
                    cargandoMas = false,
                    sinMasPaginas = siguientes.isEmpty(),
                )
            }
        }
    }

    fun publicar(texto: String, imagenes: List<Uri>, visibilidad: VisibilidadPublicacion, confirmadoTelefono: Boolean = false) {
        if (!confirmadoTelefono && repository.contieneNumeroTelefonico(texto)) {
            sendEvent(PublicacionesEvent.RequiereConfirmacionTelefono(texto, imagenes, visibilidad))
            return
        }
        val uid = firebaseAuth.currentUser?.uid ?: return
        val borrador = Publicacion(
            academiaId = currentState.academiaId,
            autorUid = uid,
            autorNombre = firebaseAuth.currentUser?.displayName.orEmpty().ifBlank { "Usuario SportPro" },
            autorEsMenorDeEdad = currentState.autorEsMenorDeEdad,
            texto = texto,
            visibilidad = visibilidad,
        )
        viewModelScope.launch {
            runCatching { repository.publicar(borrador, imagenes) }
                .onSuccess { sendEvent(PublicacionesEvent.Publicada(it.imagenesFallidas, it.quedoEnRevision)) }
                .onFailure { sendEvent(PublicacionesEvent.Error(it.message ?: "No se pudo publicar")) }
        }
    }

    fun abrirPublicacion(publicacion: Publicacion) {
        setState { copy(publicacionAbierta = publicacion, comentarios = emptyList()) }
        trabajoComentarios?.cancel()
        trabajoComentarios = repository.observarComentarios(publicacion.id)
            .onEach { lista -> setState { copy(comentarios = lista) } }
            .launchIn(viewModelScope)
    }

    fun cerrarPublicacion() {
        trabajoComentarios?.cancel()
        setState { copy(publicacionAbierta = null, comentarios = emptyList()) }
    }

    fun editarPublicacion(nuevoTexto: String) {
        val publicacion = currentState.publicacionAbierta ?: return
        viewModelScope.launch {
            val ok = repository.editar(publicacion, nuevoTexto)
            if (!ok) sendEvent(PublicacionesEvent.Error("Ya no se puede editar (pasaron 15 minutos)"))
        }
    }

    fun eliminarPublicacion(publicacionId: String) {
        viewModelScope.launch {
            repository.eliminar(publicacionId)
            cerrarPublicacion()
        }
    }

    fun comentar(texto: String) {
        val publicacion = currentState.publicacionAbierta ?: return
        val uid = firebaseAuth.currentUser?.uid ?: return
        viewModelScope.launch {
            repository.comentar(
                Comentario(
                    publicacionId = publicacion.id,
                    autorUid = uid,
                    autorNombre = firebaseAuth.currentUser?.displayName.orEmpty().ifBlank { "Usuario SportPro" },
                    texto = texto,
                ),
            )
        }
    }

    fun reaccionar(publicacionId: String, tipo: TipoReaccion?) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        viewModelScope.launch { repository.reaccionar(publicacionId, uid, tipo) }
    }

    fun reportarPublicacion(publicacionId: String, autorUid: String, esMenor: Boolean, motivo: MotivoReporte, detalle: String?) =
        reportar(TipoContenidoReportado.PUBLICACION, publicacionId, null, autorUid, esMenor, motivo, detalle)

    fun reportarComentario(comentarioId: String, publicacionId: String, autorUid: String, motivo: MotivoReporte, detalle: String?) =
        reportar(TipoContenidoReportado.COMENTARIO, comentarioId, publicacionId, autorUid, esMenor = false, motivo, detalle)

    private fun reportar(
        tipo: TipoContenidoReportado,
        contenidoId: String,
        publicacionId: String?,
        autorContenidoUid: String,
        esMenor: Boolean,
        motivo: MotivoReporte,
        detalle: String?,
    ) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        viewModelScope.launch {
            moderacionRepository.crearReporte(
                Reporte(
                    tipoContenido = tipo,
                    contenidoId = contenidoId,
                    publicacionId = publicacionId,
                    motivo = motivo,
                    detalle = detalle,
                    reportadoPorUid = uid,
                    autorContenidoUid = autorContenidoUid,
                    involucraMenor = esMenor,
                ),
            ).onSuccess { sendEvent(PublicacionesEvent.Reportada) }
                .onFailure { sendEvent(PublicacionesEvent.Error(it.message ?: "No se pudo reportar")) }
        }
    }
}
