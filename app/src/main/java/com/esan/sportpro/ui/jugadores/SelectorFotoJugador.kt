package com.esan.sportpro.ui.jugadores

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import java.io.File

/**
 * US-006, criterio 7: foto del jugador tomada con la cámara o elegida de la galería. La imagen se
 * comprime a 1024 px y se sube recién al guardar la ficha (ver `JugadorRepository.subirFoto`).
 */
@Composable
internal fun SelectorFotoJugador(
    fotoVisible: String?,
    iniciales: String,
    onFotoElegida: (String) -> Unit,
    onQuitarFotoNueva: (() -> Unit)?,
) {
    val context = LocalContext.current
    var uriCamara by rememberSaveable { mutableStateOf<String?>(null) }

    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { tomada ->
        val uri = uriCamara
        if (tomada && uri != null) onFotoElegida(uri)
    }
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) onFotoElegida(uri.toString())
    }

    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        FotoJugador(fotoVisible, iniciales, tamano = 88.dp)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = {
                val uri = uriTemporal(context)
                uriCamara = uri.toString()
                camara.launch(uri)
            }) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null)
                Text("  Tomar foto")
            }
            OutlinedButton(onClick = { galeria.launch("image/*") }) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                Text("  Elegir de la galería")
            }
            if (onQuitarFotoNueva != null) {
                TextButton(onClick = onQuitarFotoNueva) { Text("Descartar foto nueva") }
            }
        }
    }
}

/** Foto del jugador, o sus iniciales si no tiene. */
@Composable
internal fun FotoJugador(url: String?, iniciales: String, tamano: Dp = 72.dp) {
    Box(
        modifier = Modifier.size(tamano).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = "Foto del jugador",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(iniciales, style = MaterialTheme.typography.titleLarge)
        }
    }
}

/** Archivo temporal en la caché para que la app de cámara guarde la foto. */
private fun uriTemporal(context: Context): Uri {
    val carpeta = File(context.cacheDir, "fotos").apply { mkdirs() }
    val archivo = File(carpeta, "jugador_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.archivos", archivo)
}
