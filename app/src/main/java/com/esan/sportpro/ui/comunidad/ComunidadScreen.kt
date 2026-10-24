package com.esan.sportpro.ui.comunidad

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.comunidad.Aviso
import com.esan.sportpro.domain.comunidad.MotivoReporte
import com.esan.sportpro.domain.comunidad.Publicacion

/**
 * Punto de entrada de la pestaña "Comunidad" del Home (US-028, US-029, US-030 — Página 13 del
 * backlog). Agrupa Muro, Avisos y, solo para el rol Administrador, la bandeja de Moderación con
 * un [TabRow], igual que [com.esan.sportpro.ui.estadisticas.EstadisticasComunicacionEntryScreen]
 * hace con Estadísticas y Anuncios.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComunidadScreen(modifier: Modifier = Modifier, moderacionViewModel: ModeracionViewModel = hiltViewModel()) {
    val moderacionState by moderacionViewModel.uiState.collectAsState()
    var seccion by rememberSaveable { mutableIntStateOf(0) }
    var mostrarBloqueados by rememberSaveable { mutableStateOf(false) }
    val titulos = if (moderacionState.puedeModerar) listOf("Muro", "Avisos", "Moderación") else listOf("Muro", "Avisos")

    if (mostrarBloqueados) {
        Scaffold(
            modifier = modifier,
            topBar = {
                TopAppBar(
                    title = { Text("Usuarios bloqueados") },
                    navigationIcon = {
                        IconButton(onClick = { mostrarBloqueados = false }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                        }
                    },
                )
            },
        ) { padding ->
            UsuariosBloqueadosScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                viewModel = moderacionViewModel,
            )
        }
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        moderacionState.suspensionActual?.let { UsuarioSuspendidoBanner(suspension = it) }

        Row(verticalAlignment = Alignment.CenterVertically) {
            TabRow(selectedTabIndex = seccion, modifier = Modifier.weight(1f)) {
                titulos.forEachIndexed { index, titulo ->
                    Tab(selected = seccion == index, onClick = { seccion = index }, text = { Text(titulo) })
                }
            }
            IconButton(onClick = { mostrarBloqueados = true }) {
                Icon(Icons.Default.PersonOff, contentDescription = "Usuarios bloqueados")
            }
        }
        when (seccion) {
            0 -> MuroSeccionScreen(modifier = Modifier.weight(1f), suspendido = moderacionState.estaSuspendido)
            1 -> AvisosSeccionScreen(modifier = Modifier.weight(1f))
            else -> ModeracionSeccionScreen(modifier = Modifier.weight(1f), viewModel = moderacionViewModel)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Muro (US-028)
// ---------------------------------------------------------------------------------------------

private sealed interface MuroDestino {
    data object Lista : MuroDestino
    data object Nueva : MuroDestino
    data class Detalle(val publicacionId: String) : MuroDestino
}

private data class ReporteEnCurso(
    val tipo: TipoReporteRapido,
    val contenidoId: String,
    val publicacionId: String?,
    val autorUid: String,
    val esMenor: Boolean,
)

private enum class TipoReporteRapido { PUBLICACION, COMENTARIO, AVISO }

@Composable
private fun MuroSeccionScreen(modifier: Modifier = Modifier, suspendido: Boolean, viewModel: PublicacionesViewModel = hiltViewModel()) {
    var destino by rememberSaveable(stateSaver = MuroDestinoSaver) { mutableStateOf<MuroDestino>(MuroDestino.Lista) }
    // Estado de diálogo efímero, no se restaura tras un cambio de configuración (ReporteEnCurso no es Serializable).
    var reporteEnCurso by remember { mutableStateOf<ReporteEnCurso?>(null) }
    val state by viewModel.uiState.collectAsState()

    when (val actual = destino) {
        MuroDestino.Lista -> MuroScreen(
            onNuevaPublicacion = { if (!suspendido) destino = MuroDestino.Nueva },
            onAbrirPublicacion = { publicacion -> viewModel.abrirPublicacion(publicacion); destino = MuroDestino.Detalle(publicacion.id) },
            onReportar = { id, autorUid, esMenor -> reporteEnCurso = ReporteEnCurso(TipoReporteRapido.PUBLICACION, id, null, autorUid, esMenor) },
            modifier = modifier,
            viewModel = viewModel,
        )
        MuroDestino.Nueva -> PublicacionNuevaScreen(
            onPublicada = { destino = MuroDestino.Lista },
            onCancelar = { destino = MuroDestino.Lista },
            modifier = modifier,
            viewModel = viewModel,
        )
        is MuroDestino.Detalle -> {
            val publicacion: Publicacion? = state.publicaciones.firstOrNull { it.id == actual.publicacionId } ?: state.publicacionAbierta
            if (publicacion != null) {
                PublicacionDetalleScreen(
                    publicacion = publicacion,
                    onVolver = { destino = MuroDestino.Lista },
                    onReportarPublicacion = { id, autorUid, esMenor -> reporteEnCurso = ReporteEnCurso(TipoReporteRapido.PUBLICACION, id, null, autorUid, esMenor) },
                    onReportarComentario = { comentarioId, publicacionId, autorUid -> reporteEnCurso = ReporteEnCurso(TipoReporteRapido.COMENTARIO, comentarioId, publicacionId, autorUid, esMenor = false) },
                    modifier = modifier,
                    viewModel = viewModel,
                )
            } else {
                destino = MuroDestino.Lista
            }
        }
    }

    reporteEnCurso?.let { pendiente ->
        ReportarMotivoDialog(
            onConfirmar = { motivo, detalle ->
                enviarReporte(viewModel, pendiente, motivo, detalle)
                reporteEnCurso = null
            },
            onCancelar = { reporteEnCurso = null },
        )
    }
}

private fun enviarReporte(viewModel: PublicacionesViewModel, pendiente: ReporteEnCurso, motivo: MotivoReporte, detalle: String?) {
    when (pendiente.tipo) {
        TipoReporteRapido.PUBLICACION -> viewModel.reportarPublicacion(pendiente.contenidoId, pendiente.autorUid, pendiente.esMenor, motivo, detalle)
        TipoReporteRapido.COMENTARIO -> viewModel.reportarComentario(pendiente.contenidoId, pendiente.publicacionId.orEmpty(), pendiente.autorUid, motivo, detalle)
        TipoReporteRapido.AVISO -> Unit
    }
}

private val MuroDestinoSaver = Saver<MuroDestino, String>(
    save = { when (it) {
        MuroDestino.Lista -> "lista"
        MuroDestino.Nueva -> "nueva"
        is MuroDestino.Detalle -> "detalle|${it.publicacionId}"
    } },
    restore = { guardado ->
        when {
            guardado == "lista" -> MuroDestino.Lista
            guardado == "nueva" -> MuroDestino.Nueva
            guardado.startsWith("detalle|") -> MuroDestino.Detalle(guardado.removePrefix("detalle|"))
            else -> MuroDestino.Lista
        }
    },
)

// ---------------------------------------------------------------------------------------------
// Avisos (US-029)
// ---------------------------------------------------------------------------------------------

private sealed interface AvisosDestino {
    data object Buscador : AvisosDestino
    data object Nuevo : AvisosDestino
    data class Detalle(val avisoId: String) : AvisosDestino
}

@Composable
private fun AvisosSeccionScreen(modifier: Modifier = Modifier, viewModel: AvisosViewModel = hiltViewModel()) {
    var destino by rememberSaveable(stateSaver = AvisosDestinoSaver) { mutableStateOf<AvisosDestino>(AvisosDestino.Buscador) }
    var reporteEnCurso by rememberSaveable { mutableStateOf<Pair<String, String>?>(null) } // avisoId, autorUid
    val state by viewModel.uiState.collectAsState()

    when (val actual = destino) {
        AvisosDestino.Buscador -> AvisosBuscadorScreen(
            onNuevoAviso = { destino = AvisosDestino.Nuevo },
            onAbrirAviso = { aviso -> destino = AvisosDestino.Detalle(aviso.id) },
            modifier = modifier,
            viewModel = viewModel,
        )
        AvisosDestino.Nuevo -> AvisoNuevoScreen(
            onPublicado = { destino = AvisosDestino.Buscador },
            onCancelar = { destino = AvisosDestino.Buscador },
            modifier = modifier,
            viewModel = viewModel,
        )
        is AvisosDestino.Detalle -> {
            val aviso: Aviso? = state.avisos.firstOrNull { it.id == actual.avisoId }
            if (aviso != null) {
                AvisoDetalleScreen(
                    aviso = aviso,
                    onVolver = { destino = AvisosDestino.Buscador },
                    onReportar = { id, autorUid -> reporteEnCurso = id to autorUid },
                    modifier = modifier,
                )
            } else {
                destino = AvisosDestino.Buscador
            }
        }
    }

    reporteEnCurso?.let { (avisoId, autorUid) ->
        ReportarMotivoDialog(
            onConfirmar = { motivo, detalle -> viewModel.reportar(avisoId, autorUid, motivo, detalle); reporteEnCurso = null },
            onCancelar = { reporteEnCurso = null },
        )
    }
}

private val AvisosDestinoSaver = Saver<AvisosDestino, String>(
    save = { when (it) {
        AvisosDestino.Buscador -> "buscador"
        AvisosDestino.Nuevo -> "nuevo"
        is AvisosDestino.Detalle -> "detalle|${it.avisoId}"
    } },
    restore = { guardado ->
        when {
            guardado == "buscador" -> AvisosDestino.Buscador
            guardado == "nuevo" -> AvisosDestino.Nuevo
            guardado.startsWith("detalle|") -> AvisosDestino.Detalle(guardado.removePrefix("detalle|"))
            else -> AvisosDestino.Buscador
        }
    },
)

// ---------------------------------------------------------------------------------------------
// Moderación (US-030) — solo visible para el rol Administrador (ver ModeracionUiState.puedeModerar)
// ---------------------------------------------------------------------------------------------

private sealed interface ModeracionDestino {
    data object Bandeja : ModeracionDestino
    data object Resolver : ModeracionDestino
    data object Bloqueados : ModeracionDestino
}

@Composable
private fun ModeracionSeccionScreen(modifier: Modifier = Modifier, viewModel: ModeracionViewModel) {
    var destino by rememberSaveable(stateSaver = ModeracionDestinoSaver) { mutableStateOf<ModeracionDestino>(ModeracionDestino.Bandeja) }

    when (destino) {
        ModeracionDestino.Bandeja -> ModeracionBandejaScreen(
            onAbrirReporte = { destino = ModeracionDestino.Resolver },
            modifier = modifier,
            viewModel = viewModel,
        )
        ModeracionDestino.Resolver -> ModeracionResolverScreen(
            onResuelto = { destino = ModeracionDestino.Bandeja },
            onVolver = { destino = ModeracionDestino.Bandeja },
            modifier = modifier,
            viewModel = viewModel,
        )
        ModeracionDestino.Bloqueados -> UsuariosBloqueadosScreen(modifier = modifier, viewModel = viewModel)
    }
}

private val ModeracionDestinoSaver = Saver<ModeracionDestino, String>(
    save = { when (it) {
        ModeracionDestino.Bandeja -> "bandeja"
        ModeracionDestino.Resolver -> "resolver"
        ModeracionDestino.Bloqueados -> "bloqueados"
    } },
    restore = { guardado ->
        when (guardado) {
            "resolver" -> ModeracionDestino.Resolver
            "bloqueados" -> ModeracionDestino.Bloqueados
            else -> ModeracionDestino.Bandeja
        }
    },
)
