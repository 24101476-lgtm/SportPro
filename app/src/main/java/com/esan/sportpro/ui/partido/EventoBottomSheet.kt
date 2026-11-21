package com.esan.sportpro.ui.partido

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.esan.sportpro.domain.partido.Campos
import com.esan.sportpro.domain.partido.EquipoEvento
import com.esan.sportpro.domain.partido.EstadoEvento
import com.esan.sportpro.domain.partido.JugadorConvocado
import com.esan.sportpro.domain.partido.JugadorEspecial
import com.esan.sportpro.domain.partido.ReglasPartido
import com.esan.sportpro.domain.partido.ResultadoPenal
import com.esan.sportpro.domain.partido.TiposEvento

/** Lo que ya lleva cada jugador en el partido, para mostrarlo en su ficha del selector. */
private data class MarcasJugador(val goles: Int = 0, val amarillas: Int = 0, val roja: Boolean = false)

/**
 * Hoja inferior que solicita solo los campos obligatorios del tipo de evento.
 * El minuto y el equipo propio vienen precargados para registrar en máximo tres toques.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventoBottomSheet(
    hoja: HojaEvento,
    state: RegistroUiState,
    viewModel: RegistroPartidoViewModel,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val partido = state.partido
    val tipo = hoja.tipo
    val esCambio = tipo.id == TiposEvento.CAMBIO
    val esGol = tipo.id == TiposEvento.GOL
    val acento = colorTipo(tipo.id)

    // Para una corrección, las listas se calculan sin el propio evento.
    val eventosBase = hoja.editando?.let { ed -> state.eventos.filter { it.grupoId != ed.grupoId } } ?: state.eventos
    val enCanchaIds = ReglasPartido.enCancha(state.convocados, eventosBase)
    val enCancha = state.convocados.filter { it.id in enCanchaIds }
    val suplentes = ReglasPartido.suplentesDisponibles(state.convocados, eventosBase)
    val propio = hoja.equipo != EquipoEvento.RIVAL
    val marcas = remember(eventosBase) { marcasPorJugador(eventosBase) }

    var mostrarObservacion by remember(hoja.tipo.id, hoja.editando?.id) {
        mutableStateOf(hoja.observacion.isNotBlank())
    }

    ModalBottomSheet(
        onDismissRequest = viewModel::cerrarHoja,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            EncabezadoEvento(
                tipoId = tipo.id,
                nombre = tipo.nombre,
                subtitulo = if (hoja.editando != null) "Corregir evento" else "Registrar evento",
                minutoTexto = hoja.minutoTexto,
                onMinuto = viewModel::onMinuto,
            )

            // Los eventos de control del tiempo cambian el estado del partido: se explica qué pasará.
            val explicacion = when (tipo.id) {
                TiposEvento.INICIO_PARTIDO -> "El cronómetro empezará a correr en todos los dispositivos."
                TiposEvento.FIN_PRIMER_TIEMPO -> "El reloj se detiene y comienza el descanso."
                TiposEvento.INICIO_SEGUNDO_TIEMPO -> "El reloj continúa desde el minuto ${partido?.duracionTiempoMin ?: 45}."
                TiposEvento.FIN_PARTIDO ->
                    "El partido pasará a Finalizado. Podrás corregir eventos hasta cerrar el acta."
                else -> null
            }
            if (explicacion != null && hoja.editando == null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        explicacion,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            }

            if (tipo.requiere(Campos.EQUIPO)) {
                Bloque("Equipo") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OpcionEquipo(
                            nombre = partido?.nombreEquipo ?: "Propio",
                            seleccionado = hoja.equipo == EquipoEvento.PROPIO,
                            acento = acento,
                            modifier = Modifier.weight(1f),
                        ) { viewModel.onEquipo(EquipoEvento.PROPIO) }
                        OpcionEquipo(
                            nombre = partido?.nombreRival ?: "Rival",
                            seleccionado = hoja.equipo == EquipoEvento.RIVAL,
                            acento = acento,
                            modifier = Modifier.weight(1f),
                        ) { viewModel.onEquipo(EquipoEvento.RIVAL) }
                    }
                }
            }

            if (tipo.requiere(Campos.JUGADOR) || tipo.requiere(Campos.JUGADOR_SALE)) {
                SelectorJugador(
                    titulo = if (esCambio) "Sale" else if (esGol) "¿Quién anotó?" else "Jugador",
                    // En un cambio solo puede salir alguien que está en cancha.
                    enCancha = if (propio) enCancha else emptyList(),
                    suplentes = if (propio && !esCambio) suplentes else emptyList(),
                    incluirRival = !esCambio || !propio,
                    seleccionado = hoja.jugadorId,
                    fotos = state.fotosJugadores,
                    marcas = marcas,
                    acento = if (esCambio) ColoresPartido.Rojo else acento,
                    onSeleccionar = viewModel::onJugador,
                )
            }

            if (esCambio && tipo.requiere(Campos.JUGADOR_ENTRA)) {
                SelectorJugador(
                    titulo = "Entra",
                    enCancha = emptyList(),
                    suplentes = if (propio) suplentes else emptyList(),
                    incluirRival = !propio,
                    seleccionado = hoja.secundarioId,
                    fotos = state.fotosJugadores,
                    marcas = marcas,
                    acento = ColoresPartido.Verde,
                    onSeleccionar = { id, nombre -> viewModel.onSecundario(id, nombre) },
                )
            }

            if (esGol) {
                Bloque("Asistencia (opcional)") {
                    val candidatos = if (propio) enCancha.filter { it.id != hoja.jugadorId } else emptyList()
                    CuadriculaJugadores(
                        jugadores = candidatos,
                        seleccionado = hoja.secundarioId,
                        fotos = state.fotosJugadores,
                        marcas = marcas,
                        acento = acento,
                        primero = {
                            FichaEspecial(
                                texto = "Sin asistencia",
                                icono = Icons.Default.Close,
                                seleccionado = hoja.secundarioId == null,
                                acento = acento,
                                modifier = it,
                            ) { viewModel.onSecundario(null, null) }
                        },
                        onSeleccionar = { id, nombre -> viewModel.onSecundario(id, nombre) },
                    )
                }
            }

            if (tipo.requiere(Campos.RESULTADO)) {
                Bloque("Resultado") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OpcionGrande(
                            texto = "Convertido",
                            icono = Icons.Default.Check,
                            color = ColoresPartido.Verde,
                            seleccionado = hoja.resultado == ResultadoPenal.CONVERTIDO,
                            modifier = Modifier.weight(1f),
                        ) { viewModel.onResultado(ResultadoPenal.CONVERTIDO) }
                        OpcionGrande(
                            texto = "Fallado",
                            icono = Icons.Default.Close,
                            color = ColoresPartido.Rojo,
                            seleccionado = hoja.resultado == ResultadoPenal.FALLADO,
                            modifier = Modifier.weight(1f),
                        ) { viewModel.onResultado(ResultadoPenal.FALLADO) }
                    }
                }
            }

            if (mostrarObservacion) {
                OutlinedTextField(
                    value = hoja.observacion,
                    onValueChange = viewModel::onObservacion,
                    label = { Text("Observación (opcional)") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                TextButton(onClick = { mostrarObservacion = true }) {
                    Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Agregar observación")
                }
            }

            if (hoja.editando != null) {
                OutlinedTextField(
                    value = hoja.motivo,
                    onValueChange = viewModel::onMotivo,
                    label = { Text("Motivo de la corrección") },
                    supportingText = {
                        Text("${hoja.motivo.trim().length}/${ReglasPartido.MOTIVO_MAX} · mínimo ${ReglasPartido.MOTIVO_MIN}")
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            hoja.error?.let {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.errorContainer) {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BotonSecundario("Cancelar", viewModel::cerrarHoja, Modifier.weight(1f))
                val esAmarilla = tipo.id == TiposEvento.TARJETA_AMARILLA
                Button(
                    onClick = viewModel::guardarHoja,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = acento,
                        contentColor = if (esAmarilla) Color(0xFF1A1A1A) else Color.White,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp),
                ) {
                    Text(
                        when {
                            hoja.editando != null -> "Guardar corrección"
                            explicacion != null -> "Confirmar"
                            else -> "Guardar"
                        },
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

private fun marcasPorJugador(eventos: List<com.esan.sportpro.domain.partido.EventoPartido>): Map<String, MarcasJugador> {
    val vigentes = eventos.filter { it.estado == EstadoEvento.VIGENTE && it.equipo != EquipoEvento.RIVAL }
    val mapa = HashMap<String, MarcasJugador>()
    vigentes.forEach { e ->
        val id = e.jugadorId ?: return@forEach
        val actual = mapa[id] ?: MarcasJugador()
        mapa[id] = when (e.tipoId) {
            TiposEvento.GOL -> actual.copy(goles = actual.goles + 1)
            TiposEvento.PENAL ->
                if (e.resultadoPenal == ResultadoPenal.CONVERTIDO) actual.copy(goles = actual.goles + 1) else actual
            TiposEvento.TARJETA_AMARILLA -> actual.copy(amarillas = actual.amarillas + 1)
            TiposEvento.TARJETA_ROJA -> actual.copy(roja = true)
            else -> actual
        }
    }
    return mapa
}

// ============================================================
// ENCABEZADO
// ============================================================

@Composable
private fun EncabezadoEvento(
    tipoId: String,
    nombre: String,
    subtitulo: String,
    minutoTexto: String,
    onMinuto: (String) -> Unit,
) {
    val color = colorTipo(tipoId)
    val tinta = if (tipoId == TiposEvento.TARJETA_AMARILLA) Color(0xFF1A1A1A) else Color.White

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(color, color.copy(alpha = 0.78f))))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(tinta.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                SimboloEvento(tipoId = tipoId, tamano = 30.dp, tinta = tinta)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(subtitulo, style = MaterialTheme.typography.labelMedium, color = tinta.copy(alpha = 0.8f))
                Text(
                    nombre,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = tinta,
                    maxLines = 2,
                )
            }
            SelectorMinuto(minutoTexto = minutoTexto, tinta = tinta, onMinuto = onMinuto)
        }
    }
}

/** Minuto con − / + para corregir rápido sin abrir el teclado; también se puede escribir. */
@Composable
private fun SelectorMinuto(minutoTexto: String, tinta: Color, onMinuto: (String) -> Unit) {
    val actual = minutoTexto.toIntOrNull() ?: 0
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(tinta.copy(alpha = 0.16f))
                .padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            BotonMinuto(Icons.Default.Remove, tinta) {
                onMinuto((actual - 1).coerceAtLeast(ReglasPartido.MINUTO_MIN).toString())
            }
            BasicTextField(
                value = minutoTexto,
                onValueChange = { onMinuto(it.filter(Char::isDigit).take(3)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(
                    color = tinta,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                ),
                cursorBrush = SolidColor(tinta),
                modifier = Modifier.width(40.dp),
            )
            BotonMinuto(Icons.Default.Add, tinta) {
                onMinuto((actual + 1).coerceAtMost(ReglasPartido.MINUTO_MAX).toString())
            }
        }
        Text(
            "MINUTO",
            style = MaterialTheme.typography.labelSmall,
            color = tinta.copy(alpha = 0.75f),
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
private fun BotonMinuto(icono: ImageVector, tinta: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(tinta.copy(alpha = 0.18f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icono, contentDescription = null, tint = tinta, modifier = Modifier.size(18.dp))
    }
}

/**
 * Símbolo del evento: las tarjetas se dibujan como tarjetas reales (inclinadas, con su color);
 * el resto usa su ícono.
 */
@Composable
fun SimboloEvento(tipoId: String, tamano: Dp, tinta: Color = Color.White) {
    when (tipoId) {
        TiposEvento.TARJETA_AMARILLA, TiposEvento.TARJETA_ROJA -> {
            Box(
                modifier = Modifier
                    .rotate(-12f)
                    .size(width = tamano * 0.68f, height = tamano)
                    .clip(RoundedCornerShape(tamano * 0.12f))
                    .background(colorTipo(tipoId))
                    .border(2.dp, Color.White, RoundedCornerShape(tamano * 0.12f)),
            )
        }
        else -> Icon(iconoTipo(tipoId), contentDescription = null, tint = tinta, modifier = Modifier.size(tamano))
    }
}

// ============================================================
// EQUIPO / OPCIONES
// ============================================================

@Composable
private fun Bloque(titulo: String, contenido: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            titulo.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.8.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        contenido()
    }
}

@Composable
private fun OpcionEquipo(
    nombre: String,
    seleccionado: Boolean,
    acento: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (seleccionado) acento.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(if (seleccionado) 2.dp else 1.dp, if (seleccionado) acento else MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.heightIn(min = 60.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Escudo(nombre = nombre, fondo = acento, tamano = 34.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                nombre,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun OpcionGrande(
    texto: String,
    icono: ImageVector,
    color: Color,
    seleccionado: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (seleccionado) color else color.copy(alpha = 0.10f),
        contentColor = if (seleccionado) Color.White else color,
        modifier = modifier.heightIn(min = 64.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp),
        ) {
            Icon(icono, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(texto, fontWeight = FontWeight.Bold)
        }
    }
}

// ============================================================
// JUGADORES
// ============================================================

@Composable
private fun SelectorJugador(
    titulo: String,
    enCancha: List<JugadorConvocado>,
    suplentes: List<JugadorConvocado>,
    incluirRival: Boolean,
    seleccionado: String?,
    fotos: Map<String, String>,
    marcas: Map<String, MarcasJugador>,
    acento: Color,
    onSeleccionar: (String, String) -> Unit,
) {
    Bloque(titulo) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (enCancha.isNotEmpty()) {
                Subtitulo("En cancha · ${enCancha.size}")
                CuadriculaJugadores(enCancha, seleccionado, fotos, marcas, acento, onSeleccionar = onSeleccionar)
            }
            if (suplentes.isNotEmpty()) {
                Subtitulo("Banco · ${suplentes.size}")
                CuadriculaJugadores(suplentes, seleccionado, fotos, marcas, acento, onSeleccionar = onSeleccionar)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (incluirRival) {
                    PildoraEspecial(
                        texto = "Jugador del rival",
                        icono = Icons.Default.Groups,
                        seleccionado = seleccionado == JugadorEspecial.RIVAL,
                        acento = acento,
                        modifier = Modifier.weight(1f),
                    ) { onSeleccionar(JugadorEspecial.RIVAL, JugadorEspecial.NOMBRE_RIVAL) }
                }
                PildoraEspecial(
                    texto = "No identificado",
                    icono = Icons.Default.HelpOutline,
                    seleccionado = seleccionado == JugadorEspecial.NO_IDENTIFICADO,
                    acento = acento,
                    modifier = Modifier.weight(1f),
                ) { onSeleccionar(JugadorEspecial.NO_IDENTIFICADO, JugadorEspecial.NOMBRE_NO_IDENTIFICADO) }
            }
        }
    }
}

@Composable
private fun Subtitulo(texto: String) {
    Text(texto, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Cuadrícula de 4 columnas: los 11 de la cancha entran a la vista sin desplazar de lado. */
@Composable
private fun CuadriculaJugadores(
    jugadores: List<JugadorConvocado>,
    seleccionado: String?,
    fotos: Map<String, String>,
    marcas: Map<String, MarcasJugador>,
    acento: Color,
    primero: (@Composable (Modifier) -> Unit)? = null,
    onSeleccionar: (String, String) -> Unit,
) {
    val celdas: List<@Composable (Modifier) -> Unit> = buildList {
        primero?.let { add(it) }
        jugadores.forEach { j ->
            add { m ->
                FichaSeleccion(
                    jugador = j,
                    fotoUrl = fotos[j.id].orEmpty(),
                    marcas = marcas[j.id] ?: MarcasJugador(),
                    seleccionado = seleccionado == j.id,
                    acento = acento,
                    modifier = m,
                ) { onSeleccionar(j.id, j.nombre) }
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        celdas.chunked(4).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fila.forEach { celda -> celda(Modifier.weight(1f)) }
                repeat(4 - fila.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun FichaSeleccion(
    jugador: JugadorConvocado,
    fotoUrl: String,
    marcas: MarcasJugador,
    seleccionado: Boolean,
    acento: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (seleccionado) acento.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = if (seleccionado) BorderStroke(2.dp, acento) else null,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 10.dp, bottom = 8.dp, start = 4.dp, end = 4.dp),
        ) {
            Box {
                AvatarJugador(nombre = jugador.nombre, fotoUrl = fotoUrl, tamano = 50.dp, anillo = if (seleccionado) acento else null)

                // Dorsal.
                Text(
                    jugador.dorsal?.toString() ?: "–",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = (-4).dp, y = 2.dp)
                        .background(Color(0xFF13261A), RoundedCornerShape(6.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                )

                // Seleccionado.
                if (seleccionado) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-2).dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(acento)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                } else {
                    MarcasMini(marcas, Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-2).dp))
                }
            }

            Text(
                jugador.nombre.trim().substringBefore(" ").replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (seleccionado) FontWeight.Black else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                abreviaturaPosicion(jugador.posicion),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = colorPosicion(jugador.posicion),
                maxLines = 1,
            )
        }
    }
}

/** Tarjetas y goles que ya lleva el jugador: ayuda a no olvidar una segunda amarilla. */
@Composable
private fun MarcasMini(marcas: MarcasJugador, modifier: Modifier) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = modifier) {
        if (marcas.roja) {
            TarjetaMini(ColoresPartido.Rojo)
        } else {
            repeat(marcas.amarillas.coerceAtMost(2)) { TarjetaMini(ColoresPartido.Amarillo) }
        }
        if (marcas.goles > 0) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, ColoresPartido.Verde, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (marcas.goles == 1) {
                    Icon(Icons.Default.SportsSoccer, contentDescription = "1 gol", tint = ColoresPartido.Verde, modifier = Modifier.size(12.dp))
                } else {
                    Text("${marcas.goles}", fontSize = 9.sp, fontWeight = FontWeight.Black, color = ColoresPartido.Verde)
                }
            }
        }
    }
}

@Composable
private fun TarjetaMini(color: Color) {
    Box(
        modifier = Modifier
            .size(width = 10.dp, height = 14.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(color)
            .border(1.dp, Color.White, RoundedCornerShape(2.dp)),
    )
}

@Composable
fun AvatarJugador(nombre: String, fotoUrl: String, tamano: Dp, anillo: Color? = null) {
    Box(
        modifier = Modifier
            .size(tamano)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(listOf(Color(0xFFFFE9A3), Color(0xFFE9B949)))
            )
            .then(if (anillo != null) Modifier.border(2.5.dp, anillo, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (fotoUrl.isNotBlank()) {
            AsyncImage(
                model = fotoUrl,
                contentDescription = "Foto de $nombre",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                nombre.trim().split(Regex("\\s+")).mapNotNull { it.firstOrNull()?.uppercaseChar() }.take(2).joinToString("").ifBlank { "?" },
                color = Color(0xFF13261A),
                fontWeight = FontWeight.Black,
                fontSize = (tamano.value * 0.32f).sp,
            )
        }
    }
}

@Composable
private fun FichaEspecial(
    texto: String,
    icono: ImageVector,
    seleccionado: Boolean,
    acento: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (seleccionado) acento.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = if (seleccionado) BorderStroke(2.dp, acento) else null,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 10.dp, bottom = 8.dp, start = 4.dp, end = 4.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                texto,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun PildoraEspecial(
    texto: String,
    icono: ImageVector,
    seleccionado: Boolean,
    acento: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (seleccionado) acento.copy(alpha = 0.16f) else Color.Transparent,
        border = BorderStroke(if (seleccionado) 2.dp else 1.dp, if (seleccionado) acento else MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.heightIn(min = 48.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Icon(icono, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                texto,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
