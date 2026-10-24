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

data class ConvocatoriaSeguimientoUiState(
    val partido: Partido? = null,
    val respuestas: List<RespuestaConvocatoria> = emptyList(),

    val confirmados: Int = 0,
    val noDisponibles: Int = 0,
    val sinResponder: Int = 0,

    val cargando: Boolean = true,
    val enviandoRecordatorio: Boolean = false,

    val mensaje: String? = null,
)

@HiltViewModel
class ConvocatoriaSeguimientoViewModel @Inject constructor(
    private val repository: PartidoRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val partidoId: String =
        savedStateHandle["partidoId"] ?: ""

    private val _uiState =
        MutableStateFlow(
            ConvocatoriaSeguimientoUiState()
        )

    val uiState: StateFlow<ConvocatoriaSeguimientoUiState> =
        _uiState.asStateFlow()

    init {
        cargarSeguimiento()
        escucharErrores()
    }

    private fun cargarSeguimiento() {

        if (partidoId.isBlank()) {

            _uiState.value =
                _uiState.value.copy(
                    cargando = false,
                    mensaje = "No se encontró el partido."
                )

            return
        }

        viewModelScope.launch {

            combine(
                repository.observarPartido(
                    partidoId
                ),

                repository.observarRespuestasConvocatoria(
                    partidoId
                ),
            ) { partido, respuestas ->

                partido to respuestas
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
                .collect { (partido, respuestas) ->

                    val confirmados =
                        respuestas.count {
                            it.estado ==
                                    EstadoRespuestaConvocatoria.CONFIRMADO
                        }

                    val noDisponibles =
                        respuestas.count {
                            it.estado ==
                                    EstadoRespuestaConvocatoria.NO_DISPONIBLE
                        }

                    val sinResponder =
                        respuestas.count {
                            it.estado ==
                                    EstadoRespuestaConvocatoria.SIN_RESPONDER
                        }

                    _uiState.value =
                        _uiState.value.copy(
                            partido = partido,
                            respuestas = respuestas,
                            confirmados = confirmados,
                            noDisponibles = noDisponibles,
                            sinResponder = sinResponder,
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
                            enviandoRecordatorio = false,
                            mensaje = error,
                        )
                }
        }
    }

    fun enviarRecordatorioPendientes() {

        val pendientes =
            _uiState.value
                .respuestas
                .filter {
                    it.estado ==
                            EstadoRespuestaConvocatoria.SIN_RESPONDER
                }

        if (pendientes.isEmpty()) {

            _uiState.value =
                _uiState.value.copy(
                    mensaje =
                        "No hay jugadores pendientes de responder."
                )

            return
        }

        val idsPendientes =
            pendientes.map {
                it.jugadorId
            }

        _uiState.value =
            _uiState.value.copy(
                enviandoRecordatorio = true,
                mensaje = null,
            )

        repository.enviarRecordatorioConvocatoria(
            partidoId = partidoId,
            jugadoresPendientesIds = idsPendientes,
        )

        _uiState.value =
            _uiState.value.copy(
                enviandoRecordatorio = false,
                mensaje =
                    "Recordatorio registrado para " +
                            "${idsPendientes.size} jugador(es) pendiente(s)."
            )
    }

    fun mensajeMostrado() {

        _uiState.value =
            _uiState.value.copy(
                mensaje = null
            )
    }
}