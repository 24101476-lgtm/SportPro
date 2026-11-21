package com.esan.sportpro.ui.partido

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.esan.sportpro.core.viewmodel.BaseViewModel
import com.esan.sportpro.domain.partido.EstadoPartido
import com.esan.sportpro.domain.partido.EstadoRespuestaConvocatoria
import com.esan.sportpro.domain.partido.NuevoPartido
import com.esan.sportpro.domain.partido.JugadorConvocado
import com.esan.sportpro.domain.partido.Partido
import com.esan.sportpro.domain.partido.PartidoRepository
import com.esan.sportpro.domain.jugadores.JugadorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject



data class PosicionTacticaUi(
    val x: Float,
    val y: Float,
)


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

    /**
     * Coordenadas normalizadas (0f..1f) de los titulares sobre la cancha.
     * Se mantienen en el ViewModel mientras el entrenador arma la alineación.
     */
    val posicionesTacticas: Map<String, PosicionTacticaUi> =
        emptyMap(),

    /** Foto de cada jugador (id → URL), leída de su ficha; vacía si el rol no puede leerla. */
    val fotosJugadores: Map<String, String> =
        emptyMap(),

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

    /**
     * Modelo listo para pintar los titulares en [CanchaTactica].
     *
     * Cuando todavía no existe una coordenada manual usamos una distribución
     * 4-3-3 como posición inicial. Si el entrenador ya movió a un jugador,
     * conservamos su coordenada.
     */
    val titularesCancha: List<JugadorCanchaUi>
        get() {
            val base =
                titulares.map { jugador ->
                    val posicionGuardada =
                        posicionesTacticas[jugador.id]

                    JugadorCanchaUi(
                        id = jugador.id,
                        nombre = jugador.nombre,
                        dorsal = jugador.dorsal,
                        posicion = jugador.posicion,
                        titular = true,
                        x = posicionGuardada?.x ?: 0.5f,
                        y = posicionGuardada?.y ?: 0.5f,
                        fotoUrl = fotosJugadores[jugador.id].orEmpty(),
                    )
                }

            val distribuidos =
                posiciones433(base)

            return distribuidos.map { jugador ->
                val posicionGuardada =
                    posicionesTacticas[jugador.id]

                if (posicionGuardada != null) {
                    jugador.copy(
                        x = posicionGuardada.x,
                        y = posicionGuardada.y,
                    )
                } else {
                    jugador
                }
            }
        }

    val suplentesCancha: List<JugadorCanchaUi>
        get() =
            suplentes.map { jugador ->
                JugadorCanchaUi(
                    id = jugador.id,
                    nombre = jugador.nombre,
                    dorsal = jugador.dorsal,
                    posicion = jugador.posicion,
                    titular = false,
                    x = 0.5f,
                    y = 0.5f,
                    fotoUrl = fotosJugadores[jugador.id].orEmpty(),
                )
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
    private val jugadorRepository: JugadorRepository,
    private val borradores: BorradoresPartido,
) : BaseViewModel<AlineacionUiState, AlineacionEvent>(
    AlineacionUiState()
) {

    private val partidoId: String =
        savedStateHandle["partidoId"]
            ?: ""

    /**
     * Partido armado en "Nuevo partido" que todavía no existe en Firestore.
     * Si es null, el partido ya está publicado y se lee de la base de datos.
     */
    private val borrador: NuevoPartido? =
        borradores.obtener(partidoId)

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

            if (borrador != null) {
                cargarBorrador(borrador)
            } else {
                observarDatos()
            }
            observarFotos()
            observarErrores()
        }
    }


    // =========================================================
    // CARGA DE DATOS
    // =========================================================

    /** El partido aún no existe en Firestore: la pizarra trabaja sobre los datos en memoria. */
    private fun cargarBorrador(nuevo: NuevoPartido) {
        alineacionInicializada = true
        convocadosPreviosIds = emptyList()

        setState {
            copy(
                partido = Partido(
                    id = partidoId,
                    nombreEquipo = nuevo.nombreEquipo,
                    nombreRival = nuevo.nombreRival,
                    categoria = nuevo.categoria,
                    estado = EstadoPartido.PROGRAMADO,
                    alineacionConfirmada = false,
                    cambiosPermitidos = nuevo.cambiosPermitidos,
                    duracionTiempoMin = nuevo.duracionTiempoMin,
                    inicioPrimerTiempoMs = null,
                    inicioSegundoTiempoMs = null,
                    enDescanso = false,
                    operadorUid = null,
                    entrenadorUid = null,
                    creadoPor = null,
                    fechaMs = null,
                    equipoId = nuevo.equipoId,
                    academiaId = nuevo.academiaId,
                    tipo = nuevo.tipo,
                    temporada = nuevo.temporada,
                ),
                jugadores = nuevo.convocados,
                cargando = false,
            )
        }
    }

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
                     * Hay dos formas válidas de llegar a la alineación:
                     *
                     * 1. Flujo convocatoria:
                     *    solo entran quienes respondieron CONFIRMADO.
                     *
                     * 2. Flujo "Nuevo partido":
                     *    los jugadores ya fueron guardados directamente
                     *    en la subcolección convocados.
                     *
                     * Si no existen respuestas de convocatoria y sí hay
                     * convocados, usamos esos convocados como base.
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

                    val usarConvocadosDirectos =
                        respuestas.isEmpty() &&
                                convocados.isNotEmpty()

                    val jugadoresAlineacion =
                        if (usarConvocadosDirectos) {

                            convocados.map {
                                    jugadorGuardado ->

                                val jugadorActual =
                                    actualesPorId[
                                        jugadorGuardado.id
                                    ]

                                if (
                                    alineacionInicializada &&
                                    jugadorActual != null
                                ) {
                                    jugadorActual.copy(
                                        nombre =
                                            jugadorGuardado.nombre
                                    )
                                } else {
                                    jugadorGuardado
                                }
                            }

                        } else {

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


    /**
     * Fotos de las fichas de la academia del partido para pintarlas en la pizarra.
     * Si las reglas no dejan leer `jugadores` (o no hay conexión) las fichas muestran iniciales.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observarFotos() {
        val academia =
            if (borrador != null) flowOf(borrador.academiaId)
            else repository.observarPartido(partidoId).map { it?.academiaId }

        viewModelScope.launch {
            academia
                .distinctUntilChanged()
                .flatMapLatest { academiaId ->
                    if (academiaId.isNullOrBlank()) {
                        flowOf(emptyMap())
                    } else {
                        jugadorRepository.observarJugadores(academiaId)
                            .map { jugadores ->
                                jugadores
                                    .filter { it.fotoUrl.isNotBlank() }
                                    .associate { it.id to it.fotoUrl }
                            }
                            .catch { emit(emptyMap()) }
                    }
                }
                .catch { emit(emptyMap()) }
                .collect { fotos ->
                    setState { copy(fotosJugadores = fotos) }
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

            val nuevoEstadoTitular =
                !jugador.titular

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
                                    nuevoEstadoTitular
                            )

                        } else {

                            actual
                        }
                    },

                posicionesTacticas =
                    if (nuevoEstadoTitular) {
                        posicionesTacticas
                    } else {
                        posicionesTacticas - jugadorId
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
    // PIZARRA TÁCTICA
    // =========================================================

    /**
     * Actualiza la posición visual de un titular cuando el entrenador
     * lo arrastra sobre la cancha.
     *
     * X e Y son coordenadas normalizadas entre 0f y 1f.
     */
    fun moverJugadorEnCancha(
        jugadorId: String,
        x: Float,
        y: Float,
    ) {

        val esTitular =
            currentState.titulares.any {
                it.id == jugadorId
            }

        if (!esTitular) {
            return
        }

        setState {
            copy(
                posicionesTacticas =
                    posicionesTacticas +
                            (
                                    jugadorId to
                                            PosicionTacticaUi(
                                                x = x.coerceIn(
                                                    0.05f,
                                                    0.95f
                                                ),
                                                y = y.coerceIn(
                                                    0.05f,
                                                    0.95f
                                                ),
                                            )
                                    )
            )
        }
    }


    /**
     * Restaura la distribución visual 4-3-3 para los titulares actuales.
     */
    fun aplicarFormacion433() {

        val base =
            currentState.titulares.map { jugador ->
                JugadorCanchaUi(
                    id = jugador.id,
                    nombre = jugador.nombre,
                    dorsal = jugador.dorsal,
                    posicion = jugador.posicion,
                    titular = true,
                    x = 0.5f,
                    y = 0.5f,
                )
            }

        val distribuidos =
            posiciones433(base)

        setState {
            copy(
                posicionesTacticas =
                    distribuidos.associate {
                            jugador ->
                        jugador.id to
                                PosicionTacticaUi(
                                    x = jugador.x,
                                    y = jugador.y,
                                )
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

        if (borrador != null) {
            /*
             * Recién aquí el partido se escribe en Firestore, ya con la
             * alineación confirmada. Antes solo existía en memoria.
             */
            repository.crearPartido(
                nuevo = borrador.copy(convocados = state.jugadores),
                id = partidoId,
                alineacionConfirmada = true,
            )
            borradores.quitar(partidoId)
        } else {
            repository.guardarAlineacionFinal(
                partidoId =
                    partidoId,

                jugadores =
                    state.jugadores,

                convocadosPreviosIds =
                    convocadosPreviosIds,
            )
        }

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

    /** Si el entrenador sale de la pizarra sin crear el partido, el borrador se descarta. */
    override fun onCleared() {
        borradores.quitar(partidoId)
        super.onCleared()
    }
}
