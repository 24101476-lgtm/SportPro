package com.esan.sportpro.ui.partido

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esan.sportpro.domain.partido.EstadoRespuestaConvocatoria
import com.esan.sportpro.domain.partido.Partido
import com.esan.sportpro.domain.partido.PartidoRepository
import com.esan.sportpro.domain.partido.RespuestaConvocatoria
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConvocatoriaRespuestaUiState(
    val partido: Partido? = null,
    val respuesta: RespuestaConvocatoria? = null,

    val cargando: Boolean = true,
    val guardando: Boolean = false,

    val vencida: Boolean = false,
    val respuestaGuardada: Boolean = false,

    val mensaje: String? = null,
)

@HiltViewModel
class ConvocatoriaRespuestaViewModel @Inject constructor(
    private val repository: PartidoRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val partidoId: String =
        savedStateHandle["partidoId"] ?: ""

    private val jugadorId: String =
        savedStateHandle["jugadorId"] ?: ""

    private val _uiState =
        MutableStateFlow(
            ConvocatoriaRespuestaUiState()
        )

    val uiState: StateFlow<ConvocatoriaRespuestaUiState> =
        _uiState.asStateFlow()

    init {
        cargarConvocatoria()
        escucharErrores()
    }

    private fun cargarConvocatoria() {

        if (
            partidoId.isBlank() ||
            jugadorId.isBlank()
        ) {

            _uiState.value =
                _uiState.value.copy(
                    cargando = false,
                    mensaje =
                        "No se encontró la convocatoria."
                )

            return
        }

        viewModelScope.launch {

            combine(
                repository.observarPartido(
                    partidoId
                ),

                repository.observarRespuestaConvocatoria(
                    partidoId = partidoId,
                    jugadorId = jugadorId,
                )
            ) { partido, respuesta ->

                partido to respuesta
            }
                .catch { error ->

                    _uiState.value =
                        _uiState.value.copy(
                            cargando = false,
                            mensaje =
                                error.message
                                    ?: "No se pudo cargar la convocatoria."
                        )
                }
                .collect { (partido, respuesta) ->

                    val fechaLimite =
                        partido
                            ?.fechaLimiteRespuestaMs

                    val vencida =
                        fechaLimite != null &&
                                System.currentTimeMillis() >
                                fechaLimite

                    _uiState.value =
                        _uiState.value.copy(
                            partido = partido,
                            respuesta = respuesta,
                            vencida = vencida,
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

    fun confirmarAsistencia() {

        val estadoActual =
            _uiState.value

        if (estadoActual.vencida) {

            _uiState.value =
                estadoActual.copy(
                    mensaje =
                        "La fecha límite para responder ya venció."
                )

            return
        }

        if (
            estadoActual.respuesta == null
        ) {

            _uiState.value =
                estadoActual.copy(
                    mensaje =
                        "No se encontró tu convocatoria."
                )

            return
        }

        _uiState.value =
            estadoActual.copy(
                guardando = true,
                mensaje = null,
            )

        repository.responderConvocatoria(
            partidoId = partidoId,
            jugadorId = jugadorId,
            estado =
                EstadoRespuestaConvocatoria.CONFIRMADO,
            motivo = null,
        )

        _uiState.value =
            _uiState.value.copy(
                guardando = false,
                respuestaGuardada = true,
                mensaje =
                    "Asistencia confirmada."
            )
    }

    fun responderNoDisponible(
        motivo: String
    ) {

        val estadoActual =
            _uiState.value

        if (estadoActual.vencida) {

            _uiState.value =
                estadoActual.copy(
                    mensaje =
                        "La fecha límite para responder ya venció."
                )

            return
        }

        if (motivo.isBlank()) {

            _uiState.value =
                estadoActual.copy(
                    mensaje =
                        "Indica el motivo por el que no podrás asistir."
                )

            return
        }

        if (
            estadoActual.respuesta == null
        ) {

            _uiState.value =
                estadoActual.copy(
                    mensaje =
                        "No se encontró tu convocatoria."
                )

            return
        }

        _uiState.value =
            estadoActual.copy(
                guardando = true,
                mensaje = null,
            )

        repository.responderConvocatoria(
            partidoId = partidoId,
            jugadorId = jugadorId,
            estado =
                EstadoRespuestaConvocatoria.NO_DISPONIBLE,
            motivo = motivo.trim(),
        )

        _uiState.value =
            _uiState.value.copy(
                guardando = false,
                respuestaGuardada = true,
                mensaje =
                    "Respuesta registrada."
            )
    }

    fun respuestaConsumida() {

        _uiState.value =
            _uiState.value.copy(
                respuestaGuardada = false
            )
    }

    fun mensajeMostrado() {

        _uiState.value =
            _uiState.value.copy(
                mensaje = null
            )
    }
}