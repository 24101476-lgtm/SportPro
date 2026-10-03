package com.esan.sportpro.ui.entrenamientos

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.esan.sportpro.domain.entrenamientos.CampoEjercicio
import com.esan.sportpro.domain.entrenamientos.CampoEntrenamiento
import com.esan.sportpro.domain.entrenamientos.Ejercicio
import com.esan.sportpro.domain.entrenamientos.Entrenamiento
import com.esan.sportpro.domain.entrenamientos.EstadoAsistencia
import com.esan.sportpro.ui.common.CampoFormulario
import com.esan.sportpro.ui.common.EmptyStateMessage

/**
 * Módulo Entrenamientos (US-009 a US-013). Navegación interna por estado del ViewModel:
 * lista → detalle → formulario / asistencia. Administrador y entrenador planifican y toman
 * asistencia; el jugador solo consulta las sesiones.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntrenamientosScreen(
    modifier: Modifier = Modifier,
    viewModel: EntrenamientosViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { evento ->
            if (evento is EntrenamientosEvent.Mensaje) snackbarHostState.showSnackbar(evento.texto)
        }
    }

    BackHandler(enabled = state.pantalla == PantallaEntrenamientos.Formulario) { viewModel.cancelarFormulario() }
    BackHandler(enabled = state.pantalla is PantallaEntrenamientos.Detalle) { viewModel.volverALista() }
    (state.pantalla as? PantallaEntrenamientos.Asistencia)?.let { pantalla ->
        BackHandler { viewModel.volverADetalle(pantalla.entrenamientoId) }
    }

    when {
        state.cargando -> Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) { CircularProgressIndicator() }

        state.error != null -> EmptyStateMessage(mensaje = state.error.orEmpty(), modifier = modifier)

        else -> when (val pantalla = state.pantalla) {
            is PantallaEntrenamientos.Lista -> Scaffold(
                modifier = modifier,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                floatingActionButton = {
                    if (state.puedeGestionar) {
                        FloatingActionButton(onClick = viewModel::nuevoEntrenamiento) {
                            Icon(Icons.Default.Add, contentDescription = "Nuevo entrenamiento")
                        }
                    }
                },
            ) { padding ->
                ListaEntrenamientos(state = state, viewModel = viewModel, modifier = Modifier.padding(padding))
            }

            is PantallaEntrenamientos.Detalle -> {
                val entrenamiento = state.entrenamiento(pantalla.entrenamientoId)
                Scaffold(
                    modifier = modifier,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        TopAppBar(
                            title = { Text("Entrenamiento") },
                            navigationIcon = {
                                IconButton(onClick = viewModel::volverALista) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                                }
                            },
                        )
                    },
                ) { padding ->
                    if (entrenamiento == null) {
                        EmptyStateMessage("No se encontró el entrenamiento.", Modifier.padding(padding))
                    } else {
                        DetalleEntrenamiento(
                            entrenamiento = entrenamiento,
                            puedeGestionar = state.puedeGestionar,
                            onEditar = { viewModel.editarEntrenamiento(entrenamiento) },
                            onAsistencia = { viewModel.abrirAsistencia(entrenamiento) },
                            modifier = Modifier.padding(padding),
                        )
                    }
                }
            }

            is PantallaEntrenamientos.Formulario -> Scaffold(
                modifier = modifier,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = { Text(if (state.formulario.id == null) "Nuevo entrenamiento" else "Editar entrenamiento") },
                        navigationIcon = {
                            IconButton(onClick = viewModel::cancelarFormulario) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancelar")
                            }
                        },
                    )
                },
            ) { padding ->
                FormularioEntrenamientoContenido(state = state, viewModel = viewModel, modifier = Modifier.padding(padding))
            }

            is PantallaEntrenamientos.Asistencia -> {
                val entrenamiento = state.entrenamiento(pantalla.entrenamientoId)
                Scaffold(
                    modifier = modifier,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        TopAppBar(
                            title = { Text("Asistencia") },
                            navigationIcon = {
                                IconButton(onClick = { viewModel.volverADetalle(pantalla.entrenamientoId) }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                                }
                            },
                        )
                    },
                ) { padding ->
                    if (entrenamiento == null) {
                        EmptyStateMessage("No se encontró el entrenamiento.", Modifier.padding(padding))
                    } else {
                        AsistenciaContenido(
                            state = state,
                            entrenamiento = entrenamiento,
                            viewModel = viewModel,
                            modifier = Modifier.padding(padding),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ListaEntrenamientos(
    state: EntrenamientosUiState,
    viewModel: EntrenamientosViewModel,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
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

        if (state.entrenamientosVisibles.isEmpty()) {
            EmptyStateMessage(
                mensaje = if (state.puedeGestionar) {
                    "No hay entrenamientos planificados. Crea uno con el botón +."
                } else {
                    "Todavía no hay entrenamientos planificados."
                },
                icono = Icons.Default.Groups2,
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.entrenamientosVisibles, key = { it.id }) { entrenamiento ->
                    EntrenamientoCard(entrenamiento, onClick = { viewModel.abrirDetalle(entrenamiento.id) })
                }
            }
        }
    }
}

@Composable
private fun EntrenamientoCard(entrenamiento: Entrenamiento, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${entrenamiento.fecha} · ${entrenamiento.hora}", style = MaterialTheme.typography.titleMedium)
            Text(
                entrenamiento.equipoNombre,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(entrenamiento.objetivo, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${entrenamiento.ejercicios.size} ejercicios · ${entrenamiento.duracionTotalMin} min" +
                    if (entrenamiento.asistenciaRegistrada) " · Asistencia registrada" else "",
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun DetalleEntrenamiento(
    entrenamiento: Entrenamiento,
    puedeGestionar: Boolean,
    onEditar: () -> Unit,
    onAsistencia: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(entrenamiento.equipoNombre, style = MaterialTheme.typography.titleLarge)
        Text("${entrenamiento.fecha} a las ${entrenamiento.hora}", style = MaterialTheme.typography.bodyLarge)
        if (entrenamiento.lugar.isNotBlank()) Text("Lugar: ${entrenamiento.lugar}", style = MaterialTheme.typography.bodyMedium)

        Subtitulo("Objetivo")
        Text(entrenamiento.objetivo, style = MaterialTheme.typography.bodyMedium)

        Subtitulo("Ejercicios (${entrenamiento.duracionTotalMin} min en total)")
        entrenamiento.ejercicios.forEachIndexed { indice, ejercicio ->
            EjercicioItem(numero = indice + 1, ejercicio = ejercicio)
        }

        Subtitulo("Asistencia")
        if (entrenamiento.asistenciaRegistrada) {
            val estados = entrenamiento.asistencia.values.mapNotNull { EstadoAsistencia.desde(it) }
            val presentes = estados.count { it.participo }
            Text("$presentes de ${estados.size} jugadores asistieron.", style = MaterialTheme.typography.bodyMedium)
        } else {
            Text("Todavía no se registró la asistencia.", style = MaterialTheme.typography.bodyMedium)
        }

        if (puedeGestionar) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(onClick = onEditar, modifier = Modifier.weight(1f)) { Text("Editar") }
                Button(onClick = onAsistencia, modifier = Modifier.weight(1f)) { Text("Tomar asistencia") }
            }
        }
    }
}

@Composable
private fun Subtitulo(texto: String) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(texto, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun EjercicioItem(numero: Int, ejercicio: Ejercicio, onQuitar: (() -> Unit)? = null) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("$numero. ${ejercicio.nombre} · ${ejercicio.duracionMin} min", style = MaterialTheme.typography.titleSmall)
                if (ejercicio.descripcion.isNotBlank()) {
                    Text(ejercicio.descripcion, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (onQuitar != null) {
                IconButton(onClick = onQuitar) {
                    Icon(Icons.Default.Close, contentDescription = "Quitar ejercicio")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormularioEntrenamientoContenido(
    state: EntrenamientosUiState,
    viewModel: EntrenamientosViewModel,
    modifier: Modifier = Modifier,
) {
    val formulario = state.formulario
    val datos = formulario.datos
    val errores = formulario.errores

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Equipo", style = MaterialTheme.typography.labelLarge)
        if (state.equiposActivos.isEmpty()) {
            Text("No hay equipos activos. Crea uno en Academia.", style = MaterialTheme.typography.bodySmall)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.equiposActivos.forEach { equipo ->
                FilterChip(
                    selected = datos.equipoId == equipo.id,
                    onClick = { viewModel.onCampoChanged(CampoEntrenamiento.EQUIPO) { copy(equipoId = equipo.id) } },
                    label = { Text(equipo.etiqueta) },
                )
            }
        }
        errores[CampoEntrenamiento.EQUIPO]?.let { ErrorTexto(it) }

        CampoFormulario(
            valor = datos.fecha,
            onCambio = { v -> viewModel.onCampoChanged(CampoEntrenamiento.FECHA) { copy(fecha = v) } },
            etiqueta = "Fecha (AAAA-MM-DD)",
            error = errores[CampoEntrenamiento.FECHA],
            teclado = KeyboardType.Number,
        )
        CampoFormulario(
            valor = datos.hora,
            onCambio = { v -> viewModel.onCampoChanged(CampoEntrenamiento.HORA) { copy(hora = v) } },
            etiqueta = "Hora (HH:mm)",
            error = errores[CampoEntrenamiento.HORA],
            teclado = KeyboardType.Number,
        )
        CampoFormulario(
            valor = datos.lugar,
            onCambio = { v -> viewModel.onCampoChanged(CampoEntrenamiento.LUGAR) { copy(lugar = v) } },
            etiqueta = "Lugar (opcional)",
            error = errores[CampoEntrenamiento.LUGAR],
        )
        CampoFormulario(
            valor = datos.objetivo,
            onCambio = { v -> viewModel.onCampoChanged(CampoEntrenamiento.OBJETIVO) { copy(objetivo = v) } },
            etiqueta = "Objetivo de la sesión",
            error = errores[CampoEntrenamiento.OBJETIVO],
            ayuda = "${datos.objetivo.length}/200",
            minLineas = 2,
        )

        val total = datos.ejercicios.sumOf { it.duracionMin }
        Subtitulo("Ejercicios ($total min)")
        datos.ejercicios.forEachIndexed { indice, ejercicio ->
            EjercicioItem(numero = indice + 1, ejercicio = ejercicio, onQuitar = { viewModel.quitarEjercicio(indice) })
        }
        errores[CampoEntrenamiento.EJERCICIOS]?.let { ErrorTexto(it) }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = viewModel::abrirDialogoEjercicio, modifier = Modifier.weight(1f)) {
                Text("Nuevo ejercicio")
            }
            OutlinedButton(
                onClick = viewModel::abrirBiblioteca,
                enabled = state.biblioteca.isNotEmpty(),
                modifier = Modifier.weight(1f),
            ) { Text("Desde biblioteca") }
        }

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

    state.dialogoEjercicio?.let { dialogo ->
        AlertDialog(
            onDismissRequest = viewModel::cerrarDialogoEjercicio,
            title = { Text("Nuevo ejercicio") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CampoFormulario(
                        valor = dialogo.nombre,
                        onCambio = { v -> viewModel.onEjercicioChanged { copy(nombre = v, errores = this.errores - CampoEjercicio.NOMBRE) } },
                        etiqueta = "Nombre",
                        error = dialogo.errores[CampoEjercicio.NOMBRE],
                    )
                    CampoFormulario(
                        valor = dialogo.duracion,
                        onCambio = { v -> viewModel.onEjercicioChanged { copy(duracion = v, errores = this.errores - CampoEjercicio.DURACION) } },
                        etiqueta = "Duración (minutos)",
                        error = dialogo.errores[CampoEjercicio.DURACION],
                        teclado = KeyboardType.Number,
                    )
                    CampoFormulario(
                        valor = dialogo.descripcion,
                        onCambio = { v -> viewModel.onEjercicioChanged { copy(descripcion = v, errores = this.errores - CampoEjercicio.DESCRIPCION) } },
                        etiqueta = "Descripción (opcional)",
                        error = dialogo.errores[CampoEjercicio.DESCRIPCION],
                        minLineas = 2,
                    )
                    Row(
                        modifier = Modifier.clickable {
                            viewModel.onEjercicioChanged { copy(guardarEnBiblioteca = !guardarEnBiblioteca) }
                        },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = dialogo.guardarEnBiblioteca,
                            onCheckedChange = { marcado ->
                                viewModel.onEjercicioChanged { copy(guardarEnBiblioteca = marcado) }
                            },
                        )
                        Text("Guardar en la biblioteca para reutilizarlo")
                    }
                }
            },
            confirmButton = { TextButton(onClick = viewModel::confirmarEjercicio) { Text("Agregar") } },
            dismissButton = { TextButton(onClick = viewModel::cerrarDialogoEjercicio) { Text("Cancelar") } },
        )
    }

    if (state.mostrarBiblioteca) {
        AlertDialog(
            onDismissRequest = viewModel::cerrarBiblioteca,
            title = { Text("Biblioteca de ejercicios") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(state.biblioteca, key = { it.id }) { item ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.agregarDeBiblioteca(item) }
                                .padding(vertical = 8.dp),
                        ) {
                            Text("${item.nombre} · ${item.duracionMin} min", style = MaterialTheme.typography.titleSmall)
                            if (item.descripcion.isNotBlank()) {
                                Text(item.descripcion, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        HorizontalDivider()
                    }
                }
            },
            confirmButton = { TextButton(onClick = viewModel::cerrarBiblioteca) { Text("Cerrar") } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AsistenciaContenido(
    state: EntrenamientosUiState,
    entrenamiento: Entrenamiento,
    viewModel: EntrenamientosViewModel,
    modifier: Modifier = Modifier,
) {
    val plantel = state.plantelDe(entrenamiento)

    if (plantel.isEmpty()) {
        EmptyStateMessage(
            mensaje = "El equipo ${entrenamiento.equipoNombre} todavía no tiene jugadores activos.",
            modifier = modifier,
            icono = Icons.Default.Groups2,
        )
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${entrenamiento.fecha} · ${entrenamiento.equipoNombre}", style = MaterialTheme.typography.titleSmall)
            TextButton(onClick = viewModel::marcarTodosPresentes) { Text("Todos presentes") }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(plantel, key = { it.id }) { jugador ->
                val actual = EstadoAsistencia.desde(state.asistenciaEdicion[jugador.id])
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(jugador.nombreCompleto, style = MaterialTheme.typography.titleSmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            EstadoAsistencia.entries.forEach { estado ->
                                FilterChip(
                                    selected = actual == estado,
                                    onClick = { viewModel.onEstadoAsistencia(jugador.id, estado) },
                                    label = { Text(estado.etiqueta) },
                                )
                            }
                        }
                    }
                }
            }
        }
        Button(
            onClick = { viewModel.guardarAsistencia(entrenamiento.id) },
            enabled = !state.guardandoAsistencia,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            if (state.guardandoAsistencia) {
                CircularProgressIndicator(modifier = Modifier.padding(2.dp), strokeWidth = 2.dp)
            } else {
                Text("Guardar asistencia")
            }
        }
    }
}

@Composable
private fun ErrorTexto(texto: String) {
    Text(texto, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}
