package com.esan.sportpro.data.ia

import com.esan.sportpro.data.esperarConfirmacion
import com.esan.sportpro.domain.ia.BloquesResumen
import com.esan.sportpro.domain.ia.EstadoResumen
import com.esan.sportpro.domain.ia.MencionNoVerificada
import com.esan.sportpro.domain.ia.ResumenPartido
import com.esan.sportpro.domain.ia.ResumenRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Versiones del resumen en `partidos/{partidoId}/resumenes` (US-023). */
@Singleton
class FirestoreResumenRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : ResumenRepository {

    private fun coleccion(partidoId: String) =
        firestore.collection("partidos").document(partidoId).collection("resumenes")

    override fun observarResumenes(partidoId: String): Flow<List<ResumenPartido>> = callbackFlow {
        val registro = coleccion(partidoId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.documents.orEmpty().mapNotNull { it.aResumen(partidoId) }.sortedByDescending { it.version })
        }
        awaitClose { registro.remove() }
    }

    override suspend fun guardarBorrador(resumen: ResumenPartido): Boolean {
        val referencia = coleccion(resumen.partidoId).document()
        val datos = mapOf(
            "estado" to EstadoResumen.BORRADOR.name,
            "version" to resumen.version,
            "hechos" to resumen.bloques.hechos,
            "desarrollo" to resumen.bloques.desarrollo,
            "interpretacion" to resumen.bloques.interpretacion,
            "modelo" to resumen.modelo,
            "versionPrompt" to resumen.versionPrompt,
            "solicitadoPor" to resumen.solicitadoPor,
            "creadoEn" to FieldValue.serverTimestamp(),
            "marcador" to resumen.marcador,
            "eventosUsados" to resumen.eventosUsados,
            "cantidadEventos" to resumen.eventosUsados.size,
            "advertencias" to resumen.advertencias,
            "mencionesNoVerificadas" to resumen.mencionesNoVerificadas.map {
                mapOf("texto" to it.texto, "motivo" to it.motivo)
            },
        )
        return referencia.set(datos).esperarConfirmacion()
    }

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.aResumen(partidoId: String): ResumenPartido? {
        val hechos = getString("hechos") ?: return null
        val menciones = (get("mencionesNoVerificadas") as? List<Map<String, Any?>>).orEmpty().map {
            MencionNoVerificada(it["texto"] as? String ?: "", it["motivo"] as? String ?: "")
        }
        return ResumenPartido(
            id = id,
            partidoId = partidoId,
            version = getLong("version")?.toInt() ?: 1,
            estado = EstadoResumen.entries.firstOrNull { it.name == getString("estado") } ?: EstadoResumen.BORRADOR,
            bloques = BloquesResumen(
                hechos = hechos,
                desarrollo = getString("desarrollo").orEmpty(),
                interpretacion = getString("interpretacion").orEmpty(),
            ),
            modelo = getString("modelo").orEmpty(),
            versionPrompt = getString("versionPrompt").orEmpty(),
            solicitadoPor = getString("solicitadoPor").orEmpty(),
            creadoEnMs = getTimestamp("creadoEn", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time,
            mencionesNoVerificadas = menciones,
            advertencias = (get("advertencias") as? List<String>).orEmpty(),
            eventosUsados = (get("eventosUsados") as? List<String>).orEmpty(),
            marcador = getString("marcador").orEmpty(),
        )
    }
}
