package com.esan.sportpro.ui.partido

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.partido.AmbitoEvento
import com.esan.sportpro.domain.partido.Campos
import androidx.compose.runtime.LaunchedEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventoCatalogoFormScreen(
    eventoId: String? = null,
    onVolver: () -> Unit,
    onGuardado: () -> Unit,
    viewModel: CatalogoEventosViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val eventoExistente =
        state.eventos.firstOrNull { it.id == eventoId }

    val esEdicion = eventoId != null

    var nombre by remember { mutableStateOf("") }
    var ambito by remember { mutableStateOf(AmbitoEvento.JUGADOR) }

    var icono by remember {
        mutableStateOf("sports_soccer")
    }

    var color by remember {
        mutableStateOf("#2E7D32")
    }

    var sumaMarcador by remember {
        mutableStateOf(false)
    }

    var activo by remember {
        mutableStateOf(true)
    }

    var camposObligatorios by remember {
        mutableStateOf(setOf<String>())
    }

    var camposOpcionales by remember {
        mutableStateOf(setOf<String>())
    }

    var formularioCargado by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(eventoExistente) {
        if (
            esEdicion &&
            eventoExistente != null &&
            !formularioCargado
        ) {
            nombre = eventoExistente.nombre
            ambito = eventoExistente.ambito
            icono = eventoExistente.icono
            color = eventoExistente.color
            sumaMarcador = eventoExistente.sumaMarcador
            activo = eventoExistente.activo

            camposObligatorios =
                eventoExistente.camposObligatorios.toSet()

            camposOpcionales =
                eventoExistente.camposOpcionales.toSet()

            formularioCargado = true
        }
    }

    val camposDisponibles = listOf(
        Campos.MINUTO,
        Campos.EQUIPO,
        Campos.JUGADOR,
        Campos.RESULTADO,
        Campos.JUGADOR_SALE,
        Campos.JUGADOR_ENTRA,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (esEdicion) {
                            "Configurar evento"
                        } else {
                            "Nuevo evento"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onVolver
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(16.dp),
        ) {

            Text(
                text = if (esEdicion) {
                    "US-013 · CONFIGURAR EVENTO"
                } else {
                    "US-013 · NUEVO EVENTO"
                },
                style =
                    MaterialTheme.typography.labelMedium,
                color =
                    MaterialTheme.colorScheme.primary,
            )
            if (esEdicion && eventoExistente != null) {
                Text(
                    text = "Versión actual: ${eventoExistente.version}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = "Información básica",
                style =
                    MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            OutlinedTextField(
                value = nombre,
                onValueChange = {
                    nombre = it.take(40)
                },
                label = {
                    Text("Nombre del evento")
                },
                placeholder = {
                    Text("Ejemplo: Gol de cabeza")
                },
                modifier =
                    Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Text(
                text = "Ámbito del evento",
                fontWeight = FontWeight.SemiBold,
            )

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                AmbitoEvento.entries.forEach { opcion ->

                    FilterChip(
                        selected = ambito == opcion,
                        onClick = {
                            ambito = opcion
                        },
                        label = {
                            Text(
                                opcion.name
                                    .lowercase()
                                    .replaceFirstChar {
                                        it.uppercase()
                                    }
                            )
                        }
                    )
                }
            }

            Text(
                text = "Apariencia",
                style =
                    MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            OutlinedTextField(
                value = icono,
                onValueChange = {
                    icono = it.take(40)
                },
                label = {
                    Text("Icono")
                },
                supportingText = {
                    Text(
                        "Ejemplo: sports_soccer"
                    )
                },
                modifier =
                    Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = color,
                onValueChange = {
                    color = it.take(9)
                },
                label = {
                    Text("Color")
                },
                supportingText = {
                    Text(
                        "Formato hexadecimal. Ejemplo: #2E7D32"
                    )
                },
                modifier =
                    Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Text(
                text = "Campos obligatorios",
                style =
                    MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            camposDisponibles.forEach { campo ->

                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Checkbox(
                        checked =
                            campo in camposObligatorios,
                        onCheckedChange = { marcado ->

                            camposObligatorios =
                                if (marcado) {
                                    camposObligatorios + campo
                                } else {
                                    camposObligatorios - campo
                                }

                            if (marcado) {
                                camposOpcionales =
                                    camposOpcionales - campo
                            }
                        }
                    )

                    Text(
                        text = campo,
                        modifier =
                            Modifier.padding(top = 12.dp),
                    )
                }
            }

            Text(
                text = "Campos opcionales",
                style =
                    MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            camposDisponibles.forEach { campo ->

                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Checkbox(
                        checked =
                            campo in camposOpcionales,
                        enabled =
                            campo !in camposObligatorios,
                        onCheckedChange = { marcado ->

                            camposOpcionales =
                                if (marcado) {
                                    camposOpcionales + campo
                                } else {
                                    camposOpcionales - campo
                                }
                        }
                    )

                    Text(
                        text = campo,
                        modifier =
                            Modifier.padding(top = 12.dp),
                    )
                }
            }

            Text(
                text = "Comportamiento",
                style =
                    MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
            ) {

                Column {
                    Text(
                        text = "Suma al marcador",
                        fontWeight =
                            FontWeight.SemiBold,
                    )

                    Text(
                        text =
                            "Por ejemplo, para eventos de gol.",
                        style =
                            MaterialTheme.typography.bodySmall,
                    )
                }

                Switch(
                    checked = sumaMarcador,
                    onCheckedChange = {
                        sumaMarcador = it
                    }
                )
            }

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
            ) {

                Column {
                    Text(
                        text = "Evento activo",
                        fontWeight =
                            FontWeight.SemiBold,
                    )

                    Text(
                        text =
                            if (activo) {
                                "Disponible durante los partidos"
                            } else {
                                "No aparecerá para registrar eventos"
                            },
                        style =
                            MaterialTheme.typography.bodySmall,
                    )
                }

                Switch(
                    checked = activo,
                    onCheckedChange = {
                        activo = it
                    }
                )
            }

            state.error?.let { error ->

                Text(
                    text = error,
                    color =
                        MaterialTheme.colorScheme.error,
                    style =
                        MaterialTheme.typography.bodyMedium,
                )
            }

            Button(
                onClick = {

                    val guardado =
                        if (
                            esEdicion &&
                            eventoExistente != null
                        ) {
                            viewModel.actualizarEvento(
                                original = eventoExistente,
                                nombre = nombre,
                                ambito = ambito,
                                camposObligatorios =
                                    camposObligatorios.toList(),
                                camposOpcionales =
                                    camposOpcionales.toList(),
                                sumaMarcador = sumaMarcador,
                                activo = activo,
                                icono = icono,
                                color = color,
                            )
                        } else {
                            viewModel.crearEvento(
                                nombre = nombre,
                                ambito = ambito,
                                camposObligatorios =
                                    camposObligatorios.toList(),
                                camposOpcionales =
                                    camposOpcionales.toList(),
                                sumaMarcador = sumaMarcador,
                                activo = activo,
                                icono = icono,
                                color = color,
                            )
                        }

                    if (guardado) {
                        onGuardado()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (esEdicion) {
                        "Guardar cambios"
                    } else {
                        "Guardar evento"
                    }
                )
            }
        }
    }
}

