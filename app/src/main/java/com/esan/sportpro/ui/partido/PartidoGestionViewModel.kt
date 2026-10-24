package com.esan.sportpro.ui.partido

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esan.sportpro.domain.partido.Partido
import com.esan.sportpro.domain.partido.PartidoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PartidoGestionUiState(
    val partido: Partido? = null,
    val cargando: Boolean = true,
    val guardando: Boolean = false,

    val reprogramado: Boolean = false,
    val cancelado: Boolean = false,

    val mensaje: String? = null,
)

@HiltViewModel
class PartidoGestionViewModel @Inject constructor(
    private val repository: PartidoRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val partidoId: String =
        savedStateHandle["partidoId"] ?: ""

    private val _uiState =
        MutableStateFlow(
            PartidoGestionUiState()
        )

    val uiState: StateFlow<PartidoGestionUiState> =
        _uiState.asStateFlow()

    init {
        cargarPartido()
        escucharErrores()
    }

    private fun cargarPartido() {

        if (partidoId.isBlank()) {

            _uiState.value =
                _uiState.value.copy(
                    cargando = false,
                    mensaje = "No se encontró el partido."
                )

            return
        }

        viewModelScope.launch {

            repository
                .observarPartido(partidoId)
                .catch { error ->

                    _uiState.value =
                        _uiState.value.copy(
                            cargando = false,
                            mensaje =
                                error.message
                                    ?: "No se pudo cargar el partido."
                        )
                }
                .collect { partido ->

                    _uiState.value =
                        _uiState.value.copy(
                            partido = partido,
                            cargando = false,
                        )
                }
        }
    }

    private fun escucharErrores() {

        viewModelScope.launch {

            repository
                .erroresEscritura
                .collect { error ->

                    _uiState.value =
                        _uiState.value.copy(
                            guardando = false,
                            mensaje = error,
                        )
                }
        }
    }

    fun reprogramar(
        nuevaFechaMs: Long?,
        nuevaHora: String,
        nuevoLugar: String,
        nuevaFechaLimiteRespuestaMs: Long?,
    ) {

        if (nuevaFechaMs == null) {

            mostrarMensaje(
                "Selecciona la nueva fecha del partido."
            )

            return
        }

        if (nuevaHora.isBlank()) {

            mostrarMensaje(
                "Selecciona la nueva hora."
            )

            return
        }

        if (nuevoLugar.isBlank()) {

            mostrarMensaje(
                "Ingresa el lugar del partido."
            )

            return
        }

        if (nuevaFechaLimiteRespuestaMs == null) {

            mostrarMensaje(
                "Selecciona la nueva fecha límite de respuesta."
            )

            return
        }

        if (
            nuevaFechaLimiteRespuestaMs >
            nuevaFechaMs
        ) {

            mostrarMensaje(
                "La fecha límite de respuesta no puede ser posterior al partido."
            )

            return
        }

        _uiState.value =
            _uiState.value.copy(
                guardando = true,
                mensaje = null,
            )

        repository.reprogramarPartido(
            partidoId = partidoId,
            nuevaFechaMs = nuevaFechaMs,
            nuevaHora = nuevaHora.trim(),
            nuevoLugar = nuevoLugar.trim(),
            nuevaFechaLimiteRespuestaMs =
                nuevaFechaLimiteRespuestaMs,
        )

        _uiState.value =
            _uiState.value.copy(
                guardando = false,
                reprogramado = true,
                mensaje = "Partido reprogramado."
            )
    }

    fun cancelar(
        motivo: String
    ) {

        if (motivo.isBlank()) {

            mostrarMensaje(
                "Ingresa el motivo de la cancelación."
            )

            return
        }

        _uiState.value =
            _uiState.value.copy(
                guardando = true,
                mensaje = null,
            )

        repository.cancelarPartido(
            partidoId = partidoId,
            motivo = motivo.trim(),
        )

        _uiState.value =
            _uiState.value.copy(
                guardando = false,
                cancelado = true,
                mensaje = "Partido cancelado."
            )
    }

    private fun mostrarMensaje(
        mensaje: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                mensaje = mensaje
            )
    }

    fun mensajeMostrado() {

        _uiState.value =
            _uiState.value.copy(
                mensaje = null
            )
    }

    fun resultadoConsumido() {

        _uiState.value =
            _uiState.value.copy(
                reprogramado = false,
                cancelado = false,
            )
    }
}