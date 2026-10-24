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
import com.esan.sportpro.domain.jugadores.EstadoVinculacion
import com.esan.sportpro.domain.jugadores.Jugador
import com.esan.sportpro.domain.jugadores.JugadorRepository
import com.esan.sportpro.domain.jugadores.ReglasVinculacion
import com.esan.sportpro.domain.jugadores.ValidacionJugador
import com.esan.sportpro.domain.jugadores.Vinculacion
import com.esan.sportpro.domain.jugadores.VinculacionRepository
import com.esan.sportpro.navigation.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Pantalla visible dentro del módulo Jugadores (navegación interna por estado). */
sealed interface PantallaJugadores {
    data object Lista : PantallaJugadores
    data class Detalle(val jugadorId: String) : PantallaJugadores
    data object Formulario : PantallaJugadores

    /** US-007: bandeja de solicitudes de vinculación de apoderados. */
    data object Solicitudes : PantallaJugadores
}

/** Diálogo de rechazo: el motivo es obligatorio (10 a 200 caracteres). */
data class DialogoRechazo(
    val vinculacion: Vinculacion,
    val motivo: String = "",
    val error: String? = null,
)

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
    /** US-007: solicitudes de vinculación de la academia, en cualquier estado. */
    val vinculaciones: List<Vinculacion> = emptyList(),
    val mostrarHistorialSolicitudes: Boolean = false,
    val dialogoRechazo: DialogoRechazo? = null,
    val revocando: Vinculacion? = null,
    val procesandoSolicitud: String? = null,
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

    val esAdmin: Boolean get() = perfil?.rol == UserRole.ADMINISTRADOR

    val esPadre: Boolean get() = perfil?.rol == UserRole.PADRE_DE_FAMILIA

    /** Solicitudes pendientes, de la más antigua a la más reciente (criterio 3). */
    val solicitudesPendientes: List<Vinculacion> get() = ReglasVinculacion.pendientes(vinculaciones)

    /** Solicitudes ya resueltas, de la más reciente a la más antigua. */
    val solicitudesResueltas: List<Vinculacion>
        get() = vinculaciones.filter { it.estadoVinculacion != EstadoVinculacion.PENDIENTE }
            .sortedByDescending { it.resueltaEn?.time ?: it.solicitadaEn?.time ?: 0L }

    /** Apoderados con vínculo aprobado de un jugador. */
    fun apoderadosDe(jugadorId: String): List<Vinculacion> =
        vinculaciones.filter { it.jugadorId == jugadorId && it.aprobada }

    /** Historial de asistencia a entrenamientos del jugador (US-009 a US-013). */
    fun asistenciaDe(jugador: Jugador): ResumenAsistencia =
        CalculoAsistencia.resumen(jugador.id, entrenamientos.filter { it.equipoId == jugador.equipoId })
}

sealed interface JugadoresEvent {
    data class Mensaje(val texto: String) : JugadoresEvent
}

/**
 * US-006/US-007: perfil del jugador (posición, datos físicos, contacto y contacto de emergencia) y,
 * para el personal, la bandeja de solicitudes de vinculación de apoderados.
 */
@HiltViewModel
class JugadoresViewModel @Inject constructor(
    private val sesionRepository: SesionRepository,
    private val jugadorRepository: JugadorRepository,
    private val equipoRepository: EquipoRepository,
    private val entrenamientoRepository: EntrenamientoRepository,
    private val vinculacionRepository: VinculacionRepository,
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
            launch {
                vinculacionRepository.observarSolicitudes(perfil.academiaId)
                    .catch { /* sin solicitudes el módulo sigue funcionando */ }
                    .collect { lista -> setState { copy(vinculaciones = lista) } }
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
        val original = formulario.id?.let { estado.jugador(it) }
        // US-007: todo jugador tiene un código único de 6 caracteres para la vinculación.
        val codigo = original?.codigoVinculacion?.takeIf { it.isNotBlank() }
            ?: ReglasVinculacion.generarCodigo(estado.jugadores.map { it.codigoVinculacion }.toSet())
        val jugador = formulario.datos.aJugador(
            id = formulario.id.orEmpty(),
            academiaId = perfil.academiaId,
            equipoNombre = equipo?.nombre.orEmpty(),
            creadoPor = original?.creadoPor ?: perfil.uid,
        ).copy(
            codigoVinculacion = codigo,
            activo = original?.activo ?: true,
            fotoUrl = original?.fotoUrl.orEmpty(),
        )

        setState { copy(formulario = formulario.copy(guardando = true)) }
        viewModelScope.launch {
            try {
                val confirmado = jugadorRepository.guardar(jugador, equipoEtiqueta = equipo?.etiqueta ?: jugador.equipoNombre)
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
                val confirmado = jugadorRepository.cambiarEstado(jugador, !jugador.activo)
                val texto = if (jugador.activo) "Jugador desactivado" else "Jugador activado"
                sendEvent(JugadoresEvent.Mensaje(if (confirmado) texto else "$texto. Se sincronizará cuando vuelva la conexión."))
            } catch (e: Exception) {
                sendEvent(JugadoresEvent.Mensaje(describirErrorFirestore(e)))
            }
        }
    }

    // ---- US-007: vinculación con apoderados ---------------------------------------------------

    /** Para jugadores registrados antes de US-007, que todavía no tienen código. */
    fun generarCodigo(jugador: Jugador) {
        if (!currentState.tieneAcceso || jugador.codigoVinculacion.isNotBlank()) return
        val codigo = ReglasVinculacion.generarCodigo(currentState.jugadores.map { it.codigoVinculacion }.toSet())
        viewModelScope.launch {
            try {
                val confirmado = vinculacionRepository.asignarCodigo(jugador, codigo)
                val texto = "Código generado: $codigo"
                sendEvent(JugadoresEvent.Mensaje(if (confirmado) texto else "$texto. Se sincronizará cuando vuelva la conexión."))
            } catch (e: Exception) {
                sendEvent(JugadoresEvent.Mensaje(describirErrorFirestore(e)))
            }
        }
    }

    fun abrirSolicitudes() = setState { copy(pantalla = PantallaJugadores.Solicitudes, mostrarHistorialSolicitudes = false) }

    fun onMostrarHistorialSolicitudes(valor: Boolean) = setState { copy(mostrarHistorialSolicitudes = valor) }

    fun aprobar(vinculacion: Vinculacion) {
        val estado = currentState
        val perfil = estado.perfil ?: return
        if (!estado.tieneAcceso || estado.procesandoSolicitud != null) return
        // Criterio 4: máximo dos apoderados aprobados por jugador.
        if (estado.apoderadosDe(vinculacion.jugadorId).size >= ReglasVinculacion.MAX_APODERADOS) {
            sendEvent(JugadoresEvent.Mensaje("El jugador ya tiene el máximo de apoderados vinculados"))
            return
        }
        procesar(vinculacion, "Vinculación aprobada: ${vinculacion.padreNombre} ahora ve a ${vinculacion.jugadorNombre}") {
            vinculacionRepository.aprobar(vinculacion, perfil.uid, perfil.nombre)
        }
    }

    fun abrirRechazo(vinculacion: Vinculacion) = setState { copy(dialogoRechazo = DialogoRechazo(vinculacion)) }

    fun onMotivoRechazo(motivo: String) = setState { copy(dialogoRechazo = dialogoRechazo?.copy(motivo = motivo, error = null)) }

    fun cerrarRechazo() = setState { copy(dialogoRechazo = null) }

    fun confirmarRechazo() {
        val dialogo = currentState.dialogoRechazo ?: return
        val perfil = currentState.perfil ?: return
        ReglasVinculacion.validarMotivoRechazo(dialogo.motivo)?.let { error ->
            setState { copy(dialogoRechazo = dialogo.copy(error = error)) }
            return
        }
        setState { copy(dialogoRechazo = null) }
        procesar(dialogo.vinculacion, "Solicitud rechazada") {
            vinculacionRepository.rechazar(dialogo.vinculacion, dialogo.motivo, perfil.uid, perfil.nombre)
        }
    }

    /** Criterio 8: solo el administrador revoca una vinculación existente. */
    fun pedirRevocar(vinculacion: Vinculacion) {
        if (!currentState.esAdmin) return
        setState { copy(revocando = vinculacion) }
    }

    fun cancelarRevocar() = setState { copy(revocando = null) }

    fun confirmarRevocar() {
        val vinculacion = currentState.revocando ?: return
        val perfil = currentState.perfil ?: return
        if (!currentState.esAdmin) return
        setState { copy(revocando = null) }
        procesar(vinculacion, "Vinculación revocada: ${vinculacion.padreNombre} ya no ve a ${vinculacion.jugadorNombre}") {
            vinculacionRepository.revocar(vinculacion, perfil.uid, perfil.nombre)
        }
    }

    private fun procesar(vinculacion: Vinculacion, texto: String, accion: suspend () -> Boolean) {
        setState { copy(procesandoSolicitud = vinculacion.id) }
        viewModelScope.launch {
            try {
                val confirmado = accion()
                sendEvent(JugadoresEvent.Mensaje(if (confirmado) texto else "$texto. Se sincronizará cuando vuelva la conexión."))
            } catch (e: Exception) {
                sendEvent(JugadoresEvent.Mensaje(describirErrorFirestore(e)))
            } finally {
                setState { copy(procesandoSolicitud = null) }
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
