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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups2
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.unit.sp
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

    // "Datos de la academia" y "Códigos de invitación" se abren como pantallas propias.
    var verCodigos by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    val formularioDatos = state.editandoConfiguracion
    if (formularioDatos != null) {
        androidx.activity.compose.BackHandler(enabled = !formularioDatos.guardando) { viewModel.cerrarConfiguracion() }
        PantallaDatosAcademia(
            formulario = formularioDatos,
            snackbarHostState = snackbarHostState,
            onCampo = viewModel::onCampoAcademia,
            onEscudo = viewModel::onEscudoAcademia,
            onGuardar = viewModel::guardarConfiguracion,
            onVolver = viewModel::cerrarConfiguracion,
            modifier = modifier,
        )
        return
    }
    if (verCodigos && state.puedeEditar) {
        androidx.activity.compose.BackHandler { verCodigos = false }
        PantallaCodigos(
            state = state,
            snackbarHostState = snackbarHostState,
            onGenerar = viewModel::generarCodigo,
            onRevocar = viewModel::revocarCodigo,
            onVolver = { verCodigos = false },
            modifier = modifier,
        )
        return
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (state.puedeEditar) {
                androidx.compose.material3.ExtendedFloatingActionButton(
                    onClick = viewModel::nuevoEquipo,
                    containerColor = com.esan.sportpro.core.theme.PaletaSportPro.Lima,
                    contentColor = com.esan.sportpro.core.theme.PaletaSportPro.Carbon,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Nuevo equipo", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                )
            }
        },
    ) { padding ->
        when {
            state.cargando -> CargandoSportPro(modifier = Modifier.padding(padding))

            state.error != null -> ErrorSportPro(
                mensaje = state.error.orEmpty(),
                modifier = Modifier.padding(padding),
            )

            else -> {
                // Toda la pantalla es una sola lista: antes los equipos solo tenían el espacio que
                // sobraba abajo y en un celular se veía un solo equipo.
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item(key = "cabecera") {
                        CabeceraAcademia(state = state, onEditar = viewModel::editarConfiguracion)
                    }
                    if (state.puedeEditar) {
                        item(key = "tarjetas") {
                            TarjetasAcademia(
                                state = state,
                                onEditar = viewModel::editarConfiguracion,
                                onVerCodigos = { verCodigos = true },
                            )
                        }
                    }
                    // Cuentas de entrenador que esperan aprobación: hasta aprobarlas no son personal.
                    if (state.puedeEditar && state.entrenadoresPendientes.isNotEmpty()) {
                        item(key = "pendientes") {
                            EntrenadoresPendientes(
                                pendientes = state.entrenadoresPendientes,
                                onResolver = viewModel::resolverEntrenador,
                            )
                        }
                    }
                    item(key = "titulo-equipos") {
                        val activos = state.equipos.count { it.activo }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Equipos",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                )
                                Text(
                                    "$activos ${if (activos == 1) "activo" else "activos"} · por categoría",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (state.puedeEditar) {
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
                        item(key = "filtros") {
                            androidx.compose.foundation.lazy.LazyRow(
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
                    }
                    if (state.equiposVisibles.isEmpty()) {
                        item(key = "vacio") {
                            EmptyStateMessage(
                                mensaje = if (state.filtroCategoria != null) {
                                    "No hay equipos en la categoría ${state.filtroCategoria}."
                                } else if (state.puedeEditar) {
                                    "Aún no hay equipos. Crea el primero con \"Nuevo equipo\"."
                                } else {
                                    "Tu academia todavía no tiene equipos registrados."
                                },
                                icono = Icons.Default.Groups2,
                            )
                        }
                    } else {
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
    var menu by remember { androidx.compose.runtime.mutableStateOf(false) }
    val colorUniforme = runCatching { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(equipo.colorUniforme)) }
        .getOrNull()
    androidx.compose.material3.Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
            // Franja con el color del uniforme: cada equipo se reconoce de un vistazo.
            androidx.compose.foundation.layout.Box(
                Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(colorUniforme ?: com.esan.sportpro.core.theme.PaletaSportPro.MusgoClaro),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f).padding(start = 12.dp, end = 4.dp, top = 14.dp, bottom = 14.dp),
            ) {
                EscudoEquipo(nombre = equipo.nombre, tamano = 52.dp, url = equipo.escudoUrl)
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            equipo.nombre,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        ChipEstado(
                            if (equipo.activo) "Activo" else "Inactivo",
                            if (equipo.activo) TonoEstado.EXITO else TonoEstado.ERROR,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    Text(
                        buildString {
                            append("Categoría ${equipo.categoria}")
                            if (equipo.rangoAnios.isNotBlank()) append(" · ${equipo.rangoAnios}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        buildString {
                            append("$jugadores ${if (jugadores == 1) "jugador" else "jugadores"} · ")
                            append(if (equipo.entrenadorNombre.isNotBlank()) "DT ${equipo.entrenadorNombre}" else "Sin entrenador")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        color = if (equipo.sinEntrenador()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    if (equipo.colorUniforme.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            PuntoColor(equipo.colorUniforme, tamano = 12)
                            Text(
                                "Uniforme ${ColoresUniforme.nombreDe(equipo.colorUniforme).lowercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                    }
                }
                // Acciones en un menú: la tarjeta queda limpia y no se toca "Eliminar" por error.
                if (puedeEditar || esAdmin) {
                    androidx.compose.foundation.layout.Box {
                        IconButton(onClick = { menu = true }) {
                            Icon(androidx.compose.material.icons.Icons.Default.MoreVert, contentDescription = "Opciones de ${equipo.nombre}")
                        }
                        androidx.compose.material3.DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            if (puedeEditar) {
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text("Editar equipo") },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = { menu = false; onEditar() },
                                )
                            }
                            if (esAdmin) {
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(if (equipo.activo) "Desactivar" else "Activar") },
                                    leadingIcon = {
                                        Icon(
                                            if (equipo.activo) {
                                                androidx.compose.material.icons.Icons.Default.PauseCircle
                                            } else {
                                                androidx.compose.material.icons.Icons.Default.PlayCircle
                                            },
                                            contentDescription = null,
                                        )
                                    },
                                    onClick = { menu = false; onCambiarEstado() },
                                )
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = { menu = false; onEliminar() },
                                )
                            }
                        }
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
        modifier = Modifier.fillMaxWidth(),
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
 * Cabecera compacta de la academia: escudo, nombre, temporada y tres cifras (equipos, jugadores y
 * entrenadores). Reemplaza la tarjeta alta que solo decía "Academia · 5 equipos activos".
 */
@Composable
private fun CabeceraAcademia(state: AcademiaUiState, onEditar: () -> Unit) {
    val c = state.configuracion
    val activos = state.equipos.filter { it.activo }
    val jugadores = activos.sumOf { state.jugadoresDe(it) }
    val entrenadores = activos.map { it.entrenadorNombre.trim() }.filter { it.isNotEmpty() }.distinct().size
    val temporada = com.esan.sportpro.core.fecha.FechasSportPro.hoy().year
    val ubicacion = listOf(c.distrito, c.ciudad).filter { it.isNotBlank() }.joinToString(", ")
    com.esan.sportpro.ui.common.FondoFotoSportPro(
        foto = com.esan.sportpro.ui.common.FotoSportPro.ESTADIO,
        forma = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        velo = androidx.compose.ui.graphics.Brush.verticalGradient(
            0f to com.esan.sportpro.core.theme.PaletaSportPro.Musgo.copy(alpha = 0.80f),
            1f to com.esan.sportpro.core.theme.PaletaSportPro.Musgo.copy(alpha = 0.95f),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EscudoEquipo(nombre = c.nombre.ifBlank { "Academia" }, tamano = 56.dp, url = c.escudoUrl.ifBlank { null })
                Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(
                        "Temporada $temporada" + if (ubicacion.isNotBlank()) " · $ubicacion" else "",
                        style = MaterialTheme.typography.labelMedium,
                        color = com.esan.sportpro.core.theme.PaletaSportPro.Lima,
                    )
                    Text(
                        c.nombre.ifBlank { "Mi academia" },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color.White,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
                if (state.puedeEditar) {
                    androidx.compose.material3.FilledIconButton(
                        onClick = onEditar,
                        colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                            containerColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.16f),
                            contentColor = androidx.compose.ui.graphics.Color.White,
                        ),
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Default.Settings, contentDescription = "Datos de la academia")
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) {
                CifraAcademia(activos.size, if (activos.size == 1) "Equipo" else "Equipos", Modifier.weight(1f))
                CifraAcademia(jugadores, if (jugadores == 1) "Jugador" else "Jugadores", Modifier.weight(1f))
                CifraAcademia(entrenadores, if (entrenadores == 1) "Entrenador" else "Entrenadores", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CifraAcademia(valor: Int, etiqueta: String, modifier: Modifier) {
    androidx.compose.material3.Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.13f),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                valor.toString(),
                style = com.esan.sportpro.core.theme.EstilosSportPro.NumeroMediano,
                color = androidx.compose.ui.graphics.Color.White,
            )
            Text(
                etiqueta,
                style = MaterialTheme.typography.labelSmall,
                color = com.esan.sportpro.core.theme.PaletaSportPro.MusgoClaro,
                maxLines = 1,
            )
        }
    }
}

/**
 * US-004: dos tarjetas con su propia acción — Mensualidades (día de vencimiento → editar datos) e
 * Invitaciones (códigos vigentes → gestionar). Antes compartían una sola tarjeta gris.
 */
@Composable
private fun TarjetasAcademia(
    state: AcademiaUiState,
    onEditar: () -> Unit,
    onVerCodigos: () -> Unit,
) {
    val vigentes = state.codigos.count { it.vigente() }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        TarjetaAccion(
            icono = androidx.compose.material.icons.Icons.Default.Payments,
            titulo = "Mensualidades",
            valor = "Día ${state.configuracion.diaCorte}",
            detalle = "de cada mes vencen",
            accion = if (state.configuracion.nombre.isBlank()) "Registrar datos" else "Editar datos",
            onClick = onEditar,
            modifier = Modifier.weight(1f),
        )
        TarjetaAccion(
            icono = androidx.compose.material.icons.Icons.Default.Key,
            titulo = "Invitaciones",
            valor = vigentes.toString(),
            detalle = if (vigentes == 1) "código vigente" else "códigos vigentes",
            accion = "Gestionar códigos",
            onClick = onVerCodigos,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TarjetaAccion(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    valor: String,
    detalle: String,
    accion: String,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.layout.Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(com.esan.sportpro.core.theme.PaletaSportPro.LimaSuave),
                ) {
                    Icon(icono, contentDescription = null, tint = com.esan.sportpro.core.theme.PaletaSportPro.Musgo, modifier = Modifier.size(18.dp))
                }
                Text(
                    titulo,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Text(
                valor,
                style = com.esan.sportpro.core.theme.EstilosSportPro.NumeroMediano,
                modifier = Modifier.padding(top = 10.dp),
            )
            Text(detalle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "$accion →",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = com.esan.sportpro.core.theme.SportProTema.colores.textoAcento,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

internal fun etiquetaRol(rol: com.esan.sportpro.navigation.UserRole): String = when (rol) {
    com.esan.sportpro.navigation.UserRole.ADMINISTRADOR -> "Administrador"
    com.esan.sportpro.navigation.UserRole.ENTRENADOR -> "Entrenador"
    com.esan.sportpro.navigation.UserRole.JUGADOR -> "Jugador"
    com.esan.sportpro.navigation.UserRole.PADRE_DE_FAMILIA -> "Padre"
}

internal fun tonoDe(estado: com.esan.sportpro.domain.academia.EstadoCodigo): TonoEstado = when (estado) {
    com.esan.sportpro.domain.academia.EstadoCodigo.VIGENTE -> TonoEstado.EXITO
    com.esan.sportpro.domain.academia.EstadoCodigo.USADO -> TonoEstado.NEUTRO
    com.esan.sportpro.domain.academia.EstadoCodigo.EXPIRADO -> TonoEstado.ADVERTENCIA
    com.esan.sportpro.domain.academia.EstadoCodigo.REVOCADO -> TonoEstado.ERROR
}
