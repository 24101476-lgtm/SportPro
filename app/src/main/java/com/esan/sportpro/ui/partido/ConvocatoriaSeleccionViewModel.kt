package com.esan.sportpro.ui.partido

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esan.sportpro.domain.partido.JugadorPlantel
import com.esan.sportpro.domain.partido.PartidoRepository
import com.esan.sportpro.domain.partido.RespuestaConvocatoria
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConvocatoriaSeleccionUiState(
    val jugadores: List<JugadorPlantel> = emptyList(),
    val seleccionados: Set<String> = emptySet(),
    val maxConvocados: Int = 23,
    val cargando: Boolean = true,
    val publicando: Boolean = false,
    val publicada: Boolean = false,
    val mensaje: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ConvocatoriaSeleccionViewModel @Inject constructor(
    private val repository: PartidoRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val partidoId: String =
        savedStateHandle["partidoId"] ?: ""


    private val _uiState =
        MutableStateFlow(
            ConvocatoriaSeleccionUiState()
        )

    val uiState: StateFlow<ConvocatoriaSeleccionUiState> =
        _uiState.asStateFlow()

    init {
        cargarDatos()
        escucharErrores()
    }

    private fun cargarDatos() {

        if (partidoId.isBlank()) {
            _uiState.value =
                _uiState.value.copy(
                    cargando = false,
                    mensaje = "No se encontró el partido."
                )

            return
        }

        viewModelScope.launch {

            // El plantel es el del equipo real del partido (antes se usaba un "equipo-demo" fijo
            // que no existía, así que la convocatoria no mostraba jugadores).
            repository.observarPartido(partidoId)
                .flatMapLatest { partido ->
                    val equipoId = partido?.equipoId
                    val maximo = partido?.maxConvocados ?: 23
                    if (equipoId.isNullOrBlank()) {
                        flowOf(Pair(emptyList<JugadorPlantel>(), maximo))
                    } else {
                        repository.observarJugadoresEquipo(equipoId).map { jugadores -> Pair(jugadores, maximo) }
                    }
                }
                .catch { error ->

                    _uiState.value =
                        _uiState.value.copy(
                            cargando = false,
                            mensaje =
                                error.message
                                    ?: "No se pudieron cargar los jugadores."
                        )
                }
                .collect { (jugadores, maxConvocados) ->

                    /*
                     * Si algún jugador desaparece del plantel,
                     * también lo quitamos de la selección.
                     */
                    val idsDisponibles =
                        jugadores
                            .map { it.id }
                            .toSet()

                    val seleccionValidos =
                        _uiState.value
                            .seleccionados
                            .filter {
                                it in idsDisponibles
                            }
                            .toSet()

                    _uiState.value =
                        _uiState.value.copy(
                            jugadores = jugadores,
                            seleccionados = seleccionValidos,
                            maxConvocados = maxConvocados,
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
                            publicando = false,
                            mensaje = error
                        )
                }
        }
    }

    fun alternarSeleccion(
        jugadorId: String
    ) {

        val estadoActual =
            _uiState.value

        val seleccionActual =
            estadoActual.seleccionados

        /*
         * Si ya estaba seleccionado,
         * simplemente lo quitamos.
         */
        if (jugadorId in seleccionActual) {

            _uiState.value =
                estadoActual.copy(
                    seleccionados =
                        seleccionActual - jugadorId,
                    mensaje = null,
                )

            return
        }

        /*
         * No permitimos superar el máximo
         * configurado al crear el partido.
         */
        if (
            seleccionActual.size >=
            estadoActual.maxConvocados
        ) {

            _uiState.value =
                estadoActual.copy(
                    mensaje =
                        "Puedes convocar como máximo " +
                                "${estadoActual.maxConvocados} jugadores."
                )

            return
        }

        _uiState.value =
            estadoActual.copy(
                seleccionados =
                    seleccionActual + jugadorId,
                mensaje = null,
            )
    }

    fun seleccionarTodos() {

        val estadoActual =
            _uiState.value

        val ids =
            estadoActual
                .jugadores
                .take(
                    estadoActual.maxConvocados
                )
                .map {
                    it.id
                }
                .toSet()

        _uiState.value =
            estadoActual.copy(
                seleccionados = ids,
                mensaje = null,
            )
    }

    fun limpiarSeleccion() {

        _uiState.value =
            _uiState.value.copy(
                seleccionados = emptySet(),
                mensaje = null,
            )
    }

    fun publicarConvocatoria() {

        val estadoActual =
            _uiState.value

        if (
            estadoActual
                .seleccionados
                .isEmpty()
        ) {

            _uiState.value =
                estadoActual.copy(
                    mensaje =
                        "Selecciona al menos un jugador."
                )

            return
        }

        if (partidoId.isBlank()) {

            _uiState.value =
                estadoActual.copy(
                    mensaje =
                        "No se encontró el partido."
                )

            return
        }

        val jugadoresSeleccionados =
            estadoActual
                .jugadores
                .filter {
                    it.id in
                            estadoActual.seleccionados
                }

        val respuestas =
            jugadoresSeleccionados.map {
                    jugador ->

                RespuestaConvocatoria(
                    jugadorId =
                        jugador.id,

                    nombre =
                        jugador.nombre,
                )
            }

        _uiState.value =
            estadoActual.copy(
                publicando = true,
                mensaje = null,
            )

        repository.publicarConvocatoria(
            partidoId = partidoId,
            jugadores = respuestas,
        )

        /*
         * El repositorio trabaja con escrituras
         * asíncronas de Firestore.
         *
         * Si Firebase rechaza la operación,
         * erroresEscritura lo notificará.
         */
        _uiState.value =
            _uiState.value.copy(
                publicando = false,
                publicada = true,
                mensaje =
                    "Convocatoria publicada."
            )
    }

    fun publicacionConsumida() {

        _uiState.value =
            _uiState.value.copy(
                publicada = false
            )
    }

    fun mensajeMostrado() {

        _uiState.value =
            _uiState.value.copy(
                mensaje = null
            )
    }
}