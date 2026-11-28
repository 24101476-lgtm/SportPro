package com.esan.sportpro.ui.comunidad

import com.esan.sportpro.ui.common.BarraSuperiorSportPro
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding

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
            BarraSuperiorSportPro(
                title = { Text("Publicación") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.cerrarPublicacion(); onVolver() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
        bottomBar = {
            // Barra de comentario tipo chat: tu inicial, el campo en píldora y el botón lima.
            androidx.compose.material3.Surface(color = MaterialTheme.colorScheme.background) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AvatarAutor(state.autorNombre, tamano = 36.dp)
                    OutlinedTextField(
                        value = textoComentario,
                        onValueChange = { textoComentario = it.take(300) },
                        placeholder = { Text("Escribe un comentario…") },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                        maxLines = 4,
                        supportingText = if (textoComentario.length > 250) {
                            { Text("${textoComentario.length}/300") }
                        } else {
                            null
                        },
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    )
                    androidx.compose.material3.FilledIconButton(
                        onClick = {
                            if (textoComentario.isNotBlank()) {
                                viewModel.comentar(textoComentario)
                                textoComentario = ""
                            }
                        },
                        enabled = textoComentario.isNotBlank(),
                        colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                            containerColor = com.esan.sportpro.core.theme.PaletaSportPro.Lima,
                            contentColor = com.esan.sportpro.core.theme.PaletaSportPro.Carbon,
                        ),
                    ) { Icon(Icons.Default.Send, contentDescription = "Enviar comentario") }
                }
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
                    onBloquear = { viewModel.bloquearAutor(publicacion.autorUid); onVolver() },
                    onRevisar = if (state.puedeRevisar) ({ aprobar -> viewModel.revisarPublicacion(publicacion.id, aprobar) }) else null,
                    modifier = Modifier.padding(vertical = 8.dp),
                    completo = true,
                )
            }
            item {
                Text(
                    "Comentarios · ${state.comentarios.size}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                )
            }
            if (state.comentarios.isEmpty()) {
                item {
                    Text(
                        "Aún no hay comentarios. ¡Escribe el primero!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
            }
            items(state.comentarios, key = { it.id }) { comentario ->
                ComentarioItem(
                    comentario = comentario,
                    onReportar = { onReportarComentario(comentario.id, publicacion.id, comentario.autorUid) },
                )
            }
        }
    }
}
