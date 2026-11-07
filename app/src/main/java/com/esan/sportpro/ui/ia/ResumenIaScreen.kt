package com.esan.sportpro.ui.ia

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.ia.MencionNoVerificada
import com.esan.sportpro.domain.ia.ReglasResumen
import com.esan.sportpro.domain.ia.ResumenPartido
import com.esan.sportpro.ui.partido.ColoresPartido
import com.esan.sportpro.ui.partido.Etiqueta
import com.esan.sportpro.ui.partido.cantidad
import com.esan.sportpro.ui.partido.formatearFechaHora

/** US-023: generación del resumen narrativo de un partido con el acta cerrada. */
@Composable
fun ResumenIaScreen(
    onVolver: () -> Unit,
    viewModel: ResumenIaViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val partido = state.partido

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
            IconButton(onClick = onVolver) { Icon(Icons.Default.ArrowBack, contentDescription = "Volver") }
            Column {
                Text("Resumen con IA", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (partido != null) {
                    Text(
                        "${partido.nombreEquipo} vs ${partido.nombreRival}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                PanelGenerar(
                    generando = state.generando,
                    hayResumenes = state.resumenes.isNotEmpty(),
                    motivoNoGenerar = state.motivoNoGenerar,
                    error = state.error,
                    onGenerar = viewModel::generar,
                )
            }
            if (state.resumenes.size > 1) {
                item {
                    SelectorVersiones(
                        resumenes = state.resumenes,
                        elegido = state.resumenMostrado?.id,
                        onElegir = viewModel::elegirVersion,
                    )
                }
            }
            state.resumenMostrado?.let { resumen -> item { TarjetaResumen(resumen) } }
        }
    }
}

@Composable
private fun PanelGenerar(
    generando: Boolean,
    hayResumenes: Boolean,
    motivoNoGenerar: String?,
    error: String?,
    onGenerar: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Genera un borrador a partir de los eventos registrados. A la IA solo llegan eventos, alineación y " +
                    "marcador, con los jugadores como alias; los nombres de pila se reponen en tu teléfono. " +
                    "Tú revisas el texto antes de publicarlo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (generando) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Generando el resumen… puede tardar hasta 30 segundos", style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (error != null) {
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            val puede = motivoNoGenerar == null && !generando
            Button(
                onClick = onGenerar,
                enabled = puede,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    when {
                        error != null && motivoNoGenerar == null -> "Reintentar"
                        hayResumenes -> "Generar un resumen nuevo"
                        else -> "Generar resumen"
                    },
                    fontWeight = FontWeight.Bold,
                )
            }

            if (motivoNoGenerar != null && error != motivoNoGenerar) {
                Text(motivoNoGenerar, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SelectorVersiones(resumenes: List<ResumenPartido>, elegido: String?, onElegir: (String) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.horizontalScroll(rememberScrollState()),
    ) {
        Text("Versiones", style = MaterialTheme.typography.labelLarge)
        resumenes.forEach { resumen ->
            val activa = resumen.id == elegido
            Surface(
                shape = RoundedCornerShape(50),
                color = if (activa) ColoresPartido.Verde else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.clickable { onElegir(resumen.id) },
            ) {
                Text(
                    "v${resumen.version}",
                    color = if (activa) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun TarjetaResumen(resumen: ResumenPartido) {
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
                "Borrador: no es visible para jugadores, padres ni la comunidad hasta que sea aprobado.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (resumen.mencionesNoVerificadas.isNotEmpty()) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.errorContainer) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "${cantidad(resumen.mencionesNoVerificadas.size, "dato no verificado", "datos no verificados")}. " +
                                "Corrígelos o elimínalos antes de aprobar.",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        resumen.mencionesNoVerificadas.forEach { mencion ->
                            Text(
                                "• «${mencion.texto}»: ${mencion.motivo}",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            Bloque("Hechos registrados", resumen.bloques.hechos, resumen.mencionesNoVerificadas)
            Bloque("Desarrollo del partido", resumen.bloques.desarrollo, resumen.mencionesNoVerificadas)
            Bloque(
                titulo = "Interpretación del entrenador",
                texto = resumen.bloques.interpretacion,
                menciones = emptyList(),
                vacio = "Pendiente: la completas tú al revisar el resumen.",
            )

            if (resumen.advertencias.isNotEmpty()) {
                Text(
                    "Avisos de la IA: " + resumen.advertencias.joinToString(" "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Bloque(titulo: String, texto: String, menciones: List<MencionNoVerificada>, vacio: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        if (texto.isBlank()) {
            Text(
                vacio ?: "Sin contenido.",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(resaltar(texto, menciones), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Cada mención no verificada va resaltada y seguida de la etiqueta "Dato no verificado" (criterio 7). */
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
