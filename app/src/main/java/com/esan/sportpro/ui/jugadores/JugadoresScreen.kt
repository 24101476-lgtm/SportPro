package com.esan.sportpro.ui.jugadores

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.academia.Equipo
import com.esan.sportpro.domain.comunidad.PosicionJugador
import com.esan.sportpro.domain.entrenamientos.ResumenAsistencia
import com.esan.sportpro.domain.jugadores.CampoJugador
import com.esan.sportpro.domain.jugadores.Jugador
import com.esan.sportpro.ui.common.CampoFormulario
import com.esan.sportpro.ui.common.EmptyStateMessage

/**
 * Módulo Jugadores (US-006/US-007). Navegación interna por estado del ViewModel:
 * lista → detalle → formulario. Solo administrador y entrenador acceden (privacidad de menores).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JugadoresScreen(
    modifier: Modifier = Modifier,
    viewModel: JugadoresViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { evento ->
            if (evento is JugadoresEvent.Mensaje) snackbarHostState.showSnackbar(evento.texto)
        }
    }

    BackHandler(enabled = state.pantalla == PantallaJugadores.Formulario) { viewModel.cancelarFormulario() }
    BackHandler(enabled = state.pantalla is PantallaJugadores.Detalle) { viewModel.volverALista() }

    when {
        state.cargando -> Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) { CircularProgressIndicator() }

        state.error != null -> EmptyStateMessage(mensaje = state.error.orEmpty(), modifier = modifier)

        !state.tieneAcceso -> EmptyStateMessage(
            mensaje = "Los perfiles de jugadores solo están disponibles para el administrador y el entrenador.",
            modifier = modifier,
            icono = Icons.Default.Group,
        )

        else -> when (val pantalla = state.pantalla) {
            is PantallaJugadores.Lista -> Scaffold(
                modifier = modifier,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                floatingActionButton = {
                    FloatingActionButton(onClick = viewModel::nuevoJugador) {
                        Icon(Icons.Default.Add, contentDescription = "Nuevo jugador")
                    }
                },
            ) { padding ->
                ListaJugadores(state = state, viewModel = viewModel, modifier = Modifier.padding(padding))
            }

            is PantallaJugadores.Detalle -> {
                val jugador = state.jugador(pantalla.jugadorId)
                Scaffold(
                    modifier = modifier,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        TopAppBar(
                            title = { Text(jugador?.nombreCompleto ?: "Jugador") },
                            navigationIcon = {
                                IconButton(onClick = viewModel::volverALista) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                                }
                            },
                        )
                    },
                ) { padding ->
                    if (jugador == null) {
                        EmptyStateMessage("No se encontró el jugador.", Modifier.padding(padding))
                    } else {
                        DetalleJugador(
                            jugador = jugador,
                            resumen = state.asistenciaDe(jugador),
                            onEditar = { viewModel.editarJugador(jugador) },
                            onCambiarEstado = { viewModel.cambiarEstado(jugador) },
                            modifier = Modifier.padding(padding),
                        )
                    }
                }
            }

            is PantallaJugadores.Formulario -> Scaffold(
                modifier = modifier,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = { Text(if (state.formulario.id == null) "Nuevo jugador" else "Editar jugador") },
                        navigationIcon = {
                            IconButton(onClick = viewModel::cancelarFormulario) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancelar")
                            }
                        },
                    )
                },
            ) { padding ->
                FormularioJugadorContenido(
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ListaJugadores(
    state: JugadoresUiState,
    viewModel: JugadoresViewModel,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.filtroEquipoId == null,
                    onClick = { viewModel.onFiltroEquipo(null) },
                    label = { Text("Todos") },
                )
                state.equiposActivos.forEach { equipo ->
                    FilterChip(
                        selected = state.filtroEquipoId == equipo.id,
                        onClick = { viewModel.onFiltroEquipo(equipo.id) },
                        label = { Text(equipo.etiqueta) },
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Mostrar inactivos", style = MaterialTheme.typography.labelMedium)
                Switch(
                    checked = state.mostrarInactivos,
                    onCheckedChange = viewModel::onMostrarInactivos,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        if (state.jugadoresVisibles.isEmpty()) {
            EmptyStateMessage(
                mensaje = if (state.equipos.isEmpty()) {
                    "Primero crea un equipo en Academia y luego registra a sus jugadores."
                } else {
                    "No hay jugadores para este filtro. Registra uno con el botón +."
                },
                icono = Icons.Default.Group,
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.jugadoresVisibles, key = { it.id }) { jugador ->
                    JugadorCard(jugador = jugador, onClick = { viewModel.abrirDetalle(jugador.id) })
                }
            }
        }
    }
}

@Composable
private fun JugadorCard(jugador: Jugador, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = jugador.dorsal?.toString() ?: "–",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(jugador.nombreCompleto, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${jugador.posicion} · ${jugador.equipoNombre}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!jugador.activo) {
                    Text("Inactivo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                }
            }
            if (jugador.esMenor()) {
                Text("Menor", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun DetalleJugador(
    jugador: Jugador,
    resumen: ResumenAsistencia,
    onEditar: () -> Unit,
    onCambiarEstado: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Seccion("Datos deportivos")
        FilaDato("Equipo", jugador.equipoNombre)
        FilaDato("Posición", jugador.posicion)
        FilaDato("Dorsal", jugador.dorsal?.toString())

        Seccion("Datos personales y físicos")
        FilaDato("Fecha de nacimiento", jugador.fechaNacimiento)
        FilaDato("Edad", jugador.edad()?.let { "$it años" })
        FilaDato("Estatura", jugador.estaturaCm?.let { "$it cm" })
        FilaDato("Peso", jugador.pesoKg?.let { "$it kg" })

        Seccion("Contacto")
        FilaDato("Teléfono", jugador.telefono.ifBlank { null })
        FilaDato("Correo", jugador.correo.ifBlank { null })

        Seccion("Contacto de emergencia")
        FilaDato("Nombre", jugador.contactoEmergenciaNombre)
        FilaDato("Parentesco", jugador.contactoEmergenciaParentesco)
        FilaDato("Teléfono", jugador.contactoEmergenciaTelefono)

        Seccion("Asistencia a entrenamientos")
        if (resumen.registrados == 0) {
            Text("Todavía no hay entrenamientos con asistencia registrada.", style = MaterialTheme.typography.bodyMedium)
        } else {
            FilaDato("Participación", "${resumen.porcentaje}% (${resumen.participaciones} de ${resumen.registrados})")
            FilaDato("Presente / Tarde", "${resumen.presentes} / ${resumen.tardes}")
            FilaDato("Justificado / Ausente", "${resumen.justificados} / ${resumen.ausentes}")
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = onCambiarEstado, modifier = Modifier.weight(1f)) {
                Text(if (jugador.activo) "Desactivar" else "Activar")
            }
            Button(onClick = onEditar, modifier = Modifier.weight(1f)) { Text("Editar") }
        }
    }
}

@Composable
private fun Seccion(titulo: String) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(titulo, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun FilaDato(etiqueta: String, valor: String?) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor?.ifBlank { null } ?: "Sin información", style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormularioJugadorContenido(
    state: JugadoresUiState,
    viewModel: JugadoresViewModel,
    modifier: Modifier = Modifier,
) {
    val formulario = state.formulario
    val datos = formulario.datos
    val errores = formulario.errores

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Seccion("Datos personales")
        CampoFormulario(
            valor = datos.nombres,
            onCambio = { v -> viewModel.onCampoChanged(CampoJugador.NOMBRES) { copy(nombres = v) } },
            etiqueta = "Nombres",
            error = errores[CampoJugador.NOMBRES],
        )
        CampoFormulario(
            valor = datos.apellidos,
            onCambio = { v -> viewModel.onCampoChanged(CampoJugador.APELLIDOS) { copy(apellidos = v) } },
            etiqueta = "Apellidos",
            error = errores[CampoJugador.APELLIDOS],
        )
        CampoFormulario(
            valor = datos.fechaNacimiento,
            onCambio = { v -> viewModel.onCampoChanged(CampoJugador.FECHA_NACIMIENTO) { copy(fechaNacimiento = v) } },
            etiqueta = "Fecha de nacimiento (AAAA-MM-DD)",
            error = errores[CampoJugador.FECHA_NACIMIENTO],
            teclado = KeyboardType.Number,
        )

        Seccion("Equipo y posición")
        Text("Equipo", style = MaterialTheme.typography.labelLarge)
        if (state.equiposActivos.isEmpty()) {
            Text("No hay equipos activos. Crea uno en Academia.", style = MaterialTheme.typography.bodySmall)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.equiposActivos.forEach { equipo: Equipo ->
                FilterChip(
                    selected = datos.equipoId == equipo.id,
                    onClick = { viewModel.onCampoChanged(CampoJugador.EQUIPO) { copy(equipoId = equipo.id) } },
                    label = { Text(equipo.etiqueta) },
                )
            }
        }
        errores[CampoJugador.EQUIPO]?.let { ErrorTexto(it) }

        Text("Posición", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PosicionJugador.entries.forEach { posicion ->
                FilterChip(
                    selected = datos.posicion == posicion.etiqueta,
                    onClick = { viewModel.onCampoChanged(CampoJugador.POSICION) { copy(posicion = posicion.etiqueta) } },
                    label = { Text(posicion.etiqueta) },
                )
            }
        }
        errores[CampoJugador.POSICION]?.let { ErrorTexto(it) }

        CampoFormulario(
            valor = datos.dorsal,
            onCambio = { v -> viewModel.onCampoChanged(CampoJugador.DORSAL) { copy(dorsal = v) } },
            etiqueta = "Dorsal (opcional)",
            error = errores[CampoJugador.DORSAL],
            teclado = KeyboardType.Number,
        )

        Seccion("Datos físicos (opcionales)")
        CampoFormulario(
            valor = datos.estatura,
            onCambio = { v -> viewModel.onCampoChanged(CampoJugador.ESTATURA) { copy(estatura = v) } },
            etiqueta = "Estatura (cm)",
            error = errores[CampoJugador.ESTATURA],
            teclado = KeyboardType.Number,
        )
        CampoFormulario(
            valor = datos.peso,
            onCambio = { v -> viewModel.onCampoChanged(CampoJugador.PESO) { copy(peso = v) } },
            etiqueta = "Peso (kg)",
            error = errores[CampoJugador.PESO],
            teclado = KeyboardType.Decimal,
        )

        Seccion("Contacto (opcional)")
        CampoFormulario(
            valor = datos.telefono,
            onCambio = { v -> viewModel.onCampoChanged(CampoJugador.TELEFONO) { copy(telefono = v) } },
            etiqueta = "Teléfono",
            error = errores[CampoJugador.TELEFONO],
            teclado = KeyboardType.Phone,
        )
        CampoFormulario(
            valor = datos.correo,
            onCambio = { v -> viewModel.onCampoChanged(CampoJugador.CORREO) { copy(correo = v) } },
            etiqueta = "Correo",
            error = errores[CampoJugador.CORREO],
            teclado = KeyboardType.Email,
        )

        Seccion("Contacto de emergencia")
        CampoFormulario(
            valor = datos.emergenciaNombre,
            onCambio = { v -> viewModel.onCampoChanged(CampoJugador.EMERGENCIA_NOMBRE) { copy(emergenciaNombre = v) } },
            etiqueta = "Nombre completo",
            error = errores[CampoJugador.EMERGENCIA_NOMBRE],
        )
        CampoFormulario(
            valor = datos.emergenciaParentesco,
            onCambio = { v -> viewModel.onCampoChanged(CampoJugador.EMERGENCIA_PARENTESCO) { copy(emergenciaParentesco = v) } },
            etiqueta = "Parentesco (ej. Madre)",
            error = errores[CampoJugador.EMERGENCIA_PARENTESCO],
        )
        CampoFormulario(
            valor = datos.emergenciaTelefono,
            onCambio = { v -> viewModel.onCampoChanged(CampoJugador.EMERGENCIA_TELEFONO) { copy(emergenciaTelefono = v) } },
            etiqueta = "Teléfono de emergencia",
            error = errores[CampoJugador.EMERGENCIA_TELEFONO],
            teclado = KeyboardType.Phone,
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = viewModel::cancelarFormulario,
                enabled = !formulario.guardando,
                modifier = Modifier.weight(1f),
            ) { Text("Cancelar") }
            Button(
                onClick = viewModel::guardar,
                enabled = !formulario.guardando,
                modifier = Modifier.weight(1f),
            ) {
                if (formulario.guardando) {
                    CircularProgressIndicator(modifier = Modifier.padding(2.dp), strokeWidth = 2.dp)
                } else {
                    Text("Guardar")
                }
            }
        }
    }
}

@Composable
private fun ErrorTexto(texto: String) {
    Text(texto, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}
