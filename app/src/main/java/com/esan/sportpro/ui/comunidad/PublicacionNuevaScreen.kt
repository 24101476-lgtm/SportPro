package com.esan.sportpro.ui.comunidad

import com.esan.sportpro.ui.common.BarraSuperiorSportPro
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.esan.sportpro.core.theme.PaletaSportPro
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.comunidad.VisibilidadPublicacion
import kotlinx.coroutines.flow.collectLatest

/** Ideas para empezar una publicación: etiqueta del botón y texto que se agrega al inicio. */
private val IDEAS = listOf(
    "🏆 Logro" to "🏆 ",
    "⚽ Partido" to "⚽ ",
    "📸 Foto del día" to "📸 ",
    "💪 Entrenamiento" to "💪 ",
)

/** US-028, criterios 1 y 2 — Frame 132_Publicacion_Nueva (incluye la alerta del frame 135). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicacionNuevaScreen(
    onPublicada: () -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PublicacionesViewModel = hiltViewModel(),
) {
    var texto by remember { mutableStateOf("") }
    var imagenes by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var visibilidad by remember { mutableStateOf(VisibilidadPublicacion.SOLO_ACADEMIA) }
    var mostrarAlertaTelefono by remember { mutableStateOf(false) }
    var errorMensaje by remember { mutableStateOf<String?>(null) }

    val textoValido = texto.length in 1..1000

    val estado by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            BarraSuperiorSportPro(
                title = { Text("Nueva publicación") },
                navigationIcon = {
                    IconButton(onClick = onCancelar) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Cancelar")
                    }
                },
            )
        },
        bottomBar = {
            // Barra fija: siempre se ve el botón aunque el texto sea largo o salga el teclado.
            Surface(color = MaterialTheme.colorScheme.background) {
                Row(
                    modifier = Modifier.fillMaxWidth().imePadding().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${texto.length}/1000",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (texto.length > 900) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = { viewModel.publicar(texto, imagenes, visibilidad) },
                        enabled = textoValido,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PaletaSportPro.Lima,
                            contentColor = PaletaSportPro.Carbon,
                        ),
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Publicar", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Tarjeta del texto: quién publica y el campo sin bordes, como una red social.
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AvatarAutor(estado.autorNombre)
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(
                                estado.autorNombre.ifBlank { "Tú" },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                visibilidad.etiqueta,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    TextField(
                        value = texto,
                        onValueChange = { texto = it.take(1000) },
                        placeholder = { Text("¿Qué quieres compartir con tu academia?") },
                        minLines = 5,
                        textStyle = MaterialTheme.typography.bodyLarge,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    )
                    // Ideas rápidas: ponen un emoji y un inicio de frase para no empezar en blanco.
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                    ) {
                        IDEAS.forEach { (etiqueta, inicio) ->
                            Surface(
                                onClick = { if (!texto.startsWith(inicio)) texto = (inicio + texto).take(1000) },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                            ) {
                                Text(
                                    etiqueta,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Fotos (hasta 3)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                SelectorImagenes(imagenes = imagenes, onImagenesChange = { imagenes = it })
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("¿Quién lo verá?", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VisibilidadPublicacion.entries.forEach { opcion ->
                        val activa = visibilidad == opcion
                        Surface(
                            onClick = { visibilidad = opcion },
                            shape = RoundedCornerShape(18.dp),
                            color = if (activa) PaletaSportPro.Musgo else MaterialTheme.colorScheme.surface,
                            contentColor = if (activa) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                            ) {
                                Icon(
                                    if (opcion == VisibilidadPublicacion.SOLO_ACADEMIA) Icons.Default.Lock else Icons.Default.Public,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    opcion.etiqueta,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (activa) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }
                    }
                }
            }

            if (errorMensaje != null) {
                Text(errorMensaje.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    if (mostrarAlertaTelefono) {
        AlertaDatosContactoDialog(
            onConfirmar = {
                mostrarAlertaTelefono = false
                viewModel.publicar(texto, imagenes, visibilidad, confirmadoTelefono = true)
            },
            onCancelar = { mostrarAlertaTelefono = false },
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is PublicacionesEvent.RequiereConfirmacionTelefono -> mostrarAlertaTelefono = true
                is PublicacionesEvent.Publicada -> onPublicada()
                is PublicacionesEvent.Error -> errorMensaje = event.mensaje
                else -> Unit
            }
        }
    }
}
