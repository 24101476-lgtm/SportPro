package com.esan.sportpro.ui.academia

import androidx.lifecycle.viewModelScope
import com.esan.sportpro.core.viewmodel.BaseViewModel
import com.esan.sportpro.data.partido.describirErrorFirestore
import com.esan.sportpro.domain.academia.CampoEquipo
import com.esan.sportpro.domain.academia.Equipo
import com.esan.sportpro.domain.academia.EquipoRepository
import com.esan.sportpro.domain.academia.ValidacionEquipo
import com.esan.sportpro.domain.cuentas.PerfilActual
import com.esan.sportpro.domain.cuentas.SesionRepository
import com.esan.sportpro.navigation.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Formulario de alta/edición de equipo. [id] es `null` al crear. */
data class FormularioEquipo(
    val id: String? = null,
    val nombre: String = "",
    val categoria: String = "",
    val entrenador: String = "",
    val errores: Map<CampoEquipo, String> = emptyMap(),
    val guardando: Boolean = false,
)

data class AcademiaUiState(
    val perfil: PerfilActual? = null,
    val equipos: List<Equipo> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null,
    val mostrarInactivos: Boolean = false,
    val formulario: FormularioEquipo? = null,
) {
    /** Solo el administrador crea y edita equipos; el entrenador los consulta. */
    val puedeEditar: Boolean get() = perfil?.rol == UserRole.ADMINISTRADOR

    val equiposVisibles: List<Equipo>
        get() = if (mostrarInactivos) equipos else equipos.filter { it.activo }
}

sealed interface AcademiaEvent {
    data class Mensaje(val texto: String) : AcademiaEvent
}

/** US-004/US-005: gestión de equipos de la academia por categoría. */
@HiltViewModel
class AcademiaViewModel @Inject constructor(
    private val sesionRepository: SesionRepository,
    private val equipoRepository: EquipoRepository,
) : BaseViewModel<AcademiaUiState, AcademiaEvent>(AcademiaUiState()) {

    init {
        viewModelScope.launch {
            val perfil = sesionRepository.perfilActual()
            if (perfil == null) {
                setState { copy(cargando = false, error = "No se pudo leer tu perfil. Vuelve a iniciar sesión.") }
                return@launch
            }
            setState { copy(perfil = perfil) }
            equipoRepository.observarEquipos(perfil.academiaId)
                .catch { setState { copy(cargando = false, error = describirErrorFirestore(it)) } }
                .collect { lista -> setState { copy(equipos = lista, cargando = false, error = null) } }
        }
    }

    fun onMostrarInactivos(valor: Boolean) = setState { copy(mostrarInactivos = valor) }

    fun nuevoEquipo() {
        if (!currentState.puedeEditar) return
        setState { copy(formulario = FormularioEquipo()) }
    }

    fun editarEquipo(equipo: Equipo) {
        if (!currentState.puedeEditar) return
        setState {
            copy(
                formulario = FormularioEquipo(
                    id = equipo.id,
                    nombre = equipo.nombre,
                    categoria = equipo.categoria,
                    entrenador = equipo.entrenadorNombre,
                ),
            )
        }
    }

    fun cerrarFormulario() = setState { copy(formulario = null) }

    fun onNombreChanged(valor: String) =
        editarFormulario { copy(nombre = valor, errores = errores - CampoEquipo.NOMBRE) }

    fun onCategoriaChanged(valor: String) =
        editarFormulario { copy(categoria = valor, errores = errores - CampoEquipo.CATEGORIA) }

    fun onEntrenadorChanged(valor: String) =
        editarFormulario { copy(entrenador = valor, errores = errores - CampoEquipo.ENTRENADOR) }

    fun guardar() {
        val estado = currentState
        val formulario = estado.formulario ?: return
        val perfil = estado.perfil ?: return
        if (!estado.puedeEditar || formulario.guardando) return

        val errores = ValidacionEquipo.validar(
            nombre = formulario.nombre,
            categoria = formulario.categoria,
            entrenadorNombre = formulario.entrenador,
            existentes = estado.equipos,
            idEditando = formulario.id,
        )
        if (errores.isNotEmpty()) {
            editarFormulario { copy(errores = errores) }
            return
        }

        editarFormulario { copy(guardando = true) }
        viewModelScope.launch {
            try {
                val confirmado = equipoRepository.guardar(
                    Equipo(
                        id = formulario.id.orEmpty(),
                        academiaId = perfil.academiaId,
                        nombre = formulario.nombre,
                        categoria = formulario.categoria,
                        entrenadorNombre = formulario.entrenador,
                        creadoPor = perfil.uid,
                    ),
                )
                setState { copy(formulario = null) }
                sendEvent(AcademiaEvent.Mensaje(mensajeGuardado(confirmado, "Equipo guardado")))
            } catch (e: Exception) {
                editarFormulario { copy(guardando = false) }
                sendEvent(AcademiaEvent.Mensaje(describirErrorFirestore(e)))
            }
        }
    }

    fun cambiarEstado(equipo: Equipo) {
        if (!currentState.puedeEditar) return
        viewModelScope.launch {
            try {
                val confirmado = equipoRepository.cambiarEstado(equipo.id, !equipo.activo)
                val texto = if (equipo.activo) "Equipo desactivado" else "Equipo activado"
                sendEvent(AcademiaEvent.Mensaje(mensajeGuardado(confirmado, texto)))
            } catch (e: Exception) {
                sendEvent(AcademiaEvent.Mensaje(describirErrorFirestore(e)))
            }
        }
    }

    private fun editarFormulario(cambio: FormularioEquipo.() -> FormularioEquipo) =
        setState { copy(formulario = formulario?.cambio()) }

    private fun mensajeGuardado(confirmado: Boolean, texto: String) =
        if (confirmado) texto else "$texto. Se sincronizará cuando vuelva la conexión."
}
