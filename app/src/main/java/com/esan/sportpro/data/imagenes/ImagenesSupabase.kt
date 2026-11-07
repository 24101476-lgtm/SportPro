package com.esan.sportpro.data.imagenes

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

internal const val URL_SUPABASE = "https://ghowgsannwmpuwncwrcc.supabase.co"
private const val URL_FUNCION = "$URL_SUPABASE/functions/v1/imagenes"
private const val PREFIJO_IMAGENES = "$URL_SUPABASE/storage/v1/object/public/imagenes/"
private const val TIEMPO_MAXIMO_MS = 20_000L

/** Carpetas del bucket `imagenes`; la Edge Function rechaza cualquier otra. */
enum class CarpetaImagen(val nombre: String) {
    JUGADORES("jugadores"),
    EJERCICIOS("ejercicios"),
    PUBLICACIONES("publicaciones"),
    ESCUDOS("escudos"),
}

/** `true` si la URL apunta al bucket de Supabase; las URLs antiguas de Firebase se ignoran. */
fun esImagenDeSupabase(url: String): Boolean = url.startsWith(PREFIJO_IMAGENES)

/**
 * Sube y elimina imágenes en Supabase Storage a través de la Edge Function `imagenes`, que valida
 * el ID token de Firebase Auth (ver `supabase/functions/imagenes/index.ts`). Como Storage, no tiene
 * cola offline: sin conexión las operaciones lanzan [IOException] y quien llama decide qué hacer.
 */
@Singleton
class ImagenesSupabase @Inject constructor(
    private val auth: FirebaseAuth,
) {
    private val cliente = OkHttpClient.Builder()
        .callTimeout(TIEMPO_MAXIMO_MS, TimeUnit.MILLISECONDS)
        .build()

    /** @return la URL pública de la imagen. El nombre es aleatorio: cada subida genera una URL nueva. */
    suspend fun subir(carpeta: CarpetaImagen, jpeg: ByteArray): String = runCatching {
        val url = URL_FUNCION.toHttpUrl().newBuilder().addQueryParameter("carpeta", carpeta.nombre).build()
        val peticion = peticionAutenticada(url).post(jpeg.toRequestBody(TIPO_JPEG)).build()
        JSONObject(ejecutar(peticion)).getString("url")
    }.onFailure { Log.w(TAG, "No se pudo subir la imagen a ${carpeta.nombre}", it) }.getOrThrow()

    /** Elimina la imagen. La función solo lo permite a quien la subió. */
    suspend fun eliminar(urlImagen: String) {
        if (!esImagenDeSupabase(urlImagen)) return
        val url = URL_FUNCION.toHttpUrl().newBuilder().addQueryParameter("url", urlImagen).build()
        ejecutar(peticionAutenticada(url).delete().build())
    }

    private suspend fun peticionAutenticada(url: HttpUrl): Request.Builder {
        val token = auth.currentUser?.getIdToken(false)?.await()?.token
            ?: throw IOException("No hay una sesión activa")
        return Request.Builder().url(url).header("Authorization", "Bearer $token")
    }

    private suspend fun ejecutar(peticion: Request): String = withContext(Dispatchers.IO) {
        cliente.newCall(peticion).execute().use { respuesta ->
            val cuerpo = respuesta.body?.string().orEmpty()
            if (!respuesta.isSuccessful) throw IOException("Supabase respondió ${respuesta.code}: $cuerpo")
            cuerpo
        }
    }

    private companion object {
        const val TAG = "ImagenesSupabase"
        val TIPO_JPEG = "image/jpeg".toMediaType()
    }
}
