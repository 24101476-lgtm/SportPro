package com.esan.sportpro.ui.entrenamientos

import com.esan.sportpro.ui.common.BarraSuperiorSportPro
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.esan.sportpro.domain.entrenamientos.CampoBiblioteca
import com.esan.sportpro.domain.entrenamientos.CategoriaTecnica
import com.esan.sportpro.domain.entrenamientos.EjercicioBiblioteca
import com.esan.sportpro.domain.entrenamientos.RangoDuracion
import com.esan.sportpro.domain.entrenamientos.ValidacionBiblioteca
import com.esan.sportpro.ui.common.CampoFormulario
import com.esan.sportpro.ui.common.EmptyStateMessage

/**
 * US-009 — Biblioteca reutilizable de ejercicios (Figma node 10-53). Se abre desde el módulo
 * Entrenamientos. Navegación interna por estado: lista (buscador) → detalle → formulario.
 *
 * @param onSalir vuelve a la lista de entrenamientos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibliotecaEjerciciosScreen(
    onSalir: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BibliotecaEjerciciosViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { evento ->
            if (evento is BibliotecaEvent.Mensaje) snackbarHostState.showSnackbar(evento.texto)
        }
    }

    val atras: () -> Unit = { if (!viewModel.atras()) onSalir() }
    BackHandler(onBack = atras)

    val pantalla = state.pantalla
    val titulo = when (pantalla) {
        PantallaBiblioteca.Lista -> "Biblioteca de ejercicios"
        is PantallaBiblioteca.Detalle -> "Ejercicio"
        PantallaBiblioteca.Formulario -> if (state.formulario.id == null) "Nuevo ejercicio" else "Editar ejercicio"
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            BarraSuperiorSportPro(
                title = { Text(titulo) },
                navigationIcon = {
                    IconButton(onClick = atras) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
        floatingActionButton = {
            if (pantalla == PantallaBiblioteca.Lista && state.puedeGestionar && !state.cargando) {
                FloatingActionButton(onClick = viewModel::nuevoEjercicio) {
                    Icon(Icons.Default.Add, contentDescription = "Nuevo ejercicio")
                }
            }
        },
    ) { padding ->
        val contenido = Modifier.padding(padding)
        when {
            state.cargando -> Box(contenido.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            state.error != null -> EmptyStateMessage(mensaje = state.error.orEmpty(), modifier = contenido)

            else -> when (pantalla) {
                PantallaBiblioteca.Lista -> ListaBiblioteca(state, viewModel, contenido)

                is PantallaBiblioteca.Detalle -> {
                    val ejercicio = state.ejercicio(pantalla.ejercicioId)
                    if (ejercicio == null) {
                        EmptyStateMessage("No se encontró el ejercicio.", contenido)
                    } else {
                        DetalleBiblioteca(state, ejercicio, viewModel, contenido)
                    }
                }

                PantallaBiblioteca.Formulario -> FormularioBibliotecaContenido(state, viewModel, contenido)
            }
        }
    }

    state.confirmacion?.let { confirmacion ->
        val (tituloDialogo, texto, accion) = when (confirmacion) {
            is ConfirmacionBiblioteca.Eliminar -> Triple(
                "Eliminar ejercicio",
                "\"${confirmacion.ejercicio.nombre}\" se eliminará de la biblioteca. Esta acción no se puede deshacer.",
                "Eliminar",
            )
            is ConfirmacionBiblioteca.Desactivar -> Triple(
                "Desactivar ejercicio",
                if (confirmacion.porUsoEnSesiones) {
                    "\"${confirmacion.ejercicio.nombre}\" se usó en sesiones ya realizadas, por eso no puede " +
                        "eliminarse. Se desactivará: dejará de aparecer en el buscador y las sesiones " +
                        "históricas lo conservarán."
                } else {
                    "\"${confirmacion.ejercicio.nombre}\" dejará de aparecer en el buscador. Podrás reactivarlo después."
                },
                "Desactivar",
            )
        }
        AlertDialog(
            onDismissRequest = viewModel::cancelarConfirmacion,
            title = { Text(tituloDialogo) },
            text = { Text(texto) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmar, enabled = !state.procesando) { Text(accion) }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelarConfirmacion) { Text("Cancelar") } },
        )
    }
}

// ---- Lista y buscador ------------------------------------------------------------------------

@Composable
private fun ListaBiblioteca(
    state: BibliotecaUiState,
    viewModel: BibliotecaEjerciciosViewModel,
    modifier: Modifier = Modifier,
) {
    val filtro = state.filtro
    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = filtro.texto,
            onValueChange = viewModel::onTextoBusqueda,
            placeholder = { Text("Buscar por nombre") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CategoriaTecnica.entries.forEach { categoria ->
                FilterChip(
                    selected = filtro.categoria == categoria,
                    onClick = { viewModel.onFiltroCategoria(categoria) },
                    label = { Text(categoria.etiqueta) },
                )
            }
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RangoDuracion.entries.forEach { rango ->
                FilterChip(
                    selected = filtro.duracion == rango,
                    onClick = { viewModel.onFiltroDuracion(rango) },
                    label = { Text(rango.etiqueta) },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Ver desactivados", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            if (filtro.activo) TextButton(onClick = viewModel::limpiarFiltros) { Text("Limpiar filtros") }
            Switch(checked = state.mostrarDesactivados, onCheckedChange = viewModel::onMostrarDesactivados)
        }

        val visibles = state.visibles
        when {
            visibles.isNotEmpty() -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(visibles, key = { it.id }) { ejercicio ->
                    EjercicioBibliotecaCard(ejercicio, onClick = { viewModel.abrirDetalle(ejercicio.id) })
                }
            }

            filtro.activo -> EmptyStateMessage(
                mensaje = "No hay ejercicios que coincidan con esos filtros.",
                icono = Icons.Default.SearchOff,
            )

            state.mostrarDesactivados -> EmptyStateMessage(
                mensaje = "No tienes ejercicios desactivados.",
                icono = Icons.Default.FitnessCenter,
            )

            else -> EmptyStateMessage(
                mensaje = "La biblioteca está vacía. Crea el primer ejercicio con el botón +.",
                icono = Icons.Default.FitnessCenter,
            )
        }
    }
}

@Composable
private fun EjercicioBibliotecaCard(ejercicio: EjercicioBiblioteca, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Miniatura(ejercicio.imagenUrl, Modifier.size(56.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(ejercicio.nombre, style = MaterialTheme.typography.titleSmall)
                Text(
                    listOfNotNull(
                        ejercicio.categoriaTecnica?.etiqueta,
                        "${ejercicio.duracionMin} min",
                        ejercicio.minJugadores.takeIf { it > 0 }?.let { "mín. $it jugadores" },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!ejercicio.activo) {
                    Text("Desactivado", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun Miniatura(url: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (url.isNotBlank()) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

// ---- Detalle ---------------------------------------------------------------------------------

@Composable
private fun DetalleBiblioteca(
    state: BibliotecaUiState,
    ejercicio: EjercicioBiblioteca,
    viewModel: BibliotecaEjerciciosViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (ejercicio.imagenUrl.isNotBlank()) {
            AsyncImage(
                model = ejercicio.imagenUrl,
                contentDescription = "Imagen del ejercicio",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp)),
            )
        }
        Text(ejercicio.nombre, style = MaterialTheme.typography.titleLarge)
        if (!ejercicio.activo) {
            Text(
                "Desactivado: no aparece en el buscador.",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Dato("Categoría técnica", ejercicio.categoriaTecnica?.etiqueta ?: "Sin categoría")
        Dato("Duración sugerida", "${ejercicio.duracionMin} minutos")
        Dato("Mínimo de jugadores", ejercicio.minJugadores.takeIf { it > 0 }?.toString() ?: "—")
        Dato("Objetivo", ejercicio.objetivo.ifBlank { "—" })
        Dato("Materiales", ejercicio.materiales.ifBlank { "Ninguno" })
        Dato("Descripción", ejercicio.descripcion.ifBlank { "—" })

        if (state.puedeEditar(ejercicio)) {
            if (ejercicio.activo) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(onClick = { viewModel.solicitarDesactivar(ejercicio) }, modifier = Modifier.weight(1f)) {
                        Text("Desactivar")
                    }
                    Button(onClick = { viewModel.editarEjercicio(ejercicio) }, modifier = Modifier.weight(1f)) {
                        Text("Editar")
                    }
                }
                TextButton(onClick = { viewModel.solicitarEliminar(ejercicio) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            } else {
                Button(onClick = { viewModel.reactivar(ejercicio) }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Text("Reactivar")
                }
            }
        } else {
            Text(
                "Solo el autor del ejercicio o el administrador del club pueden editarlo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String) {
    Column(modifier = Modifier.padding(top = 4.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(valor, style = MaterialTheme.typography.bodyMedium)
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
    }
}

// ---- Formulario ------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormularioBibliotecaContenido(
    state: BibliotecaUiState,
    viewModel: BibliotecaEjerciciosViewModel,
    modifier: Modifier = Modifier,
) {
    val formulario = state.formulario
    val datos = formulario.datos
    val errores = formulario.errores
    val selectorImagen = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.onImagenElegida(uri.toString())
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CampoFormulario(
            valor = datos.nombre,
            onCambio = { v -> viewModel.onCampoChanged(CampoBiblioteca.NOMBRE) { copy(nombre = v) } },
            etiqueta = "Nombre",
            error = errores[CampoBiblioteca.NOMBRE],
            ayuda = "${datos.nombre.length}/${ValidacionBiblioteca.NOMBRE_MAX}",
        )
        CampoFormulario(
            valor = datos.objetivo,
            onCambio = { v -> viewModel.onCampoChanged(CampoBiblioteca.OBJETIVO) { copy(objetivo = v) } },
            etiqueta = "Objetivo",
            error = errores[CampoBiblioteca.OBJETIVO],
            minLineas = 2,
        )

        Text("Categoría técnica", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CategoriaTecnica.entries.forEach { categoria ->
                FilterChip(
                    selected = datos.categoria == categoria,
                    onClick = { viewModel.onCampoChanged(CampoBiblioteca.CATEGORIA) { copy(categoria = categoria) } },
                    label = { Text(categoria.etiqueta) },
                )
            }
        }
        errores[CampoBiblioteca.CATEGORIA]?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CampoFormulario(
                valor = datos.duracion,
                onCambio = { v -> viewModel.onCampoChanged(CampoBiblioteca.DURACION) { copy(duracion = v) } },
                etiqueta = "Duración (min)",
                error = errores[CampoBiblioteca.DURACION],
                ayuda = "1 a ${ValidacionBiblioteca.DURACION_MAX}",
                teclado = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
            CampoFormulario(
                valor = datos.minJugadores,
                onCambio = { v -> viewModel.onCampoChanged(CampoBiblioteca.MIN_JUGADORES) { copy(minJugadores = v) } },
                etiqueta = "Mín. jugadores",
                error = errores[CampoBiblioteca.MIN_JUGADORES],
                teclado = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
        }
        CampoFormulario(
            valor = datos.materiales,
            onCambio = { v -> viewModel.onCampoChanged(CampoBiblioteca.MATERIALES) { copy(materiales = v) } },
            etiqueta = "Materiales (opcional)",
            error = errores[CampoBiblioteca.MATERIALES],
            ayuda = "Ej.: 10 conos, 4 balones, petos",
        )
        CampoFormulario(
            valor = datos.descripcion,
            onCambio = { v -> viewModel.onCampoChanged(CampoBiblioteca.DESCRIPCION) { copy(descripcion = v) } },
            etiqueta = "Descripción",
            error = errores[CampoBiblioteca.DESCRIPCION],
            ayuda = ValidacionBiblioteca.caracteresRestantes(datos.descripcion),
            minLineas = 4,
        )

        Text("Imagen (opcional)", style = MaterialTheme.typography.labelLarge)
        val imagen = formulario.imagenVisible
        if (imagen != null) {
            AsyncImage(
                model = imagen,
                contentDescription = "Vista previa",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp)),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { selectorImagen.launch("image/*") }) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                Text(if (imagen == null) " Elegir imagen" else " Cambiar imagen")
            }
            if (imagen != null) TextButton(onClick = viewModel::quitarImagen) { Text("Quitar") }
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
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Guardar")
                }
            }
        }
    }
}
