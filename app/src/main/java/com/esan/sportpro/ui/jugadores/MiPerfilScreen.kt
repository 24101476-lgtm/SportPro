package com.esan.sportpro.ui.jugadores

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.jugadores.PieHabil
import com.esan.sportpro.ui.common.EncabezadoModulo
import com.esan.sportpro.ui.common.EstadoVacioModulo

/** US-006 (criterio 9) y US-012: "Mi perfil" del jugador. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiPerfilScreen(
    modifier: Modifier = Modifier,
    viewModel: MiPerfilViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { evento ->
            if (evento is MiPerfilEvent.Mensaje) snackbar.showSnackbar(evento.texto)
        }
    }

    val ficha = state.ficha
    if (state.viendoHistorial && ficha != null) {
        BackHandler(onBack = viewModel::cerrarHistorial)
        Scaffold(
            modifier = modifier,
            topBar = {
                androidx.compose.material3.TopAppBar(
                    title = { Text("Mi historial") },
                    navigationIcon = {
                        IconButton(onClick = viewModel::cerrarHistorial) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    },
                )
            },
        ) { padding ->
            HistorialParticipacion(
                jugador = ficha,
                entrenamientos = state.entrenamientos,
                partidos = state.historialPartidos,
                cargandoPartidos = state.cargandoPartidos,
                modifier = Modifier.padding(padding),
            )
        }
        return
    }

    Scaffold(modifier = modifier, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        when {
            state.cargando -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.error != null -> EstadoVacioModulo(
                titulo = "No pudimos cargar tu perfil",
                mensaje = state.error.orEmpty(),
                icono = Icons.Default.Person,
                modifier = Modifier.padding(padding),
            )
            state.sinFicha || ficha == null -> Column(Modifier.padding(padding).padding(16.dp)) {
                Encabezado()
                EstadoVacioModulo(
                    titulo = "Tu cuenta aún no está enlazada a tu ficha",
                    mensaje = "Pide a tu entrenador o al administrador que la enlace desde Jugadores › Editar ficha. " +
                        "Así verás tu perfil, tu historial y tus convocatorias.",
                    icono = Icons.Default.Person,
                )
            }
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Encabezado()

                // Foto (editable por el propio jugador).
                SelectorFotoJugador(
                    fotoVisible = state.fotoNueva ?: ficha.fotoUrl.takeIf { it.isNotBlank() },
                    iniciales = ficha.iniciales,
                    onFotoElegida = viewModel::onFotoElegida,
                    onQuitarFotoNueva = if (state.fotoNueva != null) viewModel::quitarFotoNueva else null,
                )

                Tarjeta {
                    Text(ficha.nombreCompleto, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        listOfNotNull(
                            ficha.equipoNombre.takeIf { it.isNotBlank() },
                            ficha.posicion.takeIf { it.isNotBlank() },
                            ficha.dorsal?.let { "Dorsal $it" },
                        ).joinToString(" · ").ifBlank { "Sin equipo asignado" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Dato("Edad", ficha.edad()?.let { "$it años" })
                    Dato("Posición secundaria", ficha.posicionSecundaria)
                    Dato("Estatura", ficha.estaturaCm?.let { "%.2f m".format(it / 100.0) })
                    Dato("Peso", ficha.pesoKg?.let { "%.1f kg".format(it) })
                    Dato("Correo", ficha.correo)
                    Dato("Contacto de emergencia", listOf(ficha.contactoEmergenciaNombre, ficha.contactoEmergenciaTelefono).filter { it.isNotBlank() }.joinToString(" · "))
                }

                state.asistencia?.let { a ->
                    Tarjeta {
                        Text("Asistencia a entrenamientos", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            if (a.registrados == 0) "Aún no hay sesiones registradas"
                            else "${a.porcentaje}% · ${a.participaciones} de ${a.registrados} sesiones",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        OutlinedButton(onClick = viewModel::abrirHistorial, modifier = Modifier.padding(top = 4.dp)) {
                            Text("Ver mi historial")
                        }
                    }
                }

                // Lo único que el jugador puede cambiar (US-006, criterio 9).
                Tarjeta {
                    Text("Puedes actualizar", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = state.telefono,
                        onValueChange = viewModel::onTelefono,
                        label = { Text("Teléfono") },
                        singleLine = true,
                        isError = state.errorTelefono != null,
                        supportingText = state.errorTelefono?.let { { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("Pie hábil", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PieHabil.entries.forEach { pie ->
                            FilterChip(
                                selected = state.pieHabil == pie.etiqueta,
                                onClick = { viewModel.onPieHabil(pie.etiqueta) },
                                label = { Text(pie.etiqueta) },
                            )
                        }
                    }
                    Button(
                        onClick = viewModel::guardar,
                        enabled = state.hayCambios && !state.guardando,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        if (state.guardando) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Guardar cambios")
                        }
                    }
                    Text(
                        "El resto de tu ficha la actualiza tu entrenador o el administrador.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun Encabezado() {
    EncabezadoModulo(
        foto = com.esan.sportpro.ui.common.FotoSportPro.VESTUARIO,
        titulo = "Mi perfil",
        subtitulo = "Tu ficha de jugador y tu historial",
        icono = Icons.Default.Person,
    )
}

@Composable
private fun Tarjeta(contenido: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { contenido() }
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String?) {
    if (valor.isNullOrBlank()) return
    Row {
        Text("$etiqueta: ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
