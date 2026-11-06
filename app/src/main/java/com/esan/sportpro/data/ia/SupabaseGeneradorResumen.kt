package com.esan.sportpro.data.ia

import android.util.Log
import com.esan.sportpro.data.imagenes.URL_SUPABASE
import com.esan.sportpro.domain.ia.Afirmacion
import com.esan.sportpro.domain.ia.ErrorResumen
import com.esan.sportpro.domain.ia.GeneradorResumen
import com.esan.sportpro.domain.ia.InsumoResumen
import com.esan.sportpro.domain.ia.SalidaIa
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val URL_FUNCION = "$URL_SUPABASE/functions/v1/resumen-ia"
private const val TIEMPO_MAXIMO_MS = 30_000L

/** Códigos con los que la función explica por qué no genera; su mensaje sí se muestra tal cual. */
private val CODIGOS_CON_MENSAJE = setOf(403, 409, 422, 429)

/**
 * Llama a la Edge Function `resumen-ia` de Supabase (ver `supabase/functions/resumen-ia/index.ts`),
 * que valida el ID token de Firebase, guarda la clave de Gemini como secreto y devuelve el resumen
 * con los jugadores como alias. Sin conexión o pasados 30 s lanza [ErrorResumen].
 */
@Singleton
class SupabaseGeneradorResumen @Inject constructor(
    private val auth: FirebaseAuth,
) : GeneradorResumen {

    // readTimeout también se fija: su valor por defecto (10 s) cortaba la llamada mucho antes de los
    // 30 s, justo cuando la función estaba probando los modelos de respaldo.
    private val cliente = OkHttpClient.Builder()
        .callTimeout(TIEMPO_MAXIMO_MS, TimeUnit.MILLISECONDS)
        .readTimeout(TIEMPO_MAXIMO_MS, TimeUnit.MILLISECONDS)
        .build()

    override suspend fun generar(partidoId: String, insumo: InsumoResumen): SalidaIa = try {
        val token = auth.currentUser?.getIdToken(false)?.await()?.token
            ?: throw ErrorResumen("Vuelve a iniciar sesión para generar el resumen")
        val cuerpo = JSONObject()
            .put("partidoId", partidoId)
            .put("insumo", aJson(insumo))
            .toString()
        val peticion = Request.Builder()
            .url(URL_FUNCION)
            .header("Authorization", "Bearer $token")
            .post(cuerpo.toRequestBody(TIPO_JSON))
            .build()
        leer(ejecutar(peticion))
    } catch (e: ErrorResumen) {
        throw e
    } catch (e: IOException) {
        Log.w(TAG, "Fallo de red al generar el resumen", e)
        throw ErrorResumen(ErrorResumen.GENERICO)
    } catch (e: JSONException) {
        Log.w(TAG, "Respuesta no válida de la función", e)
        throw ErrorResumen(ErrorResumen.GENERICO)
    }

    private suspend fun ejecutar(peticion: Request): String = withContext(Dispatchers.IO) {
        cliente.newCall(peticion).execute().use { respuesta ->
            val texto = respuesta.body?.string().orEmpty()
            if (!respuesta.isSuccessful) {
                Log.w(TAG, "La función respondió ${respuesta.code}")
                val mensaje = runCatching { JSONObject(texto).getString("error") }.getOrNull()
                throw ErrorResumen(if (respuesta.code in CODIGOS_CON_MENSAJE && mensaje != null) mensaje else ErrorResumen.GENERICO)
            }
            texto
        }
    }

    private fun leer(texto: String): SalidaIa {
        val json = JSONObject(texto)
        return SalidaIa(
            modelo = json.getString("modelo"),
            hechos = afirmaciones(json.getJSONArray("hechos")),
            desarrollo = afirmaciones(json.getJSONArray("desarrollo")),
            advertencias = json.optJSONArray("advertencias").textos(),
        )
    }

    private fun afirmaciones(lista: JSONArray): List<Afirmacion> =
        (0 until lista.length()).map { i ->
            val item = lista.getJSONObject(i)
            Afirmacion(item.getString("texto"), item.optJSONArray("eventoIds").textos())
        }

    private fun JSONArray?.textos(): List<String> =
        if (this == null) emptyList() else (0 until length()).map { getString(it) }

    /** Solo lo que puede salir hacia el proveedor: el mapa de alias a nombres se queda en el dispositivo. */
    private fun aJson(insumo: InsumoResumen): JSONObject = JSONObject()
        .put(
            "partido",
            JSONObject()
                .put("categoria", insumo.partido.categoria ?: JSONObject.NULL)
                .put("equipo", insumo.partido.equipo)
                .put("rival", insumo.partido.rival)
                .put("duracionTiempoMin", insumo.partido.duracionTiempoMin)
                .put("marcadorPropio", insumo.partido.marcadorPropio)
                .put("marcadorRival", insumo.partido.marcadorRival),
        )
        .put(
            "alineacion",
            JSONArray(
                insumo.alineacion.map { j ->
                    JSONObject()
                        .put("alias", j.alias)
                        .put("dorsal", j.dorsal ?: JSONObject.NULL)
                        .put("titular", j.titular)
                        .put("posicion", j.posicion ?: JSONObject.NULL)
                },
            ),
        )
        .put(
            "eventos",
            JSONArray(
                insumo.eventos.map { e ->
                    JSONObject()
                        .put("id", e.id)
                        .put("minuto", e.minuto)
                        .put("periodo", e.periodo)
                        .put("tipo", e.tipo)
                        .put("equipo", e.equipo)
                        .put("jugador", e.jugador ?: JSONObject.NULL)
                        .put("jugadorSecundario", e.jugadorSecundario ?: JSONObject.NULL)
                        .put("resultadoPenal", e.resultadoPenal ?: JSONObject.NULL)
                        .put("observacion", e.observacion ?: JSONObject.NULL)
                },
            ),
        )
        .put("incompletos", JSONArray(insumo.incompletos))

    private companion object {
        const val TAG = "GeneradorResumen"
        val TIPO_JSON = "application/json; charset=utf-8".toMediaType()
    }
}
