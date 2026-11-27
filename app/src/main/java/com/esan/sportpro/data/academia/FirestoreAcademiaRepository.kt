package com.esan.sportpro.data.academia

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import com.esan.sportpro.data.esperarConfirmacion
import com.esan.sportpro.data.imagenes.CarpetaImagen
import com.esan.sportpro.data.imagenes.CompresorImagen
import com.esan.sportpro.data.imagenes.ImagenesSupabase
import com.esan.sportpro.domain.academia.AcademiaRepository
import com.esan.sportpro.domain.academia.CodigoInvitacion
import com.esan.sportpro.domain.academia.ConfiguracionAcademia
import com.esan.sportpro.domain.academia.EscudoMuyGrandeException
import com.esan.sportpro.domain.academia.NombreAcademiaDuplicadoException
import com.esan.sportpro.domain.academia.ValidacionAcademia
import com.esan.sportpro.navigation.UserRole
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * US-004: datos de la academia (`academias/{id}`), su auditoría (`academias/{id}/auditoria`), la
 * reserva de nombres únicos (`nombresAcademia/{clave}`) y los códigos de invitación.
 */
@Singleton
class FirestoreAcademiaRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context,
    private val imagenes: ImagenesSupabase,
    private val compresor: CompresorImagen,
) : AcademiaRepository {

    private val academias get() = firestore.collection("academias")
    private val nombres get() = firestore.collection("nombresAcademia")
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
                    escudoUrl = snapshot?.getString("escudoUrl").orEmpty(),
                    ciudad = snapshot?.getString("ciudad").orEmpty(),
                    distrito = snapshot?.getString("distrito").orEmpty(),
                    direccion = snapshot?.getString("direccion").orEmpty(),
                    telefono = snapshot?.getString("telefono").orEmpty(),
                    correo = snapshot?.getString("correo").orEmpty(),
                    anioFundacion = snapshot?.getLong("anioFundacion")?.toInt(),
                    adminUid = snapshot?.getString("adminUid").orEmpty(),
                ),
            )
        }
        awaitClose { registro.remove() }
    }

    // Con merge: cambiar el día de corte no borra el resto de los datos de la academia.
    override suspend fun guardarConfiguracion(configuracion: ConfiguracionAcademia): Boolean =
        academias.document(configuracion.id).set(
            mapOf(
                "nombre" to configuracion.nombre.trim(),
                "diaCorte" to configuracion.diaCorte,
                "actualizadoEn" to FieldValue.serverTimestamp(),
            ),
            SetOptions.merge(),
        ).esperarConfirmacion()

    override suspend fun guardarDatos(
        anterior: ConfiguracionAcademia,
        nueva: ConfiguracionAcademia,
        uid: String,
    ): Boolean {
        val academiaId = nueva.id
        val claveNueva = ValidacionAcademia.claveNombre(nueva.nombre)
        val claveAnterior = ValidacionAcademia.claveNombre(anterior.nombre)

        // Nombre único: se reserva en nombresAcademia/{clave}. Si ya lo tiene otra academia, error.
        if (claveNueva != claveAnterior || anterior.nombre.isBlank()) {
            val reservado = withContext(Dispatchers.IO) { nombres.document(claveNueva).get().await() }
            val duenio = reservado.getString("academiaId")
            if (reservado.exists() && duenio != academiaId) throw NombreAcademiaDuplicadoException()
        }

        val lote = firestore.batch()
        val documento = academias.document(academiaId)
        val datos = mutableMapOf<String, Any?>(
            "nombre" to nueva.nombre.trim(),
            "diaCorte" to nueva.diaCorte,
            "escudoUrl" to nueva.escudoUrl,
            "ciudad" to nueva.ciudad.trim(),
            "distrito" to nueva.distrito.trim(),
            "direccion" to nueva.direccion.trim(),
            "telefono" to nueva.telefono.trim(),
            "correo" to nueva.correo.trim().lowercase(),
            "anioFundacion" to nueva.anioFundacion,
            "actualizadoEn" to FieldValue.serverTimestamp(),
        )
        if (anterior.adminUid.isBlank()) {
            // Primera vez que se registran los datos: queda el administrador y la fecha de creación.
            datos["adminUid"] = uid
            datos["fechaCreacion"] = FieldValue.serverTimestamp()
        }
        lote.set(documento, datos, SetOptions.merge())

        if (claveNueva != claveAnterior || anterior.nombre.isBlank()) {
            lote.set(nombres.document(claveNueva), mapOf("academiaId" to academiaId, "nombre" to nueva.nombre.trim()))
            if (claveAnterior.isNotBlank() && claveAnterior != claveNueva) lote.delete(nombres.document(claveAnterior))
        }

        // Auditoría: una fila por campo modificado (solo al editar datos que ya existían).
        if (anterior.adminUid.isNotBlank() || anterior.nombre.isNotBlank()) {
            ValidacionAcademia.cambios(anterior, nueva).forEach { cambio ->
                lote.set(
                    documento.collection("auditoria").document(),
                    mapOf(
                        "uid" to uid,
                        "campo" to cambio.campo,
                        "valorAnterior" to cambio.valorAnterior,
                        "valorNuevo" to cambio.valorNuevo,
                        "fecha" to FieldValue.serverTimestamp(),
                    ),
                )
            }
        }
        return lote.commit().esperarConfirmacion()
    }

    override suspend fun subirEscudo(academiaId: String, uriLocal: String): String? {
        val uri = Uri.parse(uriLocal)
        val tamano = withContext(Dispatchers.IO) { tamanoDe(uri) }
        if (tamano != null && ValidacionAcademia.validarEscudo(tamano) != null) throw EscudoMuyGrandeException()
        return runCatching {
            val comprimida = compresor.comprimir(uri) ?: return null
            val cuadrada = withContext(Dispatchers.Default) { recortarCuadrado(comprimida) }
            imagenes.subir(CarpetaImagen.ESCUDOS, cuadrada)
        }.getOrNull()
    }

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
                    rol = doc.getString("rol")?.let { nombre -> UserRole.entries.firstOrNull { it.name == nombre } }
                        ?: UserRole.ADMINISTRADOR,
                )
            }.sortedByDescending { it.creadoEn ?: Date(0) }
            trySend(lista)
        }
        awaitClose { registro.remove() }
    }

    override suspend fun generarCodigo(academiaId: String, adminUid: String, rol: UserRole): String {
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
                // US-004: rol que podrá registrarse con este código.
                "rol" to rol.name,
            ),
        ).await()
        return codigo
    }

    override suspend fun revocarCodigo(codigo: String): Boolean =
        codigos.document(codigo).update("revocado", true).esperarConfirmacion()

    /** Tamaño del archivo elegido en bytes, o `null` si el proveedor no lo informa. */
    private fun tamanoDe(uri: Uri): Long? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            val columna = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (columna >= 0 && cursor.moveToFirst() && !cursor.isNull(columna)) cursor.getLong(columna) else null
        }
    }.getOrNull()

    /** US-004: el escudo se guarda recortado en proporción 1:1 (centro de la imagen). */
    private fun recortarCuadrado(jpeg: ByteArray): ByteArray {
        val original = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size) ?: return jpeg
        if (original.width == original.height) return jpeg
        val lado = minOf(original.width, original.height)
        val cuadrada = Bitmap.createBitmap(original, (original.width - lado) / 2, (original.height - lado) / 2, lado, lado)
        val salida = ByteArrayOutputStream()
        cuadrada.compress(Bitmap.CompressFormat.JPEG, CALIDAD_JPEG, salida)
        if (cuadrada !== original) cuadrada.recycle()
        original.recycle()
        return salida.toByteArray()
    }

    private companion object {
        const val LARGO = 8
        const val DIAS_VIGENCIA = 7L
        const val CALIDAD_JPEG = 85

        /** Sin 0/O ni 1/I/L para que el código se dicte sin confusiones. */
        const val ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    }
}
