package com.esan.sportpro.data.notificaciones

import android.util.Log
import com.esan.sportpro.data.imagenes.URL_SUPABASE
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * US-027: pide a la Edge Function `notificar` que envíe un push a su tema de FCM
 * (ver `supabase/functions/notificar/index.ts`). La app solo manda el id: el servidor arma el texto
 * con los datos de Firestore y comprueba que quien lo pide hizo la acción.
 *
 * Nunca lanza excepción: si no hay red o la función no está desplegada devuelve `false` y la
 * pantalla lo informa (criterio 10).
 */
@Singleton
class NotificadorPush @Inject constructor(
    private val auth: FirebaseAuth,
) {
    private val cliente = OkHttpClient.Builder()
        .callTimeout(15_000L, TimeUnit.MILLISECONDS)
        .build()

    suspend fun notificarAnuncio(anuncioId: String): Boolean =
        enviar(JSONObject().put("anuncioId", anuncioId))

    /** US-010: avisa a jugadores y padres del equipo que la sesión se canceló, con su motivo. */
    suspend fun notificarSesionCancelada(entrenamientoId: String): Boolean =
        enviar(JSONObject().put("tipo", "sesion_cancelada").put("entrenamientoId", entrenamientoId))

    /** US-007: avisa al padre que su solicitud se aprobó, se rechazó o se revocó. */
    suspend fun notificarVinculacion(vinculacionId: String): Boolean =
        enviar(JSONObject().put("tipo", "vinculacion").put("vinculacionId", vinculacionId))

    private suspend fun enviar(cuerpo: JSONObject): Boolean = runCatching {
        val token = auth.currentUser?.getIdToken(false)?.await()?.token ?: return false
        val peticion = Request.Builder()
            .url("$URL_SUPABASE/functions/v1/notificar")
            .header("Authorization", "Bearer $token")
            .post(cuerpo.toString().toRequestBody(JSON))
            .build()
        withContext(Dispatchers.IO) {
            cliente.newCall(peticion).execute().use { respuesta ->
                if (!respuesta.isSuccessful) {
                    Log.w(TAG, "notificar respondió ${respuesta.code}")
                    false
                } else {
                    JSONObject(respuesta.body?.string().orEmpty()).optBoolean("enviada", false)
                }
            }
        }
    }.onFailure { Log.w(TAG, "No se pudo enviar el push", it) }.getOrDefault(false)

    private companion object {
        const val TAG = "NotificadorPush"
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
