package com.esan.sportpro.ui.ia

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.ia.ResumenPartido
import com.esan.sportpro.ui.partido.ColoresPartido
import com.esan.sportpro.ui.partido.Etiqueta

/** US-023: generación del resumen. US-024: revisión y aprobación. US-025: evaluación del resumen aprobado. */
@Composable
fun ResumenIaScreen(
    onVolver: () -> Unit,
    viewModel: ResumenIaViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val partido = state.partido
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.mensaje) {
        state.mensaje?.let {
            snackbar.showSnackbar(it)
            viewModel.limpiarMensaje()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                IconButton(
                    onClick = { if (state.formularioEvaluacion != null) viewModel.posponerEvaluacion() else onVolver() },
                ) { Icon(Icons.Default.ArrowBack, contentDescription = "Volver") }
                Column {
                    Text(
                        if (state.formularioEvaluacion != null) "Evaluación del resumen" else "Resumen con IA",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (partido != null) {
                        Text(
                            "${partido.nombreEquipo} vs ${partido.nombreRival}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (state.formularioEvaluacion != null) {
                FormularioEvaluacionResumen(state, viewModel)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item {
                        PanelGenerar(
                            generando = state.generando,
                            hayResumenes = state.resumenes.isNotEmpty(),
                            etapa = etapaDe(state.resumenes),
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
                    state.resumenMostrado?.let { resumen ->
                        item(key = resumen.id) {
                            if (state.enRevision) {
                                RevisionBorrador(state, resumen, viewModel)
                            } else {
                                TarjetaResumenGuardado(state, resumen, viewModel)
                            }
                        }
                    }
                }
            }
        }
        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
        DialogosResumen(state, viewModel)
    }
}

/** Paso del flujo: 0 generar, 1 revisar el borrador, 2 publicado. */
private fun etapaDe(resumenes: List<ResumenPartido>): Int = when {
    resumenes.any { it.publicado } -> 2
    resumenes.isNotEmpty() -> 1
    else -> 0
}

@Composable
private fun PanelGenerar(
    generando: Boolean,
    hayResumenes: Boolean,
    etapa: Int,
    motivoNoGenerar: String?,
    error: String?,
    onGenerar: () -> Unit,
) {
    val lima = com.esan.sportpro.core.theme.PaletaSportPro.Lima
    val carbon = com.esan.sportpro.core.theme.PaletaSportPro.Carbon
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = ColoresPartido.VerdeOscuro,
        contentColor = Color.White,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = lima, modifier = Modifier.size(42.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = carbon, modifier = Modifier.size(22.dp))
                    }
                }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("Crónica del partido", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "La IA escribe un borrador con los eventos registrados",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
            }

            // Pasos: generar → revisar → publicar.
            Row(verticalAlignment = Alignment.CenterVertically) {
                listOf("Generar", "Revisar", "Publicar").forEachIndexed { i, paso ->
                    val hecho = i < etapa || (i == 2 && etapa == 2)
                    val actual = i == etapa && etapa < 2
                    Surface(
                        shape = CircleShape,
                        color = when {
                            hecho -> lima
                            actual -> Color.White
                            else -> Color.White.copy(alpha = 0.18f)
                        },
                        modifier = Modifier.size(24.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (hecho) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = carbon, modifier = Modifier.size(15.dp))
                            } else {
                                Text(
                                    "${i + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Black,
                                    color = if (actual) carbon else Color.White,
                                )
                            }
                        }
                    }
                    Text(
                        paso,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (actual) FontWeight.Bold else FontWeight.Normal,
                        color = Color.White.copy(alpha = if (hecho || actual) 1f else 0.6f),
                        modifier = Modifier.padding(start = 6.dp),
                    )
                    if (i < 2) {
                        Box(
                            Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                                .height(2.dp)
                                .background(Color.White.copy(alpha = if (i < etapa) 0.8f else 0.2f)),
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp).padding(top = 2.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "A la IA solo llegan eventos, alineación y marcador, con los jugadores como alias; los nombres se " +
                        "reponen en tu teléfono. Nada se publica sin tu revisión.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                )
            }

            if (generando) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LinearProgressIndicator(
                        color = lima,
                        trackColor = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("Escribiendo el resumen… puede tardar hasta 30 segundos", style = MaterialTheme.typography.bodySmall)
                }
            }

            if (error != null) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.errorContainer) {
                    Text(
                        error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                    )
                }
            }

            val puede = motivoNoGenerar == null && !generando
            Button(
                onClick = onGenerar,
                enabled = puede,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = lima,
                    contentColor = carbon,
                    disabledContainerColor = Color.White.copy(alpha = 0.15f),
                    disabledContentColor = Color.White.copy(alpha = 0.5f),
                ),
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    when {
                        error != null && motivoNoGenerar == null -> "Reintentar"
                        hayResumenes -> "Generar una versión nueva"
                        else -> "Generar resumen"
                    },
                    fontWeight = FontWeight.Bold,
                )
            }

            if (motivoNoGenerar != null && error != motivoNoGenerar) {
                Text(motivoNoGenerar, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.75f))
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
        Text("Versiones", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        resumenes.forEach { resumen ->
            val activa = resumen.id == elegido
            val estado = if (resumen.publicado) ColoresPartido.Verde else ColoresPartido.Naranja
            Surface(
                shape = RoundedCornerShape(50),
                color = if (activa) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.clickable { onElegir(resumen.id) },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Box(Modifier.size(8.dp).background(estado, CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "v${resumen.version} · " + if (resumen.publicado) "Publicada" else "Borrador",
                        color = if (activa) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}
