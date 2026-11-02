package com.esan.sportpro.ui.partido

import androidx.lifecycle.viewModelScope
import com.esan.sportpro.core.viewmodel.BaseViewModel
import com.esan.sportpro.domain.partido.AmbitoEvento
import com.esan.sportpro.domain.partido.CatalogoBase
import com.esan.sportpro.domain.partido.PartidoRepository
import com.esan.sportpro.domain.partido.TipoEvento
import com.esan.sportpro.domain.partido.TiposEvento
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CatalogoEventosUiState(
    val cargando: Boolean = true,
    val eventos: List<TipoEvento> = emptyList(),
    val rol: String? = null,
    val error: String? = null,
    val mensaje: String? = null,
)

@HiltViewModel
class CatalogoEventosViewModel @Inject constructor(
    private val repo: PartidoRepository,
) : BaseViewModel<CatalogoEventosUiState, Unit>(
    CatalogoEventosUiState()
) {

    init {
        observarCatalogo()
        observarRol()
    }

    fun mensajeMostrado() {
        setState {
            copy(mensaje = null)
        }
    }

    fun crearEvento(
        nombre: String,
        ambito: AmbitoEvento,
        camposObligatorios: List<String>,
        camposOpcionales: List<String>,
        sumaMarcador: Boolean,
        activo: Boolean,
        icono: String,
        color: String,
    ): Boolean {

        if (uiState.value.rol != "ADMINISTRADOR") {
            setState {
                copy(
                    error = "Solo el administrador puede crear eventos."
                )
            }
            return false
        }

        validarNombre(nombre, idActual = null)?.let { error ->
            setState { copy(error = error) }
            return false
        }

        val siguienteOrden =
            (uiState.value.eventos.maxOfOrNull { it.orden } ?: 0) + 1

        val nuevo = TipoEvento(
            id = "",
            nombre = nombre.trim(),
            ambito = ambito,
            camposObligatorios = camposObligatorios,
            camposOpcionales = camposOpcionales,
            sumaMarcador = sumaMarcador,
            orden = siguienteOrden,
            activo = activo,
            version = 1,
            icono = icono.ifBlank { "sports_soccer" },
            color = color.ifBlank { "#2E7D32" },
        )

        repo.crearTipoEvento(nuevo)

        setState {
            copy(
                mensaje = "Evento creado correctamente",
                error = null,
            )
        }

        return true
    }

    fun actualizarEvento(
        original: TipoEvento,
        nombre: String,
        ambito: AmbitoEvento,
        camposObligatorios: List<String>,
        camposOpcionales: List<String>,
        sumaMarcador: Boolean,
        activo: Boolean,
        icono: String,
        color: String,
    ): Boolean {

        if (uiState.value.rol != "ADMINISTRADOR") {
            setState {
                copy(
                    error = "Solo el administrador puede modificar eventos."
                )
            }
            return false
        }

        validarNombre(nombre, idActual = original.id)?.let { error ->
            setState { copy(error = error) }
            return false
        }

        // Los eventos base sostienen la lógica del partido (reloj, marcador, cambios, tarjetas):
        // se les pueden agregar campos, pero no quitar los obligatorios de origen ni cambiar si
        // suman al marcador. Los de control del tiempo, además, no se desactivan.
        val base = CatalogoBase.porId(original.id)
        if (base != null && !activo && original.id in EVENTOS_CONTROL_TIEMPO) {
            setState { copy(error = "\"${base.nombre}\" controla el reloj del partido y no se puede desactivar.") }
            return false
        }
        val obligatorios = if (base != null) (base.camposObligatorios + camposObligatorios).distinct() else camposObligatorios

        val actualizado = original.copy(
            nombre = nombre.trim(),
            ambito = base?.ambito ?: ambito,
            camposObligatorios = obligatorios,
            camposOpcionales = camposOpcionales.filterNot { it in obligatorios },
            sumaMarcador = base?.sumaMarcador ?: sumaMarcador,
            activo = activo,
            icono = icono.ifBlank { "sports_soccer" },
            color = color.ifBlank { "#2E7D32" },
        )

        repo.actualizarTipoEvento(actualizado)

        setState {
            copy(
                mensaje = "Evento actualizado correctamente",
                error = null,
            )
        }

        return true
    }

    /** US-013: nombre de 3 a 30 caracteres y único dentro del catálogo (sin distinguir mayúsculas). */
    private fun validarNombre(nombre: String, idActual: String?): String? {
        val limpio = nombre.trim()
        if (limpio.length !in NOMBRE_MIN..NOMBRE_MAX) {
            return "El nombre debe tener entre $NOMBRE_MIN y $NOMBRE_MAX caracteres."
        }
        val repetido = uiState.value.eventos.any {
            it.id != idActual && it.nombre.trim().equals(limpio, ignoreCase = true)
        }
        return if (repetido) "Ya existe un evento con ese nombre." else null
    }

    private companion object {
        const val NOMBRE_MIN = 3
        const val NOMBRE_MAX = 30
        val EVENTOS_CONTROL_TIEMPO = setOf(
            TiposEvento.INICIO_PARTIDO,
            TiposEvento.FIN_PRIMER_TIEMPO,
            TiposEvento.INICIO_SEGUNDO_TIEMPO,
            TiposEvento.FIN_PARTIDO,
        )
    }

    private fun observarCatalogo() {
        viewModelScope.launch {
            repo.observarCatalogoCompleto()
                .catch { e ->
                    setState {
                        copy(
                            cargando = false,
                            error = e.message
                                ?: "No se pudo cargar el catálogo",
                        )
                    }
                }
                .collect { lista ->
                    setState {
                        copy(
                            cargando = false,
                            eventos = lista,
                            error = null,
                        )
                    }
                }
        }
    }

    private fun observarRol() {
        viewModelScope.launch {
            repo.observarRolActual()
                .collect { rol ->
                    setState {
                        copy(rol = rol)
                    }
                }
        }
    }
}