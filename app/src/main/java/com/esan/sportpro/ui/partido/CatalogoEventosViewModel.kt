package com.esan.sportpro.ui.partido

import androidx.lifecycle.viewModelScope
import com.esan.sportpro.core.viewmodel.BaseViewModel
import com.esan.sportpro.domain.partido.AmbitoEvento
import com.esan.sportpro.domain.partido.PartidoRepository
import com.esan.sportpro.domain.partido.TipoEvento
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

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

        if (nombre.isBlank()) {
            setState {
                copy(
                    error = "El nombre del evento es obligatorio."
                )
            }
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

        if (nombre.isBlank()) {
            setState {
                copy(
                    error = "El nombre del evento es obligatorio."
                )
            }
            return false
        }

        val actualizado = original.copy(
            nombre = nombre.trim(),
            ambito = ambito,
            camposObligatorios = camposObligatorios,
            camposOpcionales = camposOpcionales,
            sumaMarcador = sumaMarcador,
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