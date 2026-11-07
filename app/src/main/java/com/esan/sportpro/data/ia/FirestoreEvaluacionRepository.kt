package com.esan.sportpro.data.ia

import com.esan.sportpro.data.esperarConfirmacion
import com.esan.sportpro.domain.ia.EvaluacionRepository
import com.esan.sportpro.domain.ia.EvaluacionResumen
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val COLECCION = "evaluacionesResumen"

/** Evaluaciones documentadas de los resúmenes (US-025) en `evaluacionesResumen/{partidoId}_{resumenId}`. */
@Singleton
class FirestoreEvaluacionRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : EvaluacionRepository {

    private val coleccion get() = firestore.collection(COLECCION)

    override fun observarEvaluacion(partidoId: String, resumenId: String): Flow<EvaluacionResumen?> = callbackFlow {
        val registro = coleccion.document("${partidoId}_$resumenId").addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.takeIf { it.exists() }?.aEvaluacion())
        }
        awaitClose { registro.remove() }
    }

    override fun observarEvaluaciones(
        uid: String,
        esAdministrador: Boolean,
        academiaId: String,
    ): Flow<List<EvaluacionResumen>> = callbackFlow {
        // Las reglas solo dejan leer las evaluaciones propias del entrenador o las de la academia del
        // administrador, por eso cada rol consulta con su filtro.
        val consulta: Query = if (esAdministrador) {
            coleccion.whereEqualTo("academiaId", academiaId)
        } else {
            coleccion.whereEqualTo("aprobadoPor", uid)
        }
        val registro = consulta.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val lista = snapshot?.documents.orEmpty().mapNotNull { it.aEvaluacion() }
                // Las que aún no tienen la marca del servidor son las recién guardadas: van primero.
                .sortedByDescending { it.creadoEnMs ?: Long.MAX_VALUE }
            trySend(lista)
        }
        awaitClose { registro.remove() }
    }

    override suspend fun guardar(evaluacion: EvaluacionResumen): Boolean =
        coleccion.document("${evaluacion.partidoId}_${evaluacion.resumenId}").set(
            mapOf(
                "partidoId" to evaluacion.partidoId,
                "resumenId" to evaluacion.resumenId,
                "version" to evaluacion.version,
                "partidoNombre" to evaluacion.partidoNombre,
                "fechaPartidoMs" to evaluacion.fechaPartidoMs,
                "academiaId" to evaluacion.academiaId,
                "aprobadoPor" to evaluacion.aprobadoPor,
                "evaluadorUid" to evaluacion.evaluadorUid,
                "utilidad" to evaluacion.utilidad,
                "omisiones" to evaluacion.omisiones,
                "errores" to evaluacion.errores,
                "inventados" to evaluacion.inventados,
                "comentario" to evaluacion.comentario.trim(),
                "correcciones" to evaluacion.correcciones.trim(),
                "creadoEn" to FieldValue.serverTimestamp(),
            ),
        ).esperarConfirmacion()

    private fun DocumentSnapshot.aEvaluacion(): EvaluacionResumen? {
        val partidoId = getString("partidoId") ?: return null
        val resumenId = getString("resumenId") ?: return null
        return EvaluacionResumen(
            id = id,
            partidoId = partidoId,
            resumenId = resumenId,
            version = getLong("version")?.toInt() ?: 1,
            partidoNombre = getString("partidoNombre").orEmpty(),
            fechaPartidoMs = getLong("fechaPartidoMs"),
            academiaId = getString("academiaId").orEmpty(),
            aprobadoPor = getString("aprobadoPor").orEmpty(),
            evaluadorUid = getString("evaluadorUid").orEmpty(),
            utilidad = getLong("utilidad")?.toInt() ?: 0,
            omisiones = getLong("omisiones")?.toInt() ?: 0,
            errores = getLong("errores")?.toInt() ?: 0,
            inventados = getLong("inventados")?.toInt() ?: 0,
            comentario = getString("comentario").orEmpty(),
            correcciones = getString("correcciones").orEmpty(),
            creadoEnMs = getTimestamp("creadoEn", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time,
        )
    }
}
