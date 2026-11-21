package com.esan.sportpro.ui.jugadores

import androidx.lifecycle.viewModelScope
import com.esan.sportpro.core.viewmodel.BaseViewModel
import com.esan.sportpro.data.jugadores.ParticipacionPartidosRepository
import com.esan.sportpro.data.partido.describirErrorFirestore
import com.esan.sportpro.domain.cuentas.SesionRepository
import com.esan.sportpro.domain.entrenamientos.CalculoAsistencia
import com.esan.sportpro.domain.entrenamientos.Entrenamiento
import com.esan.sportpro.domain.entrenamientos.EntrenamientoRepository
import com.esan.sportpro.domain.entrenamientos.RegistroParticipacion
import com.esan.sportpro.domain.entrenamientos.ResumenAsistencia
import com.esan.sportpro.domain.jugadores.Jugador
import com.esan.sportpro.domain.jugadores.JugadorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MiPerfilUiState(
    val ficha: Jugador? = null,
    val cargando: Boolean = true,
    /** La cuenta aún no está enlazada a una ficha (lo hace el personal desde Jugadores). */
    val sinFicha: Boolean = false,
    val error: String? = null,
    val entrenamientos: List<Entrenamiento> = emptyList(),
    val historialPartidos: List<RegistroParticipacion> = emptyList(),
    val cargandoPartidos: Boolean = false,
    val viendoHistorial: Boolean = false,
    // Campos que el propio jugador puede editar (US-006, criterio 9).
    val telefono: String = "",
    val pieHabil: String = "",
    val fotoNueva: String? = null,
    val guardando: Boolean = false,
    val errorTelefono: String? = null,
) {
    val asistencia: ResumenAsistencia?
        get() = ficha?.let { f -> CalculoAsistencia.resumen(f.id, entrenamientos.filter { it.equipoId == f.equipoId }) }

    val hayCambios: Boolean
        get() = ficha != null && (fotoNueva != null || telefono.trim() != ficha.telefono || pieHabil != ficha.pieHabil)
}

sealed interface MiPerfilEvent {
    data class Mensaje(val texto: String) : MiPerfilEvent
}

/**
 * US-006 (criterio 9) y US-012: perfil del propio jugador. Lee la ficha enlazada a su cuenta
 * (`usuarioUid`), muestra sus datos e historial, y le deja cambiar solo foto, teléfono y pie hábil.
 */
@HiltViewModel
class MiPerfilViewModel @Inject constructor(
    private val sesionRepository: SesionRepository,
    private val jugadorRepository: JugadorRepository,
    private val entrenamientoRepository: EntrenamientoRepository,
    private val participacionPartidos: ParticipacionPartidosRepository,
) : BaseViewModel<MiPerfilUiState, MiPerfilEvent>(MiPerfilUiState()) {

    private var partidosJob: Job? = null

    init {
        viewModelScope.launch {
            val perfil = sesionRepository.perfilActual()
            if (perfil == null) {
                setState { copy(cargando = false, error = "No se pudo leer tu perfil. Vuelve a iniciar sesión.") }
                return@launch
            }
            launch {
                entrenamientoRepository.observarEntrenamientos(perfil.academiaId)
                    .catch { /* la asistencia es informativa */ }
                    .collect { lista -> setState { copy(entrenamientos = lista) } }
            }
            jugadorRepository.observarFichaPropia(perfil.uid)
                .catch { e -> setState { copy(cargando = false, error = describirErrorFirestore(e)) } }
                .distinctUntilChangedBy { it }
                .collect { ficha ->
                    setState {
                        // Se respetan los cambios que el jugador esté escribiendo.
                        val editando = hayCambios
                        copy(
                            ficha = ficha,
                            cargando = false,
                            sinFicha = ficha == null,
                            telefono = if (editando) telefono else ficha?.telefono.orEmpty(),
                            pieHabil = if (editando) pieHabil else ficha?.pieHabil.orEmpty(),
                        )
                    }
                }
        }
    }

    fun onTelefono(valor: String) = setState {
        copy(telefono = valor.filter { it.isDigit() || it == '+' || it == ' ' }.take(15), errorTelefono = null)
    }

    fun onPieHabil(valor: String) = setState { copy(pieHabil = valor) }

    fun onFotoElegida(uri: String) = setState { copy(fotoNueva = uri) }

    fun quitarFotoNueva() = setState { copy(fotoNueva = null) }

    fun guardar() {
        val estado = currentState
        val ficha = estado.ficha ?: return
        if (estado.guardando || !estado.hayCambios) return
        val digitos = estado.telefono.count { it.isDigit() }
        if (estado.telefono.isNotBlank() && digitos !in 7..15) {
            setState { copy(errorTelefono = "Ingresa un teléfono válido (7 a 15 dígitos)") }
            return
        }
        setState { copy(guardando = true) }
        viewModelScope.launch {
            try {
                val datosCambiados = estado.telefono.trim() != ficha.telefono || estado.pieHabil != ficha.pieHabil
                val confirmado = if (datosCambiados) {
                    jugadorRepository.actualizarDatosPropios(ficha.id, estado.telefono, estado.pieHabil)
                } else {
                    true
                }
                val fotoFallida = estado.fotoNueva?.let { jugadorRepository.subirFoto(ficha.id, it) == null } ?: false
                setState { copy(guardando = false, fotoNueva = null) }
                sendEvent(
                    MiPerfilEvent.Mensaje(
                        when {
                            fotoFallida -> "Datos guardados, pero no se pudo subir la foto. Inténtalo de nuevo."
                            !confirmado -> "Guardado sin conexión. Se sincronizará al volver la señal."
                            else -> "Perfil actualizado"
                        },
                    ),
                )
            } catch (e: Exception) {
                setState { copy(guardando = false) }
                sendEvent(MiPerfilEvent.Mensaje(describirErrorFirestore(e)))
            }
        }
    }

    /** US-012: historial de entrenamientos y partidos del propio jugador. */
    fun abrirHistorial() {
        val ficha = currentState.ficha ?: return
        setState { copy(viendoHistorial = true, historialPartidos = emptyList(), cargandoPartidos = true) }
        partidosJob?.cancel()
        partidosJob = viewModelScope.launch {
            val registros = participacionPartidos.registros(ficha.id, ficha.academiaId)
            setState { copy(historialPartidos = registros, cargandoPartidos = false) }
        }
    }

    fun cerrarHistorial() = setState { copy(viendoHistorial = false) }
}
