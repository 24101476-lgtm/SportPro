package com.esan.sportpro.data.comunidad

import com.esan.sportpro.domain.comunidad.AccionModeracion
import com.esan.sportpro.domain.comunidad.Bloqueo
import com.esan.sportpro.domain.comunidad.DuracionSuspension
import com.esan.sportpro.domain.comunidad.Reporte
import com.esan.sportpro.domain.comunidad.Suspension
import com.esan.sportpro.domain.comunidad.TipoContenidoReportado
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/** El contenido se oculta automáticamente al acumular este número de reportes (US-030, criterio 4). */
private const val REPORTES_PARA_OCULTAR_AUTOMATICO = 3

/**
 * Acceso a reportes, moderación, bloqueos y suspensiones (US-030). Colecciones: "reportes",
 * "bloqueos" y "suspensiones".
 *
 * TODO(Firestore): las reglas de seguridad que impiden escribir a un usuario suspendido
 * (criterio 10) y que restringen la escritura de `reportesCount`/`estado`/`oculto` a Cloud
 * Functions o a administradores viven en `firestore.rules`, que está fuera del alcance de este
 * módulo cliente; este repositorio asume que esas reglas existen y solo implementa la
 * comprobación equivalente en la app (ver [observarSuspensionActiva]).
 */
@Singleton
class ModeracionRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    private val reportes get() = firestore.collection("reportes")
    private val bloqueos get() = firestore.collection("bloqueos")
    private val suspensiones get() = firestore.collection("suspensiones")

    /** US-030, criterio 2: un usuario no puede reportar dos veces el mismo contenido. */
    suspend fun crearReporte(reporte: Reporte): Result<Unit> = runCatching {
        require((reporte.detalle?.length ?: 0) <= 300) { "El detalle no puede superar 300 caracteres" }

        val yaReportado = reportes
            .whereEqualTo("contenidoId", reporte.contenidoId)
            .whereEqualTo("reportadoPorUid", reporte.reportadoPorUid)
            .get().await().documents.isNotEmpty()
        if (yaReportado) error("Ya reportaste este contenido")

        reportes.add(reporte).await()
        val nuevoConteo = incrementarContadorDeReportes(reporte)
        if (nuevoConteo >= REPORTES_PARA_OCULTAR_AUTOMATICO) {
            ocultarAutomaticamente(reporte.tipoContenido, reporte.contenidoId, reporte.publicacionId)
        }
    }

    private suspend fun incrementarContadorDeReportes(reporte: Reporte): Long {
        val referencia = referenciaDeContenido(reporte.tipoContenido, reporte.contenidoId, reporte.publicacionId)
        referencia.update("reportesCount", FieldValue.increment(1)).await()
        return (referencia.get().await().getLong("reportesCount")) ?: 0L
    }

    private suspend fun ocultarAutomaticamente(tipo: TipoContenidoReportado, contenidoId: String, publicacionId: String?) {
        val referencia = referenciaDeContenido(tipo, contenidoId, publicacionId)
        val campo = if (tipo == TipoContenidoReportado.AVISO) "oculto" else "estado"
        val valor: Any = if (tipo == TipoContenidoReportado.AVISO) true else "EN_REVISION"
        referencia.update(campo, valor).await()
    }

    private fun referenciaDeContenido(tipo: TipoContenidoReportado, contenidoId: String, publicacionId: String?) =
        when (tipo) {
            TipoContenidoReportado.PUBLICACION -> firestore.collection("publicaciones").document(contenidoId)
            TipoContenidoReportado.AVISO -> firestore.collection("avisos").document(contenidoId)
            TipoContenidoReportado.COMENTARIO -> firestore.collection("publicaciones")
                .document(requireNotNull(publicacionId) { "Un reporte de comentario requiere publicacionId" })
                .collection("comentarios").document(contenidoId)
        }

    /** Bandeja de moderación (criterio 3), priorizando contenido de menores (criterio 7) en el cliente. */
    fun observarBandeja(): Flow<List<Reporte>> = callbackFlow {
        val registro = reportes
            .whereEqualTo("resuelto", false)
            .orderBy("fechaReporte", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val lista = snapshot?.documents
                    ?.mapNotNull { it.toObject<Reporte>() }
                    ?.sortedWith(compareByDescending<Reporte> { it.involucraMenor })
                    .orEmpty()
                trySend(lista)
            }
        awaitClose { registro.remove() }
    }

    /**
     * Resuelve un reporte (criterio 6): aplica la acción sobre el contenido, registra quién y
     * cuándo resolvió, y notifica — hoy solo deja constancia en el propio documento del reporte,
     * ya que el envío de notificación push real requiere una Cloud Function (mismo límite que
     * [com.esan.sportpro.data.anuncios.AnuncioRepository.publicar]).
     */
    suspend fun resolver(
        reporte: Reporte,
        accion: AccionModeracion,
        motivoResolucion: String,
        moderadorUid: String,
        duracion: DuracionSuspension? = null,
    ) {
        require(motivoResolucion.length in 10..300) { "El motivo debe tener entre 10 y 300 caracteres" }

        reportes.document(reporte.id).update(
            mapOf(
                "resuelto" to true,
                "accionTomada" to accion.name,
                "motivoResolucion" to motivoResolucion,
                "moderadorUid" to moderadorUid,
                "fechaResolucion" to FieldValue.serverTimestamp(),
            ),
        ).await()

        val referencia = referenciaDeContenido(reporte.tipoContenido, reporte.contenidoId, reporte.publicacionId)
        when (accion) {
            AccionModeracion.MANTENER_PUBLICADO -> {
                val campo = if (reporte.tipoContenido == TipoContenidoReportado.AVISO) "oculto" else "estado"
                val valor: Any = if (reporte.tipoContenido == TipoContenidoReportado.AVISO) false else "PUBLICADA"
                referencia.update(campo, valor).await()
            }
            AccionModeracion.OCULTAR, AccionModeracion.OCULTAR_Y_ADVERTIR -> {
                val campo = if (reporte.tipoContenido == TipoContenidoReportado.AVISO) "oculto" else "estado"
                val valor: Any = if (reporte.tipoContenido == TipoContenidoReportado.AVISO) true else "OCULTA"
                referencia.update(campo, valor).await()
                // TODO(backend): notificar al autor vía Cloud Function cuando la acción incluye "advertir".
            }
            AccionModeracion.OCULTAR_Y_SUSPENDER -> {
                val campo = if (reporte.tipoContenido == TipoContenidoReportado.AVISO) "oculto" else "estado"
                val valor: Any = if (reporte.tipoContenido == TipoContenidoReportado.AVISO) true else "OCULTA"
                referencia.update(campo, valor).await()
                requireNotNull(duracion) { "OCULTAR_Y_SUSPENDER requiere una duración" }
                suspenderUsuario(reporte.autorContenidoUid, duracion, motivoResolucion, moderadorUid)
            }
        }
    }

    suspend fun suspenderUsuario(uid: String, duracion: DuracionSuspension, motivo: String, moderadorUid: String) {
        val fechaFin = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, duracion.dias) }.time
        suspensiones.document(uid).set(
            mapOf(
                "uid" to uid,
                "fechaFin" to fechaFin,
                "motivo" to motivo,
                "moderadorUid" to moderadorUid,
                "fechaCreacion" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    /** US-030, criterio 9: el usuario suspendido ve la fecha de fin de su suspensión. */
    fun observarSuspensionActiva(uid: String): Flow<Suspension?> = callbackFlow {
        val registro = suspensiones.document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            val suspension = snapshot?.toObject<Suspension>()
            trySend(suspension?.takeIf { it.estaActivaRespectoDe(Date()) })
        }
        awaitClose { registro.remove() }
    }

    /** US-030, criterio 8: bloqueo inmediato y recíproco. */
    suspend fun bloquear(uidBloqueador: String, uidBloqueado: String) {
        bloqueos.document("${uidBloqueador}_$uidBloqueado").set(
            mapOf(
                "uidBloqueador" to uidBloqueador,
                "uidBloqueado" to uidBloqueado,
                "fecha" to FieldValue.serverTimestamp(),
            ),
            SetOptions.merge(),
        ).await()
    }

    suspend fun desbloquear(uidBloqueador: String, uidBloqueado: String) {
        bloqueos.document("${uidBloqueador}_$uidBloqueado").delete().await()
    }

    fun observarBloqueadosPor(uid: String): Flow<List<Bloqueo>> = callbackFlow {
        val registro = bloqueos.whereEqualTo("uidBloqueador", uid).addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            trySend(snapshot?.documents?.mapNotNull { it.toObject<Bloqueo>() }.orEmpty())
        }
        awaitClose { registro.remove() }
    }

    /** Para filtrar el muro: uids que bloquearon a [uid] o que [uid] bloqueó (visibilidad recíproca). */
    suspend fun obtenerUidsOcultosParaMuro(uid: String): List<String> {
        val bloqueadosPorMi = bloqueos.whereEqualTo("uidBloqueador", uid).get().await()
            .documents.mapNotNull { it.getString("uidBloqueado") }
        val queMeBloquearon = bloqueos.whereEqualTo("uidBloqueado", uid).get().await()
            .documents.mapNotNull { it.getString("uidBloqueador") }
        return (bloqueadosPorMi + queMeBloquearon).distinct()
    }
}
