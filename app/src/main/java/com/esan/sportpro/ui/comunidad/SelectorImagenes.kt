package com.esan.sportpro.ui.comunidad

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Selector de hasta 3 imágenes locales para una publicación nueva (US-028, criterio 1). La subida
 * real a Cloud Storage ocurre recién al publicar, en [com.esan.sportpro.data.comunidad.PublicacionRepository.publicar].
 */
@Composable
fun SelectorImagenes(
    imagenes: List<Uri>,
    onImagenesChange: (List<Uri>) -> Unit,
    modifier: Modifier = Modifier,
    maximo: Int = 3,
) {
    val selector = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && imagenes.size < maximo) onImagenesChange(imagenes + uri)
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        imagenes.forEach { uri ->
            Box(modifier = Modifier.size(72.dp)) {
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
                IconButton(
                    onClick = { onImagenesChange(imagenes - uri) },
                    modifier = Modifier.size(20.dp).align(Alignment.TopEnd),
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Quitar imagen", tint = Color.White)
                }
            }
        }
        if (imagenes.size < maximo) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(onClick = { selector.launch("image/*") }) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Agregar imagen")
                }
            }
        }
    }
}
