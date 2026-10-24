package com.esan.sportpro.ui.entrenamientos

import androidx.lifecycle.viewModelScope
import com.esan.sportpro.core.viewmodel.BaseViewModel
import com.esan.sportpro.data.partido.describirErrorFirestore
import com.esan.sportpro.domain.cuentas.PerfilActual
import com.esan.sportpro.domain.cuentas.SesionRepository
import com.esan.sportpro.domain.entrenamientos.BibliotecaEjercicios
import com.esan.sportpro.domain.entrenamientos.CampoBiblioteca
import com.esan.sportpro.domain.entrenamientos.CategoriaTecnica
import com.esan.sportpro.domain.entrenamientos.DatosEjercicioBiblioteca
import com.esan.sportpro.domain.entrenamientos.EjercicioBiblioteca
import com.esan.sportpro.domain.entrenamientos.Entrenamiento
import com.esan.sportpro.domain.entrenamientos.EntrenamientoRepository
import com.esan.sportpro.domain.entrenamientos.FiltroBiblioteca
import com.esan.sportpro.domain.entrenamientos.RangoDuracion
import com.esan.sportpro.domain.entrenamientos.ValidacionBiblioteca
import com.esan.sportpro.navigation.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Pantalla visible dentro de la biblioteca (navegación interna por estado). */
sealed interface PantallaBiblioteca {
    data object Lista : PantallaBiblioteca
    data class Detalle(val ejercicioId: String) : PantallaBiblioteca
    data object Formulario : PantallaBiblioteca
}

/** Formulario de alta/edición. [id] es `null` al crear un ejercicio. */
data class FormularioBiblioteca(
    val id: String? = null,
    val datos: DatosEjercicioBiblioteca = DatosEjercicioBiblioteca(),
    val errores: Map<CampoBiblioteca, String> = emptyMap(),
    /** URL de la imagen ya guardada en Cloud Storage (al editar). */
    val imagenActual: String = "",
    /** URI local de una imagen recién elegida, todavía sin subir. */
    val imagenNueva: String? = null,
    val quitarImagen: Boolean = false,
    val guardando: Boolean = false,
) {
    /** Lo que se muestra como vista previa: la imagen nueva o la actual (si no se quitó). */
    val imagenVisible: String? get() = imagenNueva ?: imagenActual.takeIf { it.isNotBlank() && !quitarImagen }
}

/** Acción que espera confirmación del usuario. */
sealed interface ConfirmacionBiblioteca {
    val ejercicio: EjercicioBiblioteca

    /** El ejercicio nunca se usó en una sesión realizada: se puede borrar. */
    data class Eliminar(override val ejercicio: EjercicioBiblioteca) : ConfirmacionBiblioteca

    /** Se usó en sesiones realizadas (o el usuario eligió desactivar): solo se desactiva. */
    data class Desactivar(
        override val ejercicio: EjercicioBiblioteca,
        val porUsoEnSesiones: Boolean,
    ) : ConfirmacionBiblioteca
}

data class BibliotecaUiState(
    val perfil: PerfilActual? = null,
    val ejercicios: List<EjercicioBiblioteca> = emptyList(),
    val entrenamientos: List<Entrenamiento> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null,
    val filtro: FiltroBiblioteca = FiltroBiblioteca(),
    val mostrarDesactivados: Boolean = false,
    val pantalla: PantallaBiblioteca = PantallaBiblioteca.Lista,
    val formulario: FormularioBiblioteca = FormularioBiblioteca(),
    val confirmacion: ConfirmacionBiblioteca? = null,
    val procesando: Boolean = false,
) {
    val puedeGestionar: Boolean get() = perfil?.esStaff == true
    val esAdmin: Boolean get() = perfil?.rol == UserRole.ADMINISTRADOR

    /**
     * Resultado del buscador: solo activos (criterio 8). Con "Ver desactivados" se listan los
     * desactivados que el usuario puede reactivar (los suyos, o todos si es administrador).
     */
    val visibles: List<EjercicioBiblioteca>
        get() = if (mostrarDesactivados) {
            BibliotecaEjercicios.buscar(ejercicios.filter { puedeEditar(it) }, filtro, desactivados = true)
        } else {
            BibliotecaEjercicios.buscar(ejercicios, filtro)
        }

    val hayActivos: Boolean get() = ejercicios.any { it.activo }

    fun ejercicio(id: String): EjercicioBiblioteca? = ejercicios.firstOrNull { it.id == id }

    fun puedeEditar(ejercicio: EjercicioBiblioteca): Boolean {
        val uid = perfil?.uid ?: return false
        return puedeGestionar && BibliotecaEjercicios.puedeEditar(ejercicio, uid, esAdmin)
    }

    fun usadoEnSesionRealizada(ejercicio: EjercicioBiblioteca): Boolean =
        BibliotecaEjercicios.usadoEnSesionRealizada(ejercicio, entrenamientos)
}

sealed interface BibliotecaEvent {
    data class Mensaje(val texto: String) : BibliotecaEvent
}

/** US-009: biblioteca reutilizable de ejercicios del club. */
@HiltViewModel
class BibliotecaEjerciciosViewModel @Inject constructor(
    private val sesionRepository: SesionRepository,
    private val entrenamientoRepository: EntrenamientoRepository,
) : BaseViewModel<BibliotecaUiState, BibliotecaEvent>(BibliotecaUiState()) {

    init {
        viewModelScope.launch {
            val perfil = sesionRepository.perfilActual()
            if (perfil == null) {
                setState { copy(cargando = false, error = "No se pudo leer tu perfil. Vuelve a iniciar sesión.") }
                return@launch
            }
            if (!perfil.esStaff) {
                setState { copy(perfil = perfil, cargando = false, error = "La biblioteca es solo para entrenadores y administradores.") }
                return@launch
            }
            setState { copy(perfil = perfil) }
            launch {
                entrenamientoRepository.observarBiblioteca(perfil.academiaId)
                    .catch { setState { copy(cargando = false, error = describirErrorFirestore(it)) } }
                    .collect { lista -> setState { copy(ejercicios = lista, cargando = false, error = null) } }
            }
            launch {
                // Solo para saber qué ejercicios se usaron en sesiones realizadas (criterio 8).
                entrenamientoRepository.observarEntrenamientos(perfil.academiaId)
                    .catch { /* sin sesiones, ningún ejercicio cuenta como usado */ }
                    .collect { lista -> setState { copy(entrenamientos = lista) } }
            }
        }
    }

    // ---- Buscador ------------------------------------------------------------------------

    fun onTextoBusqueda(texto: String) = setState { copy(filtro = filtro.copy(texto = texto)) }

    fun onFiltroCategoria(categoria: CategoriaTecnica) = setState {
        copy(filtro = filtro.copy(categoria = if (filtro.categoria == categoria) null else categoria))
    }

    fun onFiltroDuracion(rango: RangoDuracion) = setState {
        copy(filtro = filtro.copy(duracion = if (filtro.duracion == rango) null else rango))
    }

    fun limpiarFiltros() = setState { copy(filtro = FiltroBiblioteca()) }

    fun onMostrarDesactivados(valor: Boolean) = setState { copy(mostrarDesactivados = valor) }

    // ---- Navegación interna --------------------------------------------------------------

    fun abrirDetalle(id: String) = setState { copy(pantalla = PantallaBiblioteca.Detalle(id)) }

    fun volverALista() = setState { copy(pantalla = PantallaBiblioteca.Lista) }

    /** @return `true` si consumió el "atrás" (navegación interna); `false` si hay que salir. */
    fun atras(): Boolean = when (currentState.pantalla) {
        PantallaBiblioteca.Lista -> false
        is PantallaBiblioteca.Detalle -> { volverALista(); true }
        PantallaBiblioteca.Formulario -> { cancelarFormulario(); true }
    }

    // ---- Formulario ----------------------------------------------------------------------

    fun nuevoEjercicio() {
        if (!currentState.puedeGestionar) return
        setState { copy(formulario = FormularioBiblioteca(), pantalla = PantallaBiblioteca.Formulario) }
    }

    fun editarEjercicio(ejercicio: EjercicioBiblioteca) {
        if (!currentState.puedeEditar(ejercicio)) return
        setState {
            copy(
                formulario = FormularioBiblioteca(
                    id = ejercicio.id,
                    datos = DatosEjercicioBiblioteca(
                        nombre = ejercicio.nombre,
                        objetivo = ejercicio.objetivo,
                        categoria = ejercicio.categoriaTecnica,
                        duracion = ejercicio.duracionMin.takeIf { it > 0 }?.toString().orEmpty(),
                        minJugadores = ejercicio.minJugadores.takeIf { it > 0 }?.toString().orEmpty(),
                        materiales = ejercicio.materiales,
                        descripcion = ejercicio.descripcion,
                    ),
                    imagenActual = ejercicio.imagenUrl,
                ),
                pantalla = PantallaBiblioteca.Formulario,
            )
        }
    }

    fun cancelarFormulario() {
        val id = currentState.formulario.id
        setState { copy(pantalla = if (id != null) PantallaBiblioteca.Detalle(id) else PantallaBiblioteca.Lista) }
    }

    /** Aplica un cambio sobre los datos del formulario y limpia el error de [campo]. */
    fun onCampoChanged(campo: CampoBiblioteca, cambio: DatosEjercicioBiblioteca.() -> DatosEjercicioBiblioteca) =
        setState {
            copy(formulario = formulario.copy(datos = formulario.datos.cambio(), errores = formulario.errores - campo))
        }

    fun onImagenElegida(uri: String) =
        setState { copy(formulario = formulario.copy(imagenNueva = uri, quitarImagen = false)) }

    fun quitarImagen() = setState {
        copy(formulario = formulario.copy(imagenNueva = null, quitarImagen = formulario.imagenActual.isNotBlank()))
    }

    fun guardar() {
        val estado = currentState
        val perfil = estado.perfil ?: return
        val formulario = estado.formulario
        if (!estado.puedeGestionar || formulario.guardando) return

        val errores = ValidacionBiblioteca.validar(formulario.datos, estado.ejercicios, formulario.id)
        if (errores.isNotEmpty()) {
            setState { copy(formulario = formulario.copy(errores = errores)) }
            return
        }

        val original = formulario.id?.let { estado.ejercicio(it) }
        if (original != null && !estado.puedeEditar(original)) return

        val datos = formulario.datos
        val ejercicio = EjercicioBiblioteca(
            id = formulario.id.orEmpty(),
            academiaId = perfil.academiaId,
            nombre = datos.nombre.trim(),
            objetivo = datos.objetivo.trim(),
            categoria = datos.categoria?.name.orEmpty(),
            duracionMin = datos.duracion.trim().toInt(),
            minJugadores = datos.minJugadores.trim().toInt(),
            materiales = datos.materiales.trim(),
            descripcion = datos.descripcion.trim(),
            imagenUrl = formulario.imagenActual,
            creadoPor = original?.creadoPor ?: perfil.uid,
        )

        setState { copy(formulario = formulario.copy(guardando = true)) }
        viewModelScope.launch {
            try {
                val resultado = entrenamientoRepository.guardarEnBiblioteca(
                    ejercicio = ejercicio,
                    imagenLocal = formulario.imagenNueva,
                    quitarImagen = formulario.quitarImagen,
                )
                setState {
                    copy(
                        formulario = FormularioBiblioteca(),
                        pantalla = if (formulario.id != null) PantallaBiblioteca.Detalle(formulario.id) else PantallaBiblioteca.Lista,
                    )
                }
                val texto = buildString {
                    append("Ejercicio guardado")
                    if (resultado.imagenFallida) append(", pero no se pudo subir la imagen. Inténtalo de nuevo con conexión")
                    if (!resultado.confirmado) append(". Se sincronizará cuando vuelva la conexión")
                    append(".")
                }
                sendEvent(BibliotecaEvent.Mensaje(texto))
            } catch (e: Exception) {
                setState { copy(formulario = formulario.copy(guardando = false)) }
                sendEvent(BibliotecaEvent.Mensaje(describirErrorFirestore(e)))
            }
        }
    }

    // ---- Desactivar / eliminar -----------------------------------------------------------

    /** Si el ejercicio ya se usó en una sesión realizada no se puede borrar: se ofrece desactivarlo. */
    fun solicitarEliminar(ejercicio: EjercicioBiblioteca) {
        if (!currentState.puedeEditar(ejercicio)) return
        val confirmacion = if (currentState.usadoEnSesionRealizada(ejercicio)) {
            ConfirmacionBiblioteca.Desactivar(ejercicio, porUsoEnSesiones = true)
        } else {
            ConfirmacionBiblioteca.Eliminar(ejercicio)
        }
        setState { copy(confirmacion = confirmacion) }
    }

    fun solicitarDesactivar(ejercicio: EjercicioBiblioteca) {
        if (!currentState.puedeEditar(ejercicio)) return
        setState { copy(confirmacion = ConfirmacionBiblioteca.Desactivar(ejercicio, porUsoEnSesiones = false)) }
    }

    fun cancelarConfirmacion() = setState { copy(confirmacion = null) }

    fun confirmar() {
        val confirmacion = currentState.confirmacion ?: return
        if (currentState.procesando) return
        setState { copy(procesando = true) }
        viewModelScope.launch {
            try {
                val (confirmado, texto) = when (confirmacion) {
                    is ConfirmacionBiblioteca.Eliminar ->
                        entrenamientoRepository.eliminarEjercicio(confirmacion.ejercicio) to "Ejercicio eliminado"
                    is ConfirmacionBiblioteca.Desactivar ->
                        entrenamientoRepository.cambiarEstadoEjercicio(confirmacion.ejercicio.id, activo = false) to
                            "Ejercicio desactivado. Ya no aparece en el buscador"
                }
                setState { copy(confirmacion = null, procesando = false, pantalla = PantallaBiblioteca.Lista) }
                sendEvent(BibliotecaEvent.Mensaje(if (confirmado) "$texto." else "$texto. Se sincronizará cuando vuelva la conexión."))
            } catch (e: Exception) {
                setState { copy(confirmacion = null, procesando = false) }
                sendEvent(BibliotecaEvent.Mensaje(describirErrorFirestore(e)))
            }
        }
    }

    fun reactivar(ejercicio: EjercicioBiblioteca) {
        if (!currentState.puedeEditar(ejercicio)) return
        viewModelScope.launch {
            try {
                val confirmado = entrenamientoRepository.cambiarEstadoEjercicio(ejercicio.id, activo = true)
                val texto = "Ejercicio reactivado"
                sendEvent(BibliotecaEvent.Mensaje(if (confirmado) "$texto." else "$texto. Se sincronizará cuando vuelva la conexión."))
            } catch (e: Exception) {
                sendEvent(BibliotecaEvent.Mensaje(describirErrorFirestore(e)))
            }
        }
    }
}
