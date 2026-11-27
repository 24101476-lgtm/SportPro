package com.esan.sportpro.ui.partido

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.esan.sportpro.domain.partido.EstadoPartido
import com.esan.sportpro.domain.partido.Partido
import com.esan.sportpro.domain.partido.ReglasPartido
import com.esan.sportpro.domain.partido.TiposEvento

/**
 * Partido en vivo: el once que está jugando, en la misma pizarra que al crear la alineación.
 * Muestra junto a cada carta sus goles, tarjetas y cambios; el operador puede cambiar la
 * formación, mover cartas y tocar un jugador para registrarle un evento.
 */
@Composable
fun AlineacionEnVivoDialog(
    state: RegistroUiState,
    partido: Partido,
    viewModel: RegistroPartidoViewModel,
) {
    val alineacion = state.alineacionEnVivo
    // Solo quien opera el partido mueve cartas o registra eventos; el resto solo mira.
    val operable = state.paleta.isNotEmpty()
    var seleccionado by remember { mutableStateOf<JugadorCanchaUi?>(null) }

    Dialog(
        onDismissRequest = viewModel::cerrarAlineacion,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp),
                ) {
                    IconButton(onClick = viewModel::cerrarAlineacion) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                    Escudo(partido.nombreEquipo, ColoresPartido.Azul, 36.dp)
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(partido.nombreEquipo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            subtitulo(state, partido),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        "${state.marcador.propio} – ${state.marcador.rival}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                    )
                }

                if (operable) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        (FORMACIONES_DISPONIBLES + FORMACION_PERSONALIZADA).forEach { opcion ->
                            FilterChip(
                                selected = state.formacionVivo == opcion,
                                onClick = { viewModel.cambiarFormacionVivo(opcion) },
                                label = { Text(opcion) },
                            )
                        }
                    }
                }

                CanchaTactica(
                    titulares = alineacion.enCancha,
                    suplentes = alineacion.banco,
                    fuera = alineacion.fuera,
                    editable = operable,
                    modifier = Modifier.padding(horizontal = 12.dp),
                    onJugadorClick = { seleccionado = it },
                    onPosicionJugadorChange = viewModel::moverJugadorVivo,
                    onIntercambiarJugadores = viewModel::intercambiarVivo,
                    ayuda = if (operable) {
                        "Toca un jugador para registrarle un gol, una tarjeta o un cambio. Arrastra para moverlo; " +
                            "la formación se guarda solo en este celular."
                    } else {
                        "Toca un jugador para ver lo que lleva en el partido."
                    },
                )
            }
        }
    }

    seleccionado?.let { jugador ->
        AccionesJugador(
            jugador = jugador,
            operable = operable && partido.estado == EstadoPartido.EN_CURSO,
            enCancha = alineacion.enCancha.any { it.id == jugador.id },
            enBanco = alineacion.banco.any { it.id == jugador.id },
            puedeCambiar = state.paleta.any { it.id == TiposEvento.CAMBIO },
            onAccion = { tipoId ->
                seleccionado = null
                viewModel.registrarPara(tipoId, jugador)
            },
            onCerrar = { seleccionado = null },
        )
    }
}

private fun subtitulo(state: RegistroUiState, partido: Partido): String {
    val enCancha = state.alineacionEnVivo.enCancha.size
    val cambios = ReglasPartido.cambiosRealizados(state.eventos)
    val minuto = if (partido.estado == EstadoPartido.EN_CURSO) {
        "min ${ReglasPartido.textoMinuto(state.reloj.minuto, if (partido.inicioSegundoTiempoMs != null) 2 else 1, partido.duracionTiempoMin)} · "
    } else {
        ""
    }
    return "$minuto$enCancha en cancha · $cambios/${partido.cambiosPermitidos} cambios"
}

/** Datos del jugador en el partido y, para el operador, accesos directos para registrar un evento. */
@Composable
private fun AccionesJugador(
    jugador: JugadorCanchaUi,
    operable: Boolean,
    enCancha: Boolean,
    enBanco: Boolean,
    puedeCambiar: Boolean,
    onAccion: (String) -> Unit,
    onCerrar: () -> Unit,
) {
    val m = jugador.marcas
    AlertDialog(
        onDismissRequest = onCerrar,
        icon = { FotoJugadorGrande(jugador, 72.dp) },
        title = {
            Text(
                listOfNotNull(jugador.dorsal?.toString(), jugador.nombre).joinToString(" · "),
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    listOfNotNull(
                        jugador.posicion,
                        when {
                            enCancha -> "En cancha"
                            enBanco -> "En el banco"
                            m?.roja == true -> "Expulsado"
                            m?.salioMinuto != null -> "Salió al ${m.salioMinuto}'"
                            else -> null
                        },
                    ).joinToString(" · "),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (m != null) {
                    Text(
                        listOfNotNull(
                            m.goles.takeIf { it > 0 }?.let { "⚽ $it gol${if (it > 1) "es" else ""}" },
                            m.asistencias.takeIf { it > 0 }?.let { "$it asist." },
                            m.amarillas.takeIf { it > 0 }?.let { "$it amarilla${if (it > 1) "s" else ""}" },
                            "roja".takeIf { m.roja },
                            m.entroMinuto?.let { "entró al $it'" },
                        ).joinToString(" · "),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (operable && (enCancha || enBanco)) {
                    Text("Registrar", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (enCancha) {
                            Accion("Gol", TiposEvento.GOL, onAccion)
                            Accion("Amarilla", TiposEvento.TARJETA_AMARILLA, onAccion)
                            Accion("Roja", TiposEvento.TARJETA_ROJA, onAccion)
                            Accion("Falta", TiposEvento.FALTA, onAccion)
                        }
                        if (puedeCambiar) {
                            Accion(if (enCancha) "Cambio (sale)" else "Cambio (entra)", TiposEvento.CAMBIO, onAccion)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } },
    )
}

@Composable
private fun Accion(texto: String, tipoId: String, onAccion: (String) -> Unit) {
    val color = colorTipo(tipoId)
    Surface(
        onClick = { onAccion(tipoId) },
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f)),
        modifier = Modifier.heightIn(min = 44.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            SimboloEvento(tipoId = tipoId, tamano = 18.dp, tinta = color)
            Spacer(Modifier.width(6.dp))
            Text(texto, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
