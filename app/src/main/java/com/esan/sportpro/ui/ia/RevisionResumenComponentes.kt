package com.esan.sportpro.ui.ia

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.esan.sportpro.domain.ia.CampoBloque
import com.esan.sportpro.domain.ia.CampoEvaluacion
import com.esan.sportpro.domain.ia.EstadoResumen
import com.esan.sportpro.domain.ia.EvaluacionResumen
import com.esan.sportpro.domain.ia.MencionNoVerificada
import com.esan.sportpro.domain.ia.ReglasEvaluacion
import com.esan.sportpro.domain.ia.ReglasResumen
import com.esan.sportpro.domain.ia.ReglasRevision
import com.esan.sportpro.domain.ia.ResumenPartido
import com.esan.sportpro.domain.partido.Partido
import com.esan.sportpro.domain.partido.ReglasPartido
import com.esan.sportpro.ui.partido.ColoresPartido
import com.esan.sportpro.ui.partido.Etiqueta
import com.esan.sportpro.ui.partido.IconoEvento
import com.esan.sportpro.ui.partido.cantidad
import com.esan.sportpro.ui.partido.detalleEvento
import com.esan.sportpro.ui.partido.formatearFechaHora
import com.esan.sportpro.ui.partido.minutoEvento

/** Ancho a partir del cual el texto y la cronología se muestran uno al lado del otro. */
private val ANCHO_LADO_A_LADO = 600.dp

private val ALTO_CRONOLOGIA = 420.dp

// ---------------------------------------------------------------------------------------------
// Revisión de un borrador (US-024)
// ---------------------------------------------------------------------------------------------

/**
 * Editor del borrador con los tres bloques y, junto a él, la cronología de eventos para comprobar cada
 * afirmación (criterio 3). En pantallas angostas la cronología queda debajo del editor.
 */
@Composable
internal fun RevisionBorrador(state: ResumenIaUiState, resumen: ResumenPartido, viewModel: ResumenIaViewModel) {
    val edicion = state.edicion ?: return
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EncabezadoVersion(resumen, "Borrador", ColoresPartido.Naranja)
            Banda(
                icono = Icons.Default.VisibilityOff,
                color = ColoresPartido.Naranja,
                texto = "Solo lo ves tú. Jugadores, padres y comunidad lo verán cuando lo apruebes.",
            )

            PanelMenciones(state.mencionesEdicion)
            if (state.mencionesEdicion.isNotEmpty()) VistaConMarcas(edicion.hechos, edicion.desarrollo, state.mencionesEdicion)

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                if (maxWidth >= ANCHO_LADO_A_LADO) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                        EditorBloques(state, viewModel, Modifier.weight(1f))
                        CronologiaVerificacion(state, state.partido, Modifier.weight(1f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                        EditorBloques(state, viewModel, Modifier.fillMaxWidth())
                        CronologiaVerificacion(state, state.partido, Modifier.fillMaxWidth())
                    }
                }
            }

            if (resumen.advertencias.isNotEmpty()) {
                Text(
                    "Avisos de la IA: " + resumen.advertencias.joinToString(" "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            state.avisoAprobacion?.let { aviso ->
                Text(aviso, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = viewModel::guardarBorrador,
                    enabled = edicion.modificado && !state.guardando,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Guardar")
                }
                Button(
                    onClick = viewModel::pedirAprobacion,
                    enabled = !state.guardando,
                    shape = RoundedCornerShape(16.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = ColoresPartido.VerdeOscuro),
                    modifier = Modifier.weight(1.4f).heightIn(min = 48.dp),
                ) {
                    Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Aprobar y publicar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** Versión, estado, modelo y fecha de generación. */
@Composable
private fun EncabezadoVersion(resumen: ResumenPartido, estado: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.14f), modifier = Modifier.size(42.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text("v${resumen.version}", color = color, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleSmall)
            }
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text("Versión ${resumen.version}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                resumen.modelo + " · " + (resumen.creadoEnMs?.let { formatearFechaHora(it) } ?: "guardando…"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Etiqueta(estado, color)
    }
}

/** Aviso de una línea con ícono y color. */
@Composable
private fun Banda(icono: androidx.compose.ui.graphics.vector.ImageVector, color: Color, texto: String) {
    Surface(shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.12f)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(texto, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/**
 * Dónde queda el resumen al aprobarlo: siempre en el detalle del partido (jugadores y apoderados
 * del equipo) y, si se elige, como publicación abierta en el muro de la comunidad.
 */
@Composable
private fun DondeSePublica(enComunidad: Boolean, activo: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            if (activo) "Publicado en" else "Al aprobarlo se publica en",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
        LugarPublicacion(
            icono = Icons.Default.SportsSoccer,
            titulo = "Detalle del partido",
            detalle = "Lo ven los jugadores del equipo y sus apoderados",
            incluido = true,
        )
        LugarPublicacion(
            icono = Icons.Default.Public,
            titulo = "Muro de la comunidad",
            detalle = if (enComunidad) "Publicación abierta con el marcador y los hechos" else "No se publica",
            incluido = enComunidad,
        )
    }
}

@Composable
private fun LugarPublicacion(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    detalle: String,
    incluido: Boolean,
) {
    val color = if (incluido) ColoresPartido.Verde else MaterialTheme.colorScheme.onSurfaceVariant
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = color.copy(alpha = 0.12f), modifier = Modifier.size(32.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(17.dp))
            }
        }
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(titulo, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(detalle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            if (incluido) Icons.Default.CheckCircle else Icons.Default.RemoveCircleOutline,
            contentDescription = if (incluido) "Incluido" else "No incluido",
            tint = color,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** Lista de las menciones que siguen sin corregir; desaparece sola cuando el texto las resuelve. */
@Composable
private fun PanelMenciones(menciones: List<MencionNoVerificada>) {
    if (menciones.isEmpty()) {
        Banda(
            icono = Icons.Default.Verified,
            color = ColoresPartido.Verde,
            texto = "Todo lo que menciona el texto coincide con los eventos registrados.",
        )
        return
    }
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.errorContainer) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "${cantidad(menciones.size, "dato no verificado", "datos no verificados")}. " +
                    "Corrígelos o elimínalos antes de aprobar.",
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            menciones.forEach { mencion ->
                Text(
                    "• Dato no verificado «${mencion.texto}»: ${mencion.motivo}",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

/** Los tres bloques de la estructura definida, editables (criterio 2). */
@Composable
private fun EditorBloques(state: ResumenIaUiState, viewModel: ResumenIaViewModel, modifier: Modifier) {
    val edicion = state.edicion ?: return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CampoBloqueTexto(
            etiqueta = "Hechos registrados",
            valor = edicion.hechos,
            error = state.erroresEdicion[CampoBloque.HECHOS],
            ayuda = "Un hecho por línea.",
            minLineas = 4,
            onCambio = viewModel::onHechosChanged,
        )
        CampoBloqueTexto(
            etiqueta = "Desarrollo del partido",
            valor = edicion.desarrollo,
            error = state.erroresEdicion[CampoBloque.DESARROLLO],
            ayuda = null,
            minLineas = 6,
            onCambio = viewModel::onDesarrolloChanged,
        )
        CampoBloqueTexto(
            etiqueta = "Interpretación del entrenador",
            valor = edicion.interpretacion,
            error = state.erroresEdicion[CampoBloque.INTERPRETACION],
            ayuda = "Tu análisis. No se contrasta con los eventos (hasta ${ReglasRevision.MAX_INTERPRETACION} caracteres).",
            minLineas = 3,
            onCambio = viewModel::onInterpretacionChanged,
        )
    }
}

@Composable
private fun CampoBloqueTexto(
    etiqueta: String,
    valor: String,
    error: String?,
    ayuda: String?,
    minLineas: Int,
    onCambio: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                when (etiqueta) {
                    "Hechos registrados" -> Icons.Default.FactCheck
                    "Desarrollo del partido" -> Icons.Default.Article
                    else -> Icons.Default.RecordVoiceOver
                },
                contentDescription = null,
                tint = ColoresPartido.VerdeOscuro,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(etiqueta, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(
                "${valor.trim().length} car.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedTextField(
            value = valor,
            onValueChange = onCambio,
            isError = error != null,
            supportingText = { (error ?: ayuda)?.let { texto -> Text(texto) } },
            minLines = minLineas,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Eventos vigentes en orden, para contrastar cada afirmación del texto (criterio 3). */
@Composable
private fun CronologiaVerificacion(state: ResumenIaUiState, partido: Partido?, modifier: Modifier) {
    val vigentes = ReglasPartido.vigentes(state.eventos)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Timeline, contentDescription = null, tint = ColoresPartido.VerdeOscuro, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Cronología para verificar", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = ALTO_CRONOLOGIA)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                if (vigentes.isEmpty()) {
                    Text("No hay eventos vigentes.", style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
                }
                vigentes.forEachIndexed { i, evento ->
                    val rival = evento.equipo == com.esan.sportpro.domain.partido.EquipoEvento.RIVAL
                    Row(modifier = Modifier.height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
                        Text(
                            minutoEvento(evento, state.eventos, partido),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            modifier = Modifier.width(46.dp).padding(top = 6.dp),
                        )
                        // Ícono con la línea que une los eventos.
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxHeight()) {
                            IconoEvento(evento.tipoId, 28.dp)
                            if (i < vigentes.lastIndex) {
                                Box(
                                    Modifier
                                        .width(2.dp)
                                        .weight(1f)
                                        .background(MaterialTheme.colorScheme.outlineVariant),
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f).padding(top = 2.dp, bottom = 12.dp)) {
                            Text(evento.tipoNombre, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            val detalle = detalleEvento(evento, partido)
                            if (detalle.isNotBlank()) {
                                Text(
                                    detalle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (rival) ColoresPartido.Rojo else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Resumen aprobado
// ---------------------------------------------------------------------------------------------

/** Texto de un resumen con los tres bloques; los vacíos se omiten salvo que se indique un texto de reemplazo. */
@Composable
internal fun BloquesDelResumen(
    resumen: ResumenPartido,
    mostrarInterpretacionVacia: Boolean,
    menciones: List<MencionNoVerificada> = emptyList(),
) {
    val bloques = resumen.bloquesVigentes
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BloqueTexto("Hechos registrados", bloques.hechos, menciones = menciones)
        BloqueTexto("Desarrollo del partido", bloques.desarrollo, menciones = menciones)
        if (bloques.interpretacion.isNotBlank() || mostrarInterpretacionVacia) {
            BloqueTexto(
                "Interpretación del entrenador",
                bloques.interpretacion,
                vacio = "Sin interpretación del entrenador.",
            )
        }
    }
}

@Composable
private fun BloqueTexto(
    titulo: String,
    texto: String,
    vacio: String = "Sin contenido.",
    menciones: List<MencionNoVerificada> = emptyList(),
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        if (texto.isBlank()) {
            Text(vacio, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(resaltar(texto, menciones), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Cada mención no verificada va resaltada y seguida de la etiqueta "Dato no verificado" (US-023, criterio 7). */
private fun resaltar(texto: String, menciones: List<MencionNoVerificada>): AnnotatedString = buildAnnotatedString {
    ReglasResumen.segmentar(texto, menciones).forEach { segmento ->
        if (segmento.noVerificado) {
            pushStyle(SpanStyle(background = Color(0x55FF6D00), fontWeight = FontWeight.Bold))
            append(segmento.texto)
            pop()
            pushStyle(SpanStyle(color = Color(0xFFB71C1C), fontSize = 11.sp, fontWeight = FontWeight.Bold))
            append(" [Dato no verificado]")
            pop()
        } else {
            append(segmento.texto)
        }
    }
}

/** Vista del texto que se está editando con cada mención pendiente resaltada, para ubicarla rápido. */
@Composable
private fun VistaConMarcas(hechos: String, desarrollo: String, menciones: List<MencionNoVerificada>) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Vista con las marcas", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(resaltar(hechos, menciones), style = MaterialTheme.typography.bodyMedium)
            Text(resaltar(desarrollo, menciones), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Leyenda permanente de todo resumen publicado (criterio 8). */
@Composable
internal fun LeyendaResumenIa(modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = modifier) {
        Text(
            ReglasRevision.LEYENDA_IA,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

/**
 * Resumen aprobado tal como lo ven jugadores, padres y el personal en el detalle del partido
 * (criterios 7 y 8): el texto final, el marcador y la leyenda de IA.
 */
@Composable
fun ResumenPublicado(resumen: ResumenPartido, partido: Partido, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "${partido.nombreEquipo} ${resumen.marcador.replace("-", " - ")} ${partido.nombreRival}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        BloquesDelResumen(resumen, mostrarInterpretacionVacia = false)
        LeyendaResumenIa()
        resumen.aprobadoEnMs?.let {
            Text(
                "Revisado y aprobado el ${formatearFechaHora(it)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Tarjeta de una versión que no se está editando: aprobada, o borrador para quien no puede revisar. */
@Composable
internal fun TarjetaResumenGuardado(state: ResumenIaUiState, resumen: ResumenPartido, viewModel: ResumenIaViewModel) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val aprobado = resumen.estado == EstadoResumen.APROBADO
            if (aprobado) {
                EncabezadoVersion(resumen, "Publicado", ColoresPartido.Verde)
            } else {
                EncabezadoVersion(resumen, "Borrador", ColoresPartido.Naranja)
            }

            if (aprobado) {
                DondeSePublica(enComunidad = resumen.visibleEnComunidad || resumen.publicacionComunidadId != null, activo = true)
                LeyendaResumenIa()
            } else {
                Text(
                    "Borrador: no es visible para jugadores, padres ni la comunidad hasta que sea aprobado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (resumen.mencionesNoVerificadas.isNotEmpty()) {
                    PanelMenciones(resumen.mencionesNoVerificadas)
                }
            }

            BloquesDelResumen(
                resumen,
                mostrarInterpretacionVacia = true,
                menciones = if (aprobado) emptyList() else resumen.mencionesNoVerificadas,
            )

            if (aprobado) {
                resumen.aprobadoEnMs?.let {
                    Text(
                        "Aprobado el ${formatearFechaHora(it)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.puedeRevisar) {
                    OutlinedButton(
                        onClick = viewModel::pedirEditarAprobado,
                        enabled = !state.guardando,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) { Text("Editar de nuevo") }
                }
                SeccionEvaluacion(state, viewModel)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Evaluación (US-025)
// ---------------------------------------------------------------------------------------------

/** Estado de la evaluación del resumen aprobado: pendiente, con acceso al formulario, o ya registrada. */
@Composable
private fun SeccionEvaluacion(state: ResumenIaUiState, viewModel: ResumenIaViewModel) {
    if (!state.puedeEvaluar) return
    val evaluacion = state.evaluacion
    if (evaluacion != null) {
        ResumenDeEvaluacion(evaluacion)
        return
    }
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Evaluación pendiente",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Text(
                "Registra qué tan útil fue el resumen y qué omitió, erró o inventó.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Button(
                onClick = viewModel::abrirEvaluacion,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text("Evaluar resumen") }
        }
    }
}

@Composable
private fun ResumenDeEvaluacion(evaluacion: EvaluacionResumen) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Evaluación registrada", fontWeight = FontWeight.Bold)
            Text(
                "Utilidad ${evaluacion.utilidad}/${ReglasEvaluacion.UTILIDAD_MAX} · Omisiones ${evaluacion.omisiones} · " +
                    "Errores ${evaluacion.errores} · Inventados ${evaluacion.inventados}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(evaluacion.comentario, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Formulario de evaluación (criterios 2 y 3): cinco datos, comentario y correcciones precargadas. */
@Composable
internal fun FormularioEvaluacionResumen(state: ResumenIaUiState, viewModel: ResumenIaViewModel) {
    val formulario = state.formularioEvaluacion ?: return
    val datos = formulario.datos
    val errores = formulario.errores
    val resumen = state.resumenes.firstOrNull { it.id == formulario.resumenId }
    val utilidad = datos.utilidad.trim().toIntOrNull()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Evalúa el resumen",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            resumen?.let {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(
                        "Versión ${it.version}",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
        }
        Text(
            "Documenta qué tan útil fue y lo que la IA omitió, erró o inventó. Solo la ven el administrador y tú.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // 1. Utilidad: cinco estrellas grandes con su significado debajo.
        BloqueEvaluacion(titulo = "¿Qué tan útil fue?") {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                (ReglasEvaluacion.UTILIDAD_MIN..ReglasEvaluacion.UTILIDAD_MAX).forEach { valor ->
                    val encendida = utilidad != null && valor <= utilidad
                    Surface(
                        onClick = { viewModel.onEvaluacionChanged(CampoEvaluacion.UTILIDAD) { copy(utilidad = valor.toString()) } },
                        shape = RoundedCornerShape(16.dp),
                        color = if (encendida) com.esan.sportpro.core.theme.PaletaSportPro.Lima else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f).height(56.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (encendida) androidx.compose.material.icons.Icons.Default.Star else androidx.compose.material.icons.Icons.Default.StarBorder,
                                contentDescription = "$valor de ${ReglasEvaluacion.UTILIDAD_MAX}",
                                tint = if (encendida) com.esan.sportpro.core.theme.PaletaSportPro.Musgo else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                }
            }
            Text(
                when (utilidad) {
                    1 -> "1 · Nada útil"
                    2 -> "2 · Poco útil"
                    3 -> "3 · Útil a medias"
                    4 -> "4 · Útil"
                    5 -> "5 · Muy útil"
                    else -> "Toca una estrella: 1 = nada útil, 5 = muy útil"
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (utilidad != null) FontWeight.Bold else FontWeight.Normal,
                color = if (utilidad != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            errores[CampoEvaluacion.UTILIDAD]?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }

        // 2. Fallos de la IA: contadores con − y + (más rápido que escribir un número).
        BloqueEvaluacion(titulo = "¿En qué falló la IA?") {
            Contador("Omisiones", "Hechos del partido que no mencionó", datos.omisiones, errores[CampoEvaluacion.OMISIONES]) {
                viewModel.onEvaluacionChanged(CampoEvaluacion.OMISIONES) { copy(omisiones = it) }
            }
            HorizontalDividerSuave()
            Contador("Errores", "Datos que dijo mal", datos.errores, errores[CampoEvaluacion.ERRORES]) {
                viewModel.onEvaluacionChanged(CampoEvaluacion.ERRORES) { copy(errores = it) }
            }
            HorizontalDividerSuave()
            Contador("Inventados", "Cosas que no pasaron", datos.inventados, errores[CampoEvaluacion.INVENTADOS]) {
                viewModel.onEvaluacionChanged(CampoEvaluacion.INVENTADOS) { copy(inventados = it) }
            }
        }

        // 3. Comentario y correcciones.
        BloqueEvaluacion(titulo = "Tu comentario") {
            OutlinedTextField(
                value = datos.comentario,
                onValueChange = { viewModel.onEvaluacionChanged(CampoEvaluacion.COMENTARIO) { copy(comentario = it) } },
                placeholder = { Text("Ej.: Resumió bien los goles, pero olvidó el cambio del minuto 60") },
                isError = errores[CampoEvaluacion.COMENTARIO] != null,
                supportingText = {
                    Text(
                        errores[CampoEvaluacion.COMENTARIO]
                            ?: "${datos.comentario.trim().length} de ${ReglasEvaluacion.COMENTARIO_MIN} a ${ReglasEvaluacion.COMENTARIO_MAX} caracteres",
                    )
                },
                shape = RoundedCornerShape(16.dp),
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = datos.correcciones,
                onValueChange = { viewModel.onEvaluacionChanged(CampoEvaluacion.CORRECCIONES) { copy(correcciones = it) } },
                label = { Text("Correcciones realizadas") },
                isError = errores[CampoEvaluacion.CORRECCIONES] != null,
                supportingText = {
                    Text(errores[CampoEvaluacion.CORRECCIONES] ?: "Ya vienen con lo que cambiaste del texto de la IA. Puedes editarlas.")
                },
                shape = RoundedCornerShape(16.dp),
                minLines = 3,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            OutlinedButton(
                onClick = viewModel::posponerEvaluacion,
                enabled = !formulario.guardando,
                shape = CircleShape,
                modifier = Modifier.weight(1f).heightIn(min = 52.dp),
            ) { Text("Más tarde") }
            Button(
                onClick = viewModel::guardarEvaluacion,
                enabled = !formulario.guardando,
                shape = CircleShape,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = com.esan.sportpro.core.theme.PaletaSportPro.Musgo,
                    contentColor = Color.White,
                ),
                modifier = Modifier.weight(1.4f).heightIn(min = 52.dp),
            ) {
                if (formulario.guardando) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Guardar evaluación", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

/** Bloque blanco con título: separa la evaluación en tres pasos claros. */
@Composable
private fun BloqueEvaluacion(titulo: String, contenido: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                titulo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 10.dp),
            )
            contenido()
        }
    }
}

@Composable
private fun HorizontalDividerSuave() {
    androidx.compose.material3.HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        modifier = Modifier.padding(vertical = 6.dp),
    )
}

/** Fila "Omisiones · descripción   [−] 2 [+]". Guarda el valor como texto, igual que antes. */
@Composable
private fun Contador(etiqueta: String, ayuda: String, valor: String, error: String?, onCambio: (String) -> Unit) {
    val numero = valor.trim().toIntOrNull() ?: 0
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(etiqueta, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(ayuda, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            BotonPaso(simbolo = "−", descripcion = "Restar $etiqueta", habilitado = numero > 0) {
                onCambio((numero - 1).coerceAtLeast(0).toString())
            }
            Text(
                if (valor.isBlank()) "0" else numero.toString(),
                style = androidx.compose.ui.text.TextStyle(fontFamily = com.esan.sportpro.core.theme.FuenteNumeros, fontSize = 24.sp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.width(44.dp),
            )
            BotonPaso(simbolo = "+", descripcion = "Sumar $etiqueta", habilitado = numero < 99) {
                onCambio((numero + 1).coerceAtMost(99).toString())
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun BotonPaso(simbolo: String, descripcion: String, habilitado: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = habilitado,
        shape = CircleShape,
        color = if (habilitado) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.size(40.dp).semantics { contentDescription = descripcion },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                simbolo,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (habilitado) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Diálogos
// ---------------------------------------------------------------------------------------------

@Composable
internal fun DialogosResumen(state: ResumenIaUiState, viewModel: ResumenIaViewModel) {
    if (state.dialogoAprobar) {
        AlertDialog(
            onDismissRequest = { if (!state.guardando) viewModel.cerrarDialogoAprobar() },
            title = { Text("Aprobar y publicar") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.clickable { viewModel.onConfirmacionMarcada(!state.confirmacionMarcada) },
                    ) {
                        Checkbox(checked = state.confirmacionMarcada, onCheckedChange = viewModel::onConfirmacionMarcada)
                        Text(ReglasRevision.TEXTO_CONFIRMACION, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp))
                    }
                    DondeSePublica(enComunidad = state.publicarEnComunidad, activo = false)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Publicar también en la comunidad", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Se crea una publicación abierta en el muro con el marcador y los hechos.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = state.publicarEnComunidad, onCheckedChange = viewModel::onPublicarEnComunidad)
                    }
                }
            },
            confirmButton = {
                Button(onClick = viewModel::confirmarAprobacion, enabled = state.confirmacionMarcada && !state.guardando) {
                    if (state.guardando) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Aprobar y publicar")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cerrarDialogoAprobar, enabled = !state.guardando) { Text("Cancelar") }
            },
        )
    }

    if (state.dialogoEditarAprobado) {
        AlertDialog(
            onDismissRequest = { if (!state.guardando) viewModel.cerrarDialogoEditarAprobado() },
            title = { Text("Editar el resumen aprobado") },
            text = {
                Text(
                    "Al editarlo vuelve a Borrador y deja de ser visible para jugadores, padres y comunidad hasta que " +
                        "lo apruebes de nuevo.",
                )
            },
            confirmButton = {
                Button(onClick = viewModel::confirmarEditarAprobado, enabled = !state.guardando) { Text("Editar") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cerrarDialogoEditarAprobado, enabled = !state.guardando) { Text("Cancelar") }
            },
        )
    }

    if (state.ofrecerEvaluacion) {
        AlertDialog(
            onDismissRequest = viewModel::posponerEvaluacion,
            title = { Text("Resumen aprobado") },
            text = {
                Text(
                    "¿Quieres evaluarlo ahora? Puedes dejarlo para después: la evaluación seguirá pendiente en la " +
                        "pantalla del partido.",
                )
            },
            confirmButton = { Button(onClick = viewModel::abrirEvaluacion) { Text("Evaluar ahora") } },
            dismissButton = { TextButton(onClick = viewModel::posponerEvaluacion) { Text("Más tarde") } },
        )
    }
}
