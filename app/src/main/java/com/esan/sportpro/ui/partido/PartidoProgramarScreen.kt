package com.esan.sportpro.ui.partido

import com.esan.sportpro.core.fecha.FechasSportPro
import com.esan.sportpro.ui.common.BarraSuperiorSportPro
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.partido.TiposPartido
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartidoProgramarScreen(
    onVolver: () -> Unit,
    onPartidoCreado: (String) -> Unit,
    viewModel: PartidoProgramarViewModel = hiltViewModel(),
) {

    val context =
        LocalContext.current

    val ui by viewModel.uiState.collectAsState()

    var equipoId by rememberSaveable {
        mutableStateOf<String?>(null)
    }
    // Se preselecciona el primer equipo activo apenas llegan los equipos.
    if (equipoId == null && ui.equipos.isNotEmpty()) {
        equipoId = ui.equipos.first().id
    }

    var nombreRival by rememberSaveable {
        mutableStateOf("")
    }

    var fechaMs by rememberSaveable {
        mutableStateOf<Long?>(null)
    }

    var hora by rememberSaveable {
        mutableStateOf("")
    }

    var lugar by rememberSaveable {
        mutableStateOf("")
    }

    var tipoPartido by rememberSaveable {
        mutableStateOf("Amistoso")
    }

    var duracion by rememberSaveable {
        mutableStateOf("45")
    }

    var maxConvocados by rememberSaveable {
        mutableStateOf("23")
    }

    var fechaLimiteMs by rememberSaveable {
        mutableStateOf<Long?>(null)
    }

    var errorFormulario by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    val formatoFecha =
        FechasSportPro.formatoFecha()

    Scaffold(
        topBar = {
            BarraSuperiorSportPro(
                title = {
                    Text(
                        "Programar partido"
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding =
                PaddingValues(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(14.dp),
        ) {

            item {
                Text(
                    text = "US-014 · NUEVO PARTIDO",
                    style =
                        MaterialTheme
                            .typography
                            .labelMedium,
                    color =
                        MaterialTheme
                            .colorScheme
                            .primary
                )
            }

            item {
                Text(
                    text = "Datos del partido",
                    style =
                        MaterialTheme
                            .typography
                            .titleLarge,
                    fontWeight =
                        FontWeight.Bold
                )
            }

            item {
                Text(
                    text =
                        "Completa la información antes de seleccionar a los jugadores que serán convocados.",
                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }

            item {
                Text("Equipo", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 6.dp))
                when {
                    ui.cargando -> Text(
                        "Cargando equipos…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ui.equipos.isEmpty() -> Text(
                        "Tu academia aún no tiene equipos activos. El administrador puede crearlos en la pestaña Academia.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    else -> Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                    ) {
                        ui.equipos.forEach { e ->
                            FilterChip(
                                selected = equipoId == e.id,
                                onClick = {
                                    equipoId = e.id
                                    errorFormulario = null
                                },
                                label = { Text(e.etiqueta) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ColoresPartido.Verde,
                                    selectedLabelColor = Color.White,
                                ),
                                modifier = Modifier.heightIn(min = 48.dp),
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = nombreRival,
                    onValueChange = {
                        nombreRival = it
                        errorFormulario = null
                    },
                    label = {
                        Text("Rival")
                    },
                    placeholder = {
                        Text(
                            "Nombre del equipo rival"
                        )
                    },
                    singleLine = true,
                    modifier =
                        Modifier.fillMaxWidth()
                )
            }

            item {

                OutlinedTextField(
                    value =
                        fechaMs?.let {
                            formatoFecha.format(
                                Date(it)
                            )
                        }.orEmpty(),

                    onValueChange = { },

                    readOnly = true,

                    label = {
                        Text(
                            "Fecha del partido"
                        )
                    },

                    placeholder = {
                        Text(
                            "Seleccionar fecha"
                        )
                    },

                    trailingIcon = {
                        IconButton(
                            onClick = {

                                val calendario =
                                    Calendar
                                        .getInstance()

                                DatePickerDialog(
                                    context,
                                    {
                                            _,
                                            year,
                                            month,
                                            day ->

                                        val seleccionado =
                                            Calendar
                                                .getInstance()
                                                .apply {
                                                    set(
                                                        Calendar.YEAR,
                                                        year
                                                    )

                                                    set(
                                                        Calendar.MONTH,
                                                        month
                                                    )

                                                    set(
                                                        Calendar.DAY_OF_MONTH,
                                                        day
                                                    )

                                                    set(
                                                        Calendar.HOUR_OF_DAY,
                                                        0
                                                    )

                                                    set(
                                                        Calendar.MINUTE,
                                                        0
                                                    )

                                                    set(
                                                        Calendar.SECOND,
                                                        0
                                                    )

                                                    set(
                                                        Calendar.MILLISECOND,
                                                        0
                                                    )
                                                }

                                        fechaMs =
                                            seleccionado
                                                .timeInMillis

                                        errorFormulario =
                                            null
                                    },
                                    calendario.get(
                                        Calendar.YEAR
                                    ),
                                    calendario.get(
                                        Calendar.MONTH
                                    ),
                                    calendario.get(
                                        Calendar.DAY_OF_MONTH
                                    )
                                ).show()
                            }
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription =
                                    "Seleccionar fecha"
                            )
                        }
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                )
            }

            item {

                OutlinedTextField(
                    value = hora,
                    onValueChange = { },
                    readOnly = true,

                    label = {
                        Text(
                            "Hora"
                        )
                    },

                    placeholder = {
                        Text(
                            "Seleccionar hora"
                        )
                    },

                    trailingIcon = {

                        IconButton(
                            onClick = {

                                val calendario =
                                    Calendar
                                        .getInstance()

                                TimePickerDialog(
                                    context,
                                    {
                                            _,
                                            horaSeleccionada,
                                            minutoSeleccionado ->

                                        hora =
                                            String.format(
                                                Locale.getDefault(),
                                                "%02d:%02d",
                                                horaSeleccionada,
                                                minutoSeleccionado
                                            )

                                        errorFormulario =
                                            null
                                    },

                                    calendario.get(
                                        Calendar.HOUR_OF_DAY
                                    ),

                                    calendario.get(
                                        Calendar.MINUTE
                                    ),

                                    true
                                ).show()
                            }
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription =
                                    "Seleccionar hora"
                            )
                        }
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = lugar,
                    onValueChange = {
                        lugar = it
                        errorFormulario = null
                    },

                    label = {
                        Text("Lugar")
                    },

                    placeholder = {
                        Text(
                            "Ej. Estadio Municipal"
                        )
                    },

                    singleLine = true,

                    modifier =
                        Modifier.fillMaxWidth()
                )
            }

            item {
                // Solo los tres tipos que entiende Estadísticas (antes era texto libre).
                Text("Tipo de partido", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TiposPartido.todos.forEach { (_, etiqueta) ->
                        FilterChip(
                            selected = tipoPartido == etiqueta,
                            onClick = {
                                tipoPartido = etiqueta
                                errorFormulario = null
                            },
                            label = { Text(etiqueta) },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ColoresPartido.Verde,
                                selectedLabelColor = Color.White,
                            ),
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                }
            }

            item {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(
                            12.dp
                        )
                ) {

                    OutlinedTextField(
                        value =
                            duracion,

                        onValueChange = {
                                valor ->

                            if (
                                valor.all {
                                    it.isDigit()
                                }
                            ) {
                                duracion =
                                    valor

                                errorFormulario =
                                    null
                            }
                        },

                        label = {
                            Text(
                                "Duración"
                            )
                        },

                        suffix = {
                            Text("min")
                        },

                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType =
                                    KeyboardType.Number
                            ),

                        singleLine =
                            true,

                        modifier =
                            Modifier.weight(
                                1f
                            )
                    )

                    OutlinedTextField(
                        value =
                            maxConvocados,

                        onValueChange = {
                                valor ->

                            if (
                                valor.all {
                                    it.isDigit()
                                }
                            ) {
                                maxConvocados =
                                    valor

                                errorFormulario =
                                    null
                            }
                        },

                        label = {
                            Text(
                                "Máx. convocados"
                            )
                        },

                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType =
                                    KeyboardType.Number
                            ),

                        singleLine =
                            true,

                        modifier =
                            Modifier.weight(
                                1f
                            )
                    )
                }
            }

            item {

                OutlinedTextField(
                    value =
                        fechaLimiteMs?.let {
                            formatoFecha.format(
                                Date(it)
                            )
                        }.orEmpty(),

                    onValueChange = { },

                    readOnly = true,

                    label = {
                        Text(
                            "Fecha límite de respuesta"
                        )
                    },

                    placeholder = {
                        Text(
                            "Seleccionar fecha"
                        )
                    },

                    trailingIcon = {

                        IconButton(
                            onClick = {

                                val calendario =
                                    Calendar
                                        .getInstance()

                                DatePickerDialog(
                                    context,
                                    {
                                            _,
                                            year,
                                            month,
                                            day ->

                                        val seleccionado =
                                            Calendar
                                                .getInstance()
                                                .apply {
                                                    set(
                                                        Calendar.YEAR,
                                                        year
                                                    )

                                                    set(
                                                        Calendar.MONTH,
                                                        month
                                                    )

                                                    set(
                                                        Calendar.DAY_OF_MONTH,
                                                        day
                                                    )

                                                    set(
                                                        Calendar.HOUR_OF_DAY,
                                                        0
                                                    )

                                                    set(
                                                        Calendar.MINUTE,
                                                        0
                                                    )

                                                    set(
                                                        Calendar.SECOND,
                                                        0
                                                    )

                                                    set(
                                                        Calendar.MILLISECOND,
                                                        0
                                                    )
                                                }

                                        fechaLimiteMs =
                                            seleccionado
                                                .timeInMillis

                                        errorFormulario =
                                            null
                                    },

                                    calendario.get(
                                        Calendar.YEAR
                                    ),

                                    calendario.get(
                                        Calendar.MONTH
                                    ),

                                    calendario.get(
                                        Calendar.DAY_OF_MONTH
                                    )
                                ).show()
                            }
                        ) {

                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription =
                                    "Seleccionar fecha límite"
                            )
                        }
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                )
            }

            errorFormulario?.let {
                    mensaje ->

                item {
                    Text(
                        text =
                            mensaje,

                        color =
                            MaterialTheme
                                .colorScheme
                                .error,

                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium
                    )
                }
            }

            item {

                Spacer(
                    modifier =
                        Modifier.height(
                            4.dp
                        )
                )

                Button(
                    onClick = {

                        val duracionNumero =
                            duracion
                                .toIntOrNull()

                        val maxNumero =
                            maxConvocados
                                .toIntOrNull()

                        when (
                            val resultado = viewModel.crearPartido(
                                equipoId = equipoId,
                                nombreRival = nombreRival,
                                fechaMs = fechaMs,
                                hora = hora,
                                lugar = lugar,
                                tipoPartido = tipoPartido,
                                duracionTiempoMin = duracionNumero,
                                maxConvocados = maxNumero,
                                fechaLimiteRespuestaMs = fechaLimiteMs,
                            )
                        ) {
                            is ResultadoProgramar.Creado -> onPartidoCreado(resultado.partidoId)
                            is ResultadoProgramar.Error -> errorFormulario = resultado.mensaje
                        }
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        "Continuar a convocatoria"
                    )
                }
            }

            item {
                Spacer(
                    modifier =
                        Modifier.height(
                            16.dp
                        )
                )
            }
        }
    }
}
