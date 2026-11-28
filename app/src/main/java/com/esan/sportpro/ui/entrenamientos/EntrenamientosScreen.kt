package com.esan.sportpro.ui.entrenamientos

import com.esan.sportpro.domain.entrenamientos.EstadoSesion
import com.esan.sportpro.ui.common.EncabezadoModulo
import com.esan.sportpro.core.fecha.FechasSportPro
import com.esan.sportpro.ui.common.CampoHora
import com.esan.sportpro.ui.common.CampoFecha
import com.esan.sportpro.ui.common.BarraSuperiorSportPro
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
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
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Groups2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.esan.sportpro.domain.entrenamientos.ReglasAsistencia
import com.esan.sportpro.domain.entrenamientos.ValidacionEntrenamiento
import com.esan.sportpro.ui.common.CampoFormulario
import com.esan.sportpro.ui.common.EmptyStateMessage

/**
 * Módulo Entrenamientos (US-009 a US-013). Navegación interna por estado del ViewModel:
 * lista → detalle → formulario / asistencia, y lista → biblioteca de ejercicios (US-009). Administrador y entrenador planifican y toman
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

    if (state.pantalla == PantallaEntrenamientos.Biblioteca) {
        BibliotecaEjerciciosScreen(onSalir = viewModel::volverALista, modifier = modifier)
        return
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
                        BarraSuperiorSportPro(
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
                            onDuplicar = { viewModel.abrirDuplicar(entrenamiento) },
                            onCancelar = { viewModel.abrirCancelacion(entrenamiento) },
                            modifier = Modifier.padding(padding),
                            biblioteca = state.biblioteca,
                        )
                    }
                }
            }

            is PantallaEntrenamientos.Formulario -> Scaffold(
                modifier = modifier,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    BarraSuperiorSportPro(
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

            // Se atiende antes del `when` (la biblioteca tiene su propio Scaffold y ViewModel).
            PantallaEntrenamientos.Biblioteca -> Unit

            is PantallaEntrenamientos.Asistencia -> AsistenciaScreen(
                entrenamientoId = pantalla.entrenamientoId,
                onVolver = { viewModel.volverADetalle(pantalla.entrenamientoId) },
                modifier = modifier,
            )
        }
    }

    DialogosPlanificacion(state = state, viewModel = viewModel)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ListaEntrenamientos(
    state: EntrenamientosUiState,
    viewModel: EntrenamientosViewModel,
    modifier: Modifier = Modifier,
) {
    // Rediseño: mes y tira de la semana arriba, sesiones separadas en Próximas y Anteriores.
    val hoy = java.time.LocalDate.now()
    val visibles = state.entrenamientosVisibles
    val (proximas, anteriores) = visibles.partition { (fechaDe(it.fecha) ?: hoy) >= hoy }
    val programadas = visibles.count { it.estadoSesion == EstadoSesion.PROGRAMADA }
    val canceladas = visibles.count { it.estadoSesion == EstadoSesion.CANCELADA }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                Text(
                    MESES_LARGOS[hoy.monthValue - 1],
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    buildString {
                        append("$programadas ${if (programadas == 1) "programada" else "programadas"}")
                        if (canceladas > 0) append(" · $canceladas ${if (canceladas == 1) "cancelada" else "canceladas"}")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item { TiraSemana(visibles, hoy = hoy) }
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            ) {
                com.esan.sportpro.ui.common.SelectorEquipo(
                    equipos = state.equiposActivos,
                    seleccionadoId = state.filtroEquipoId,
                    onSeleccionar = viewModel::onFiltroEquipo,
                    modifier = Modifier.weight(1f),
                )
                if (state.puedeGestionar) {
                    androidx.compose.material3.Surface(
                        onClick = viewModel::abrirGestionBiblioteca,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                        color = com.esan.sportpro.core.theme.PaletaSportPro.Lima,
                        contentColor = com.esan.sportpro.core.theme.PaletaSportPro.Carbon,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        ) {
                            Icon(Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Ejercicios", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                }
            }
        }

        if (visibles.isEmpty()) {
            item {
                EmptyStateMessage(
                    mensaje = if (state.puedeGestionar) {
                        "No hay entrenamientos planificados. Crea uno con el botón +."
                    } else {
                        "Todavía no hay entrenamientos planificados."
                    },
                    icono = Icons.Default.Groups2,
                )
            }
        }
        if (proximas.isNotEmpty()) {
            item { TituloGrupo("Próximas") }
            items(proximas.sortedBy { it.clave }, key = { it.id }) { entrenamiento ->
                EntrenamientoCard(entrenamiento, hoy, onClick = { viewModel.abrirDetalle(entrenamiento.id) })
            }
        }
        if (anteriores.isNotEmpty()) {
            item { TituloGrupo("Anteriores") }
            items(anteriores.sortedByDescending { it.clave }, key = { it.id }) { entrenamiento ->
                EntrenamientoCard(entrenamiento, hoy, onClick = { viewModel.abrirDetalle(entrenamiento.id) })
            }
        }
    }
}

@Composable
private fun TituloGrupo(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
    )
}

/** Tarjeta de sesión: bloque de fecha, objetivo, equipo y hora, y chips de estado y duración. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EntrenamientoCard(entrenamiento: Entrenamiento, hoy: java.time.LocalDate, onClick: () -> Unit) {
    val cancelada = entrenamiento.estadoSesion == EstadoSesion.CANCELADA
    androidx.compose.material3.Surface(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth().alpha(if (cancelada) 0.6f else 1f),
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BloqueFecha(
                entrenamiento.fecha,
                fondo = if (cancelada) MaterialTheme.colorScheme.outline else com.esan.sportpro.core.theme.PaletaSportPro.Musgo,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    entrenamiento.objetivo.ifBlank { "Entrenamiento" },
                    style = MaterialTheme.typography.titleSmall,
                    textDecoration = if (cancelada) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(
                    listOf(entrenamiento.equipoNombre, entrenamiento.hora, entrenamiento.lugar)
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ChipEstadoSesion(entrenamiento, hoy)
                    if (!cancelada) {
                        ChipInfo("${entrenamiento.duracionSesionMin} min")
                        if (entrenamiento.ejercicios.isNotEmpty()) {
                            ChipInfo("${entrenamiento.ejercicios.size} ${if (entrenamiento.ejercicios.size == 1) "ejercicio" else "ejercicios"}")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Detalle de la sesión: cabecera con fecha y horario, plan con la barra de tiempos y la línea de
 * tiempo (hora de inicio de cada ejercicio), asistencia y acciones.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetalleEntrenamiento(
    entrenamiento: Entrenamiento,
    puedeGestionar: Boolean,
    onEditar: () -> Unit,
    onAsistencia: () -> Unit,
    onDuplicar: () -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier,
    biblioteca: List<com.esan.sportpro.domain.entrenamientos.EjercicioBiblioteca> = emptyList(),
) {
    val categorias = remember(biblioteca) { biblioteca.associate { it.id to it.categoriaTecnica } }
    fun categoriaDe(ejercicio: Ejercicio) = categorias[ejercicio.ejercicioId]
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Cabecera oscura con la foto de conos.
        com.esan.sportpro.ui.common.FondoFotoSportPro(
            foto = com.esan.sportpro.ui.common.FotoSportPro.CONOS,
            forma = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            velo = androidx.compose.ui.graphics.Brush.verticalGradient(
                0f to com.esan.sportpro.core.theme.PaletaSportPro.Carbon.copy(alpha = 0.78f),
                1f to com.esan.sportpro.core.theme.PaletaSportPro.Carbon.copy(alpha = 0.94f),
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BloqueFecha(
                        entrenamiento.fecha,
                        fondo = com.esan.sportpro.core.theme.PaletaSportPro.Lima,
                        texto = com.esan.sportpro.core.theme.PaletaSportPro.Carbon,
                        acento = com.esan.sportpro.core.theme.PaletaSportPro.Musgo,
                        ancho = 62.dp,
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            entrenamiento.objetivo.ifBlank { "Entrenamiento" },
                            style = MaterialTheme.typography.titleLarge,
                            color = androidx.compose.ui.graphics.Color.White,
                        )
                        Text(
                            entrenamiento.equipoNombre,
                            style = MaterialTheme.typography.bodySmall,
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.75f),
                        )
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    val blanco = androidx.compose.ui.graphics.Color.White
                    val fin = horaMas(entrenamiento.hora, entrenamiento.duracionSesionMin)
                    ChipInfo(
                        "🕓 ${entrenamiento.hora}" + (fin?.let { " – $it" } ?: ""),
                        fondo = blanco.copy(alpha = 0.16f),
                        color = blanco,
                    )
                    if (entrenamiento.lugar.isNotBlank()) {
                        ChipInfo("📍 ${entrenamiento.lugar}", fondo = blanco.copy(alpha = 0.16f), color = blanco)
                    }
                    ChipEstadoSesion(entrenamiento)
                }
            }
        }

        if (entrenamiento.estadoSesion == EstadoSesion.CANCELADA) {
            Text(
                "Cancelada" + entrenamiento.canceladaPorNombre.takeIf { it.isNotBlank() }?.let { " por $it" }.orEmpty() +
                    ": ${entrenamiento.motivoCancelacion}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        // Plan: barra proporcional al tiempo de cada ejercicio y línea de tiempo con su hora de inicio.
        Subtitulo("Plan de la sesión · ${entrenamiento.duracionTotalMin} min")
        if (entrenamiento.ejercicios.isEmpty()) {
            Text("La sesión no tiene ejercicios cargados.", style = MaterialTheme.typography.bodyMedium)
        } else {
            Row(
                Modifier.fillMaxWidth().height(14.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(50)),
            ) {
                entrenamiento.ejercicios.forEach { ejercicio ->
                    androidx.compose.foundation.layout.Box(
                        Modifier.weight(ejercicio.duracionMin.coerceAtLeast(1).toFloat()).fillMaxHeight()
                            .background(colorCategoria(categoriaDe(ejercicio))),
                    )
                }
            }
            val presentes = entrenamiento.ejercicios.mapNotNull { categoriaDe(it) }.distinct()
            if (presentes.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    presentes.forEach { categoria ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.foundation.layout.Box(
                                Modifier.size(8.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                                    .background(colorCategoria(categoria)),
                            )
                            Text(categoria.etiqueta, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
            }
            androidx.compose.material3.Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                    var acumulado = 0
                    entrenamiento.ejercicios.forEachIndexed { i, ejercicio ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        FilaPlan(
                            hora = horaMas(entrenamiento.hora, acumulado) ?: "${i + 1}.",
                            ejercicio = ejercicio,
                            color = colorCategoria(categoriaDe(ejercicio)),
                        )
                        acumulado += ejercicio.duracionMin
                    }
                }
            }
        }

        Subtitulo("Asistencia")
        val estados = entrenamiento.asistencia.values.mapNotNull { EstadoAsistencia.desde(it) }
        val presentes = estados.count { it.participo }
        androidx.compose.material3.Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                AnilloAsistencia(
                    when {
                        entrenamiento.realizada && entrenamiento.porcentajeAsistencia != null ->
                            (entrenamiento.porcentajeAsistencia / 100.0).toFloat()
                        entrenamiento.realizada && estados.isNotEmpty() -> presentes.toFloat() / estados.size
                        else -> null
                    },
                )
                Column(Modifier.padding(start = 14.dp)) {
                    Text(
                        when {
                            entrenamiento.realizada -> "$presentes de ${estados.size} asistieron"
                            estados.isNotEmpty() -> "Asistencia en curso"
                            else -> "Aún no se toma"
                        },
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        when {
                            entrenamiento.realizada -> "Sesión realizada"
                            estados.isNotEmpty() -> "${estados.size} jugadores marcados"
                            else -> "Se registra al empezar la sesión"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (puedeGestionar) {
            val programada = entrenamiento.estadoSesion == EstadoSesion.PROGRAMADA
            if (entrenamiento.estadoSesion != EstadoSesion.CANCELADA) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (programada) {
                        OutlinedButton(onClick = onEditar, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Editar") }
                    }
                    Button(onClick = onAsistencia, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                        Text(if (entrenamiento.realizada) "Ver asistencia" else "Tomar asistencia")
                    }
                }
            }
            // US-010: duplicar hacia otra fecha (criterio 8) y cancelar con motivo (criterio 9).
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(onClick = onDuplicar, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Duplicar") }
                if (programada) {
                    OutlinedButton(
                        onClick = onCancelar,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    ) {
                        Text("Cancelar sesión", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

/** Fila de la línea de tiempo: hora de inicio, punto del color de la categoría, nombre y minutos. */
@Composable
private fun FilaPlan(hora: String, ejercicio: Ejercicio, color: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.Top) {
        Text(
            hora,
            style = com.esan.sportpro.core.theme.EstilosSportPro.NumeroMediano.copy(fontSize = 15.sp, lineHeight = 18.sp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(46.dp),
        )
        androidx.compose.foundation.layout.Box(
            Modifier.padding(top = 4.dp).size(10.dp).clip(androidx.compose.foundation.shape.CircleShape).background(color),
        )
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(ejercicio.nombre, style = MaterialTheme.typography.titleSmall)
            if (ejercicio.descripcion.isNotBlank()) {
                Text(
                    ejercicio.descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        }
        ChipInfo("${ejercicio.duracionMin}'")
    }
}

@Composable
private fun Subtitulo(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
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
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Rediseño: el formulario se ordena en tres bloques (cuándo y dónde, objetivo y plan).
      SeccionFormulario("Cuándo y dónde", "1") {
        Text("Equipo", style = MaterialTheme.typography.labelLarge)
        if (state.equiposActivos.isEmpty()) {
            Text("No hay equipos activos. Crea uno en Academia.", style = MaterialTheme.typography.bodySmall)
        }
        com.esan.sportpro.ui.common.SelectorEquipo(
            equipos = state.equiposActivos,
            seleccionadoId = datos.equipoId.ifBlank { null },
            onSeleccionar = { id -> id?.let { viewModel.onCampoChanged(CampoEntrenamiento.EQUIPO) { copy(equipoId = it) } } },
            permitirTodos = false,
            error = errores[CampoEntrenamiento.EQUIPO] != null,
        )
        errores[CampoEntrenamiento.EQUIPO]?.let { ErrorTexto(it) }

        // Una sesión nueva no puede quedar en el pasado; al editar se respeta la fecha original.
        CampoFecha(
            valorIso = datos.fecha,
            onCambio = { v -> viewModel.onCampoChanged(CampoEntrenamiento.FECHA) { copy(fecha = v) } },
            etiqueta = "Fecha",
            error = errores[CampoEntrenamiento.FECHA],
            fechaMinima = if (formulario.id == null) FechasSportPro.hoy() else null,
            fechaMaxima = FechasSportPro.hoy().plusYears(1),
        )
        CampoHora(
            valor = datos.hora,
            onCambio = { v -> viewModel.onCampoChanged(CampoEntrenamiento.HORA) { copy(hora = v) } },
            etiqueta = "Hora de inicio",
            error = errores[CampoEntrenamiento.HORA],
        )
        CampoFormulario(
            valor = datos.duracion,
            onCambio = { v -> viewModel.onCampoChanged(CampoEntrenamiento.DURACION) { copy(duracion = v) } },
            etiqueta = "Duración total (minutos)",
            error = errores[CampoEntrenamiento.DURACION],
            ayuda = "Entre ${ValidacionEntrenamiento.DURACION_SESION_MIN} y ${ValidacionEntrenamiento.DURACION_SESION_MAX} minutos",
            teclado = KeyboardType.Number,
        )
        CampoFormulario(
            valor = datos.lugar,
            onCambio = { v -> viewModel.onCampoChanged(CampoEntrenamiento.LUGAR) { copy(lugar = v) } },
            etiqueta = "Lugar (opcional)",
            error = errores[CampoEntrenamiento.LUGAR],
        )
      }
      SeccionFormulario("Objetivo", "2") {
        CampoFormulario(
            valor = datos.objetivo,
            onCambio = { v -> viewModel.onCampoChanged(CampoEntrenamiento.OBJETIVO) { copy(objetivo = v) } },
            etiqueta = "Objetivo de la sesión",
            error = errores[CampoEntrenamiento.OBJETIVO],
            ayuda = "${datos.objetivo.length}/200",
            minLineas = 2,
        )
      }
      SeccionFormulario("Plan de la sesión", "3") {
        val total = datos.ejercicios.sumOf { it.duracionMin }
        val planificada = datos.duracion.trim().toIntOrNull() ?: 0
        // Minutos con ejercicios frente a la duración de la sesión.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$total de $planificada min con ejercicios",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${datos.ejercicios.size} ${if (datos.ejercicios.size == 1) "ejercicio" else "ejercicios"}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        androidx.compose.material3.LinearProgressIndicator(
            progress = { if (planificada > 0) (total.toFloat() / planificada).coerceIn(0f, 1f) else 0f },
            color = if (planificada in 1 until total) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
            drawStopIndicator = {},
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(50)),
        )
        if (datos.ejercicios.size > 1) {
            Text(
                "Mantén presionado ⠿ y arrastra para cambiar el orden. La duración que ajustes aquí no cambia la biblioteca.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ListaEjerciciosReordenable(
            ejercicios = datos.ejercicios,
            onMover = viewModel::moverEjercicio,
            onAjustar = viewModel::ajustarDuracionEjercicio,
            onQuitar = viewModel::quitarEjercicio,
        )
        errores[CampoEntrenamiento.EJERCICIOS]?.let { ErrorTexto(it) }
        // US-010, criterio 4: advertencia que no bloquea el guardado.
        state.avisoExceso?.let { aviso ->
            Text(aviso, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = viewModel::abrirDialogoEjercicio, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Nuevo", modifier = Modifier.padding(start = 6.dp))
            }
            Button(
                onClick = viewModel::abrirBiblioteca,
                enabled = state.biblioteca.any { it.activo },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) {
                Icon(Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Biblioteca", modifier = Modifier.padding(start = 6.dp))
            }
        }
      }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = viewModel::cancelarFormulario,
                enabled = !formulario.guardando,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) { Text("Cancelar") }
            Button(
                onClick = { viewModel.guardar() },
                enabled = !formulario.guardando,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
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
                    Text(
                        "Este ejercicio solo se agrega a esta sesión. Para reutilizarlo, créalo en la Biblioteca de ejercicios.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.busquedaBiblioteca,
                        onValueChange = viewModel::onBusquedaBiblioteca,
                        placeholder = { Text("Buscar por nombre") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val disponibles = state.bibliotecaDisponible
                    if (disponibles.isEmpty()) {
                        Text("No hay ejercicios con ese nombre.", style = MaterialTheme.typography.bodyMedium)
                    }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(disponibles, key = { it.id }) { item ->
                            FilaBiblioteca(item, onAgregar = { viewModel.agregarDeBiblioteca(item) })
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = viewModel::cerrarBiblioteca) { Text("Cerrar") } },
        )
    }
}

/** Bloque blanco del formulario con número de paso y título. */
@Composable
private fun SeccionFormulario(titulo: String, paso: String, contenido: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    androidx.compose.material3.Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.layout.Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(24.dp).clip(androidx.compose.foundation.shape.CircleShape)
                        .background(com.esan.sportpro.core.theme.PaletaSportPro.Lima),
                ) {
                    Text(paso, style = MaterialTheme.typography.labelMedium, color = com.esan.sportpro.core.theme.PaletaSportPro.Carbon)
                }
                Text(titulo, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 10.dp))
            }
            contenido()
        }
    }
}

/** Fila del selector de biblioteca: foto (o la de su categoría), nombre, categoría y minutos. */
@Composable
private fun FilaBiblioteca(item: com.esan.sportpro.domain.entrenamientos.EjercicioBiblioteca, onAgregar: () -> Unit) {
    val categoria = item.categoriaTecnica
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            .clickable(onClick = onAgregar)
            .padding(vertical = 8.dp, horizontal = 4.dp),
    ) {
        androidx.compose.foundation.layout.Box(Modifier.size(52.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))) {
            if (item.imagenUrl.isNotBlank()) {
                coil.compose.AsyncImage(
                    model = item.imagenUrl,
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(fotoCategoria(categoria)),
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(
                        androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0f) },
                    ),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(item.nombre, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.layout.Box(
                    Modifier.size(7.dp).clip(androidx.compose.foundation.shape.CircleShape).background(colorCategoria(categoria)),
                )
                Text(
                    categoria?.etiqueta ?: "Sin categoría",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 5.dp),
                )
            }
        }
        ChipInfo("${item.duracionMin}'")
        Icon(
            Icons.Default.Add,
            contentDescription = "Agregar ${item.nombre}",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun ErrorTexto(texto: String) {
    Text(texto, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}
