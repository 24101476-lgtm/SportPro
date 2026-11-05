package com.esan.sportpro.ui.partido

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.core.fecha.FechasSportPro
import com.esan.sportpro.core.theme.EstilosSportPro
import com.esan.sportpro.core.theme.PaletaSportPro
import com.esan.sportpro.core.theme.SportProTema
import com.esan.sportpro.domain.partido.EstadoPartido
import com.esan.sportpro.domain.partido.Partido
import com.esan.sportpro.ui.common.BotonPrincipalSportPro
import com.esan.sportpro.ui.common.EscudoEquipo
import com.esan.sportpro.ui.common.EstadoVacioModulo
import com.esan.sportpro.ui.common.FondoFotoSportPro
import com.esan.sportpro.ui.common.FotoSportPro
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Filtro de la lista. Solo cambia lo que se muestra; los datos son los mismos. */
private enum class FiltroPartidos(val etiqueta: String) {
    TODOS("Todos"),
    EN_VIVO("En vivo"),
    PROGRAMADOS("Programados"),
    FINALIZADOS("Finalizados"),
}

@Composable
fun PartidosListaScreen(
    onAbrirPartido: (String) -> Unit,
    onAbrirCatalogo: () -> Unit,
    onNuevoPartido: () -> Unit,
    viewModel: PartidosViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var filtro by rememberSaveable { mutableStateOf(FiltroPartidos.TODOS) }

    LaunchedEffect(Unit) {
        viewModel.revisarPartidoEnCurso()
    }
    LaunchedEffect(state.abrirPartidoId) {
        state.abrirPartidoId?.let { id ->
            viewModel.navegacionRealizada()
            onAbrirPartido(id)
        }
    }
    LaunchedEffect(state.mensaje) {
        state.mensaje?.let {
            viewModel.mensajeMostrado()
            snackbar.showSnackbar(it)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when {
            state.cargando -> Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(color = SportProTema.colores.textoAcento)
                Text(
                    "Conectando con Firestore…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            state.error != null -> Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp),
                )
                Text("No se pudieron cargar los partidos", style = MaterialTheme.typography.titleMedium)
                Text(state.error.orEmpty(), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                BotonPrincipalSportPro(texto = "Reintentar", onClick = viewModel::escuchar)
            }

            else -> {
                val enVivo = state.partidos.filter { it.estado == EstadoPartido.EN_CURSO }
                val programados = state.partidos.filter { it.estado == EstadoPartido.PROGRAMADO }
                val finalizados = state.partidos.filter {
                    it.estado == EstadoPartido.FINALIZADO || it.estado == EstadoPartido.ACTA_CERRADA
                }
                // Agrupados por estado: lo que está en juego primero.
                val grupos = listOf(
                    Triple(FiltroPartidos.EN_VIVO, "En vivo", enVivo),
                    Triple(FiltroPartidos.PROGRAMADOS, "Programados", programados),
                    Triple(FiltroPartidos.FINALIZADOS, "Finalizados", finalizados),
                ).filter { (grupo, _, lista) -> lista.isNotEmpty() && (filtro == FiltroPartidos.TODOS || filtro == grupo) }

                LazyColumn(
                    contentPadding = PaddingValues(bottom = 112.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Encabezado(
                            partidos = state.partidos,
                            onNuevoPartido = onNuevoPartido,
                            onAbrirCatalogo = onAbrirCatalogo,
                        )
                    }
                    item {
                        Filtros(
                            actual = filtro,
                            conteo = mapOf(
                                FiltroPartidos.TODOS to state.partidos.size,
                                FiltroPartidos.EN_VIVO to enVivo.size,
                                FiltroPartidos.PROGRAMADOS to programados.size,
                                FiltroPartidos.FINALIZADOS to finalizados.size,
                            ),
                            onElegir = { filtro = it },
                        )
                    }
                    state.aviso?.let { aviso ->
                        item {
                            Text(
                                aviso,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 20.dp),
                            )
                        }
                    }
                    if (state.partidos.isEmpty()) {
                        item { EstadoVacio(state.puedeCrear) }
                    } else if (grupos.isEmpty()) {
                        item {
                            Text(
                                "No hay partidos en esta categoría.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                            )
                        }
                    }
                    grupos.forEach { (grupo, titulo, lista) ->
                        item(key = "titulo-$titulo") { TituloGrupo(titulo, lista.size) }
                        items(lista, key = { it.id }) { partido ->
                            val abrir = { onAbrirPartido(partido.id) }
                            when (grupo) {
                                FiltroPartidos.EN_VIVO -> TarjetaEnVivo(partido, abrir)
                                FiltroPartidos.PROGRAMADOS -> TarjetaProgramado(partido, abrir)
                                else -> FilaFinalizado(partido, abrir)
                            }
                        }
                    }
                }
            }
        }

        if (state.puedeCrear) {
            ExtendedFloatingActionButton(
                onClick = viewModel::abrirFormulario,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nuevo partido", fontWeight = FontWeight.Bold) },
                containerColor = SportProTema.colores.acento,
                contentColor = SportProTema.colores.sobreAcento,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp),
            )
        }
        SnackbarHost(
            snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 88.dp),
        )
    }

    state.formulario?.let { form ->
        NuevoPartidoSheet(state = state, form = form, viewModel = viewModel)
    }
}

/**
 * Encabezado: foto de los juveniles en B/N con velo musgo, título grande, resumen y las dos
 * acciones de la pantalla (programar y catálogo). El texto es blanco porque va sobre la foto.
 */
@Composable
private fun Encabezado(partidos: List<Partido>, onNuevoPartido: () -> Unit, onAbrirCatalogo: () -> Unit) {
    val enVivo = partidos.count { it.estado == EstadoPartido.EN_CURSO }
    FondoFotoSportPro(
        foto = FotoSportPro.JUVENILES,
        forma = RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp),
        velo = Brush.verticalGradient(
            0f to PaletaSportPro.MusgoProfundo.copy(alpha = 0.40f),
            1f to PaletaSportPro.MusgoProfundo.copy(alpha = 0.92f),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 22.dp)) {
            Text("Tus\npartidos", style = MaterialTheme.typography.displayMedium, color = Color.White)
            Text(
                when {
                    enVivo > 0 -> "${cantidad(enVivo, "partido", "partidos")} en vivo ahora"
                    partidos.isEmpty() -> "Registra eventos en vivo desde la cancha"
                    else -> cantidad(partidos.size, "partido registrado", "partidos registrados")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 8.dp, bottom = 18.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    onClick = onNuevoPartido,
                    shape = CircleShape,
                    color = Color.White,
                    contentColor = PaletaSportPro.Carbon,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Programar partido", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
                    }
                }
                Surface(
                    onClick = onAbrirCatalogo,
                    shape = CircleShape,
                    color = Color.Transparent,
                    contentColor = Color.White,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.55f)),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Icon(Icons.Default.ListAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Catálogo de eventos", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }
        }
        Text(
            "Que ruede\nla pelota",
            style = EstilosSportPro.NotaMano,
            color = PaletaSportPro.Lima,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 30.dp, end = 22.dp)
                .rotate(6f),
        )
    }
}

/** Chips de filtro con su cantidad; el elegido se rellena. */
@Composable
private fun Filtros(actual: FiltroPartidos, conteo: Map<FiltroPartidos, Int>, onElegir: (FiltroPartidos) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 4.dp),
    ) {
        FiltroPartidos.entries.forEach { opcion ->
            val elegido = opcion == actual
            Surface(
                onClick = { onElegir(opcion) },
                shape = CircleShape,
                color = if (elegido) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                contentColor = if (elegido) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground,
                border = if (elegido) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .semantics { selected = elegido },
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp)) {
                    Text(
                        "${opcion.etiqueta} ${conteo[opcion] ?: 0}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (elegido) FontWeight.Bold else FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun TituloGrupo(titulo: String, cantidad: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp),
    ) {
        Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text(
            "  $cantidad",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Partido en vivo: tarjeta lima con degradado, escudos, marcador grande y el punto que late. */
@Composable
private fun TarjetaEnVivo(partido: Partido, onClick: () -> Unit) {
    val carbon = PaletaSportPro.Carbon
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        contentColor = carbon,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(PaletaSportPro.LimaSuave, PaletaSportPro.Lima)))
                .padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = carbon) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        PuntoEnVivo()
                        Text(
                            if (partido.enDescanso) "Descanso" else "En vivo",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = PaletaSportPro.Lima,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                partido.categoria?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 14.dp, bottom = 12.dp)) {
                EquipoCentro(partido.nombreEquipo, Modifier.weight(1f))
                Text(
                    "${partido.golesEquipo} – ${partido.golesRival}",
                    style = EstilosSportPro.NumeroGrande,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
                EquipoCentro(partido.nombreRival, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(carbon.copy(alpha = 0.14f)))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                Text("Seguir el partido", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(12.dp), color = carbon, modifier = Modifier.size(42.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.NorthEast, contentDescription = null, tint = PaletaSportPro.Lima, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EquipoCentro(nombre: String, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        EscudoEquipo(nombre = nombre, tamano = 64.dp)
        Text(nombre, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** Partido programado: fecha grande a la izquierda y los escudos frente a frente. */
@Composable
private fun TarjetaProgramado(partido: Partido, onClick: () -> Unit) {
    val fecha = partido.fechaMs?.let { Instant.ofEpochMilli(it).atZone(FechasSportPro.ZONA).toLocalDate() }
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(14.dp)) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(width = 64.dp, height = 76.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(fecha?.dayOfMonth?.toString()?.padStart(2, '0') ?: "–", style = EstilosSportPro.NumeroMediano, color = SportProTema.colores.textoAcento)
                    Text(fecha?.let { diaSemana(it) } ?: "", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EscudoEquipo(partido.nombreEquipo, tamano = 30.dp)
                    Text("vs", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    EscudoEquipo(partido.nombreRival, tamano = 30.dp)
                }
                Text(
                    "${partido.nombreEquipo} vs ${partido.nombreRival}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(partido.categoria?.takeIf { it.isNotBlank() }, partido.hora?.takeIf { it.isNotBlank() }, partido.lugar?.takeIf { it.isNotBlank() })
                        .joinToString(", ")
                        .ifBlank { "Detalles por confirmar" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Partido finalizado: fila compacta con escudos, marcador y estado del acta. */
@Composable
private fun FilaFinalizado(partido: Partido, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    listOfNotNull(partido.categoria?.takeIf { it.isNotBlank() }, partido.fechaMs?.let { formatearFecha(it) }).joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                EstadoPartidoChip(partido)
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
                EscudoEquipo(partido.nombreEquipo, tamano = 36.dp)
                Text(
                    partido.nombreEquipo,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 10.dp),
                )
                Text(
                    "${partido.golesEquipo} – ${partido.golesRival}",
                    style = EstilosSportPro.NumeroMediano,
                    modifier = Modifier.padding(horizontal = 10.dp),
                )
                Text(
                    partido.nombreRival,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f).padding(end = 10.dp),
                )
                EscudoEquipo(partido.nombreRival, tamano = 36.dp)
            }
        }
    }
}

@Composable
private fun EstadoVacio(puedeCrear: Boolean) {
    EstadoVacioModulo(
        titulo = "Aún no hay partidos",
        mensaje = if (puedeCrear) {
            "Toca \"Nuevo partido\" para programar uno con un equipo de tu academia."
        } else {
            "Cuando el entrenador programe un partido, aparecerá aquí y podrás seguirlo en vivo."
        },
        icono = Icons.Default.SportsSoccer,
    )
}

private val FORMATO_DIA_SEMANA: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE", Locale.forLanguageTag("es-PE"))

/** "Sáb", "Jue"… sin el punto que agrega el formato abreviado. */
private fun diaSemana(fecha: LocalDate): String =
    fecha.format(FORMATO_DIA_SEMANA).removeSuffix(".").replaceFirstChar { it.uppercase() }
