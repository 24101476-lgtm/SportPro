package com.esan.sportpro.ui.ia

import androidx.compose.foundation.clickable
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Versión ${resumen.version}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Etiqueta("Borrador", ColoresPartido.Naranja)
            }
            Text(
                "Modelo ${resumen.modelo} · " + (resumen.creadoEnMs?.let { formatearFechaHora(it) } ?: "guardando…"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Borrador: no es visible para jugadores, padres ni la comunidad hasta que lo apruebes. " +
                    "Corrige el texto con la cronología a la vista.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                ) { Text("Guardar borrador") }
                Button(
                    onClick = viewModel::pedirAprobacion,
                    enabled = !state.guardando,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) { Text("Aprobar y publicar", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

/** Lista de las menciones que siguen sin corregir; desaparece sola cuando el texto las resuelve. */
@Composable
private fun PanelMenciones(menciones: List<MencionNoVerificada>) {
    if (menciones.isEmpty()) {
        Text(
            "Sin datos por verificar: todo lo que mencionas coincide con los eventos registrados.",
            style = MaterialTheme.typography.bodySmall,
            color = ColoresPartido.Verde,
            fontWeight = FontWeight.SemiBold,
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
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        isError = error != null,
        supportingText = { (error ?: ayuda)?.let { texto -> Text(texto) } },
        minLines = minLineas,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Eventos vigentes en orden, para contrastar cada afirmación del texto (criterio 3). */
@Composable
private fun CronologiaVerificacion(state: ResumenIaUiState, partido: Partido?, modifier: Modifier) {
    val vigentes = ReglasPartido.vigentes(state.eventos)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Cronología de eventos", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ALTO_CRONOLOGIA)
                    .verticalScroll(rememberScrollState())
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (vigentes.isEmpty()) {
                    Text("No hay eventos vigentes.", style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
                }
                vigentes.forEach { evento ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            minutoEvento(evento, state.eventos, partido),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.width(52.dp),
                        )
                        IconoEvento(evento.tipoId, 28.dp)
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(evento.tipoNombre, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            val detalle = detalleEvento(evento, partido)
                            if (detalle.isNotBlank()) {
                                Text(detalle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Versión ${resumen.version}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (aprobado) Etiqueta("Aprobado", ColoresPartido.Verde) else Etiqueta("Borrador", ColoresPartido.Naranja)
            }
            Text(
                "Modelo ${resumen.modelo} · " + (resumen.creadoEnMs?.let { formatearFechaHora(it) } ?: "guardando…"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (aprobado) {
                Text(
                    "Visible en el detalle del partido para los jugadores del equipo y sus apoderados" +
                        if (resumen.visibleEnComunidad) " y publicado en la comunidad." else ".",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Evalúa el resumen" + (resumen?.let { " (versión ${it.version})" } ?: ""),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Tu evaluación documenta la utilidad del resumen y lo que la IA omitió, erró o inventó. " +
                "Solo la ven el administrador y tú.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text("Utilidad percibida", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            (ReglasEvaluacion.UTILIDAD_MIN..ReglasEvaluacion.UTILIDAD_MAX).forEach { valor ->
                val elegido = datos.utilidad.trim() == valor.toString()
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (elegido) ColoresPartido.Verde else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .size(44.dp)
                        .clickable { viewModel.onEvaluacionChanged(CampoEvaluacion.UTILIDAD) { copy(utilidad = valor.toString()) } },
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(
                            valor.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (elegido) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 9.dp),
                        )
                    }
                }
            }
            Text("1 = nada útil · 5 = muy útil", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        errores[CampoEvaluacion.UTILIDAD]?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

        CampoNumero("Cantidad de omisiones detectadas", datos.omisiones, errores[CampoEvaluacion.OMISIONES]) {
            viewModel.onEvaluacionChanged(CampoEvaluacion.OMISIONES) { copy(omisiones = it) }
        }
        CampoNumero("Cantidad de errores factuales", datos.errores, errores[CampoEvaluacion.ERRORES]) {
            viewModel.onEvaluacionChanged(CampoEvaluacion.ERRORES) { copy(errores = it) }
        }
        CampoNumero("Cantidad de datos inventados", datos.inventados, errores[CampoEvaluacion.INVENTADOS]) {
            viewModel.onEvaluacionChanged(CampoEvaluacion.INVENTADOS) { copy(inventados = it) }
        }

        OutlinedTextField(
            value = datos.comentario,
            onValueChange = { viewModel.onEvaluacionChanged(CampoEvaluacion.COMENTARIO) { copy(comentario = it) } },
            label = { Text("Comentario") },
            isError = errores[CampoEvaluacion.COMENTARIO] != null,
            supportingText = {
                Text(
                    errores[CampoEvaluacion.COMENTARIO]
                        ?: "${datos.comentario.trim().length} de ${ReglasEvaluacion.COMENTARIO_MIN} a ${ReglasEvaluacion.COMENTARIO_MAX} caracteres",
                )
            },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = datos.correcciones,
            onValueChange = { viewModel.onEvaluacionChanged(CampoEvaluacion.CORRECCIONES) { copy(correcciones = it) } },
            label = { Text("Correcciones realizadas") },
            isError = errores[CampoEvaluacion.CORRECCIONES] != null,
            supportingText = {
                Text(errores[CampoEvaluacion.CORRECCIONES] ?: "Precargadas con las diferencias entre el texto de la IA y el aprobado. Puedes editarlas.")
            },
            minLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = viewModel::posponerEvaluacion,
                enabled = !formulario.guardando,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) { Text("Más tarde") }
            Button(
                onClick = viewModel::guardarEvaluacion,
                enabled = !formulario.guardando,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) {
                if (formulario.guardando) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Guardar evaluación", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CampoNumero(etiqueta: String, valor: String, error: String?, onCambio: (String) -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = { nuevo -> onCambio(nuevo.filter { it.isDigit() }.take(2)) },
        label = { Text(etiqueta) },
        isError = error != null,
        supportingText = { error?.let { texto -> Text(texto) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
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
