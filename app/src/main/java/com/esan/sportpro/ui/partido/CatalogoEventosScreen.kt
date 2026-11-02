package com.esan.sportpro.ui.partido

import com.esan.sportpro.ui.common.BarraSuperiorSportPro
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.partido.TipoEvento

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CatalogoEventosScreen(
    onVolver: () -> Unit,
    onNuevoEvento: () -> Unit,
    onAbrirEvento: (String) -> Unit,
    mensajeResultado: String? = null,
    onMensajeResultadoMostrado: () -> Unit = {},
    viewModel: CatalogoEventosViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val esAdmin = state.rol == "ADMINISTRADOR"

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    LaunchedEffect(mensajeResultado) {
        mensajeResultado?.let { mensaje ->
            snackbarHostState.showSnackbar(mensaje)
            onMensajeResultadoMostrado()
        }
    }

    Scaffold(
        topBar = {
            BarraSuperiorSportPro(
                title = {
                    Text("Catálogo de eventos")
                },
                navigationIcon = {
                    IconButton(
                        onClick = onVolver
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState
            )
        },
    ) { padding ->

        when {
            state.cargando -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()

                    Text(
                        text = "Cargando catálogo...",
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }

            state.error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "No se pudo cargar el catálogo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = state.error.orEmpty(),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {

                    item {
                        Text(
                            text = "US-013 · LISTA",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (esAdmin) {
                        item {
                            Button(
                                onClick = onNuevoEvento,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null
                                )

                                Text(
                                    text = "Nuevo evento",
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Eventos del catálogo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    items(
                        items = state.eventos,
                        key = { it.id }
                    ) { evento ->

                        EventoCatalogoCard(
                            evento = evento,
                            puedeEditar = esAdmin,
                            onClick = {
                                if (esAdmin) {
                                    onAbrirEvento(evento.id)
                                }
                            }
                        )
                    }

                    if (!esAdmin) {
                        item {
                            Text(
                                text = "Solo el administrador puede configurar el catálogo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventoCatalogoCard(
    evento: TipoEvento,
    puedeEditar: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = puedeEditar,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = evento.nombre,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = buildString {
                        append(
                            evento.ambito.name
                                .lowercase()
                                .replaceFirstChar { it.uppercase() }
                        )

                        if (evento.sumaMarcador) {
                            append(" · suma al marcador")
                        }

                        append(" · v${evento.version}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            EstadoEventoCatalogoChip(
                activo = evento.activo
            )

            if (puedeEditar) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Configurar evento",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun EstadoEventoCatalogoChip(
    activo: Boolean
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (activo) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Text(
            text = if (activo) {
                "Activo"
            } else {
                "Inactivo"
            },
            color = if (activo) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 5.dp
            )
        )
    }
}