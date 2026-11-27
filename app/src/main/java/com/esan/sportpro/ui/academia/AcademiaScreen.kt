package com.esan.sportpro.ui.academia

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.academia.CampoEquipo
import com.esan.sportpro.domain.academia.CategoriasEquipo
import com.esan.sportpro.domain.academia.Equipo
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
                        onCambio = viewModel::onConfiguracion,
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
                if (state.equiposVisibles.isEmpty()) {
                    EmptyStateMessage(
                        mensaje = if (state.puedeEditar) {
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
                                puedeEditar = state.puedeEditar,
                                onEditar = { viewModel.editarEquipo(equipo) },
                                onCambiarEstado = { viewModel.cambiarEstado(equipo) },
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
            onElegirEscudo = viewModel::onEscudoElegido,
            onQuitarEscudo = viewModel::quitarEscudoNuevo,
            onGuardar = viewModel::guardar,
            onCancelar = viewModel::cerrarFormulario,
        )
    }
}

@Composable
private fun EquipoCard(
    equipo: Equipo,
    puedeEditar: Boolean,
    onEditar: () -> Unit,
    onCambiarEstado: () -> Unit,
) {
    TarjetaSportPro {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Rediseño: el escudo del equipo (subido o generado) en lugar del ícono genérico.
            EscudoEquipo(nombre = equipo.nombre, tamano = 52.dp, url = equipo.escudoUrl)
            Column(modifier = Modifier.weight(1f)) {
                Text(equipo.nombre, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Categoría ${equipo.categoria}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (equipo.entrenadorNombre.isNotBlank()) {
                    Text("DT: ${equipo.entrenadorNombre}", style = MaterialTheme.typography.bodySmall)
                }
                if (!equipo.activo) {
                    ChipEstado("Inactivo", TonoEstado.ERROR, modifier = Modifier.padding(top = 4.dp))
                }
            }
            if (puedeEditar) {
                IconButton(onClick = onEditar) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar equipo")
                }
                TextButton(onClick = onCambiarEstado) {
                    Text(if (equipo.activo) "Desactivar" else "Activar")
                }
            }
        }
    }
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                formulario.errores[CampoEquipo.CATEGORIA]?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Text("Entrenador (DT)", style = MaterialTheme.typography.labelLarge)
                if (entrenadores.isEmpty()) {
                    // Sin cuentas de entrenador todavía: se puede anotar el nombre, pero esa persona
                    // no verá las mensualidades del equipo hasta tener cuenta y quedar asignada.
                    OutlinedTextField(
                        value = formulario.entrenador,
                        onValueChange = onEntrenador,
                        label = { Text("Nombre del entrenador (opcional)") },
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
                        FilterChip(
                            selected = formulario.entrenadorUid.isBlank() && formulario.entrenador.isBlank(),
                            onClick = { onEntrenadorElegido(null) },
                            label = { Text("Sin asignar") },
                        )
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

/** US-004: nombre y día de corte de la academia, y códigos para registrar administradores. */
@Composable
private fun ConfiguracionYCodigos(
    state: AcademiaUiState,
    onEditar: () -> Unit,
    onGenerar: () -> Unit,
    onRevocar: (String) -> Unit,
) {
    val portapapeles = androidx.compose.ui.platform.LocalClipboardManager.current
    var verCodigos by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    val vigentes = state.codigos.filter { it.vigente() }
    val formato = remember { com.esan.sportpro.core.fecha.FechasSportPro.formatoFecha() }
    androidx.compose.material3.Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        state.configuracion.nombre.ifBlank { "Academia sin nombre" },
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        "Las mensualidades vencen el día ${state.configuracion.diaCorte} de cada mes",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                TextButton(onClick = onEditar) { Text("Editar") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Códigos de administrador: ${vigentes.size} " + if (vigentes.size == 1) "vigente" else "vigentes",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { verCodigos = !verCodigos }) { Text(if (verCodigos) "Ocultar" else "Ver") }
                TextButton(onClick = onGenerar) { Text("Generar") }
            }
            if (verCodigos) {
                if (vigentes.isEmpty()) {
                    Text("No hay códigos vigentes.", style = MaterialTheme.typography.bodySmall)
                }
                vigentes.forEach { c ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                c.codigo,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                            )
                            Text(
                                "Vence el " + (c.expiraEn?.let { formato.format(it) } ?: "—") + " · un solo uso",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        TextButton(onClick = { portapapeles.setText(androidx.compose.ui.text.AnnotatedString(c.codigo)) }) {
                            Text("Copiar")
                        }
                        TextButton(onClick = { onRevocar(c.codigo) }) { Text("Revocar") }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogoConfiguracion(
    formulario: FormularioConfiguracion,
    onCambio: (String, String) -> Unit,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!formulario.guardando) onCancelar() },
        title = { Text("Configuración de la academia") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = formulario.nombre,
                    onValueChange = { onCambio(it, formulario.diaCorteTexto) },
                    label = { Text("Nombre de la academia") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = formulario.diaCorteTexto,
                    onValueChange = { onCambio(formulario.nombre, it) },
                    label = { Text("Día de corte de las mensualidades") },
                    supportingText = { Text("Entre 1 y 28. Después de ese día, lo no pagado figura como vencido.") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                formulario.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton(onClick = onGuardar, enabled = !formulario.guardando) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onCancelar, enabled = !formulario.guardando) { Text("Cancelar") } },
    )
}
