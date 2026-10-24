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
import com.esan.sportpro.domain.jugadores.CampoJugador
import com.esan.sportpro.domain.jugadores.DatosJugador
import com.esan.sportpro.domain.jugadores.Jugador
import com.esan.sportpro.domain.jugadores.JugadorRepository
import com.esan.sportpro.domain.jugadores.ValidacionJugador
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Pantalla visible dentro del módulo Jugadores (navegación interna por estado). */
sealed interface PantallaJugadores {
    data object Lista : PantallaJugadores
    data class Detalle(val jugadorId: String) : PantallaJugadores
    data object Formulario : PantallaJugadores
}

/** Formulario de alta/edición. [id] es `null` al crear un jugador. */
data class FormularioJugador(
    val id: String? = null,
    val datos: DatosJugador = DatosJugador(),
    val errores: Map<CampoJugador, String> = emptyMap(),
    val guardando: Boolean = false,
)

data class JugadoresUiState(
    val perfil: PerfilActual? = null,
    val jugadores: List<Jugador> = emptyList(),
    val equipos: List<Equipo> = emptyList(),
    val entrenamientos: List<Entrenamiento> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null,
    val filtroEquipoId: String? = null,
    val mostrarInactivos: Boolean = false,
    val pantalla: PantallaJugadores = PantallaJugadores.Lista,
    val formulario: FormularioJugador = FormularioJugador(),
) {
    /**
     * Privacidad: el perfil incluye datos físicos y de contacto de menores, así que solo lo ven
     * administrador y entrenador. Jugador y padre de familia no entran a este módulo.
     */
    val tieneAcceso: Boolean get() = perfil?.esStaff == true

    val equiposActivos: List<Equipo> get() = equipos.filter { it.activo }

    val jugadoresVisibles: List<Jugador>
        get() = jugadores
            .filter { mostrarInactivos || it.activo }
            .filter { filtroEquipoId == null || it.equipoId == filtroEquipoId }

    fun jugador(id: String): Jugador? = jugadores.firstOrNull { it.id == id }

    /** Historial de asistencia a entrenamientos del jugador (US-009 a US-013). */
    fun asistenciaDe(jugador: Jugador): ResumenAsistencia =
        CalculoAsistencia.resumen(jugador.id, entrenamientos.filter { it.equipoId == jugador.equipoId })
}

sealed interface JugadoresEvent {
    data class Mensaje(val texto: String) : JugadoresEvent
}

/** US-006/US-007: perfil del jugador (posición, datos físicos, contacto y contacto de emergencia). */
@HiltViewModel
class JugadoresViewModel @Inject constructor(
    private val sesionRepository: SesionRepository,
    private val jugadorRepository: JugadorRepository,
    private val equipoRepository: EquipoRepository,
    private val entrenamientoRepository: EntrenamientoRepository,
) : BaseViewModel<JugadoresUiState, JugadoresEvent>(JugadoresUiState()) {

    init {
        viewModelScope.launch {
            val perfil = sesionRepository.perfilActual()
            if (perfil == null) {
                setState { copy(cargando = false, error = "No se pudo leer tu perfil. Vuelve a iniciar sesión.") }
                return@launch
            }
            setState { copy(perfil = perfil) }
            if (!perfil.esStaff) {
                setState { copy(cargando = false) }
                return@launch
            }
            launch {
                jugadorRepository.observarJugadores(perfil.academiaId)
                    .catch { setState { copy(cargando = false, error = describirErrorFirestore(it)) } }
                    .collect { lista -> setState { copy(jugadores = lista, cargando = false, error = null) } }
            }
            launch {
                equipoRepository.observarEquipos(perfil.academiaId)
                    .catch { /* sin equipos el módulo sigue mostrando los jugadores */ }
                    .collect { lista -> setState { copy(equipos = lista) } }
            }
            launch {
                entrenamientoRepository.observarEntrenamientos(perfil.academiaId)
                    .catch { /* el historial de asistencia es opcional */ }
                    .collect { lista -> setState { copy(entrenamientos = lista) } }
            }
        }
    }

    fun onFiltroEquipo(equipoId: String?) = setState { copy(filtroEquipoId = equipoId) }

    fun onMostrarInactivos(valor: Boolean) = setState { copy(mostrarInactivos = valor) }

    fun abrirDetalle(jugadorId: String) = setState { copy(pantalla = PantallaJugadores.Detalle(jugadorId)) }

    fun volverALista() = setState { copy(pantalla = PantallaJugadores.Lista) }

    fun nuevoJugador() {
        val equipoInicial = currentState.filtroEquipoId.orEmpty()
        setState {
            copy(
                formulario = FormularioJugador(datos = DatosJugador(equipoId = equipoInicial)),
                pantalla = PantallaJugadores.Formulario,
            )
        }
    }

    fun editarJugador(jugador: Jugador) {
        setState {
            copy(
                formulario = FormularioJugador(id = jugador.id, datos = jugador.aDatos()),
                pantalla = PantallaJugadores.Formulario,
            )
        }
    }

    /** Cancela el formulario y vuelve a donde estaba el usuario (detalle si editaba, lista si creaba). */
    fun cancelarFormulario() {
        val id = currentState.formulario.id
        setState {
            copy(pantalla = if (id != null) PantallaJugadores.Detalle(id) else PantallaJugadores.Lista)
        }
    }

    /** Aplica un cambio sobre los datos del formulario y limpia el error de [campo]. */
    fun onCampoChanged(campo: CampoJugador, cambio: DatosJugador.() -> DatosJugador) = setState {
        copy(formulario = formulario.copy(datos = formulario.datos.cambio(), errores = formulario.errores - campo))
    }

    fun guardar() {
        val estado = currentState
        val perfil = estado.perfil ?: return
        val formulario = estado.formulario
        if (!estado.tieneAcceso || formulario.guardando) return

        val errores = ValidacionJugador.validar(formulario.datos)
        if (errores.isNotEmpty()) {
            setState { copy(formulario = formulario.copy(errores = errores)) }
            return
        }

        val equipo = estado.equipos.firstOrNull { it.id == formulario.datos.equipoId }
        val jugador = formulario.datos.aJugador(
            id = formulario.id.orEmpty(),
            academiaId = perfil.academiaId,
            equipoNombre = equipo?.nombre.orEmpty(),
            creadoPor = perfil.uid,
        )

        setState { copy(formulario = formulario.copy(guardando = true)) }
        viewModelScope.launch {
            try {
                val confirmado = jugadorRepository.guardar(jugador)
                val texto = "Jugador guardado"
                setState {
                    copy(
                        formulario = FormularioJugador(),
                        pantalla = if (formulario.id != null) PantallaJugadores.Detalle(formulario.id) else PantallaJugadores.Lista,
                    )
                }
                sendEvent(JugadoresEvent.Mensaje(if (confirmado) texto else "$texto. Se sincronizará cuando vuelva la conexión."))
            } catch (e: Exception) {
                setState { copy(formulario = formulario.copy(guardando = false)) }
                sendEvent(JugadoresEvent.Mensaje(describirErrorFirestore(e)))
            }
        }
    }

    fun cambiarEstado(jugador: Jugador) {
        if (!currentState.tieneAcceso) return
        viewModelScope.launch {
            try {
                val confirmado = jugadorRepository.cambiarEstado(jugador.id, !jugador.activo)
                val texto = if (jugador.activo) "Jugador desactivado" else "Jugador activado"
                sendEvent(JugadoresEvent.Mensaje(if (confirmado) texto else "$texto. Se sincronizará cuando vuelva la conexión."))
            } catch (e: Exception) {
                sendEvent(JugadoresEvent.Mensaje(describirErrorFirestore(e)))
            }
        }
    }
}

private fun Jugador.aDatos() = DatosJugador(
    nombres = nombres,
    apellidos = apellidos,
    fechaNacimiento = fechaNacimiento,
    equipoId = equipoId,
    posicion = posicion,
    dorsal = dorsal?.toString().orEmpty(),
    estatura = estaturaCm?.toString().orEmpty(),
    peso = pesoKg?.toString().orEmpty(),
    telefono = telefono,
    correo = correo,
    emergenciaNombre = contactoEmergenciaNombre,
    emergenciaTelefono = contactoEmergenciaTelefono,
    emergenciaParentesco = contactoEmergenciaParentesco,
)

private fun DatosJugador.aJugador(
    id: String,
    academiaId: String,
    equipoNombre: String,
    creadoPor: String,
) = Jugador(
    id = id,
    academiaId = academiaId,
    equipoId = equipoId,
    equipoNombre = equipoNombre,
    nombres = nombres,
    apellidos = apellidos,
    fechaNacimiento = fechaNacimiento,
    posicion = posicion,
    dorsal = dorsal.trim().toIntOrNull(),
    estaturaCm = estatura.trim().toIntOrNull(),
    pesoKg = ValidacionJugador.parsearPeso(peso),
    telefono = telefono,
    correo = correo,
    contactoEmergenciaNombre = emergenciaNombre,
    contactoEmergenciaTelefono = emergenciaTelefono,
    contactoEmergenciaParentesco = emergenciaParentesco,
    creadoPor = creadoPor,
)
