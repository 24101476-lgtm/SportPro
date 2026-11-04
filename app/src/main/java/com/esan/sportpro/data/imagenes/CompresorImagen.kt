package com.esan.sportpro.data.imagenes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reduce una imagen elegida por el usuario a un JPEG de máximo [LADO_MAXIMO] px por lado antes de
 * subirla a Supabase Storage. Lo usan la biblioteca de ejercicios (US-009), la foto del jugador (US-006)
 * y las publicaciones del muro (US-028).
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
            // Con inJustDecodeBounds, decodeStream siempre devuelve null: el resultado está en limites.
            val flujo = resolver.openInputStream(uri) ?: return@withContext null
            flujo.use { BitmapFactory.decodeStream(it, null, limites) }
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

            // 3) Aplicar la orientación EXIF: el JPEG de salida ya no la conserva y las fotos de
            // celular quedarían de lado.
            val grados = gradosDeRotacion(uri)
            val orientada = if (grados == 0f) {
                escalada
            } else {
                val rotacion = Matrix().apply { postRotate(grados) }
                Bitmap.createBitmap(escalada, 0, 0, escalada.width, escalada.height, rotacion, true)
            }

            val salida = ByteArrayOutputStream()
            orientada.compress(Bitmap.CompressFormat.JPEG, calidad, salida)
            if (orientada !== escalada) orientada.recycle()
            if (escalada !== original) escalada.recycle()
            original.recycle()
            salida.toByteArray()
        }

    /** Los formatos sin EXIF, como PNG, se tratan como imágenes sin rotación. */
    private fun gradosDeRotacion(uri: Uri): Float = runCatching {
        val orientacion = context.contentResolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }
        when (orientacion) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    }.getOrDefault(0f)

    companion object {
        const val LADO_MAXIMO = 1024
        const val CALIDAD_JPEG = 85
    }
}
