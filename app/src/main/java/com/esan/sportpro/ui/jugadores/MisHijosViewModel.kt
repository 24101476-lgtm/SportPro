package com.esan.sportpro.ui.jugadores

import androidx.lifecycle.viewModelScope
import com.esan.sportpro.core.viewmodel.BaseViewModel
import com.esan.sportpro.data.partido.describirErrorFirestore
import com.esan.sportpro.domain.academia.Equipo
import com.esan.sportpro.domain.academia.EquipoRepository
import com.esan.sportpro.domain.cuentas.PerfilActual
import com.esan.sportpro.domain.cuentas.SesionRepository
import com.esan.sportpro.domain.entrenamientos.CalculoAsistencia
import com.esan.sportpro.domain.entrenamientos.Entrenamiento
import com.esan.sportpro.domain.entrenamientos.EntrenamientoRepository
import com.esan.sportpro.domain.entrenamientos.ResumenAsistencia
import com.esan.sportpro.domain.jugadores.DirectorioJugador
import com.esan.sportpro.domain.jugadores.EstadoVinculacion
import com.esan.sportpro.domain.jugadores.Jugador
import com.esan.sportpro.domain.jugadores.ReglasVinculacion
import com.esan.sportpro.domain.jugadores.Vinculacion
import com.esan.sportpro.domain.jugadores.VinculacionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PantallaMisHijos {
    data object Lista : PantallaMisHijos
    data object Solicitar : PantallaMisHijos
    data class Detalle(val jugadorId: String) : PantallaMisHijos
}

/** Criterio 1: el padre identifica al jugador con el código de la academia o desde la lista de su equipo. */
enum class ModoSolicitud(val etiqueta: String) { CODIGO("Con código"), LISTA("Desde la lista") }

data class FormularioSolicitud(
    val modo: ModoSolicitud = ModoSolicitud.CODIGO,
    val codigo: String = "",
    val errorCodigo: String? = null,
    val buscando: Boolean = false,
    val equipoId: String? = null,
    /** Jugador encontrado por código o elegido de la lista: se muestra solo nombre y equipo. */
    val seleccionado: DirectorioJugador? = null,
    val enviando: Boolean = false,
)

data class MisHijosUiState(
    val perfil: PerfilActual? = null,
    val vinculaciones: List<Vinculacion> = emptyList(),
    val directorio: List<DirectorioJugador> = emptyList(),
    val equipos: List<Equipo> = emptyList(),
    val entrenamientos: List<Entrenamiento> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null,
    val pantalla: PantallaMisHijos = PantallaMisHijos.Lista,
    val formulario: FormularioSolicitud = FormularioSolicitud(),
    val jugadorDetalle: Jugador? = null,
    val cargandoDetalle: Boolean = false,
) {
    /** Vínculos que el padre ve en su lista: los revocados se muestran para que sepa qué pasó. */
    val vinculacionesOrdenadas: List<Vinculacion>
        get() = vinculaciones.sortedWith(
            compareBy<Vinculacion> { it.estadoVinculacion.ordinal }.thenBy { it.jugadorNombre.lowercase() },
        )

    val equiposConJugadores: List<Equipo>
        get() = equipos.filter { equipo -> equipo.activo && directorio.any { it.equipoId == equipo.id } }

    val jugadoresDelEquipo: List<DirectorioJugador>
        get() = formulario.equipoId?.let { id -> directorio.filter { it.equipoId == id } }.orEmpty()

    fun vinculacionCon(jugadorId: String): Vinculacion? = vinculaciones.firstOrNull { it.jugadorId == jugadorId }

    /** Asistencia del jugador vinculado (criterio 6). */
    fun asistenciaDe(jugador: Jugador): ResumenAsistencia =
        CalculoAsistencia.resumen(jugador.id, entrenamientos.filter { it.equipoId == jugador.equipoId })
}

sealed interface MisHijosEvent {
    data class Mensaje(val texto: String) : MisHijosEvent
}

/** US-007, vista del padre de familia: solicitar la vinculación y consultar a sus hijos vinculados. */
@HiltViewModel
class MisHijosViewModel @Inject constructor(
    private val sesionRepository: SesionRepository,
    private val vinculacionRepository: VinculacionRepository,
    private val equipoRepository: EquipoRepository,
    private val entrenamientoRepository: EntrenamientoRepository,
) : BaseViewModel<MisHijosUiState, MisHijosEvent>(MisHijosUiState()) {

    private var detalleJob: Job? = null

    init {
        viewModelScope.launch {
            val perfil = sesionRepository.perfilActual()
            if (perfil == null) {
                setState { copy(cargando = false, error = "No se pudo leer tu perfil. Vuelve a iniciar sesión.") }
                return@launch
            }
            setState { copy(perfil = perfil) }
            launch {
                vinculacionRepository.observarMisVinculaciones(perfil.uid)
                    .catch { setState { copy(cargando = false, error = describirErrorFirestore(it)) } }
                    .collect { lista ->
                        setState { copy(vinculaciones = lista, cargando = false, error = null) }
                        verificarAccesoDetalle()
                    }
            }
            launch {
                vinculacionRepository.observarDirectorio(perfil.academiaId)
                    .catch { /* sin directorio solo se puede buscar por código */ }
                    .collect { lista -> setState { copy(directorio = lista) } }
            }
            launch {
                equipoRepository.observarEquipos(perfil.academiaId)
                    .catch { /* sin equipos solo se puede buscar por código */ }
                    .collect { lista -> setState { copy(equipos = lista) } }
            }
            launch {
                entrenamientoRepository.observarEntrenamientos(perfil.academiaId)
                    .catch { /* la asistencia es informativa */ }
                    .collect { lista -> setState { copy(entrenamientos = lista) } }
            }
        }
    }

    // ---- Navegación -----------------------------------------------------------------------

    fun volverALista() {
        detalleJob?.cancel()
        setState { copy(pantalla = PantallaMisHijos.Lista, jugadorDetalle = null) }
    }

    fun abrirSolicitar() = setState { copy(pantalla = PantallaMisHijos.Solicitar, formulario = FormularioSolicitud()) }

    /**
     * Criterios 5 y 6: solo con el vínculo aprobado se abre el perfil. Pendiente, rechazado o
     * revocado: el padre solo ve el nombre y la categoría en la lista.
     */
    fun abrirDetalle(vinculacion: Vinculacion) {
        if (!vinculacion.aprobada) {
            val texto = when (vinculacion.estadoVinculacion) {
                EstadoVinculacion.PENDIENTE -> "Tu solicitud está pendiente de aprobación por la academia."
                EstadoVinculacion.RECHAZADA -> "La academia rechazó la solicitud: ${vinculacion.motivoRechazo}"
                EstadoVinculacion.REVOCADA -> "La academia revocó esta vinculación."
                EstadoVinculacion.APROBADA -> return
            }
            sendEvent(MisHijosEvent.Mensaje(texto))
            return
        }
        detalleJob?.cancel()
        setState { copy(pantalla = PantallaMisHijos.Detalle(vinculacion.jugadorId), jugadorDetalle = null, cargandoDetalle = true) }
        detalleJob = viewModelScope.launch {
            vinculacionRepository.observarJugador(vinculacion.jugadorId).collect { jugador ->
                setState { copy(jugadorDetalle = jugador, cargandoDetalle = false) }
            }
        }
    }

    /** Criterio 8: si la academia revoca el vínculo mientras el padre mira el perfil, se cierra al instante. */
    private fun verificarAccesoDetalle() {
        val pantalla = currentState.pantalla as? PantallaMisHijos.Detalle ?: return
        if (currentState.vinculacionCon(pantalla.jugadorId)?.aprobada != true) {
            volverALista()
            sendEvent(MisHijosEvent.Mensaje("Tu acceso a este jugador fue revocado por la academia."))
        }
    }

    // ---- Solicitud --------------------------------------------------------------------------

    fun onModo(modo: ModoSolicitud) = setState { copy(formulario = formulario.copy(modo = modo, seleccionado = null, errorCodigo = null)) }

    fun onCodigo(texto: String) = setState {
        copy(formulario = formulario.copy(codigo = texto.uppercase().take(8), errorCodigo = null, seleccionado = null))
    }

    fun buscarCodigo() {
        val estado = currentState
        val perfil = estado.perfil ?: return
        val formulario = estado.formulario
        ReglasVinculacion.validarCodigo(formulario.codigo)?.let { error ->
            setState { copy(formulario = formulario.copy(errorCodigo = error)) }
            return
        }
        setState { copy(formulario = formulario.copy(buscando = true)) }
        viewModelScope.launch {
            try {
                val encontrado = vinculacionRepository.buscarPorCodigo(perfil.academiaId, formulario.codigo)
                setState {
                    copy(
                        formulario = this.formulario.copy(
                            buscando = false,
                            seleccionado = encontrado,
                            errorCodigo = if (encontrado == null) "No encontramos un jugador con ese código" else null,
                        ),
                    )
                }
            } catch (e: Exception) {
                setState { copy(formulario = this.formulario.copy(buscando = false)) }
                sendEvent(MisHijosEvent.Mensaje(describirErrorFirestore(e)))
            }
        }
    }

    fun onEquipo(equipoId: String) = setState { copy(formulario = formulario.copy(equipoId = equipoId, seleccionado = null)) }

    fun onJugadorElegido(jugador: DirectorioJugador) = setState { copy(formulario = formulario.copy(seleccionado = jugador)) }

    fun enviarSolicitud() {
        val estado = currentState
        val perfil = estado.perfil ?: return
        val jugador = estado.formulario.seleccionado ?: return
        if (estado.formulario.enviando) return

        // Criterio 4 y solicitudes repetidas.
        ReglasVinculacion.problemaParaSolicitar(jugador, estado.vinculacionCon(jugador.jugadorId))?.let { problema ->
            sendEvent(MisHijosEvent.Mensaje(problema.mensaje))
            return
        }

        setState { copy(formulario = formulario.copy(enviando = true)) }
        viewModelScope.launch {
            try {
                val confirmado = vinculacionRepository.solicitar(
                    Vinculacion(
                        academiaId = perfil.academiaId,
                        padreUid = perfil.uid,
                        padreNombre = perfil.nombre,
                        jugadorId = jugador.jugadorId,
                        jugadorNombre = jugador.nombre,
                        equipoId = jugador.equipoId,
                        equipoNombre = jugador.equipoNombre,
                    ),
                )
                setState { copy(pantalla = PantallaMisHijos.Lista, formulario = FormularioSolicitud()) }
                val texto = "Solicitud enviada. La academia la revisará"
                sendEvent(MisHijosEvent.Mensaje(if (confirmado) "$texto." else "$texto cuando vuelva la conexión."))
            } catch (e: Exception) {
                setState { copy(formulario = formulario.copy(enviando = false)) }
                sendEvent(MisHijosEvent.Mensaje(describirErrorFirestore(e)))
            }
        }
    }
}
