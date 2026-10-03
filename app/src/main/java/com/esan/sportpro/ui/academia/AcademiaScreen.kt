package com.esan.sportpro.ui.academia

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.academia.CampoEquipo
import com.esan.sportpro.domain.academia.CategoriasEquipo
import com.esan.sportpro.domain.academia.Equipo
import com.esan.sportpro.ui.common.EmptyStateMessage

/**
 * Módulo Academia (US-004/US-005): equipos organizados por categoría. El administrador crea,
 * edita y desactiva equipos; el entrenador los consulta.
 */
@Composable
fun AcademiaScreen(
    modifier: Modifier = Modifier,
    viewModel: AcademiaViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { evento ->
            if (evento is AcademiaEvent.Mensaje) snackbarHostState.showSnackbar(evento.texto)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (state.puedeEditar) {
                FloatingActionButton(onClick = viewModel::nuevoEquipo) {
                    Icon(Icons.Default.Add, contentDescription = "Nuevo equipo")
                }
            }
        },
    ) { padding ->
        when {
            state.cargando -> Column(
                modifier = Modifier.padding(padding).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator() }

            state.error != null -> EmptyStateMessage(
                mensaje = state.error.orEmpty(),
                modifier = Modifier.padding(padding),
            )

            else -> Column(modifier = Modifier.padding(padding)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Equipos de la academia", style = MaterialTheme.typography.titleMedium)
                    if (state.puedeEditar) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Inactivos", style = MaterialTheme.typography.labelMedium)
                            Switch(
                                checked = state.mostrarInactivos,
                                onCheckedChange = viewModel::onMostrarInactivos,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
                if (state.equiposVisibles.isEmpty()) {
                    EmptyStateMessage(
                        mensaje = if (state.puedeEditar) {
                            "Aún no hay equipos. Crea el primero con el botón +."
                        } else {
                            "Tu academia todavía no tiene equipos registrados."
                        },
                        icono = Icons.Default.Groups2,
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.equiposVisibles, key = { it.id }) { equipo ->
                            EquipoCard(
                                equipo = equipo,
                                puedeEditar = state.puedeEditar,
                                onEditar = { viewModel.editarEquipo(equipo) },
                                onCambiarEstado = { viewModel.cambiarEstado(equipo) },
                            )
                        }
                    }
                }
            }
        }
    }

    state.formulario?.let { formulario ->
        FormularioEquipoDialog(
            formulario = formulario,
            onNombre = viewModel::onNombreChanged,
            onCategoria = viewModel::onCategoriaChanged,
            onEntrenador = viewModel::onEntrenadorChanged,
            onGuardar = viewModel::guardar,
            onCancelar = viewModel::cerrarFormulario,
        )
    }
}

@Composable
private fun EquipoCard(
    equipo: Equipo,
    puedeEditar: Boolean,
    onEditar: () -> Unit,
    onCambiarEstado: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(equipo.nombre, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Categoría ${equipo.categoria}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (equipo.entrenadorNombre.isNotBlank()) {
                    Text("DT: ${equipo.entrenadorNombre}", style = MaterialTheme.typography.bodySmall)
                }
                if (!equipo.activo) {
                    Text(
                        "Inactivo",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (puedeEditar) {
                IconButton(onClick = onEditar) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar equipo")
                }
                TextButton(onClick = onCambiarEstado) {
                    Text(if (equipo.activo) "Desactivar" else "Activar")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormularioEquipoDialog(
    formulario: FormularioEquipo,
    onNombre: (String) -> Unit,
    onCategoria: (String) -> Unit,
    onEntrenador: (String) -> Unit,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!formulario.guardando) onCancelar() },
        title = { Text(if (formulario.id == null) "Nuevo equipo" else "Editar equipo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = formulario.nombre,
                    onValueChange = onNombre,
                    label = { Text("Nombre del equipo") },
                    singleLine = true,
                    isError = CampoEquipo.NOMBRE in formulario.errores,
                    supportingText = { formulario.errores[CampoEquipo.NOMBRE]?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Categoría", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoriasEquipo.todas.forEach { categoria ->
                        FilterChip(
                            selected = formulario.categoria == categoria,
                            onClick = { onCategoria(categoria) },
                            label = { Text(categoria) },
                        )
                    }
                }
                formulario.errores[CampoEquipo.CATEGORIA]?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(
                    value = formulario.entrenador,
                    onValueChange = onEntrenador,
                    label = { Text("Entrenador (opcional)") },
                    singleLine = true,
                    isError = CampoEquipo.ENTRENADOR in formulario.errores,
                    supportingText = { formulario.errores[CampoEquipo.ENTRENADOR]?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onGuardar, enabled = !formulario.guardando) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onCancelar, enabled = !formulario.guardando) { Text("Cancelar") }
        },
    )
}
