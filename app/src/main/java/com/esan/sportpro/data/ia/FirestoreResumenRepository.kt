package com.esan.sportpro.data.ia

import com.esan.sportpro.data.esperarConfirmacion
import com.esan.sportpro.domain.ia.BloquesResumen
import com.esan.sportpro.domain.ia.EstadoResumen
import com.esan.sportpro.domain.ia.MencionNoVerificada
import com.esan.sportpro.domain.ia.ResumenPartido
import com.esan.sportpro.domain.ia.ResumenRepository
import com.esan.sportpro.domain.ia.RevisionResumenRepository
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
) : ResumenRepository, RevisionResumenRepository {

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


    override fun observarPublicado(partidoId: String): Flow<ResumenPartido?> = callbackFlow {
        // Los jugadores y padres no pueden leer los borradores: la consulta pide solo los aprobados.
        val registro = coleccion(partidoId).whereEqualTo("estado", EstadoResumen.APROBADO.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents.orEmpty().mapNotNull { it.aResumen(partidoId) }.maxByOrNull { it.version })
            }
        awaitClose { registro.remove() }
    }

    override suspend fun guardarEdicion(partidoId: String, resumenId: String, bloques: BloquesResumen, uid: String): Boolean =
        coleccion(partidoId).document(resumenId).update(
            mapOf(
                "hechosFinal" to bloques.hechos.trim(),
                "desarrolloFinal" to bloques.desarrollo.trim(),
                "interpretacionFinal" to bloques.interpretacion.trim(),
                "editadoPor" to uid,
                "editadoEn" to FieldValue.serverTimestamp(),
                "estado" to EstadoResumen.BORRADOR.name,
            ),
        ).esperarConfirmacion()

    override suspend fun aprobar(
        partidoId: String,
        resumenId: String,
        bloques: BloquesResumen,
        uid: String,
        visibleEnComunidad: Boolean,
    ): Boolean =
        coleccion(partidoId).document(resumenId).update(
            mapOf(
                "hechosFinal" to bloques.hechos.trim(),
                "desarrolloFinal" to bloques.desarrollo.trim(),
                "interpretacionFinal" to bloques.interpretacion.trim(),
                "editadoPor" to uid,
                "editadoEn" to FieldValue.serverTimestamp(),
                "estado" to EstadoResumen.APROBADO.name,
                "aprobadoPor" to uid,
                "aprobadoEn" to FieldValue.serverTimestamp(),
                "confirmacionAprobacion" to true,
                "visibleEnComunidad" to visibleEnComunidad,
            ),
        ).esperarConfirmacion()

    override suspend fun publicarEnComunidad(
        partidoId: String,
        resumenId: String,
        academiaId: String,
        autorUid: String,
        autorNombre: String,
        texto: String,
    ): String {
        // El id se conoce antes de escribir, así queda enlazado aunque no haya conexión.
        val publicacion = firestore.collection("publicaciones").document()
        publicacion.set(
            mapOf(
                "academiaId" to academiaId,
                "autorUid" to autorUid,
                "autorNombre" to autorNombre,
                "autorEsMenorDeEdad" to false,
                "texto" to texto,
                "imagenesUrls" to emptyList<String>(),
                "visibilidad" to "COMUNIDAD_ABIERTA",
                "estado" to "PUBLICADA",
                "fechaPublicacion" to FieldValue.serverTimestamp(),
                "editado" to false,
                "eliminada" to false,
                "reaccionesPorUsuario" to emptyMap<String, String>(),
                "comentariosCount" to 0,
                "reportesCount" to 0,
                "resumenPartidoId" to resumenId,
            ),
        ).esperarConfirmacion()
        coleccion(partidoId).document(resumenId).update("publicacionComunidadId", publicacion.id).esperarConfirmacion()
        return publicacion.id
    }

    override suspend fun retirarDeComunidad(partidoId: String, resumenId: String, publicacionId: String) {
        firestore.collection("publicaciones").document(publicacionId).update("eliminada", true).esperarConfirmacion()
        coleccion(partidoId).document(resumenId).update("publicacionComunidadId", FieldValue.delete()).esperarConfirmacion()
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
            bloquesFinal = getString("hechosFinal")?.let {
                BloquesResumen(
                    hechos = it,
                    desarrollo = getString("desarrolloFinal").orEmpty(),
                    interpretacion = getString("interpretacionFinal").orEmpty(),
                )
            },
            editadoPor = getString("editadoPor"),
            aprobadoPor = getString("aprobadoPor"),
            aprobadoEnMs = getTimestamp("aprobadoEn", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time,
            visibleEnComunidad = getBoolean("visibleEnComunidad") == true,
            publicacionComunidadId = getString("publicacionComunidadId"),
        )
    }
}
