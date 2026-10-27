package com.esan.sportpro.ui.anuncios

import com.esan.sportpro.data.partido.describirErrorFirestore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import com.esan.sportpro.domain.cuentas.PerfilActual
import com.esan.sportpro.domain.cuentas.SesionRepository
import androidx.lifecycle.viewModelScope
import com.esan.sportpro.core.viewmodel.BaseViewModel
import com.esan.sportpro.data.anuncios.AnuncioRepository
import com.esan.sportpro.domain.anuncios.Anuncio
import com.esan.sportpro.domain.anuncios.PrioridadAnuncio
import com.esan.sportpro.domain.anuncios.TipoDestinatario
import com.esan.sportpro.navigation.UserRole
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnuncioFormulario(
    val titulo: String = "",
    val mensaje: String = "",
    val destinatarios: TipoDestinatario = TipoDestinatario.TODO_EL_CLUB,
    val prioridad: PrioridadAnuncio = PrioridadAnuncio.INFORMATIVA,
) {
    val tituloValido: Boolean get() = titulo.length in 5..80
    val mensajeValido: Boolean get() = mensaje.length in 10..500
    val formularioValido: Boolean get() = tituloValido && mensajeValido
}

data class AnunciosUiState(
    val anuncios: List<Anuncio> = emptyList(),
    val anunciosUrgentesNoLeidos: List<Anuncio> = emptyList(),
    val mostrarArchivados: Boolean = false,
    val formulario: AnuncioFormulario = AnuncioFormulario(),
    val preferenciaInformativasActivas: Boolean = true,
    /** Rol del perfil autenticado; hasta cargarlo se asume el de menor privilegio. */
    val rolActual: UserRole = UserRole.JUGADOR,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val avisoPublicacion: String? = null,
) {
    val puedePublicar: Boolean get() = rolActual == UserRole.ENTRENADOR || rolActual == UserRole.ADMINISTRADOR
}

sealed interface AnunciosEvent {
    data object AnuncioPublicado : AnunciosEvent
}

/** US-027 — Frames 126 a 130 (Anuncios internos y notificaciones push). */
@HiltViewModel
class AnunciosViewModel @Inject constructor(
    private val repository: AnuncioRepository,
    private val firebaseAuth: FirebaseAuth,
    private val sesionRepository: SesionRepository,
) : BaseViewModel<AnunciosUiState, AnunciosEvent>(AnunciosUiState()) {

    private var academiaIdActual = PerfilActual.ACADEMIA_POR_DEFECTO
    private var nombreActual = ""
    private var trabajoAnuncios: Job? = null

    init {
        viewModelScope.launch {
            // Rol y academia reales del usuario autenticado.
            sesionRepository.perfilActual()?.let { perfil ->
                academiaIdActual = perfil.academiaId
                nombreActual = perfil.nombre
                setState { copy(rolActual = perfil.rol) }
            }
            observarAnuncios()
            observarUrgentesNoLeidos()
            observarPreferencias()
        }
    }

    private fun observarAnuncios() {
        trabajoAnuncios?.cancel()
        trabajoAnuncios = viewModelScope.launch {
            repository.observarAnuncios(academiaIdActual, currentState.mostrarArchivados)
                .catch { e -> setState { copy(isLoading = false, errorMessage = describirErrorFirestore(e)) } }
                .collectLatest { lista ->
                    setState { copy(anuncios = lista, isLoading = false, errorMessage = null) }
                }
        }
    }

    private fun observarUrgentesNoLeidos() {
        val uid = firebaseAuth.currentUser?.uid ?: return
        viewModelScope.launch {
            repository.observarAnunciosUrgentesNoLeidos(academiaIdActual, uid).catch { }.collectLatest { lista ->
                setState { copy(anunciosUrgentesNoLeidos = lista) }
            }
        }
    }

    private fun observarPreferencias() {
        val uid = firebaseAuth.currentUser?.uid ?: return
        viewModelScope.launch {
            repository.observarPreferenciaInformativas(uid).catch { }.collectLatest { activo ->
                setState { copy(preferenciaInformativasActivas = activo) }
            }
        }
    }

    fun onMostrarArchivadosChanged(mostrar: Boolean) {
        setState { copy(mostrarArchivados = mostrar, isLoading = true) }
        observarAnuncios()
    }

    fun onFormularioChanged(formulario: AnuncioFormulario) {
        setState { copy(formulario = formulario) }
    }

    fun limpiarFormulario() {
        setState { copy(formulario = AnuncioFormulario()) }
    }

    fun publicar() {
        val estado = currentState
        if (!estado.formulario.formularioValido) return
        val uid = firebaseAuth.currentUser?.uid ?: return
        viewModelScope.launch {
            val resultado = runCatching {
                repository.publicar(
                    Anuncio(
                        academiaId = academiaIdActual,
                        titulo = estado.formulario.titulo,
                        mensaje = estado.formulario.mensaje,
                        destinatarios = estado.formulario.destinatarios,
                        prioridad = estado.formulario.prioridad,
                        autorUid = uid,
                        autorNombre = nombreActual.ifBlank { firebaseAuth.currentUser?.displayName ?: "Entrenador" },
                    ),
                )
            }
            resultado.onSuccess { publicacion ->
                val aviso = if (!publicacion.notificacionEnviada) {
                    "El anuncio se publicó, pero algunas notificaciones no pudieron enviarse."
                } else null
                setState { copy(formulario = AnuncioFormulario(), avisoPublicacion = aviso) }
                sendEvent(AnunciosEvent.AnuncioPublicado)
            }.onFailure { error ->
                setState { copy(errorMessage = error.message ?: "No se pudo publicar el anuncio") }
            }
        }
    }

    fun marcarLeido(anuncioId: String) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        viewModelScope.launch { repository.marcarLeido(anuncioId, uid) }
    }

    fun editar(anuncio: Anuncio, nuevoTitulo: String, nuevoMensaje: String, onResultado: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = runCatching { repository.editar(anuncio, nuevoTitulo, nuevoMensaje) }.getOrDefault(false)
            onResultado(ok)
        }
    }

    fun archivar(anuncioId: String) {
        viewModelScope.launch { repository.archivar(anuncioId) }
    }

    fun cambiarPreferenciaInformativas(activo: Boolean) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        viewModelScope.launch { repository.actualizarPreferenciaInformativas(uid, activo) }
    }

    fun limpiarAviso() = setState { copy(avisoPublicacion = null, errorMessage = null) }
}
