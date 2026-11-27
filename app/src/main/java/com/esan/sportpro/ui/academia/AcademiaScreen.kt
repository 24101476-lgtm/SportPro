package com.esan.sportpro.ui.academia

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups2
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.academia.CampoEquipo
import com.esan.sportpro.domain.academia.CategoriasEquipo
import com.esan.sportpro.domain.academia.ColoresUniforme
import com.esan.sportpro.domain.academia.Equipo
import com.esan.sportpro.domain.academia.MensajesEquipo
import com.esan.sportpro.ui.common.CargandoSportPro
import com.esan.sportpro.ui.common.ChipEstado
import com.esan.sportpro.ui.common.EmptyStateMessage
import com.esan.sportpro.ui.common.EncabezadoModulo
import com.esan.sportpro.ui.common.ErrorSportPro
import com.esan.sportpro.ui.common.EscudoEquipo
import com.esan.sportpro.ui.common.IconoCircular
import com.esan.sportpro.ui.common.TarjetaSportPro
import com.esan.sportpro.ui.common.TonoEstado

/**
 * Módulo Academia (US-004/US-005): equipos organizados por categoría. El administrador crea,
 * edita y desactiva equipos; el entrenador los consulta.
 */
@Composable
fun AcademiaScreen(
    modifier: Modifier = Modifier,
    viewModel: AcademiaViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { evento ->
            if (evento is AcademiaEvent.Mensaje) snackbarHostState.showSnackbar(evento.texto)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (state.puedeEditar) {
                FloatingActionButton(onClick = viewModel::nuevoEquipo) {
                    Icon(Icons.Default.Add, contentDescription = "Nuevo equipo")
                }
            }
        },
    ) { padding ->
        when {
            state.cargando -> CargandoSportPro(modifier = Modifier.padding(padding))

            state.error != null -> ErrorSportPro(
                mensaje = state.error.orEmpty(),
                modifier = Modifier.padding(padding),
            )

            else -> Column(modifier = Modifier.padding(padding)) {
                val activos = state.equipos.count { it.activo }
                EncabezadoModulo(
                    titulo = "Academia",
                    subtitulo = "$activos ${if (activos == 1) "equipo activo" else "equipos activos"}",
                    icono = Icons.Default.School,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                )
                if (state.puedeEditar) {
                    ConfiguracionYCodigos(
                        state = state,
                        onEditar = viewModel::editarConfiguracion,
                        onGenerar = viewModel::generarCodigo,
                        onRevocar = viewModel::revocarCodigo,
                    )
                }
                state.editandoConfiguracion?.let { formulario ->
                    DialogoConfiguracion(
                        formulario = formulario,
                        onCampo = viewModel::onCampoAcademia,
                        onEscudo = viewModel::onEscudoAcademia,
                        onGuardar = viewModel::guardarConfiguracion,
                        onCancelar = viewModel::cerrarConfiguracion,
                    )
                }
                // Cuentas de entrenador que esperan aprobación: hasta aprobarlas no son personal.
                if (state.puedeEditar && state.entrenadoresPendientes.isNotEmpty()) {
                    EntrenadoresPendientes(
                        pendientes = state.entrenadoresPendientes,
                        onResolver = viewModel::resolverEntrenador,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Equipos por categoría", style = MaterialTheme.typography.titleMedium)
                    if (state.puedeEditar) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Inactivos", style = MaterialTheme.typography.labelMedium)
                            Switch(
                                checked = state.mostrarInactivos,
                                onCheckedChange = viewModel::onMostrarInactivos,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
                // US-005: filtro por categoría.
                if (state.categoriasConEquipos.size > 1) {
                    androidx.compose.foundation.lazy.LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            FilterChip(
                                selected = state.filtroCategoria == null,
                                onClick = { viewModel.onFiltroCategoria(null) },
                                label = { Text("Todas") },
                                colors = com.esan.sportpro.ui.common.coloresChipSportPro(),
                            )
                        }
                        items(state.categoriasConEquipos) { categoria ->
                            FilterChip(
                                selected = state.filtroCategoria == categoria,
                                onClick = {
                                    viewModel.onFiltroCategoria(if (state.filtroCategoria == categoria) null else categoria)
                                },
                                label = { Text(categoria) },
                                colors = com.esan.sportpro.ui.common.coloresChipSportPro(),
                            )
                        }
                    }
                }
                if (state.equiposVisibles.isEmpty()) {
                    EmptyStateMessage(
                        mensaje = if (state.filtroCategoria != null) {
                            "No hay equipos en la categoría ${state.filtroCategoria}."
                        } else if (state.puedeEditar) {
                            "Aún no hay equipos. Crea el primero con el botón +."
                        } else {
                            "Tu academia todavía no tiene equipos registrados."
                        },
                        icono = Icons.Default.Groups2,
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.equiposVisibles, key = { it.id }) { equipo ->
                            EquipoCard(
                                equipo = equipo,
                                jugadores = state.jugadoresDe(equipo),
                                puedeEditar = state.puedeEditarEquipo(equipo),
                                esAdmin = state.puedeEditar,
                                onEditar = { viewModel.editarEquipo(equipo) },
                                onCambiarEstado = { viewModel.cambiarEstado(equipo) },
                                onEliminar = { viewModel.pedirEliminar(equipo) },
                            )
                        }
                    }
                }
            }
        }
    }

    state.formulario?.let { formulario ->
        FormularioEquipoDialog(
            formulario = formulario,
            onNombre = viewModel::onNombreChanged,
            onCategoria = viewModel::onCategoriaChanged,
            onEntrenador = viewModel::onEntrenadorChanged,
            entrenadores = state.entrenadores,
            onEntrenadorElegido = viewModel::onEntrenadorElegido,
            onCategoriaOtra = viewModel::onCategoriaOtraChanged,
            onAnioMinimo = viewModel::onAnioMinimoChanged,
            onAnioMaximo = viewModel::onAnioMaximoChanged,
            onColor = viewModel::onColorUniforme,
            onElegirEscudo = viewModel::onEscudoElegido,
            onQuitarEscudo = viewModel::quitarEscudoNuevo,
            onGuardar = viewModel::guardar,
            onCancelar = viewModel::cerrarFormulario,
        )
    }

    state.eliminando?.let { (equipo, jugadores) ->
        DialogoEliminarEquipo(
            equipo = equipo,
            jugadores = jugadores,
            onEliminar = viewModel::confirmarEliminar,
            onDesactivar = viewModel::desactivarEnVezDeEliminar,
            onCancelar = viewModel::cerrarEliminar,
        )
    }
}

/**
 * US-005: con jugadores asignados no se puede eliminar; se ofrece Desactivar equipo, que lo oculta
 * de los listados sin borrar su historial.
 */
@Composable
private fun DialogoEliminarEquipo(
    equipo: Equipo,
    jugadores: Int,
    onEliminar: () -> Unit,
    onDesactivar: () -> Unit,
    onCancelar: () -> Unit,
) {
    val conJugadores = jugadores > 0
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(if (conJugadores) "No se puede eliminar" else "¿Eliminar ${equipo.nombre}?") },
        text = {
            Text(
                if (conJugadores) {
                    MensajesEquipo.NO_SE_PUEDE_ELIMINAR + ". ${equipo.nombre} tiene $jugadores " +
                        (if (jugadores == 1) "jugador activo" else "jugadores activos") +
                        ". Puedes desactivarlo: deja de aparecer en los listados y conserva su historial."
                } else {
                    "El equipo no tiene jugadores asignados. Esta acción no se puede deshacer."
                },
            )
        },
        confirmButton = {
            if (conJugadores) {
                if (equipo.activo) TextButton(onClick = onDesactivar) { Text("Desactivar equipo") }
            } else {
                TextButton(onClick = onEliminar) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    )
}

/** US-005: nombre, categoría con su rango de años, jugadores activos, DT y color de uniforme. */
@Composable
private fun EquipoCard(
    equipo: Equipo,
    jugadores: Int,
    puedeEditar: Boolean,
    esAdmin: Boolean,
    onEditar: () -> Unit,
    onCambiarEstado: () -> Unit,
    onEliminar: () -> Unit,
) {
    TarjetaSportPro {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Rediseño: el escudo del equipo (subido o generado) en lugar del ícono genérico.
                EscudoEquipo(nombre = equipo.nombre, tamano = 52.dp, url = equipo.escudoUrl)
                Column(modifier = Modifier.weight(1f)) {
                    Text(equipo.nombre, style = MaterialTheme.typography.titleMedium)
                    Text(
                        buildString {
                            append("Categoría ${equipo.categoria}")
                            if (equipo.rangoAnios.isNotBlank()) append(" (${equipo.rangoAnios})")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        if (equipo.entrenadorNombre.isNotBlank()) "DT: ${equipo.entrenadorNombre}" else "Sin entrenador asignado",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (equipo.sinEntrenador()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$jugadores", style = com.esan.sportpro.core.theme.EstilosSportPro.NumeroMediano)
                    Text(
                        if (jugadores == 1) "jugador" else "jugadores",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                if (equipo.colorUniforme.isNotBlank()) {
                    PuntoColor(equipo.colorUniforme, tamano = 14)
                    Text(
                        "Uniforme ${ColoresUniforme.nombreDe(equipo.colorUniforme).lowercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
                if (!equipo.activo) {
                    ChipEstado("Inactivo", TonoEstado.ERROR, modifier = Modifier.padding(start = 8.dp))
                }
                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                if (puedeEditar) {
                    IconButton(onClick = onEditar) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar equipo")
                    }
                }
                if (esAdmin) {
                    TextButton(onClick = onCambiarEstado) {
                        Text(if (equipo.activo) "Desactivar" else "Activar")
                    }
                    IconButton(onClick = onEliminar) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar equipo",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

/** Círculo del color de uniforme, con borde para que el blanco se vea sobre fondo claro. */
@Composable
private fun PuntoColor(hex: String, tamano: Int = 28, elegido: Boolean = false) {
    val color = runCatching { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(hex)) }
        .getOrDefault(MaterialTheme.colorScheme.outline)
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(tamano.dp)
            .then(
                if (elegido) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, androidx.compose.foundation.shape.CircleShape)
                } else {
                    Modifier
                },
            )
            .padding(if (elegido) 4.dp else 0.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(color)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, androidx.compose.foundation.shape.CircleShape),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormularioEquipoDialog(
    formulario: FormularioEquipo,
    onNombre: (String) -> Unit,
    onCategoria: (String) -> Unit,
    onEntrenador: (String) -> Unit,
    entrenadores: List<com.esan.sportpro.domain.academia.EntrenadorCuenta>,
    onEntrenadorElegido: (com.esan.sportpro.domain.academia.EntrenadorCuenta?) -> Unit,
    onCategoriaOtra: (String) -> Unit,
    onAnioMinimo: (String) -> Unit,
    onAnioMaximo: (String) -> Unit,
    onColor: (String) -> Unit,
    onElegirEscudo: (String) -> Unit,
    onQuitarEscudo: () -> Unit,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit,
) {
    // Selector de fotos del sistema: no necesita permisos y solo muestra imágenes.
    val selector = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let { onElegirEscudo(it.toString()) } }
    AlertDialog(
        onDismissRequest = { if (!formulario.guardando) onCancelar() },
        title = { Text(if (formulario.id == null) "Nuevo equipo" else "Editar equipo") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                // US-005 agrega campos (años, color): el diálogo ahora se desplaza.
                modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    val visible = formulario.escudoVisible
                    if (visible != null) {
                        androidx.compose.material3.Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(64.dp),
                        ) {
                            coil.compose.AsyncImage(
                                model = visible,
                                contentDescription = "Escudo elegido",
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                modifier = Modifier.padding(6.dp),
                            )
                        }
                    } else {
                        EscudoEquipo(nombre = formulario.nombre.ifBlank { "Equipo" }, tamano = 64.dp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Escudo", style = MaterialTheme.typography.labelLarge)
                        Row {
                            TextButton(
                                onClick = {
                                    selector.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly,
                                        ),
                                    )
                                },
                                enabled = !formulario.guardando,
                            ) { Text(if (visible == null) "Elegir escudo" else "Cambiar escudo") }
                            if (formulario.escudoNuevo != null) {
                                TextButton(onClick = onQuitarEscudo, enabled = !formulario.guardando) { Text("Quitar") }
                            }
                        }
                        if (visible == null) {
                            Text(
                                "Si no eliges uno, se genera con las iniciales y colores del equipo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = formulario.nombre,
                    onValueChange = onNombre,
                    label = { Text("Nombre del equipo") },
                    singleLine = true,
                    isError = CampoEquipo.NOMBRE in formulario.errores,
                    supportingText = { formulario.errores[CampoEquipo.NOMBRE]?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Categoría", style = MaterialTheme.typography.labelLarge)
                com.esan.sportpro.ui.common.SelectorOpciones(
                    opciones = CategoriasEquipo.todas,
                    seleccionada = formulario.categoria.takeIf { it in CategoriasEquipo.todas },
                    texto = { it },
                    onSeleccionar = { it?.let(onCategoria) },
                    placeholder = "Elige la categoría",
                    error = CampoEquipo.CATEGORIA in formulario.errores,
                )
                if (formulario.categoria == com.esan.sportpro.domain.academia.CategoriasEquipo.OTRA) {
                    OutlinedTextField(
                        value = formulario.categoriaOtra,
                        onValueChange = onCategoriaOtra,
                        label = { Text("Nombre de la categoría") },
                        singleLine = true,
                        isError = CampoEquipo.CATEGORIA in formulario.errores,
                        supportingText = {
                            Text(
                                formulario.errores[CampoEquipo.CATEGORIA]
                                    ?: "${formulario.categoriaOtra.trim().length}/${com.esan.sportpro.domain.academia.CategoriasEquipo.OTRA_MAX}",
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    formulario.errores[CampoEquipo.CATEGORIA]?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text("Años de nacimiento", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = formulario.anioMinimoTexto,
                        onValueChange = onAnioMinimo,
                        label = { Text("Desde") },
                        placeholder = { Text("2012") },
                        singleLine = true,
                        isError = CampoEquipo.ANIOS in formulario.errores,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = formulario.anioMaximoTexto,
                        onValueChange = onAnioMaximo,
                        label = { Text("Hasta") },
                        placeholder = { Text("2013") },
                        singleLine = true,
                        isError = CampoEquipo.ANIOS in formulario.errores,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    formulario.errores[CampoEquipo.ANIOS] ?: "Se propone según la categoría; puedes cambiarlo.",
                    color = if (CampoEquipo.ANIOS in formulario.errores) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
                Text("Color de uniforme", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ColoresUniforme.opciones.forEach { (nombre, hex) ->
                        val elegido = formulario.colorUniforme.equals(hex, ignoreCase = true)
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .selectable(
                                    selected = elegido,
                                    role = androidx.compose.ui.semantics.Role.RadioButton,
                                    onClick = { onColor(hex) },
                                )
                                .semantics { contentDescription = "Uniforme $nombre" },
                        ) {
                            PuntoColor(hex, tamano = 34, elegido = elegido)
                        }
                    }
                }
                Text("Entrenador responsable", style = MaterialTheme.typography.labelLarge)
                if (!formulario.puedeCambiarEntrenador) {
                    Text(
                        "${formulario.entrenador.ifBlank { "Sin asignar" }}. Solo el administrador cambia el entrenador del equipo.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else if (entrenadores.isEmpty()) {
                    // Sin cuentas de entrenador todavía: se puede anotar el nombre, pero esa persona
                    // no verá las mensualidades del equipo hasta tener cuenta y quedar asignada.
                    OutlinedTextField(
                        value = formulario.entrenador,
                        onValueChange = onEntrenador,
                        label = { Text("Nombre del entrenador") },
                        singleLine = true,
                        isError = CampoEquipo.ENTRENADOR in formulario.errores,
                        supportingText = {
                            Text(
                                formulario.errores[CampoEquipo.ENTRENADOR]
                                    ?: "Aún no hay cuentas de entrenador en la academia",
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        entrenadores.forEach { cuenta ->
                            FilterChip(
                                selected = formulario.entrenadorUid == cuenta.uid,
                                onClick = { onEntrenadorElegido(cuenta) },
                                label = { Text(cuenta.nombre) },
                            )
                        }
                    }
                    if (formulario.entrenadorUid.isBlank() && formulario.entrenador.isNotBlank()) {
                        Text(
                            "Asignado solo por nombre: ${formulario.entrenador}. Elige su cuenta para que vea " +
                                "los jugadores y las mensualidades del equipo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    formulario.errores[CampoEquipo.ENTRENADOR]?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onGuardar, enabled = !formulario.guardando) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onCancelar, enabled = !formulario.guardando) { Text("Cancelar") }
        },
    )
}

@Composable
private fun EntrenadoresPendientes(
    pendientes: List<com.esan.sportpro.domain.academia.EntrenadorCuenta>,
    onResolver: (com.esan.sportpro.domain.academia.EntrenadorCuenta, Boolean) -> Unit,
) {
    androidx.compose.material3.Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Entrenadores por aprobar (${pendientes.size})",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                "Hasta que los apruebes no pueden ver jugadores ni gestionar el plantel.",
                style = MaterialTheme.typography.bodySmall,
            )
            pendientes.forEach { cuenta ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(cuenta.nombre, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = { onResolver(cuenta, false) }) { Text("Rechazar") }
                    androidx.compose.material3.Button(onClick = { onResolver(cuenta, true) }) { Text("Aprobar") }
                }
            }
        }
    }
}

/**
 * US-004: datos de la academia (escudo, nombre, ubicación, contacto, fundación) y los códigos de
 * invitación con su rol, estado (vigente, usado, expirado o revocado) y fecha de expiración.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConfiguracionYCodigos(
    state: AcademiaUiState,
    onEditar: () -> Unit,
    onGenerar: (com.esan.sportpro.navigation.UserRole) -> Unit,
    onRevocar: (String) -> Unit,
) {
    val portapapeles = androidx.compose.ui.platform.LocalClipboardManager.current
    var verCodigos by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    var rolCodigo by androidx.compose.runtime.saveable.rememberSaveable {
        androidx.compose.runtime.mutableStateOf(com.esan.sportpro.navigation.UserRole.ADMINISTRADOR)
    }
    val c = state.configuracion
    val vigentes = state.codigos.count { it.vigente() }
    val formato = remember { com.esan.sportpro.core.fecha.FechasSportPro.formatoFecha() }
    androidx.compose.material3.Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                EscudoEquipo(nombre = c.nombre.ifBlank { "Academia" }, tamano = 48.dp, url = c.escudoUrl.ifBlank { null })
                Column(Modifier.weight(1f)) {
                    Text(c.nombre.ifBlank { "Academia sin registrar" }, style = MaterialTheme.typography.titleSmall)
                    val ubicacion = listOf(c.distrito, c.ciudad).filter { it.isNotBlank() }.joinToString(", ")
                    if (ubicacion.isNotBlank()) {
                        Text(ubicacion, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        buildString {
                            c.anioFundacion?.let { append("Fundada en $it. ") }
                            append("Las mensualidades vencen el día ${c.diaCorte}.")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onEditar) { Text(if (c.nombre.isBlank()) "Registrar" else "Editar") }
            }
            androidx.compose.material3.HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Códigos de invitación: $vigentes " + if (vigentes == 1) "vigente" else "vigentes",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { verCodigos = !verCodigos }) { Text(if (verCodigos) "Ocultar" else "Ver") }
            }
            if (verCodigos) {
                Text("Rol del nuevo código", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    com.esan.sportpro.navigation.UserRole.entries.forEach { rol ->
                        FilterChip(
                            selected = rolCodigo == rol,
                            onClick = { rolCodigo = rol },
                            label = { Text(etiquetaRol(rol)) },
                            colors = com.esan.sportpro.ui.common.coloresChipSportPro(),
                        )
                    }
                }
                androidx.compose.material3.OutlinedButton(
                    onClick = { onGenerar(rolCodigo) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Generar código de ${etiquetaRol(rolCodigo).lowercase()}") }
                if (state.codigos.isEmpty()) {
                    Text("Todavía no generaste códigos.", style = MaterialTheme.typography.bodySmall)
                }
                state.codigos.forEach { codigo ->
                    val estado = codigo.estado()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                codigo.codigo,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ChipEstado(estado.etiqueta, tonoDe(estado))
                                Text(
                                    "  ${etiquetaRol(codigo.rol)} · vence el " +
                                        (codigo.expiraEn?.let { formato.format(it) } ?: "—"),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                        if (estado == com.esan.sportpro.domain.academia.EstadoCodigo.VIGENTE) {
                            TextButton(onClick = { portapapeles.setText(androidx.compose.ui.text.AnnotatedString(codigo.codigo)) }) {
                                Text("Copiar")
                            }
                            TextButton(onClick = { onRevocar(codigo.codigo) }) { Text("Revocar") }
                        }
                    }
                }
            }
        }
    }
}

private fun etiquetaRol(rol: com.esan.sportpro.navigation.UserRole): String = when (rol) {
    com.esan.sportpro.navigation.UserRole.ADMINISTRADOR -> "Administrador"
    com.esan.sportpro.navigation.UserRole.ENTRENADOR -> "Entrenador"
    com.esan.sportpro.navigation.UserRole.JUGADOR -> "Jugador"
    com.esan.sportpro.navigation.UserRole.PADRE_DE_FAMILIA -> "Padre"
}

private fun tonoDe(estado: com.esan.sportpro.domain.academia.EstadoCodigo): TonoEstado = when (estado) {
    com.esan.sportpro.domain.academia.EstadoCodigo.VIGENTE -> TonoEstado.EXITO
    com.esan.sportpro.domain.academia.EstadoCodigo.USADO -> TonoEstado.NEUTRO
    com.esan.sportpro.domain.academia.EstadoCodigo.EXPIRADO -> TonoEstado.ADVERTENCIA
    com.esan.sportpro.domain.academia.EstadoCodigo.REVOCADO -> TonoEstado.ERROR
}

/** US-004: registro y edición de los datos de la academia, con el escudo recortado 1:1. */
@Composable
private fun DialogoConfiguracion(
    formulario: FormularioConfiguracion,
    onCampo: (com.esan.sportpro.domain.academia.CampoAcademia, String) -> Unit,
    onEscudo: (String?) -> Unit,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit,
) {
    val selector = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let { onEscudo(it.toString()) } }
    val d = formulario.datos
    AlertDialog(
        onDismissRequest = { if (!formulario.guardando) onCancelar() },
        title = { Text(if (formulario.esRegistro) "Registrar academia" else "Datos de la academia") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    val visible = formulario.escudoVisible
                    if (visible != null) {
                        androidx.compose.material3.Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(64.dp),
                        ) {
                            coil.compose.AsyncImage(
                                model = visible,
                                contentDescription = "Escudo de la academia",
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            )
                        }
                    } else {
                        EscudoEquipo(nombre = d.nombre.ifBlank { "Academia" }, tamano = 64.dp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Escudo o logotipo", style = MaterialTheme.typography.labelLarge)
                        TextButton(
                            onClick = {
                                selector.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly,
                                    ),
                                )
                            },
                            enabled = !formulario.guardando,
                        ) { Text(if (visible == null) "Elegir imagen" else "Cambiar imagen") }
                        Text(
                            formulario.errores[com.esan.sportpro.domain.academia.CampoAcademia.ESCUDO]
                                ?: "Máximo 2 MB. Se recorta en cuadrado (1:1).",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (com.esan.sportpro.domain.academia.CampoAcademia.ESCUDO in formulario.errores) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
                CampoTextoAcademia("Nombre de la academia", d.nombre, com.esan.sportpro.domain.academia.CampoAcademia.NOMBRE, formulario, onCampo)
                CampoTextoAcademia("Ciudad", d.ciudad, com.esan.sportpro.domain.academia.CampoAcademia.CIUDAD, formulario, onCampo)
                CampoTextoAcademia("Distrito", d.distrito, com.esan.sportpro.domain.academia.CampoAcademia.DISTRITO, formulario, onCampo)
                CampoTextoAcademia("Dirección", d.direccion, com.esan.sportpro.domain.academia.CampoAcademia.DIRECCION, formulario, onCampo)
                CampoTextoAcademia(
                    "Teléfono de contacto", d.telefono, com.esan.sportpro.domain.academia.CampoAcademia.TELEFONO, formulario, onCampo,
                    teclado = androidx.compose.ui.text.input.KeyboardType.Phone,
                )
                CampoTextoAcademia(
                    "Correo de contacto", d.correo, com.esan.sportpro.domain.academia.CampoAcademia.CORREO, formulario, onCampo,
                    teclado = androidx.compose.ui.text.input.KeyboardType.Email,
                )
                CampoTextoAcademia(
                    "Año de fundación", formulario.anioFundacionTexto,
                    com.esan.sportpro.domain.academia.CampoAcademia.ANIO_FUNDACION, formulario, onCampo,
                    teclado = androidx.compose.ui.text.input.KeyboardType.Number,
                )
                CampoTextoAcademia(
                    "Día de corte de las mensualidades", formulario.diaCorteTexto,
                    com.esan.sportpro.domain.academia.CampoAcademia.DIA_CORTE, formulario, onCampo,
                    teclado = androidx.compose.ui.text.input.KeyboardType.Number,
                    ayuda = "Entre 1 y 28. Después de ese día, lo no pagado figura como vencido.",
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onGuardar, enabled = !formulario.guardando) {
                Text(if (formulario.guardando) "Guardando…" else "Guardar")
            }
        },
        dismissButton = { TextButton(onClick = onCancelar, enabled = !formulario.guardando) { Text("Cancelar") } },
    )
}

@Composable
private fun CampoTextoAcademia(
    etiqueta: String,
    valor: String,
    campo: com.esan.sportpro.domain.academia.CampoAcademia,
    formulario: FormularioConfiguracion,
    onCampo: (com.esan.sportpro.domain.academia.CampoAcademia, String) -> Unit,
    teclado: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
    ayuda: String? = null,
) {
    val error = formulario.errores[campo]
    OutlinedTextField(
        value = valor,
        onValueChange = { onCampo(campo, it) },
        label = { Text(etiqueta) },
        singleLine = true,
        enabled = !formulario.guardando,
        isError = error != null,
        supportingText = if (error != null || ayuda != null) {
            { Text(error ?: ayuda.orEmpty()) }
        } else {
            null
        },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = teclado),
        modifier = Modifier.fillMaxWidth(),
    )
}
