package com.esan.sportpro.ui.comunidad

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.comunidad.Publicacion
import com.esan.sportpro.domain.comunidad.TipoReaccion
import com.esan.sportpro.ui.common.EmptyStateMessage
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

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(onClick = onNuevaPublicacion) {
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
                EmptyStateMessage(mensaje = "Aún no hay publicaciones. ¡Sé el primero en compartir algo!", icono = Icons.Default.Forum)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.publicaciones, key = { it.id }) { publicacion ->
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
