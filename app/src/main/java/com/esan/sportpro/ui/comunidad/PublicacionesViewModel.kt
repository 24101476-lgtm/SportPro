package com.esan.sportpro.ui.comunidad

import com.esan.sportpro.data.partido.describirErrorFirestore
import kotlinx.coroutines.flow.catch
import com.esan.sportpro.domain.cuentas.PerfilActual
import com.esan.sportpro.domain.cuentas.SesionRepository
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.esan.sportpro.core.viewmodel.BaseViewModel
import com.esan.sportpro.data.comunidad.ModeracionRepository
import com.esan.sportpro.data.comunidad.PublicacionRepository
import com.esan.sportpro.domain.comunidad.Comentario
import com.esan.sportpro.domain.comunidad.EstadoPublicacion
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
    /** Academia, nombre y edad del perfil autenticado (se completan al cargar el perfil). */
    val academiaId: String = PerfilActual.ACADEMIA_POR_DEFECTO,
    val autorNombre: String = "",
    val autorEsMenorDeEdad: Boolean = false,
    val primeraPagina: List<Publicacion> = emptyList(),
    val paginasAdicionales: List<Publicacion> = emptyList(),
    val cargandoMas: Boolean = false,
    val sinMasPaginas: Boolean = false,
    val isLoading: Boolean = true,
    val publicacionAbierta: Publicacion? = null,
    val comentarios: List<Comentario> = emptyList(),
    val uidActual: String? = null,
    /** Personal de la academia: aprueba o rechaza publicaciones de menores (US-028). */
    val puedeRevisar: Boolean = false,
    /** Autores bloqueados por el usuario en esta sesión (se ocultan al instante). */
    val bloqueados: Set<String> = emptySet(),
) {
    /**
     * Lo que se ve en el muro. Antes se mostraban a todos también las publicaciones en revisión y
     * las ocultadas por moderación: ahora solo las ve su autor (y el personal, las que esperan revisión).
     */
    val publicaciones: List<Publicacion>
        get() = (primeraPagina + paginasAdicionales).distinctBy { it.id }
            .filter { it.autorUid !in bloqueados }
            .filter { p ->
                when (p.estado) {
                    EstadoPublicacion.PUBLICADA -> true
                    EstadoPublicacion.EN_REVISION -> p.autorUid == uidActual || (puedeRevisar && p.academiaId == academiaId)
                    else -> p.autorUid == uidActual
                }
            }
}

sealed interface PublicacionesEvent {
    data class RequiereConfirmacionTelefono(val texto: String, val imagenes: List<Uri>, val visibilidad: VisibilidadPublicacion) : PublicacionesEvent
    data class Publicada(val imagenesFallidas: Int, val quedoEnRevision: Boolean) : PublicacionesEvent
    data class Error(val mensaje: String) : PublicacionesEvent
    data object Reportada : PublicacionesEvent
    data object Bloqueado : PublicacionesEvent
    data class Revisada(val aprobada: Boolean) : PublicacionesEvent
}

/** US-028 — Frames 131 a 135 (Muro, publicación nueva, detalle/comentarios, en revisión, alerta de contacto). */
@HiltViewModel
class PublicacionesViewModel @Inject constructor(
    private val repository: PublicacionRepository,
    private val moderacionRepository: ModeracionRepository,
    private val firebaseAuth: FirebaseAuth,
    private val sesionRepository: SesionRepository,
) : BaseViewModel<PublicacionesUiState, PublicacionesEvent>(PublicacionesUiState()) {

    private var uidsBloqueados: List<String> = emptyList()
    private var trabajoComentarios: Job? = null

    init {
        viewModelScope.launch {
            sesionRepository.perfilActual()?.let { perfil ->
                setState {
                    copy(
                        academiaId = perfil.academiaId,
                        autorNombre = perfil.nombre,
                        autorEsMenorDeEdad = perfil.esMenor,
                        puedeRevisar = perfil.esStaff,
                    )
                }
            }
            val uid = firebaseAuth.currentUser?.uid
            setState { copy(uidActual = uid) }
            uidsBloqueados = uid?.let { runCatching { moderacionRepository.obtenerUidsOcultosParaMuro(it) }.getOrNull() }.orEmpty()
            repository.observarPrimeraPagina(currentState.academiaId, uidsBloqueados)
                .onEach { lista -> setState { copy(primeraPagina = lista, isLoading = false) } }
                .catch { e -> setState { copy(isLoading = false) }; sendEvent(PublicacionesEvent.Error(describirErrorFirestore(e))) }
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
            autorNombre = currentState.autorNombre.ifBlank { firebaseAuth.currentUser?.displayName.orEmpty() }.ifBlank { "Usuario SportPro" },
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

    /** US-030: bloquear al autor. Sus publicaciones dejan de verse de inmediato y en adelante. */
    fun bloquearAutor(autorUid: String) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        if (autorUid == uid) return
        viewModelScope.launch {
            runCatching { moderacionRepository.bloquear(uid, autorUid) }
                .onSuccess {
                    uidsBloqueados = uidsBloqueados + autorUid
                    setState { copy(bloqueados = bloqueados + autorUid) }
                    sendEvent(PublicacionesEvent.Bloqueado)
                }
                .onFailure { sendEvent(PublicacionesEvent.Error(it.message ?: "No se pudo bloquear")) }
        }
    }

    /** US-028: el personal aprueba o rechaza una publicación de un menor que está en revisión. */
    fun revisarPublicacion(publicacionId: String, aprobar: Boolean) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        if (!currentState.puedeRevisar) return
        viewModelScope.launch {
            runCatching { repository.revisar(publicacionId, aprobar, uid) }
                .onSuccess { sendEvent(PublicacionesEvent.Revisada(aprobar)) }
                .onFailure { sendEvent(PublicacionesEvent.Error(it.message ?: "No se pudo revisar la publicación")) }
        }
    }

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
