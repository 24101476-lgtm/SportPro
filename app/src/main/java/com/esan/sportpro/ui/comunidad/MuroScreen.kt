package com.esan.sportpro.ui.comunidad

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.esan.sportpro.core.theme.PaletaSportPro
import com.esan.sportpro.ui.common.TarjetaSportPro
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.comunidad.Publicacion
import com.esan.sportpro.domain.comunidad.TipoReaccion
import com.esan.sportpro.ui.common.EstadoVacioModulo
import com.esan.sportpro.ui.partido.ColoresPartido
import com.google.firebase.auth.FirebaseAuth

/** US-028, criterio 5 — Frame 131_Comunidad_Muro. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuroScreen(
    onNuevaPublicacion: () -> Unit,
    onAbrirPublicacion: (Publicacion) -> Unit,
    onReportar: (publicacionId: String, autorUid: String, esMenor: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PublicacionesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val uidActual = FirebaseAuth.getInstance().currentUser?.uid
    var orden by rememberSaveable { mutableStateOf(OrdenMuro.RECIENTES) }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNuevaPublicacion,
                containerColor = ColoresPartido.Verde,
                contentColor = androidx.compose.ui.graphics.Color.White,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nueva publicación")
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = viewModel::refrescar,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (!state.isLoading && state.publicaciones.isEmpty()) {
                EstadoVacioModulo(
                    titulo = "Aún no hay publicaciones",
                    mensaje = "¡Sé el primero en compartir algo!",
                    icono = Icons.Default.Forum,
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item(key = "compositor") {
                        CompositorMuro(
                            onClick = onNuevaPublicacion,
                            orden = orden,
                            onOrden = { orden = it },
                        )
                    }
                    val lista = if (orden == OrdenMuro.POPULARES) {
                        state.publicaciones.sortedByDescending { it.reaccionesPorUsuario.size + it.comentariosCount }
                    } else {
                        state.publicaciones
                    }
                    items(lista, key = { it.id }) { publicacion ->
                        val miReaccion = publicacion.reaccionesPorUsuario[uidActual]
                            ?.let { nombre -> runCatching { TipoReaccion.valueOf(nombre) }.getOrNull() }
                        PublicacionCard(
                            publicacion = publicacion,
                            uidActual = uidActual,
                            reaccionActual = miReaccion,
                            onAbrir = { onAbrirPublicacion(publicacion) },
                            onReaccionar = { tipo -> viewModel.reaccionar(publicacion.id, tipo) },
                            onReportar = { onReportar(publicacion.id, publicacion.autorUid, publicacion.autorEsMenorDeEdad) },
                            onEliminar = { viewModel.eliminarPublicacion(publicacion.id) },
                        )
                    }
                    item {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (!state.sinMasPaginas) {
                                LaunchedEffect(state.publicaciones.size) { viewModel.cargarMas() }
                                if (state.cargandoMas) {
                                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class OrdenMuro(val etiqueta: String) { RECIENTES("Recientes"), POPULARES("Populares") }

/** Cabecera del muro: invita a publicar (foto, logro, partido) y permite ordenar la lista. */
@Composable
private fun CompositorMuro(onClick: () -> Unit, orden: OrdenMuro, onOrden: (OrdenMuro) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TarjetaSportPro(onClick = onClick) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(PaletaSportPro.MusgoMedio, PaletaSportPro.Verde))),
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White)
                    }
                    Text(
                        "¿Qué quieres compartir con tu academia?",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AtajoPublicar("📸 Foto", PaletaSportPro.Informacion)
                    AtajoPublicar("🏆 Logro", PaletaSportPro.Advertencia)
                    AtajoPublicar("⚽ Partido", PaletaSportPro.Verde)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OrdenMuro.entries.forEach { opcion ->
                FilterChip(
                    selected = orden == opcion,
                    onClick = { onOrden(opcion) },
                    label = { Text(opcion.etiqueta) },
                )
            }
        }
    }
}

@Composable
private fun AtajoPublicar(texto: String, color: Color) {
    Surface(shape = CircleShape, color = color.copy(alpha = 0.12f)) {
        Text(
            texto,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        )
    }
}
