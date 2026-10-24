package com.esan.sportpro.ui.jugadores

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.jugadores.EstadoVinculacion
import com.esan.sportpro.domain.jugadores.Jugador
import com.esan.sportpro.domain.jugadores.ReglasVinculacion
import com.esan.sportpro.domain.jugadores.Vinculacion
import com.esan.sportpro.ui.common.CampoFormulario
import com.esan.sportpro.ui.common.EmptyStateMessage

/**
 * US-007 — vista del padre de familia en el módulo Jugadores (Figma node 9-466): sus hijos
 * vinculados, el estado de cada solicitud y el perfil de los jugadores con vínculo aprobado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisHijosScreen(
    modifier: Modifier = Modifier,
    viewModel: MisHijosViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { evento ->
            if (evento is MisHijosEvent.Mensaje) snackbarHostState.showSnackbar(evento.texto)
        }
    }
    BackHandler(enabled = state.pantalla != PantallaMisHijos.Lista) { viewModel.volverALista() }

    val pantalla = state.pantalla
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (pantalla) {
                            PantallaMisHijos.Lista -> "Mis hijos"
                            PantallaMisHijos.Solicitar -> "Vincular jugador"
                            is PantallaMisHijos.Detalle -> state.jugadorDetalle?.nombreCompleto ?: "Jugador"
                        },
                    )
                },
                navigationIcon = {
                    if (pantalla != PantallaMisHijos.Lista) {
                        IconButton(onClick = viewModel::volverALista) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (pantalla == PantallaMisHijos.Lista && !state.cargando && state.error == null) {
                ExtendedFloatingActionButton(
                    onClick = viewModel::abrirSolicitar,
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                    text = { Text("Vincular jugador") },
                )
            }
        },
    ) { padding ->
        val contenido = Modifier.padding(padding)
        when {
            state.cargando -> Box(contenido.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.error != null -> EmptyStateMessage(state.error.orEmpty(), contenido)
            else -> when (pantalla) {
                PantallaMisHijos.Lista -> ListaMisHijos(state, viewModel, contenido)
                PantallaMisHijos.Solicitar -> FormularioSolicitudContenido(state, viewModel, contenido)
                is PantallaMisHijos.Detalle -> {
                    val jugador = state.jugadorDetalle
                    when {
                        state.cargandoDetalle -> Box(contenido.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        jugador == null -> EmptyStateMessage("No tienes acceso al perfil de este jugador.", contenido)
                        else -> PerfilHijo(jugador, state, contenido)
                    }
                }
            }
        }
    }
}

@Composable
private fun ListaMisHijos(state: MisHijosUiState, viewModel: MisHijosViewModel, modifier: Modifier) {
    val lista = state.vinculacionesOrdenadas
    if (lista.isEmpty()) {
        EmptyStateMessage(
            mensaje = "Todavía no tienes jugadores vinculados. Pide a la academia el código de tu hijo o búscalo en la lista de su equipo.",
            modifier = modifier,
            icono = Icons.Default.FamilyRestroom,
        )
        return
    }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(lista, key = { it.id }) { vinculacion ->
            TarjetaVinculacion(vinculacion, onClick = { viewModel.abrirDetalle(vinculacion) })
        }
    }
}

/** Mientras no está aprobada, la tarjeta muestra solo nombre y categoría (criterio 5). */
@Composable
private fun TarjetaVinculacion(vinculacion: Vinculacion, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(vinculacion.jugadorNombre, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                EstadoVinculacionChip(vinculacion.estadoVinculacion)
            }
            Text(vinculacion.equipoNombre, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            when (vinculacion.estadoVinculacion) {
                EstadoVinculacion.PENDIENTE -> Text(
                    "Esperando la aprobación del entrenador o del administrador.",
                    style = MaterialTheme.typography.bodySmall,
                )
                EstadoVinculacion.RECHAZADA -> Text(
                    "Motivo: ${vinculacion.motivoRechazo}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                EstadoVinculacion.REVOCADA -> Text(
                    "La academia revocó esta vinculación.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                EstadoVinculacion.APROBADA -> Text(
                    "Toca para ver su perfil y asistencia.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
internal fun EstadoVinculacionChip(estado: EstadoVinculacion) {
    val color = when (estado) {
        EstadoVinculacion.APROBADA -> MaterialTheme.colorScheme.primary
        EstadoVinculacion.PENDIENTE -> MaterialTheme.colorScheme.tertiary
        EstadoVinculacion.RECHAZADA, EstadoVinculacion.REVOCADA -> MaterialTheme.colorScheme.error
    }
    AssistChip(onClick = {}, label = { Text(estado.etiqueta, color = color) })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormularioSolicitudContenido(state: MisHijosUiState, viewModel: MisHijosViewModel, modifier: Modifier) {
    val formulario = state.formulario
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModoSolicitud.entries.forEach { modo ->
                FilterChip(
                    selected = formulario.modo == modo,
                    onClick = { viewModel.onModo(modo) },
                    label = { Text(modo.etiqueta) },
                )
            }
        }

        when (formulario.modo) {
            ModoSolicitud.CODIGO -> {
                Text(
                    "Ingresa el código de ${ReglasVinculacion.LONGITUD_CODIGO} caracteres que te entregó la academia.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                CampoFormulario(
                    valor = formulario.codigo,
                    onCambio = viewModel::onCodigo,
                    etiqueta = "Código del jugador",
                    error = formulario.errorCodigo,
                )
                Button(onClick = viewModel::buscarCodigo, enabled = !formulario.buscando, modifier = Modifier.fillMaxWidth()) {
                    if (formulario.buscando) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Buscar")
                    }
                }
            }

            ModoSolicitud.LISTA -> {
                Text("Elige el equipo de tu hijo:", style = MaterialTheme.typography.labelLarge)
                if (state.equiposConJugadores.isEmpty()) {
                    Text("La academia todavía no tiene jugadores registrados.", style = MaterialTheme.typography.bodySmall)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.equiposConJugadores.forEach { equipo ->
                        FilterChip(
                            selected = formulario.equipoId == equipo.id,
                            onClick = { viewModel.onEquipo(equipo.id) },
                            label = { Text(equipo.etiqueta) },
                        )
                    }
                }
                state.jugadoresDelEquipo.forEach { jugador ->
                    val elegido = formulario.seleccionado?.jugadorId == jugador.jugadorId
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.onJugadorElegido(jugador) },
                        colors = if (elegido) {
                            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        } else {
                            CardDefaults.cardColors()
                        },
                    ) {
                        Text(jugador.nombre, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        formulario.seleccionado?.let { jugador ->
            HorizontalDivider()
            Text("Vas a solicitar la vinculación con:", style = MaterialTheme.typography.labelLarge)
            Text(jugador.nombre, style = MaterialTheme.typography.titleLarge)
            Text(jugador.equipoNombre, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "El entrenador o el administrador revisará la solicitud. Hasta que la apruebe solo verás su nombre y categoría.",
                style = MaterialTheme.typography.bodySmall,
            )
            Button(onClick = viewModel::enviarSolicitud, enabled = !formulario.enviando, modifier = Modifier.fillMaxWidth()) {
                if (formulario.enviando) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Enviar solicitud")
                }
            }
        }
    }
}

/** Criterio 6: perfil, asistencia y accesos a mensualidades y estadísticas del hijo vinculado. */
@Composable
private fun PerfilHijo(jugador: Jugador, state: MisHijosUiState, modifier: Modifier) {
    val resumen = state.asistenciaDe(jugador)
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SeccionHijo("Datos deportivos")
        DatoHijo("Equipo", jugador.equipoNombre)
        DatoHijo("Posición", jugador.posicion)
        DatoHijo("Dorsal", jugador.dorsal?.toString())

        SeccionHijo("Datos personales y físicos")
        DatoHijo("Fecha de nacimiento", jugador.fechaNacimiento)
        DatoHijo("Estatura", jugador.estaturaCm?.let { "$it cm" })
        DatoHijo("Peso", jugador.pesoKg?.let { "$it kg" })

        SeccionHijo("Contacto de emergencia")
        DatoHijo("Nombre", jugador.contactoEmergenciaNombre)
        DatoHijo("Parentesco", jugador.contactoEmergenciaParentesco)
        DatoHijo("Teléfono", jugador.contactoEmergenciaTelefono)

        SeccionHijo("Asistencia a entrenamientos")
        if (resumen.registrados == 0) {
            Text("Todavía no hay sesiones realizadas.", style = MaterialTheme.typography.bodyMedium)
        } else {
            DatoHijo("Participación", "${resumen.porcentaje}% (${resumen.participaciones} de ${resumen.registrados})")
            DatoHijo("Presente / Tarde", "${resumen.presentes} / ${resumen.tardes}")
            DatoHijo("Faltas justificadas / injustificadas", "${resumen.justificados} / ${resumen.ausentes}")
        }

        SeccionHijo("Más información")
        Text(
            "Sus convocatorias, estadísticas y el estado de sus mensualidades están en las pestañas Partido, Estadísticas y Mensualidades.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun SeccionHijo(titulo: String) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(titulo, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun DatoHijo(etiqueta: String, valor: String?) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            valor?.takeIf { it.isNotBlank() } ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            color = if (valor.isNullOrBlank()) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified,
        )
    }
}
