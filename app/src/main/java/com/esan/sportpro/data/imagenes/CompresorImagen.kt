package com.esan.sportpro.data.imagenes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reduce una imagen elegida por el usuario a un JPEG de máximo [LADO_MAXIMO] px por lado antes de
 * subirla a Cloud Storage. Lo usan la biblioteca de ejercicios (US-009) y la foto del jugador (US-006).
 */
@Singleton
class CompresorImagen @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** @return los bytes del JPEG comprimido, o `null` si la URI no es una imagen legible. */
    suspend fun comprimir(uri: Uri, ladoMaximo: Int = LADO_MAXIMO, calidad: Int = CALIDAD_JPEG): ByteArray? =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver

            // 1) Leer solo las dimensiones para decodificar a un tamaño cercano sin agotar memoria.
            val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, limites) } ?: return@withContext null
            if (limites.outWidth <= 0 || limites.outHeight <= 0) return@withContext null

            var muestreo = 1
            while (maxOf(limites.outWidth, limites.outHeight) / (muestreo * 2) >= ladoMaximo) muestreo *= 2

            val opciones = BitmapFactory.Options().apply { inSampleSize = muestreo }
            val original = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }
                ?: return@withContext null

            // 2) Escalar exacto al lado máximo, manteniendo la proporción.
            val mayor = maxOf(original.width, original.height)
            val escalada = if (mayor > ladoMaximo) {
                val factor = ladoMaximo.toFloat() / mayor
                Bitmap.createScaledBitmap(
                    original,
                    (original.width * factor).toInt().coerceAtLeast(1),
                    (original.height * factor).toInt().coerceAtLeast(1),
                    true,
                )
            } else {
                original
            }

            val salida = ByteArrayOutputStream()
            escalada.compress(Bitmap.CompressFormat.JPEG, calidad, salida)
            if (escalada !== original) escalada.recycle()
            original.recycle()
            salida.toByteArray()
        }

    companion object {
        const val LADO_MAXIMO = 1024
        const val CALIDAD_JPEG = 85
    }
}
