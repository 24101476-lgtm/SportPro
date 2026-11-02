package com.esan.sportpro.ui.entrenamientos

import androidx.lifecycle.viewModelScope
import com.esan.sportpro.core.viewmodel.BaseViewModel
import com.esan.sportpro.data.partido.describirErrorFirestore
import com.esan.sportpro.domain.academia.Equipo
import com.esan.sportpro.domain.academia.EquipoRepository
import com.esan.sportpro.domain.cuentas.PerfilActual
import com.esan.sportpro.domain.cuentas.SesionRepository
import com.esan.sportpro.domain.entrenamientos.BibliotecaEjercicios
import com.esan.sportpro.domain.entrenamientos.CampoEjercicio
import com.esan.sportpro.domain.entrenamientos.CampoEntrenamiento
import com.esan.sportpro.domain.entrenamientos.DatosEntrenamiento
import com.esan.sportpro.domain.entrenamientos.EjercicioBiblioteca
import com.esan.sportpro.domain.entrenamientos.Ejercicio
import com.esan.sportpro.domain.entrenamientos.Entrenamiento
import com.esan.sportpro.domain.entrenamientos.EntrenamientoRepository
import com.esan.sportpro.domain.entrenamientos.FiltroBiblioteca
import com.esan.sportpro.domain.entrenamientos.ReglasPlanificacion
import com.esan.sportpro.domain.entrenamientos.ValidacionEntrenamiento
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

    /** Biblioteca reutilizable de ejercicios (US-009), con su propio ViewModel. */
    data object Biblioteca : PantallaEntrenamientos
}

/** Formulario de planificación. [id] es `null` al crear una sesión. */
data class FormularioEntrenamiento(
    val id: String? = null,
    val datos: DatosEntrenamiento = DatosEntrenamiento(),
    val errores: Map<CampoEntrenamiento, String> = emptyMap(),
    val guardando: Boolean = false,
)

/** US-010, criterio 9: cancelar con motivo. */
data class DialogoCancelacion(val entrenamiento: Entrenamiento, val motivo: String = "", val error: String? = null)

/** US-010, criterio 8: duplicar la sesión hacia otra fecha. */
data class DialogoDuplicar(val entrenamiento: Entrenamiento, val fecha: String = "", val error: String? = null)

/**
 * Diálogo para agregar a la sesión un ejercicio escrito solo para ella. Los ejercicios
 * reutilizables se crean en la Biblioteca (US-009), que pide la ficha completa.
 */
data class FormularioEjercicio(
    val nombre: String = "",
    val descripcion: String = "",
    val duracion: String = "",
    val errores: Map<CampoEjercicio, String> = emptyMap(),
)

data class EntrenamientosUiState(
    val perfil: PerfilActual? = null,
    val entrenamientos: List<Entrenamiento> = emptyList(),
    val equipos: List<Equipo> = emptyList(),
    val biblioteca: List<EjercicioBiblioteca> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null,
    val filtroEquipoId: String? = null,
    val pantalla: PantallaEntrenamientos = PantallaEntrenamientos.Lista,
    val formulario: FormularioEntrenamiento = FormularioEntrenamiento(),
    val dialogoEjercicio: FormularioEjercicio? = null,
    val mostrarBiblioteca: Boolean = false,
    /** Texto del buscador del selector "Desde biblioteca". */
    val busquedaBiblioteca: String = "",
    /** US-010, criterio 6: sesión del mismo equipo que se cruza con el horario elegido. */
    val sesionSolapada: Entrenamiento? = null,
    val dialogoCancelacion: DialogoCancelacion? = null,
    val dialogoDuplicar: DialogoDuplicar? = null,
    val procesando: Boolean = false,
) {
    /** US-010, criterio 4: advertencia que no bloquea el guardado. */
    val avisoExceso: String? get() = ReglasPlanificacion.avisoExceso(formulario.datos)

    /** Administrador y entrenador planifican y toman asistencia; el jugador solo consulta. */
    val puedeGestionar: Boolean get() = perfil?.esStaff == true

    val equiposActivos: List<Equipo> get() = equipos.filter { it.activo }

    /** Ejercicios activos de la biblioteca que coinciden con el buscador del selector. */
    val bibliotecaDisponible: List<EjercicioBiblioteca>
        get() = BibliotecaEjercicios.buscar(biblioteca, FiltroBiblioteca(texto = busquedaBiblioteca))

    val entrenamientosVisibles: List<Entrenamiento>
        get() = entrenamientos.filter { filtroEquipoId == null || it.equipoId == filtroEquipoId }

    fun entrenamiento(id: String): Entrenamiento? = entrenamientos.firstOrNull { it.id == id }
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
            }
        }
    }

    fun onFiltroEquipo(equipoId: String?) = setState { copy(filtroEquipoId = equipoId) }

    fun abrirDetalle(id: String) = setState { copy(pantalla = PantallaEntrenamientos.Detalle(id)) }

    fun volverALista() = setState { copy(pantalla = PantallaEntrenamientos.Lista) }

    fun abrirGestionBiblioteca() {
        if (!currentState.puedeGestionar) return
        setState { copy(pantalla = PantallaEntrenamientos.Biblioteca) }
    }

    // ---- Planificación -------------------------------------------------------------------

    fun nuevoEntrenamiento() {
        if (!currentState.puedeGestionar) return
        val equipoInicial = currentState.filtroEquipoId.orEmpty()
        setState {
            copy(
                formulario = FormularioEntrenamiento(
                    datos = DatosEntrenamiento(equipoId = equipoInicial, duracion = DURACION_POR_DEFECTO.toString()),
                ),
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
                        duracion = entrenamiento.duracionSesionMin.toString(),
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
        val errores = ValidacionEntrenamiento.validarEjercicio(dialogo.nombre, dialogo.descripcion, dialogo.duracion)
        if (errores.isNotEmpty()) {
            setState { copy(dialogoEjercicio = dialogo.copy(errores = errores)) }
            return
        }
        val duracion = dialogo.duracion.trim().toInt()
        val ejercicio = Ejercicio(dialogo.nombre.trim(), dialogo.descripcion.trim(), duracion)
        onCampoChanged(CampoEntrenamiento.EJERCICIOS) { copy(ejercicios = ejercicios + ejercicio) }
        setState { copy(dialogoEjercicio = null) }
    }

    fun abrirBiblioteca() = setState { copy(mostrarBiblioteca = true, busquedaBiblioteca = "") }

    fun cerrarBiblioteca() = setState { copy(mostrarBiblioteca = false) }

    fun onBusquedaBiblioteca(texto: String) = setState { copy(busquedaBiblioteca = texto) }

    /** Agrega una copia del ejercicio a la sesión, enlazada a la biblioteca por su id. */
    fun agregarDeBiblioteca(item: EjercicioBiblioteca) {
        val ejercicio = item.aEjercicioDeSesion()
        onCampoChanged(CampoEntrenamiento.EJERCICIOS) { copy(ejercicios = ejercicios + ejercicio) }
        setState { copy(mostrarBiblioteca = false) }
    }

    /**
     * @param ignorarSolapamiento `true` cuando el usuario eligió "Continuar" en el diálogo de
     * sesión solapada (criterio 6).
     */
    fun guardar(ignorarSolapamiento: Boolean = false) {
        val estado = currentState
        val perfil = estado.perfil ?: return
        val formulario = estado.formulario
        if (!estado.puedeGestionar || formulario.guardando) return

        val errores = ValidacionEntrenamiento.validar(
            datos = formulario.datos,
            permitirPasado = formulario.id != null,
        )
        if (errores.isNotEmpty()) {
            setState { copy(formulario = formulario.copy(errores = errores), sesionSolapada = null) }
            return
        }

        if (!ignorarSolapamiento) {
            val solapada = ReglasPlanificacion.sesionSolapada(
                equipoId = formulario.datos.equipoId,
                fecha = formulario.datos.fecha,
                hora = formulario.datos.hora,
                duracionMin = formulario.datos.duracion.trim().toInt(),
                sesiones = estado.entrenamientos,
                idEditando = formulario.id,
            )
            if (solapada != null) {
                setState { copy(sesionSolapada = solapada) }
                return
            }
        }
        setState { copy(sesionSolapada = null) }

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
            duracionPlanificadaMin = datos.duracion.trim().toInt(),
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
                val texto = if (formulario.id == null) "Sesión programada" else "Entrenamiento guardado"
                sendEvent(EntrenamientosEvent.Mensaje(if (confirmado) texto else "$texto. Se sincronizará cuando vuelva la conexión."))
            } catch (e: Exception) {
                setState { copy(formulario = formulario.copy(guardando = false)) }
                sendEvent(EntrenamientosEvent.Mensaje(describirErrorFirestore(e)))
            }
        }
    }

    fun cerrarAvisoSolapamiento() = setState { copy(sesionSolapada = null) }

    fun continuarPeseASolapamiento() = guardar(ignorarSolapamiento = true)

    // ---- Ejercicios de la sesión (criterio 3) --------------------------------------------------

    /** Reordenar por arrastre: mueve el ejercicio de [desde] a [hasta]. */
    fun moverEjercicio(desde: Int, hasta: Int) = onCampoChanged(CampoEntrenamiento.EJERCICIOS) {
        copy(ejercicios = ReglasPlanificacion.mover(ejercicios, desde, hasta))
    }

    /** Ajusta la duración del ejercicio solo en esta sesión; la biblioteca no cambia. */
    fun ajustarDuracionEjercicio(indice: Int, delta: Int) = onCampoChanged(CampoEntrenamiento.EJERCICIOS) {
        val actual = ejercicios.getOrNull(indice)?.duracionMin ?: return@onCampoChanged this
        val nueva = (actual + delta).coerceIn(1, ValidacionEntrenamiento.DURACION_EJERCICIO_MAX)
        copy(ejercicios = ReglasPlanificacion.ajustarDuracion(ejercicios, indice, nueva))
    }

    // ---- Cancelar (criterio 9) -----------------------------------------------------------------

    fun abrirCancelacion(entrenamiento: Entrenamiento) {
        if (!currentState.puedeGestionar) return
        setState { copy(dialogoCancelacion = DialogoCancelacion(entrenamiento)) }
    }

    fun onMotivoCancelacion(motivo: String) =
        setState { copy(dialogoCancelacion = dialogoCancelacion?.copy(motivo = motivo, error = null)) }

    fun cerrarCancelacion() = setState { copy(dialogoCancelacion = null) }

    fun confirmarCancelacion() {
        val dialogo = currentState.dialogoCancelacion ?: return
        val perfil = currentState.perfil ?: return
        if (currentState.procesando) return
        ReglasPlanificacion.validarCancelacion(dialogo.entrenamiento, dialogo.motivo)?.let { error ->
            setState { copy(dialogoCancelacion = dialogo.copy(error = error)) }
            return
        }
        setState { copy(procesando = true) }
        viewModelScope.launch {
            try {
                val confirmado = entrenamientoRepository.cancelar(dialogo.entrenamiento, dialogo.motivo, perfil.uid, perfil.nombre)
                setState { copy(dialogoCancelacion = null, procesando = false) }
                val texto = "Sesión cancelada. Queda en el historial con su motivo"
                sendEvent(EntrenamientosEvent.Mensaje(if (confirmado) "$texto." else "$texto. Se sincronizará cuando vuelva la conexión."))
            } catch (e: Exception) {
                setState { copy(procesando = false) }
                sendEvent(EntrenamientosEvent.Mensaje(describirErrorFirestore(e)))
            }
        }
    }

    // ---- Duplicar (criterio 8) -----------------------------------------------------------------

    fun abrirDuplicar(entrenamiento: Entrenamiento) {
        if (!currentState.puedeGestionar) return
        setState { copy(dialogoDuplicar = DialogoDuplicar(entrenamiento)) }
    }

    fun onFechaDuplicado(fecha: String) = setState { copy(dialogoDuplicar = dialogoDuplicar?.copy(fecha = fecha, error = null)) }

    fun cerrarDuplicar() = setState { copy(dialogoDuplicar = null) }

    fun confirmarDuplicar() {
        val dialogo = currentState.dialogoDuplicar ?: return
        val perfil = currentState.perfil ?: return
        if (currentState.procesando) return
        ReglasPlanificacion.validarFechaDuplicado(dialogo.fecha)?.let { error ->
            setState { copy(dialogoDuplicar = dialogo.copy(error = error)) }
            return
        }
        val copia = ReglasPlanificacion.duplicar(dialogo.entrenamiento, dialogo.fecha, perfil.uid)
        setState { copy(procesando = true) }
        viewModelScope.launch {
            try {
                val confirmado = entrenamientoRepository.guardar(copia)
                setState { copy(dialogoDuplicar = null, procesando = false, pantalla = PantallaEntrenamientos.Lista) }
                val texto = "Sesión duplicada para el ${dialogo.fecha}"
                sendEvent(EntrenamientosEvent.Mensaje(if (confirmado) texto else "$texto. Se sincronizará cuando vuelva la conexión."))
            } catch (e: Exception) {
                setState { copy(procesando = false) }
                sendEvent(EntrenamientosEvent.Mensaje(describirErrorFirestore(e)))
            }
        }
    }

    // ---- Asistencia (US-011): la pantalla tiene su propio ViewModel ---------------------------

    fun abrirAsistencia(entrenamiento: Entrenamiento) {
        if (!currentState.puedeGestionar) return
        val fecha = ValidacionEntrenamiento.parsearFecha(entrenamiento.fecha)
        if (!entrenamiento.realizada && fecha != null && fecha.isAfter(LocalDate.now())) {
            sendEvent(EntrenamientosEvent.Mensaje("La asistencia se registra el día del entrenamiento o después."))
            return
        }
        setState { copy(pantalla = PantallaEntrenamientos.Asistencia(entrenamiento.id)) }
    }

    fun volverADetalle(entrenamientoId: String) =
        setState { copy(pantalla = PantallaEntrenamientos.Detalle(entrenamientoId)) }

    private companion object {
        const val DURACION_POR_DEFECTO = 90
    }
}
