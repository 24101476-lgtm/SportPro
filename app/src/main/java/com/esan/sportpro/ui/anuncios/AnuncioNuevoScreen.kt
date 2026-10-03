package com.esan.sportpro.ui.anuncios

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.anuncios.PrioridadAnuncio
import com.esan.sportpro.domain.anuncios.TipoDestinatario
import kotlinx.coroutines.flow.collectLatest

/** US-027, criterios 1 y 2 — Frame 127_Anuncio_Nuevo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnuncioNuevoScreen(
    onPublicado: () -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnunciosViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val formulario = state.formulario
    var menuDestinatarioAbierto by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.limpiarFormulario() }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Nuevo anuncio") },
                navigationIcon = {
                    IconButton(onClick = onCancelar) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Cancelar")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { NotificacionSimuladaLegend() }

            item {
                OutlinedTextField(
                    value = formulario.titulo,
                    onValueChange = { viewModel.onFormularioChanged(formulario.copy(titulo = it.take(80))) },
                    label = { Text("Título") },
                    supportingText = { Text("${formulario.titulo.length}/80") },
                    isError = formulario.titulo.isNotEmpty() && !formulario.tituloValido,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                OutlinedTextField(
                    value = formulario.mensaje,
                    onValueChange = { viewModel.onFormularioChanged(formulario.copy(mensaje = it.take(500))) },
                    label = { Text("Mensaje") },
                    supportingText = { Text("${formulario.mensaje.length}/500") },
                    isError = formulario.mensaje.isNotEmpty() && !formulario.mensajeValido,
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                Column {
                    Text("Destinatarios", style = MaterialTheme.typography.labelLarge)
                    OutlinedButton(onClick = { menuDestinatarioAbierto = true }, modifier = Modifier.padding(top = 4.dp)) {
                        Text(formulario.destinatarios.etiqueta)
                    }
                    DropdownMenu(expanded = menuDestinatarioAbierto, onDismissRequest = { menuDestinatarioAbierto = false }) {
                        TipoDestinatario.entries.forEach { tipo ->
                            DropdownMenuItem(
                                text = { Text(tipo.etiqueta) },
                                onClick = {
                                    viewModel.onFormularioChanged(formulario.copy(destinatarios = tipo))
                                    menuDestinatarioAbierto = false
                                },
                            )
                        }
                    }
                }
            }

            item {
                Column {
                    Text("Prioridad", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PrioridadAnuncio.entries.forEach { prioridad ->
                            FilterChip(
                                selected = formulario.prioridad == prioridad,
                                onClick = { viewModel.onFormularioChanged(formulario.copy(prioridad = prioridad)) },
                                label = { Text(prioridad.etiqueta) },
                            )
                        }
                    }
                }
            }

            if (state.errorMessage != null) {
                item {
                    Text(
                        text = state.errorMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            item {
                Button(
                    onClick = viewModel::publicar,
                    enabled = formulario.formularioValido,
                    colors = ButtonDefaults.buttonColors(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Publicar anuncio") }
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            if (event is AnunciosEvent.AnuncioPublicado) onPublicado()
        }
    }
}
