package com.esan.sportpro.ui.comunidad

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.comunidad.Aviso
import com.esan.sportpro.domain.comunidad.PosicionJugador
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** US-029, criterios 1 a 5 — Frame 138_Aviso_Nuevo. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AvisoNuevoScreen(
    onPublicado: () -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AvisosViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var titulo by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("") }
    var posiciones by remember { mutableStateOf(setOf<PosicionJugador>()) }
    var fechaTexto by remember { mutableStateOf("") } // dd/MM/yyyy
    var hora by remember { mutableStateOf("") }
    var distrito by remember { mutableStateOf("") }
    var referencia by remember { mutableStateOf("") }
    var requisitos by remember { mutableStateOf("") }
    var contacto by remember { mutableStateOf("") }
    var errorMensaje by remember { mutableStateOf<String?>(null) }

    val formato = remember { SimpleDateFormat("dd/MM/yyyy", Locale("es", "PE")).apply { isLenient = false } }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Nuevo aviso") },
                navigationIcon = {
                    IconButton(onClick = onCancelar) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Cancelar")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(titulo, { titulo = it.take(80) }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
            }
            item {
                OutlinedTextField(categoria, { categoria = it }, label = { Text("Categoría") }, modifier = Modifier.fillMaxWidth())
            }
            item {
                Text("Posiciones buscadas", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PosicionJugador.entries.forEach { opcion ->
                        FilterChip(
                            selected = opcion in posiciones,
                            onClick = { posiciones = if (opcion in posiciones) posiciones - opcion else posiciones + opcion },
                            label = { Text(opcion.etiqueta) },
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        fechaTexto,
                        { fechaTexto = it },
                        label = { Text("Fecha de prueba (dd/MM/aaaa)") },
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(hora, { hora = it }, label = { Text("Hora") }, modifier = Modifier.weight(1f))
                }
            }
            item {
                OutlinedTextField(distrito, { distrito = it }, label = { Text("Distrito") }, modifier = Modifier.fillMaxWidth())
            }
            item {
                OutlinedTextField(referencia, { referencia = it }, label = { Text("Referencia de ubicación") }, modifier = Modifier.fillMaxWidth())
            }
            item {
                OutlinedTextField(requisitos, { requisitos = it }, label = { Text("Requisitos") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            }
            item {
                OutlinedTextField(
                    contacto,
                    { contacto = it },
                    label = { Text("Contacto institucional (correo o teléfono del club)") },
                    supportingText = { Text("Usa el contacto institucional del club, no un número personal") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (errorMensaje != null) {
                item { Text(errorMensaje.orEmpty(), color = MaterialTheme.colorScheme.error) }
            }
            item {
                Button(
                    onClick = {
                        errorMensaje = null
                        val fecha = runCatching { formato.parse(fechaTexto) }.getOrNull()
                        if (fecha == null || !fecha.after(Date())) {
                            errorMensaje = "La fecha debe ser posterior a hoy"
                            return@Button
                        }
                        if (contieneNumeroPersonal(contacto)) {
                            errorMensaje = "Usa el contacto institucional del club"
                            return@Button
                        }
                        viewModel.publicar(
                            Aviso(
                                titulo = titulo,
                                categoria = categoria,
                                posicionesBuscadas = posiciones.map { it.name },
                                fechaPrueba = ajustarHora(fecha, hora),
                                hora = hora,
                                ubicacionDistrito = distrito,
                                ubicacionReferencia = referencia,
                                requisitos = requisitos,
                                contactoInstitucional = contacto,
                            ),
                        )
                    },
                    enabled = titulo.length in 5..80 && contacto.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Publicar aviso") }
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is AvisosEvent.Publicado -> onPublicado()
                is AvisosEvent.Error -> errorMensaje = event.mensaje
                AvisosEvent.Reportado -> Unit
            }
        }
    }
}

/** Un número de celular peruano de 9 dígitos usado como "contacto" se considera personal, no institucional. */
private fun contieneNumeroPersonal(contacto: String): Boolean {
    val soloDigitos = contacto.replace(Regex("[\\s-]"), "")
    return Regex("(?<!\\d)9\\d{8}(?!\\d)").containsMatchIn(soloDigitos) && "@" !in contacto
}

private fun ajustarHora(fecha: Date, hora: String): Date {
    val partes = hora.split(":").mapNotNull { it.toIntOrNull() }
    if (partes.size < 2) return fecha
    return Calendar.getInstance().apply {
        time = fecha
        set(Calendar.HOUR_OF_DAY, partes[0])
        set(Calendar.MINUTE, partes[1])
    }.time
}
