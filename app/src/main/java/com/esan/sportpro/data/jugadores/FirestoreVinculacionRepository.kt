package com.esan.sportpro.data.jugadores

import com.esan.sportpro.data.esperarConfirmacion
import com.esan.sportpro.domain.jugadores.DirectorioJugador
import com.esan.sportpro.domain.jugadores.EstadoVinculacion
import com.esan.sportpro.domain.jugadores.Jugador
import com.esan.sportpro.domain.jugadores.ReglasVinculacion
import com.esan.sportpro.domain.jugadores.Vinculacion
import com.esan.sportpro.domain.jugadores.VinculacionRepository
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * US-007: vinculación padre/madre ↔ jugador.
 *
 * - `vinculaciones/{padreUid}_{jugadorId}`: la solicitud y su estado.
 * - `vinculaciones/{id}/auditoria`: cada acción (solicitud, aprobación, rechazo, revocación).
 * - `directorio_jugadores/{jugadorId}.apoderadosAprobados`: contador para el máximo de 2.
 *
 * Aprobar, rechazar y revocar escriben todo en un lote: o se aplica completo o nada.
 */
@Singleton
class FirestoreVinculacionRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : VinculacionRepository {

    private val vinculaciones get() = firestore.collection("vinculaciones")
    private val directorio get() = firestore.collection("directorio_jugadores")

    // ---- Padre de familia ------------------------------------------------------------------

    override suspend fun buscarPorCodigo(academiaId: String, codigo: String): DirectorioJugador? =
        directorio
            .whereEqualTo("academiaId", academiaId)
            .whereEqualTo("codigo", ReglasVinculacion.normalizarCodigo(codigo))
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?.toObject<DirectorioJugador>()

    override fun observarDirectorio(academiaId: String): Flow<List<DirectorioJugador>> = callbackFlow {
        val registro = directorio.whereEqualTo("academiaId", academiaId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val lista = snapshot?.documents?.mapNotNull { it.toObject<DirectorioJugador>() }.orEmpty()
                    .filter { it.activo }
                    .sortedBy { it.nombre.lowercase() }
                trySend(lista)
            }
        awaitClose { registro.remove() }
    }

    override fun observarMisVinculaciones(padreUid: String): Flow<List<Vinculacion>> = callbackFlow {
        val registro = vinculaciones.whereEqualTo("padreUid", padreUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.mapNotNull { it.toObject<Vinculacion>() }.orEmpty())
            }
        awaitClose { registro.remove() }
    }

    override suspend fun solicitar(solicitud: Vinculacion): Boolean {
        val documento = vinculaciones.document(ReglasVinculacion.idVinculacion(solicitud.padreUid, solicitud.jugadorId))
        val lote = firestore.batch()
        lote.set(
            documento,
            mapOf(
                "academiaId" to solicitud.academiaId,
                "padreUid" to solicitud.padreUid,
                "padreNombre" to solicitud.padreNombre,
                "jugadorId" to solicitud.jugadorId,
                "jugadorNombre" to solicitud.jugadorNombre,
                "equipoId" to solicitud.equipoId,
                "equipoNombre" to solicitud.equipoNombre,
                "estado" to EstadoVinculacion.PENDIENTE.name,
                "motivoRechazo" to "",
                "solicitadaEn" to FieldValue.serverTimestamp(),
                "resueltaPor" to "",
                "resueltaPorNombre" to "",
                "resueltaEn" to null,
            ),
        )
        auditar(lote, documento, "SOLICITUD", solicitud.padreUid, solicitud.padreNombre)
        return lote.commit().esperarConfirmacion()
    }

    override fun observarJugador(jugadorId: String): Flow<Jugador?> = callbackFlow {
        val registro = firestore.collection("jugadores").document(jugadorId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Sin vínculo aprobado (o recién revocado) las reglas niegan la lectura.
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(snapshot?.takeIf { it.exists() }?.toObject<Jugador>())
            }
        awaitClose { registro.remove() }
    }

    // ---- Administrador y entrenador ----------------------------------------------------------

    override fun observarSolicitudes(academiaId: String): Flow<List<Vinculacion>> = callbackFlow {
        val registro = vinculaciones.whereEqualTo("academiaId", academiaId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.mapNotNull { it.toObject<Vinculacion>() }.orEmpty())
            }
        awaitClose { registro.remove() }
    }

    override suspend fun aprobar(vinculacion: Vinculacion, uid: String, nombre: String): Boolean {
        val documento = vinculaciones.document(vinculacion.id)
        val lote = firestore.batch()
        lote.update(documento, resolucion(EstadoVinculacion.APROBADA, uid, nombre))
        // Las reglas rechazan el lote completo si el contador superara el máximo de 2.
        lote.set(
            directorio.document(vinculacion.jugadorId),
            mapOf("academiaId" to vinculacion.academiaId, "apoderadosAprobados" to FieldValue.increment(1)),
            SetOptions.merge(),
        )
        auditar(lote, documento, "APROBACION", uid, nombre)
        return lote.commit().esperarConfirmacion()
    }

    override suspend fun rechazar(vinculacion: Vinculacion, motivo: String, uid: String, nombre: String): Boolean {
        val documento = vinculaciones.document(vinculacion.id)
        val lote = firestore.batch()
        lote.update(documento, resolucion(EstadoVinculacion.RECHAZADA, uid, nombre) + ("motivoRechazo" to motivo.trim()))
        auditar(lote, documento, "RECHAZO", uid, nombre, motivo.trim())
        return lote.commit().esperarConfirmacion()
    }

    override suspend fun revocar(vinculacion: Vinculacion, uid: String, nombre: String): Boolean {
        val documento = vinculaciones.document(vinculacion.id)
        val lote = firestore.batch()
        lote.update(documento, resolucion(EstadoVinculacion.REVOCADA, uid, nombre))
        lote.set(
            directorio.document(vinculacion.jugadorId),
            mapOf("academiaId" to vinculacion.academiaId, "apoderadosAprobados" to FieldValue.increment(-1)),
            SetOptions.merge(),
        )
        auditar(lote, documento, "REVOCACION", uid, nombre)
        return lote.commit().esperarConfirmacion()
    }

    override suspend fun asignarCodigo(jugador: Jugador, codigo: String, equipoEtiqueta: String): Boolean {
        val lote = firestore.batch()
        lote.update(firestore.collection("jugadores").document(jugador.id), "codigoVinculacion", codigo)
        lote.set(
            directorio.document(jugador.id),
            mapOf(
                "academiaId" to jugador.academiaId,
                "equipoId" to jugador.equipoId,
                "equipoNombre" to equipoEtiqueta,
                "nombre" to jugador.nombreCompleto,
                "codigo" to codigo,
                "activo" to jugador.activo,
            ),
            SetOptions.merge(),
        )
        return lote.commit().esperarConfirmacion()
    }

    private fun resolucion(estado: EstadoVinculacion, uid: String, nombre: String): Map<String, Any> = mapOf(
        "estado" to estado.name,
        "resueltaPor" to uid,
        "resueltaPorNombre" to nombre,
        "resueltaEn" to FieldValue.serverTimestamp(),
    )

    /** Criterio 8: cada acción queda en `vinculaciones/{id}/auditoria`, que nunca se edita ni se borra. */
    private fun auditar(
        lote: WriteBatch,
        vinculacion: DocumentReference,
        accion: String,
        uid: String,
        nombre: String,
        detalle: String = "",
    ) {
        lote.set(
            vinculacion.collection("auditoria").document(),
            mapOf(
                "accion" to accion,
                "uid" to uid,
                "nombre" to nombre,
                "detalle" to detalle,
                "fecha" to FieldValue.serverTimestamp(),
            ),
        )
    }
}
