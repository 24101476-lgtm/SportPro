package com.esan.sportpro.ui.comunidad

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.comunidad.Publicacion
import com.esan.sportpro.domain.comunidad.TipoReaccion
import com.google.firebase.auth.FirebaseAuth

/** US-028, criterio 4 — Frame 133_Publicacion_Detalle_Comentarios (el estado "En revisión" del frame 134 se ve en [PublicacionCard]). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicacionDetalleScreen(
    publicacion: Publicacion,
    onVolver: () -> Unit,
    onReportarPublicacion: (publicacionId: String, autorUid: String, esMenor: Boolean) -> Unit,
    onReportarComentario: (comentarioId: String, publicacionId: String, autorUid: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PublicacionesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var textoComentario by remember { mutableStateOf("") }
    val uidActual = FirebaseAuth.getInstance().currentUser?.uid
    val miReaccion = publicacion.reaccionesPorUsuario[uidActual]
        ?.let { nombre -> runCatching { TipoReaccion.valueOf(nombre) }.getOrNull() }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Publicación") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.cerrarPublicacion(); onVolver() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = textoComentario,
                    onValueChange = { textoComentario = it.take(300) },
                    label = { Text("Escribe un comentario") },
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = {
                        if (textoComentario.isNotBlank()) {
                            viewModel.comentar(textoComentario)
                            textoComentario = ""
                        }
                    },
                ) { Icon(Icons.Default.Send, contentDescription = "Enviar comentario") }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                PublicacionCard(
                    publicacion = publicacion,
                    uidActual = uidActual,
                    reaccionActual = miReaccion,
                    onAbrir = {},
                    onReaccionar = { tipo -> viewModel.reaccionar(publicacion.id, tipo) },
                    onReportar = { onReportarPublicacion(publicacion.id, publicacion.autorUid, publicacion.autorEsMenorDeEdad) },
                    onEliminar = { viewModel.eliminarPublicacion(publicacion.id); onVolver() },
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            item { Text("Comentarios", style = MaterialTheme.typography.titleSmall) }
            items(state.comentarios, key = { it.id }) { comentario ->
                ComentarioItem(
                    comentario = comentario,
                    onReportar = { onReportarComentario(comentario.id, publicacion.id, comentario.autorUid) },
                )
            }
        }
    }
}
