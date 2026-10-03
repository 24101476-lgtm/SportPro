package com.esan.sportpro.data.cuentas

import com.esan.sportpro.domain.cuentas.ResultadoAuth
import com.esan.sportpro.domain.cuentas.Usuario
import com.esan.sportpro.domain.cuentas.UsuarioRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación de la cuenta de usuario con Firebase Auth + Cloud Firestore (US-001).
 *
 * - `usuarios/{uid}`: perfil del usuario.
 * - `codigosInvitacion/{codigo}`: códigos que permite crear cuentas de Administrador (US-004).
 *
 * `fechaNacimiento` se guarda como [Timestamp] y `fechaRegistro` con `serverTimestamp()` para que
 * la marca de tiempo la ponga el servidor y no el reloj del dispositivo.
 */
@Singleton
class FirestoreUsuarioRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : UsuarioRepository {

    private val usuarios get() = firestore.collection(COL_USUARIOS)
    private val codigos get() = firestore.collection(COL_CODIGOS_INVITACION)

    override suspend fun correoYaRegistrado(correo: String): Boolean = withContext(Dispatchers.IO) {
        val destino = correo.trim().lowercase()
        usuarios.whereEqualTo(CAMPO_CORREO, destino).limit(1).get().await().documents.isNotEmpty()
    }

    override suspend fun codigoInvitacionValido(codigo: String): Boolean = withContext(Dispatchers.IO) {
        val destino = codigo.trim().uppercase()
        if (destino.length != LARGO_CODIGO_INVITACION) return@withContext false

        val doc = codigos.document(destino).get().await()
        if (!doc.exists()) return@withContext false
        if (doc.getBoolean(CAMPO_USADO) == true) return@withContext false
        if (doc.getBoolean(CAMPO_REVOCADO) == true) return@withContext false

        val expiraEn = doc.getTimestamp(CAMPO_EXPIRA_EN)?.toDate()?.toInstant()
            ?.atZone(ZoneId.systemDefault())?.toLocalDate()
        if (expiraEn != null && expiraEn.isBefore(LocalDate.now())) return@withContext false

        true
    }

    override suspend fun crearCuentaEnAuth(correo: String, password: String): ResultadoAuth =
        withContext(Dispatchers.IO) {
            try {
                val resultado = auth.createUserWithEmailAndPassword(correo.trim(), password).await()
                val uid = resultado.user?.uid
                    ?: return@withContext ResultadoAuth.Error("No se pudo crear la cuenta")
                ResultadoAuth.Exito(uid)
            } catch (e: FirebaseAuthException) {
                // Se comparan los códigos literales: varias constantes FirebaseAuth.ERROR_*
                // están ocultas por deprecación en el SDK actual.
                when (e.errorCode) {
                    CODIGO_CORREO_YA_EXISTE -> ResultadoAuth.CorreoYaRegistrado
                    CODIGO_PASSWORD_DEBIL -> ResultadoAuth.Error(MSG_PASSWORD_DEBE)
                    CODIGO_SIN_RED -> ResultadoAuth.Error(MSG_SIN_CONEXION)
                    CODIGO_LOGIN_NO_PERMITIDO ->
                        ResultadoAuth.Error("El registro con correo y contraseña no está habilitado en el proyecto")
                    else -> ResultadoAuth.Error("No se pudo crear la cuenta")
                }
            } catch (e: FirebaseNetworkException) {
                ResultadoAuth.Error(MSG_SIN_CONEXION)
            } catch (e: Exception) {
                ResultadoAuth.Error("No se pudo crear la cuenta")
            }
        }

    override suspend fun eliminarCuentaEnAuth(uid: String) {
        withContext(Dispatchers.IO) {
            val usuario = auth.currentUser
            if (usuario != null && usuario.uid == uid) {
                usuario.delete().await()
            }
        }
    }

    override suspend fun enviarCorreoVerificacion() {
        withContext(Dispatchers.IO) {
            val usuario = auth.currentUser ?: error("No hay una sesión activa para verificar el correo")
            usuario.sendEmailVerification().await()
        }
    }

    override suspend fun guardarUsuario(usuario: Usuario) {
        withContext(Dispatchers.IO) {
            val datos = mapOf(
                "uid" to usuario.uid,
                "nombres" to usuario.nombres.trim(),
                "apellidos" to usuario.apellidos.trim(),
                "correo" to usuario.correo.trim().lowercase(),
                "telefono" to usuario.telefono.trim(),
                "fechaNacimiento" to usuario.fechaNacimiento.toTimestamp(),
                "rol" to usuario.rol.name,
                "esMenor" to usuario.esMenor,
                "estado" to usuario.estado.name,
                "fechaRegistro" to FieldValue.serverTimestamp(),
                "clubId" to usuario.clubId,
            )
            usuarios.document(usuario.uid).set(datos).await()
        }
    }

    override suspend fun marcarCodigoInvitacionUsado(codigo: String) {
        withContext(Dispatchers.IO) {
            val destino = codigo.trim().uppercase()
            codigos.document(destino).update(
                mapOf(
                    CAMPO_USADO to true,
                    "usadoPorUid" to auth.currentUser?.uid,
                    "usadoEn" to FieldValue.serverTimestamp(),
                ),
            ).await()
        }
    }

    private fun LocalDate.toTimestamp(): Timestamp = Timestamp(
        Date.from(atStartOfDay(ZoneId.systemDefault()).toInstant()),
    )

    companion object {
        const val COL_USUARIOS = "usuarios"
        const val COL_CODIGOS_INVITACION = "codigosInvitacion"
        const val LARGO_CODIGO_INVITACION = 8

        private const val CAMPO_CORREO = "correo"
        private const val CAMPO_USADO = "usado"
        private const val CAMPO_REVOCADO = "revocado"
        private const val CAMPO_EXPIRA_EN = "expiraEn"
        private const val MSG_SIN_CONEXION = "Sin conexión. Verifica tu red e inténtalo nuevamente"
        private const val MSG_PASSWORD_DEBE =
            "La contraseña debe tener mínimo 8 caracteres, una mayúscula, un número y un carácter especial"

        // Códigos de error de Firebase Auth (constantes literales: las oficiales están ocultas).
        private const val CODIGO_CORREO_YA_EXISTE = "ERROR_EMAIL_ALREADY_EXISTS"
        private const val CODIGO_PASSWORD_DEBIL = "ERROR_WEAK_PASSWORD"
        private const val CODIGO_SIN_RED = "ERROR_NETWORK_REQUEST_FAILED"
        private const val CODIGO_LOGIN_NO_PERMITIDO = "ERROR_OPERATION_NOT_ALLOWED"
    }
}