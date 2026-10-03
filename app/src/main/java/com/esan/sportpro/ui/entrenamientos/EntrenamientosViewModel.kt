package com.esan.sportpro.ui.entrenamientos

import androidx.lifecycle.viewModelScope
import com.esan.sportpro.core.viewmodel.BaseViewModel
import com.esan.sportpro.data.partido.describirErrorFirestore
import com.esan.sportpro.domain.academia.Equipo
import com.esan.sportpro.domain.academia.EquipoRepository
import com.esan.sportpro.domain.cuentas.PerfilActual
import com.esan.sportpro.domain.cuentas.SesionRepository
import com.esan.sportpro.domain.entrenamientos.CampoEjercicio
import com.esan.sportpro.domain.entrenamientos.CampoEntrenamiento
import com.esan.sportpro.domain.entrenamientos.DatosEntrenamiento
import com.esan.sportpro.domain.entrenamientos.EjercicioBiblioteca
import com.esan.sportpro.domain.entrenamientos.Ejercicio
import com.esan.sportpro.domain.entrenamientos.Entrenamiento
import com.esan.sportpro.domain.entrenamientos.EntrenamientoRepository
import com.esan.sportpro.domain.entrenamientos.EstadoAsistencia
import com.esan.sportpro.domain.entrenamientos.ValidacionEntrenamiento
import com.esan.sportpro.domain.jugadores.Jugador
import com.esan.sportpro.domain.jugadores.JugadorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Pantalla visible dentro del módulo Entrenamientos (navegación interna por estado). */
sealed interface PantallaEntrenamientos {
    data object Lista : PantallaEntrenamientos
    data class Detalle(val entrenamientoId: String) : PantallaEntrenamientos
    data object Formulario : PantallaEntrenamientos
    data class Asistencia(val entrenamientoId: String) : PantallaEntrenamientos
}

/** Formulario de planificación. [id] es `null` al crear una sesión. */
data class FormularioEntrenamiento(
    val id: String? = null,
    val datos: DatosEntrenamiento = DatosEntrenamiento(),
    val errores: Map<CampoEntrenamiento, String> = emptyMap(),
    val guardando: Boolean = false,
)

/** Diálogo para agregar un ejercicio nuevo a la sesión. */
data class FormularioEjercicio(
    val nombre: String = "",
    val descripcion: String = "",
    val duracion: String = "",
    val guardarEnBiblioteca: Boolean = false,
    val errores: Map<CampoEjercicio, String> = emptyMap(),
)

data class EntrenamientosUiState(
    val perfil: PerfilActual? = null,
    val entrenamientos: List<Entrenamiento> = emptyList(),
    val equipos: List<Equipo> = emptyList(),
    val jugadores: List<Jugador> = emptyList(),
    val biblioteca: List<EjercicioBiblioteca> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null,
    val filtroEquipoId: String? = null,
    val pantalla: PantallaEntrenamientos = PantallaEntrenamientos.Lista,
    val formulario: FormularioEntrenamiento = FormularioEntrenamiento(),
    val dialogoEjercicio: FormularioEjercicio? = null,
    val mostrarBiblioteca: Boolean = false,
    /** Asistencia que se está editando: `jugadorId -> nombre de EstadoAsistencia`. */
    val asistenciaEdicion: Map<String, String> = emptyMap(),
    val guardandoAsistencia: Boolean = false,
) {
    /** Administrador y entrenador planifican y toman asistencia; el jugador solo consulta. */
    val puedeGestionar: Boolean get() = perfil?.esStaff == true

    val equiposActivos: List<Equipo> get() = equipos.filter { it.activo }

    val entrenamientosVisibles: List<Entrenamiento>
        get() = entrenamientos.filter { filtroEquipoId == null || it.equipoId == filtroEquipoId }

    fun entrenamiento(id: String): Entrenamiento? = entrenamientos.firstOrNull { it.id == id }

    /** Jugadores activos del equipo del entrenamiento, para tomar asistencia. */
    fun plantelDe(entrenamiento: Entrenamiento): List<Jugador> =
        jugadores.filter { it.activo && it.equipoId == entrenamiento.equipoId }
}

sealed interface EntrenamientosEvent {
    data class Mensaje(val texto: String) : EntrenamientosEvent
}

/** US-009 a US-013: planificación de sesiones, biblioteca de ejercicios y asistencia. */
@HiltViewModel
class EntrenamientosViewModel @Inject constructor(
    private val sesionRepository: SesionRepository,
    private val entrenamientoRepository: EntrenamientoRepository,
    private val equipoRepository: EquipoRepository,
    private val jugadorRepository: JugadorRepository,
) : BaseViewModel<EntrenamientosUiState, EntrenamientosEvent>(EntrenamientosUiState()) {

    init {
        viewModelScope.launch {
            val perfil = sesionRepository.perfilActual()
            if (perfil == null) {
                setState { copy(cargando = false, error = "No se pudo leer tu perfil. Vuelve a iniciar sesión.") }
                return@launch
            }
            setState { copy(perfil = perfil) }
            launch {
                entrenamientoRepository.observarEntrenamientos(perfil.academiaId)
                    .catch { setState { copy(cargando = false, error = describirErrorFirestore(it)) } }
                    .collect { lista -> setState { copy(entrenamientos = lista, cargando = false, error = null) } }
            }
            launch {
                equipoRepository.observarEquipos(perfil.academiaId)
                    .catch { /* sin equipos solo se pierde el filtro */ }
                    .collect { lista -> setState { copy(equipos = lista) } }
            }
            if (perfil.esStaff) {
                launch {
                    entrenamientoRepository.observarBiblioteca(perfil.academiaId)
                        .catch { /* la biblioteca es opcional */ }
                        .collect { lista -> setState { copy(biblioteca = lista) } }
                }
                launch {
                    // Los jugadores solo se leen para tomar asistencia; las reglas lo limitan al staff.
                    jugadorRepository.observarJugadores(perfil.academiaId)
                        .catch { /* sin plantel no se puede tomar asistencia */ }
                        .collect { lista -> setState { copy(jugadores = lista) } }
                }
            }
        }
    }

    fun onFiltroEquipo(equipoId: String?) = setState { copy(filtroEquipoId = equipoId) }

    fun abrirDetalle(id: String) = setState { copy(pantalla = PantallaEntrenamientos.Detalle(id)) }

    fun volverALista() = setState { copy(pantalla = PantallaEntrenamientos.Lista) }

    // ---- Planificación -------------------------------------------------------------------

    fun nuevoEntrenamiento() {
        if (!currentState.puedeGestionar) return
        val equipoInicial = currentState.filtroEquipoId.orEmpty()
        setState {
            copy(
                formulario = FormularioEntrenamiento(datos = DatosEntrenamiento(equipoId = equipoInicial)),
                pantalla = PantallaEntrenamientos.Formulario,
            )
        }
    }

    fun editarEntrenamiento(entrenamiento: Entrenamiento) {
        if (!currentState.puedeGestionar) return
        setState {
            copy(
                formulario = FormularioEntrenamiento(
                    id = entrenamiento.id,
                    datos = DatosEntrenamiento(
                        equipoId = entrenamiento.equipoId,
                        fecha = entrenamiento.fecha,
                        hora = entrenamiento.hora,
                        lugar = entrenamiento.lugar,
                        objetivo = entrenamiento.objetivo,
                        ejercicios = entrenamiento.ejercicios,
                    ),
                ),
                pantalla = PantallaEntrenamientos.Formulario,
            )
        }
    }

    fun cancelarFormulario() {
        val id = currentState.formulario.id
        setState {
            copy(pantalla = if (id != null) PantallaEntrenamientos.Detalle(id) else PantallaEntrenamientos.Lista)
        }
    }

    /** Aplica un cambio sobre los datos del formulario y limpia el error de [campo]. */
    fun onCampoChanged(campo: CampoEntrenamiento, cambio: DatosEntrenamiento.() -> DatosEntrenamiento) = setState {
        copy(formulario = formulario.copy(datos = formulario.datos.cambio(), errores = formulario.errores - campo))
    }

    fun quitarEjercicio(indice: Int) = onCampoChanged(CampoEntrenamiento.EJERCICIOS) {
        copy(ejercicios = ejercicios.filterIndexed { i, _ -> i != indice })
    }

    // ---- Ejercicios ----------------------------------------------------------------------

    fun abrirDialogoEjercicio() = setState { copy(dialogoEjercicio = FormularioEjercicio()) }

    fun cerrarDialogoEjercicio() = setState { copy(dialogoEjercicio = null) }

    fun onEjercicioChanged(cambio: FormularioEjercicio.() -> FormularioEjercicio) =
        setState { copy(dialogoEjercicio = dialogoEjercicio?.cambio()) }

    fun confirmarEjercicio() {
        val dialogo = currentState.dialogoEjercicio ?: return
        val perfil = currentState.perfil ?: return
        val errores = ValidacionEntrenamiento.validarEjercicio(dialogo.nombre, dialogo.descripcion, dialogo.duracion)
        if (errores.isNotEmpty()) {
            setState { copy(dialogoEjercicio = dialogo.copy(errores = errores)) }
            return
        }
        val duracion = dialogo.duracion.trim().toInt()
        val ejercicio = Ejercicio(dialogo.nombre.trim(), dialogo.descripcion.trim(), duracion)
        onCampoChanged(CampoEntrenamiento.EJERCICIOS) { copy(ejercicios = ejercicios + ejercicio) }
        setState { copy(dialogoEjercicio = null) }

        if (dialogo.guardarEnBiblioteca) {
            viewModelScope.launch {
                try {
                    entrenamientoRepository.guardarEnBiblioteca(
                        EjercicioBiblioteca(
                            academiaId = perfil.academiaId,
                            nombre = ejercicio.nombre,
                            descripcion = ejercicio.descripcion,
                            duracionMin = ejercicio.duracionMin,
                        ),
                    )
                } catch (e: Exception) {
                    sendEvent(EntrenamientosEvent.Mensaje(describirErrorFirestore(e)))
                }
            }
        }
    }

    fun abrirBiblioteca() = setState { copy(mostrarBiblioteca = true) }

    fun cerrarBiblioteca() = setState { copy(mostrarBiblioteca = false) }

    fun agregarDeBiblioteca(item: EjercicioBiblioteca) {
        val ejercicio = Ejercicio(item.nombre, item.descripcion, item.duracionMin)
        onCampoChanged(CampoEntrenamiento.EJERCICIOS) { copy(ejercicios = ejercicios + ejercicio) }
        setState { copy(mostrarBiblioteca = false) }
    }

    fun guardar() {
        val estado = currentState
        val perfil = estado.perfil ?: return
        val formulario = estado.formulario
        if (!estado.puedeGestionar || formulario.guardando) return

        val errores = ValidacionEntrenamiento.validar(
            datos = formulario.datos,
            permitirPasado = formulario.id != null,
        )
        if (errores.isNotEmpty()) {
            setState { copy(formulario = formulario.copy(errores = errores)) }
            return
        }

        val datos = formulario.datos
        val equipo = estado.equipos.firstOrNull { it.id == datos.equipoId }
        val entrenamiento = Entrenamiento(
            id = formulario.id.orEmpty(),
            academiaId = perfil.academiaId,
            equipoId = datos.equipoId,
            equipoNombre = equipo?.etiqueta.orEmpty(),
            fecha = datos.fecha,
            hora = datos.hora,
            lugar = datos.lugar,
            objetivo = datos.objetivo,
            ejercicios = datos.ejercicios,
            creadoPor = perfil.uid,
        )

        setState { copy(formulario = formulario.copy(guardando = true)) }
        viewModelScope.launch {
            try {
                val confirmado = entrenamientoRepository.guardar(entrenamiento)
                setState {
                    copy(
                        formulario = FormularioEntrenamiento(),
                        pantalla = if (formulario.id != null) PantallaEntrenamientos.Detalle(formulario.id) else PantallaEntrenamientos.Lista,
                    )
                }
                val texto = "Entrenamiento guardado"
                sendEvent(EntrenamientosEvent.Mensaje(if (confirmado) texto else "$texto. Se sincronizará cuando vuelva la conexión."))
            } catch (e: Exception) {
                setState { copy(formulario = formulario.copy(guardando = false)) }
                sendEvent(EntrenamientosEvent.Mensaje(describirErrorFirestore(e)))
            }
        }
    }

    // ---- Asistencia ----------------------------------------------------------------------

    fun abrirAsistencia(entrenamiento: Entrenamiento) {
        if (!currentState.puedeGestionar) return
        val fecha = ValidacionEntrenamiento.parsearFecha(entrenamiento.fecha)
        if (fecha != null && fecha.isAfter(LocalDate.now())) {
            sendEvent(EntrenamientosEvent.Mensaje("La asistencia se registra el día del entrenamiento o después."))
            return
        }
        val plantel = currentState.plantelDe(entrenamiento)
        // Parte de lo ya registrado; los jugadores sin registro arrancan como presentes.
        val inicial = plantel.associate { jugador ->
            jugador.id to (entrenamiento.asistencia[jugador.id] ?: EstadoAsistencia.PRESENTE.name)
        }
        setState {
            copy(asistenciaEdicion = inicial, pantalla = PantallaEntrenamientos.Asistencia(entrenamiento.id))
        }
    }

    fun onEstadoAsistencia(jugadorId: String, estado: EstadoAsistencia) =
        setState { copy(asistenciaEdicion = asistenciaEdicion + (jugadorId to estado.name)) }

    fun marcarTodosPresentes() =
        setState { copy(asistenciaEdicion = asistenciaEdicion.mapValues { EstadoAsistencia.PRESENTE.name }) }

    fun volverADetalle(entrenamientoId: String) =
        setState { copy(pantalla = PantallaEntrenamientos.Detalle(entrenamientoId)) }

    fun guardarAsistencia(entrenamientoId: String) {
        val estado = currentState
        if (!estado.puedeGestionar || estado.guardandoAsistencia) return
        val entrenamiento = estado.entrenamiento(entrenamientoId) ?: return
        setState { copy(guardandoAsistencia = true) }
        viewModelScope.launch {
            try {
                val confirmado = entrenamientoRepository.registrarAsistencia(entrenamiento, estado.asistenciaEdicion)
                setState {
                    copy(guardandoAsistencia = false, pantalla = PantallaEntrenamientos.Detalle(entrenamientoId))
                }
                val texto = "Asistencia registrada"
                sendEvent(EntrenamientosEvent.Mensaje(if (confirmado) texto else "$texto. Se sincronizará cuando vuelva la conexión."))
            } catch (e: Exception) {
                setState { copy(guardandoAsistencia = false) }
                sendEvent(EntrenamientosEvent.Mensaje(describirErrorFirestore(e)))
            }
        }
    }
}
