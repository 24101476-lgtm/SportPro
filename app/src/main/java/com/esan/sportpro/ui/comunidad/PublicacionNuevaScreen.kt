package com.esan.sportpro.ui.comunidad

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.esan.sportpro.domain.comunidad.VisibilidadPublicacion
import kotlinx.coroutines.flow.collectLatest

/** US-028, criterios 1 y 2 — Frame 132_Publicacion_Nueva (incluye la alerta del frame 135). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicacionNuevaScreen(
    onPublicada: () -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PublicacionesViewModel = hiltViewModel(),
) {
    var texto by remember { mutableStateOf("") }
    var imagenes by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var visibilidad by remember { mutableStateOf(VisibilidadPublicacion.SOLO_ACADEMIA) }
    var mostrarAlertaTelefono by remember { mutableStateOf(false) }
    var errorMensaje by remember { mutableStateOf<String?>(null) }

    val textoValido = texto.length in 1..1000

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Nueva publicación") },
                navigationIcon = {
                    IconButton(onClick = onCancelar) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Cancelar")
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = texto,
                onValueChange = { texto = it.take(1000) },
                label = { Text("¿Qué quieres compartir?") },
                supportingText = { Text("${texto.length}/1000") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )

            SelectorImagenes(imagenes = imagenes, onImagenesChange = { imagenes = it })

            Column {
                Text("Visibilidad", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    VisibilidadPublicacion.entries.forEach { opcion ->
                        FilterChip(
                            selected = visibilidad == opcion,
                            onClick = { visibilidad = opcion },
                            label = { Text(opcion.etiqueta) },
                        )
                    }
                }
            }

            if (errorMensaje != null) {
                Text(errorMensaje.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = { viewModel.publicar(texto, imagenes, visibilidad) },
                enabled = textoValido,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Publicar") }
        }
    }

    if (mostrarAlertaTelefono) {
        AlertaDatosContactoDialog(
            onConfirmar = {
                mostrarAlertaTelefono = false
                viewModel.publicar(texto, imagenes, visibilidad, confirmadoTelefono = true)
            },
            onCancelar = { mostrarAlertaTelefono = false },
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is PublicacionesEvent.RequiereConfirmacionTelefono -> mostrarAlertaTelefono = true
                is PublicacionesEvent.Publicada -> onPublicada()
                is PublicacionesEvent.Error -> errorMensaje = event.mensaje
                PublicacionesEvent.Reportada -> Unit
            }
        }
    }
}
