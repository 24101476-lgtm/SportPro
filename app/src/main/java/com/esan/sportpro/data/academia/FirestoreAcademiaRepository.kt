package com.esan.sportpro.data.academia

import com.esan.sportpro.data.esperarConfirmacion
import com.esan.sportpro.domain.academia.AcademiaRepository
import com.esan.sportpro.domain.academia.CodigoInvitacion
import com.esan.sportpro.domain.academia.ConfiguracionAcademia
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreAcademiaRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : AcademiaRepository {

    private val academias get() = firestore.collection("academias")
    private val codigos get() = firestore.collection("codigosInvitacion")
    private val aleatorio = SecureRandom()

    override fun observarConfiguracion(academiaId: String): Flow<ConfiguracionAcademia> = callbackFlow {
        val registro = academias.document(academiaId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Sin permiso o sin red: se sigue con los valores por defecto.
                trySend(ConfiguracionAcademia(id = academiaId))
                return@addSnapshotListener
            }
            trySend(
                ConfiguracionAcademia(
                    id = academiaId,
                    nombre = snapshot?.getString("nombre").orEmpty(),
                    diaCorte = snapshot?.getLong("diaCorte")?.toInt()?.takeIf { it in 1..ConfiguracionAcademia.DIA_CORTE_MAX }
                        ?: ConfiguracionAcademia.DIA_CORTE_POR_DEFECTO,
                ),
            )
        }
        awaitClose { registro.remove() }
    }

    override suspend fun guardarConfiguracion(configuracion: ConfiguracionAcademia): Boolean =
        academias.document(configuracion.id).set(
            mapOf(
                "nombre" to configuracion.nombre.trim(),
                "diaCorte" to configuracion.diaCorte,
                "actualizadoEn" to FieldValue.serverTimestamp(),
            ),
        ).esperarConfirmacion()

    override fun observarCodigos(academiaId: String): Flow<List<CodigoInvitacion>> = callbackFlow {
        val registro = codigos.whereEqualTo("academiaId", academiaId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val lista = snapshot?.documents.orEmpty().map { doc ->
                CodigoInvitacion(
                    codigo = doc.id,
                    creadoEn = doc.getTimestamp("creadoEn")?.toDate(),
                    expiraEn = doc.getTimestamp("expiraEn")?.toDate(),
                    usado = doc.getBoolean("usado") == true,
                    revocado = doc.getBoolean("revocado") == true,
                )
            }.sortedByDescending { it.creadoEn?.time ?: Long.MAX_VALUE }
            trySend(lista)
        }
        awaitClose { registro.remove() }
    }

    override suspend fun generarCodigo(academiaId: String, adminUid: String): String {
        val codigo = (1..LARGO).map { ALFABETO[aleatorio.nextInt(ALFABETO.length)] }.joinToString("")
        val expira = Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(DIAS_VIGENCIA))
        codigos.document(codigo).set(
            mapOf(
                "academiaId" to academiaId,
                "creadoPor" to adminUid,
                "creadoEn" to FieldValue.serverTimestamp(),
                "expiraEn" to Timestamp(expira),
                "usado" to false,
                "revocado" to false,
            ),
        ).await()
        return codigo
    }

    override suspend fun revocarCodigo(codigo: String): Boolean =
        codigos.document(codigo).update("revocado", true).esperarConfirmacion()

    private companion object {
        const val LARGO = 8
        const val DIAS_VIGENCIA = 7L

        /** Sin 0/O, 1/I/L para que se pueda dictar sin errores. */
        const val ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    }
}
