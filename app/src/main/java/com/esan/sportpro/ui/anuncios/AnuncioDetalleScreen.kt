package com.esan.sportpro.ui.anuncios

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.anuncios.Anuncio
import java.time.Duration
import java.time.Instant

/** US-027, criterios 6 y 8 — Frame 128_Anuncio_Detalle. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnuncioDetalleScreen(
    anuncio: Anuncio,
    esAutor: Boolean,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnunciosViewModel = hiltViewModel(),
) {
    var mostrarDialogoEditar by remember { mutableStateOf(false) }
    var tituloEdicion by remember(anuncio.id) { mutableStateOf(anuncio.titulo) }
    var mensajeEdicion by remember(anuncio.id) { mutableStateOf(anuncio.mensaje) }

    LaunchedEffect(anuncio.id) { viewModel.marcarLeido(anuncio.id) }

    val dentroDeVentanaDeEdicion = anuncio.fechaPublicacion?.let {
        !anuncio.archivado && Duration.between(it.toInstant(), Instant.now()).toMinutes() <= 15
    } ?: false

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Anuncio") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                PrioridadAnuncioChip(prioridad = anuncio.prioridad)
                if (anuncio.editado) {
                    Text("Editado", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                text = anuncio.titulo,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                text = "Para: ${anuncio.destinatarios.etiqueta} · De: ${anuncio.autorNombre}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = anuncio.mensaje,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 16.dp),
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            if (esAutor) {
                Text(
                    text = "${anuncio.lecturas.size} de ${anuncio.destinatariosTotalEstimado.coerceAtLeast(anuncio.lecturas.size)} han leído este anuncio",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(modifier = Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (dentroDeVentanaDeEdicion) {
                        OutlinedButton(onClick = { mostrarDialogoEditar = true }) { Text("Editar") }
                    }
                    if (!anuncio.archivado) {
                        OutlinedButton(onClick = { viewModel.archivar(anuncio.id) }) { Text("Archivar") }
                    }
                }
            }
        }
    }

    if (mostrarDialogoEditar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoEditar = false },
            title = { Text("Editar anuncio") },
            text = {
                Column {
                    OutlinedTextField(
                        value = tituloEdicion,
                        onValueChange = { tituloEdicion = it.take(80) },
                        label = { Text("Título") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = mensajeEdicion,
                        onValueChange = { mensajeEdicion = it.take(500) },
                        label = { Text("Mensaje") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.editar(anuncio, tituloEdicion, mensajeEdicion) { mostrarDialogoEditar = false }
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoEditar = false }) { Text("Cancelar") }
            },
        )
    }
}
