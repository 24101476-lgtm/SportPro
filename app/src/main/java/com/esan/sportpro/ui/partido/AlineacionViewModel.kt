package com.esan.sportpro.ui.partido

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.esan.sportpro.core.viewmodel.BaseViewModel
import com.esan.sportpro.domain.partido.EstadoRespuestaConvocatoria
import com.esan.sportpro.domain.partido.JugadorConvocado
import com.esan.sportpro.domain.partido.Partido
import com.esan.sportpro.domain.partido.PartidoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject


data class AlineacionUiState(

    val partido: Partido? = null,

    /**
     * Jugadores que confirmaron su asistencia.
     *
     * Aquí iremos marcando:
     * - titular / suplente
     * - dorsal
     * - posición
     */
    val jugadores: List<JugadorConvocado> =
        emptyList(),

    val cargando: Boolean = true,

    val confirmando: Boolean = false,

    val alineacionGuardada: Boolean = false,

    val mensaje: String? = null,
) {

    val titulares: List<JugadorConvocado>
        get() =
            jugadores.filter {
                it.titular
            }

    val suplentes: List<JugadorConvocado>
        get() =
            jugadores.filterNot {
                it.titular
            }

    val cantidadTitulares: Int
        get() = titulares.size

    val puedeConfirmar: Boolean
        get() =
            cantidadTitulares == 11 &&
                    jugadores.size >= 11
}


sealed interface AlineacionEvent


@HiltViewModel
class AlineacionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PartidoRepository,
) : BaseViewModel<AlineacionUiState, AlineacionEvent>(
    AlineacionUiState()
) {

    private val partidoId: String =
        savedStateHandle["partidoId"]
            ?: ""

    /**
     * IDs que ya existían dentro de convocados.
     *
     * Los utilizamos al guardar para poder eliminar
     * jugadores que hayan quedado fuera de una
     * alineación editada.
     */
    private var convocadosPreviosIds:
            List<String> = emptyList()

    /**
     * Evita que cada actualización de Firestore
     * destruya los cambios que el entrenador está
     * haciendo localmente.
     */
    private var alineacionInicializada =
        false


    init {

        if (partidoId.isBlank()) {

            setState {
                copy(
                    cargando = false,
                    mensaje =
                        "No se pudo identificar el partido."
                )
            }

        } else {

            observarDatos()
            observarErrores()
        }
    }


    // =========================================================
    // CARGA DE DATOS
    // =========================================================

    private fun observarDatos() {

        viewModelScope.launch {

            combine(

                repository.observarPartido(
                    partidoId
                ),

                repository
                    .observarRespuestasConvocatoria(
                        partidoId
                    ),

                repository.observarConvocados(
                    partidoId
                ),

                ) {
                    partido,
                    respuestas,
                    convocados ->

                Triple(
                    partido,
                    respuestas,
                    convocados
                )
            }
                .collect {
                        (
                            partido,
                            respuestas,
                            convocados
                        ) ->

                    convocadosPreviosIds =
                        convocados.map {
                                jugador ->
                            jugador.id
                        }

                    /*
                     * US-016:
                     *
                     * SOLO pueden formar parte de
                     * la alineación quienes hayan
                     * confirmado la convocatoria.
                     */
                    val confirmados =
                        respuestas.filter {
                                respuesta ->

                            respuesta.estado ==
                                    EstadoRespuestaConvocatoria
                                        .CONFIRMADO
                        }

                    val convocadosPorId =
                        convocados.associateBy {
                                jugador ->
                            jugador.id
                        }

                    val actualesPorId =
                        currentState
                            .jugadores
                            .associateBy {
                                    jugador ->
                                jugador.id
                            }

                    /*
                     * Primera carga:
                     *
                     * 1. Si el jugador ya estaba en
                     *    convocados, conservamos dorsal,
                     *    posición y titularidad.
                     *
                     * 2. Si todavía no existe allí,
                     *    empieza como suplente.
                     *
                     * Después conservamos la edición
                     * local del entrenador.
                     */
                    val jugadoresAlineacion =
                        confirmados.map {
                                respuesta ->

                            val jugadorGuardado =
                                convocadosPorId[
                                    respuesta.jugadorId
                                ]

                            val jugadorActual =
                                actualesPorId[
                                    respuesta.jugadorId
                                ]

                            when {

                                alineacionInicializada &&
                                        jugadorActual != null -> {

                                    jugadorActual.copy(
                                        nombre =
                                            respuesta.nombre
                                    )
                                }

                                jugadorGuardado != null -> {

                                    jugadorGuardado.copy(
                                        nombre =
                                            respuesta.nombre
                                    )
                                }

                                else -> {

                                    JugadorConvocado(
                                        id =
                                            respuesta.jugadorId,
                                        nombre =
                                            respuesta.nombre,
                                        dorsal =
                                            respuesta.dorsal,
                                        titular =
                                            false,
                                        posicion =
                                            null,
                                    )
                                }
                            }
                        }

                    alineacionInicializada =
                        true

                    setState {

                        copy(
                            partido = partido,
                            jugadores =
                                jugadoresAlineacion,
                            cargando = false,
                        )
                    }
                }
        }
    }


    // =========================================================
    // TITULARES / SUPLENTES
    // =========================================================

    fun cambiarTitularidad(
        jugadorId: String
    ) {

        val jugador =
            currentState
                .jugadores
                .firstOrNull {
                    it.id == jugadorId
                }
                ?: return

        /*
         * Si está intentando convertir a un
         * suplente en titular, controlamos que
         * nunca existan más de 11 titulares.
         */
        if (
            !jugador.titular &&
            currentState.cantidadTitulares >= 11
        ) {

            setState {
                copy(
                    mensaje =
                        "La alineación solo puede tener 11 titulares."
                )
            }

            return
        }

        setState {

            copy(
                jugadores =
                    jugadores.map {
                            actual ->

                        if (
                            actual.id ==
                            jugadorId
                        ) {

                            actual.copy(
                                titular =
                                    !actual.titular
                            )

                        } else {

                            actual
                        }
                    },

                mensaje = null,
            )
        }
    }


    // =========================================================
    // POSICIÓN
    // =========================================================

    fun cambiarPosicion(
        jugadorId: String,
        posicion: String?,
    ) {

        setState {

            copy(
                jugadores =
                    jugadores.map {
                            jugador ->

                        if (
                            jugador.id ==
                            jugadorId
                        ) {

                            jugador.copy(
                                posicion =
                                    posicion
                                        ?.trim()
                                        ?.takeIf {
                                            it.isNotBlank()
                                        }
                            )

                        } else {

                            jugador
                        }
                    }
            )
        }
    }


    // =========================================================
    // DORSAL
    // =========================================================

    fun cambiarDorsal(
        jugadorId: String,
        dorsal: Int?,
    ) {

        if (
            dorsal != null &&
            dorsal !in 1..99
        ) {

            setState {
                copy(
                    mensaje =
                        "El dorsal debe estar entre 1 y 99."
                )
            }

            return
        }

        setState {

            copy(
                jugadores =
                    jugadores.map {
                            jugador ->

                        if (
                            jugador.id ==
                            jugadorId
                        ) {

                            jugador.copy(
                                dorsal = dorsal
                            )

                        } else {

                            jugador
                        }
                    },

                mensaje = null,
            )
        }
    }


    // =========================================================
    // CONFIRMAR ALINEACIÓN
    // =========================================================

    fun confirmarAlineacion() {

        val state =
            currentState

        if (
            state.jugadores.size < 11
        ) {

            setState {
                copy(
                    mensaje =
                        "Se necesitan al menos 11 jugadores confirmados para formar la alineación."
                )
            }

            return
        }

        if (
            state.cantidadTitulares != 11
        ) {

            setState {
                copy(
                    mensaje =
                        "Debes seleccionar exactamente 11 titulares."
                )
            }

            return
        }

        /*
         * Los titulares deben tener una
         * posición definida.
         */
        val titularSinPosicion =
            state.titulares.any {
                it.posicion.isNullOrBlank()
            }

        if (titularSinPosicion) {

            setState {
                copy(
                    mensaje =
                        "Asigna una posición a todos los titulares."
                )
            }

            return
        }

        /*
         * Si existen dorsales informados,
         * no permitimos repetirlos.
         */
        val dorsales =
            state.jugadores
                .mapNotNull {
                    it.dorsal
                }

        if (
            dorsales.size !=
            dorsales.distinct().size
        ) {

            setState {
                copy(
                    mensaje =
                        "No puede haber dos jugadores con el mismo dorsal."
                )
            }

            return
        }

        setState {
            copy(
                confirmando = true,
                mensaje = null,
            )
        }

        repository.guardarAlineacionFinal(
            partidoId =
                partidoId,

            jugadores =
                state.jugadores,

            convocadosPreviosIds =
                convocadosPreviosIds,
        )

        /*
         * El repositorio trabaja offline-first:
         * Firestore aplica el cambio localmente
         * y después lo sincroniza con el servidor.
         */
        setState {
            copy(
                confirmando = false,
                alineacionGuardada = true,
            )
        }
    }


    // =========================================================
    // MENSAJES
    // =========================================================

    fun mensajeMostrado() {

        setState {
            copy(
                mensaje = null
            )
        }
    }


    private fun observarErrores() {

        viewModelScope.launch {

            repository
                .erroresEscritura
                .collect {
                        error ->

                    setState {
                        copy(
                            confirmando = false,
                            alineacionGuardada =
                                false,
                            mensaje = error,
                        )
                    }
                }
        }
    }
}