package com.esan.sportpro.ui.partido

import androidx.lifecycle.viewModelScope
import com.esan.sportpro.core.viewmodel.BaseViewModel
import com.esan.sportpro.data.partido.PartidoEnCursoStore
import com.esan.sportpro.data.partido.describirErrorFirestore
import com.esan.sportpro.domain.academia.Equipo
import com.esan.sportpro.domain.academia.EquipoRepository
import com.esan.sportpro.domain.cuentas.PerfilActual
import com.esan.sportpro.domain.cuentas.SesionRepository
import com.esan.sportpro.domain.jugadores.Jugador
import com.esan.sportpro.domain.jugadores.JugadorRepository
import com.esan.sportpro.domain.partido.JugadorConvocado
import com.esan.sportpro.domain.partido.NuevoPartido
import com.esan.sportpro.domain.partido.Partido
import com.esan.sportpro.domain.partido.PartidoRepository
import com.esan.sportpro.domain.partido.TiposPartido
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Condición de cada jugador del equipo en el formulario de nuevo partido. */
enum class Convocatoria { NO_CONVOCADO, TITULAR, SUPLENTE }

/** Formulario para programar un partido con un equipo real de la academia. */
data class FormularioPartido(
    val equipoId: String? = null,
    val rival: String = "",
    val tipo: String = TiposPartido.AMISTOSO,
    val duracionTexto: String = "45",
    val cambiosTexto: String = "5",
    /** jugadorId → condición. Los jugadores que no aparecen no están convocados. */
    val convocatoria: Map<String, Convocatoria> = emptyMap(),
    val error: String? = null,
) {
    val titulares: Int get() = convocatoria.values.count { it == Convocatoria.TITULAR }
    val suplentes: Int get() = convocatoria.values.count { it == Convocatoria.SUPLENTE }
}

data class PartidosUiState(
    val cargando: Boolean = true,
    val perfil: PerfilActual? = null,
    val partidos: List<Partido> = emptyList(),
    val equipos: List<Equipo> = emptyList(),
    val jugadores: List<Jugador> = emptyList(),
    /** Formulario abierto de nuevo partido (equipo real). */
    val formulario: FormularioPartido? = null,
    /** Partido que la pantalla debe abrir (restauración o recién creado). */
    val abrirPartidoId: String? = null,
    val error: String? = null,
    val aviso: String? = null,
    val mensaje: String? = null,
) {
    /** Solo administrador y entrenador programan partidos; jugador y padre los siguen. */
    val puedeCrear: Boolean get() = perfil?.esStaff == true

    val equiposActivos: List<Equipo> get() = equipos.filter { it.activo }

    /** Jugadores activos del equipo elegido en el formulario, ordenados por dorsal. */
    val jugadoresDelEquipo: List<Jugador>
        get() = jugadoresDe(formulario?.equipoId)

    fun jugadoresDe(equipoId: String?): List<Jugador> {
        if (equipoId == null) return emptyList()
        return jugadores.filter { it.activo && it.equipoId == equipoId }
            .sortedWith(compareBy({ it.dorsal ?: Int.MAX_VALUE }, { it.apellidos }))
    }
}

@HiltViewModel
class PartidosViewModel @Inject constructor(
    private val repo: PartidoRepository,
    private val partidoEnCurso: PartidoEnCursoStore,
    private val sesionRepository: SesionRepository,
    private val equipoRepository: EquipoRepository,
    private val jugadorRepository: JugadorRepository,
) : BaseViewModel<PartidosUiState, Unit>(PartidosUiState()) {

    private var restauracionRevisada = false
    private var escucha: Job? = null
    private var todos: List<Partido> = emptyList()

    init {
        viewModelScope.launch {
            val perfil = runCatching { sesionRepository.perfilActual() }.getOrNull()
            setState { copy(perfil = perfil) }
            filtrarPorAcademia()
            if (perfil != null && perfil.esStaff) {
                launch {
                    equipoRepository.observarEquipos(perfil.academiaId)
                        .catch { /* sin equipos: el formulario lo indica */ }
                        .collect { lista ->
                            setState { copy(equipos = lista) }
                            val f = currentState.formulario
                            if (f != null && f.equipoId == null) {
                                lista.firstOrNull { it.activo }?.let { onEquipo(it.id) }
                            }
                        }
                }
                launch {
                    jugadorRepository.observarJugadores(perfil.academiaId)
                        .catch { /* sin permisos o sin red: el formulario lo indica */ }
                        .collect { lista ->
                            setState { copy(jugadores = lista) }
                            // Si el formulario se abrió antes de que llegaran los jugadores, se propone la alineación.
                            val f = currentState.formulario
                            if (f != null && f.convocatoria.isEmpty()) {
                                setState { copy(formulario = f.copy(convocatoria = propuesta(f.equipoId))) }
                            }
                        }
                }
            }
        }
        escuchar()
        viewModelScope.launch {
            repo.erroresEscritura.collect { setState { copy(mensaje = it) } }
        }
    }

    /** (Re)inicia la escucha en tiempo real de los partidos. */
    fun escuchar() {
        escucha?.cancel()
        setState { copy(cargando = true, error = null, aviso = null) }
        escucha = viewModelScope.launch {
            launch {
                delay(TIEMPO_ESPERA_MS)
                if (currentState.cargando) {
                    setState {
                        copy(
                            cargando = false,
                            aviso = "Firestore no responde todavía. Revisa tu conexión; " +
                                "los partidos aparecerán apenas lleguen.",
                        )
                    }
                }
            }
            repo.observarPartidos()
                .catch { e -> setState { copy(cargando = false, error = describirErrorFirestore(e)) } }
                .collect { lista ->
                    todos = lista
                    filtrarPorAcademia()
                    setState { copy(cargando = false, aviso = null, error = null) }
                }
        }
    }

    /** Muestra solo los partidos de la academia del usuario. */
    private fun filtrarPorAcademia() {
        val academia = currentState.perfil?.academiaId
        val visibles = todos.filter { academia == null || it.academiaId == academia }
        setState { copy(partidos = visibles) }
    }

    /**
     * Si la app se cerró en medio de un partido, se vuelve a abrir directamente su registro.
     * Se hace una sola vez por pantalla para no impedir que el usuario regrese a la lista.
     */
    fun revisarPartidoEnCurso() {
        if (restauracionRevisada) return
        restauracionRevisada = true
        partidoEnCurso.partidoId?.let { id -> setState { copy(abrirPartidoId = id) } }
    }

    // ------------------------------------------------------------ Partido con equipo real

    fun abrirFormulario() {
        val equipoId = currentState.equiposActivos.firstOrNull()?.id
        setState { copy(formulario = FormularioPartido(equipoId = equipoId, convocatoria = propuesta(equipoId))) }
    }

    fun cerrarFormulario() = setState { copy(formulario = null) }

    private fun actualizar(cambio: FormularioPartido.() -> FormularioPartido) =
        setState { copy(formulario = formulario?.cambio()?.copy(error = null)) }

    fun onEquipo(equipoId: String) {
        val propuesta = propuesta(equipoId)
        actualizar { copy(equipoId = equipoId, convocatoria = propuesta) }
    }

    fun onRival(texto: String) = actualizar { copy(rival = texto.take(40)) }
    fun onTipo(tipo: String) = actualizar { copy(tipo = tipo) }
    fun onDuracion(texto: String) = actualizar { copy(duracionTexto = texto.filter(Char::isDigit).take(2)) }
    fun onCambios(texto: String) = actualizar { copy(cambiosTexto = texto.filter(Char::isDigit).take(2)) }

    /** Rota la condición del jugador: titular → suplente → no convocado → titular. */
    fun alternarJugador(jugadorId: String) = actualizar {
        val actual = convocatoria[jugadorId] ?: Convocatoria.NO_CONVOCADO
        val siguiente = when (actual) {
            Convocatoria.NO_CONVOCADO -> if (titulares < MAX_TITULARES) Convocatoria.TITULAR else Convocatoria.SUPLENTE
            Convocatoria.TITULAR -> Convocatoria.SUPLENTE
            Convocatoria.SUPLENTE -> Convocatoria.NO_CONVOCADO
        }
        copy(convocatoria = convocatoria + (jugadorId to siguiente))
    }

    /** Propone los 11 primeros jugadores (por dorsal) como titulares y el resto como suplentes. */
    private fun propuesta(equipoId: String?): Map<String, Convocatoria> =
        currentState.jugadoresDe(equipoId).mapIndexed { i, j ->
            j.id to if (i < MAX_TITULARES) Convocatoria.TITULAR else Convocatoria.SUPLENTE
        }.toMap()

    fun crearPartido() {
        val st = currentState
        val f = st.formulario ?: return
        val perfil = st.perfil
        val equipo = st.equiposActivos.firstOrNull { it.id == f.equipoId }
        val duracion = f.duracionTexto.toIntOrNull()
        val cambios = f.cambiosTexto.toIntOrNull()
        val error = when {
            perfil == null -> "No se pudo leer tu perfil. Vuelve a iniciar sesión."
            equipo == null -> "Selecciona el equipo"
            f.rival.trim().length < 2 -> "Escribe el nombre del rival"
            duracion == null || duracion !in 10..45 -> "La duración de cada tiempo debe estar entre 10 y 45 minutos"
            cambios == null || cambios !in 0..15 -> "Los cambios permitidos deben estar entre 0 y 15"
            f.titulares == 0 -> "Elige al menos un titular"
            else -> null
        }
        if (error != null || perfil == null || equipo == null || duracion == null || cambios == null) {
            setState { copy(formulario = f.copy(error = error)) }
            return
        }
        val convocados = st.jugadoresDelEquipo.mapNotNull { j ->
            when (f.convocatoria[j.id]) {
                Convocatoria.TITULAR -> j.aConvocado(titular = true)
                Convocatoria.SUPLENTE -> j.aConvocado(titular = false)
                else -> null
            }
        }
        val id = repo.crearPartido(
            NuevoPartido(
                equipoId = equipo.id,
                academiaId = perfil.academiaId,
                nombreEquipo = equipo.nombre,
                nombreRival = f.rival.trim(),
                categoria = equipo.categoria,
                tipo = f.tipo,
                temporada = LocalDate.now().year.toString(),
                duracionTiempoMin = duracion,
                cambiosPermitidos = cambios,
                convocados = convocados,
            ),
        )
        setState { copy(formulario = null, abrirPartidoId = id) }
    }

    private fun Jugador.aConvocado(titular: Boolean) = JugadorConvocado(
        id = id,
        nombre = nombreCompleto.ifBlank { "Jugador" },
        dorsal = dorsal,
        titular = titular,
        posicion = posicion.ifBlank { null },
    )

    fun navegacionRealizada() = setState { copy(abrirPartidoId = null) }

    fun mensajeMostrado() = setState { copy(mensaje = null) }

    private companion object {
        const val TIEMPO_ESPERA_MS = 8_000L
        const val MAX_TITULARES = 11
    }
}
