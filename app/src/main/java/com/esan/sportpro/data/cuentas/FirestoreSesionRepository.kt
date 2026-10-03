package com.esan.sportpro.data.cuentas

import com.esan.sportpro.domain.cuentas.EstadoUsuario
import com.esan.sportpro.domain.cuentas.ResultadoEnvioVerificacion
import com.esan.sportpro.domain.cuentas.ResultadoSesion
import com.esan.sportpro.domain.cuentas.SesionRepository
import com.esan.sportpro.navigation.UserRole
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** US-002: autenticación con Firebase Auth y validación del estado de la cuenta. */
@Singleton
class FirestoreSesionRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : SesionRepository {

    override suspend fun iniciarSesion(correo: String, password: String): ResultadoSesion =
        withContext(Dispatchers.IO) {
            val credenciales = try {
                auth.signInWithEmailAndPassword(correo.trim(), password).await()
            } catch (e: FirebaseAuthException) {
                return@withContext when (e.errorCode) {
                    CODIGO_CREDENCIALES_INVALIDAS, CODIGO_PASSWORD_INCORRECTO, CODIGO_USUARIO_NO_EXISTE ->
                        ResultadoSesion.CredencialesIncorrectas
                    CODIGO_SIN_RED -> ResultadoSesion.SinConexion
                    else -> ResultadoSesion.Error("No se pudo iniciar sesión")
                }
            } catch (e: FirebaseNetworkException) {
                return@withContext ResultadoSesion.SinConexion
            } catch (e: Exception) {
                return@withContext ResultadoSesion.Error("No se pudo iniciar sesión")
            }

            val usuario = credenciales.user ?: return@withContext ResultadoSesion.Error(
                "No se pudo iniciar sesión",
            )

            // El correo puede haberse verificado desde otro dispositivo: se vuelve a consultar.
            if (!usuario.isEmailVerified) {
                usuario.reload().await()
            }
            if (!usuario.isEmailVerified) {
                auth.signOut()
                return@withContext ResultadoSesion.CorreoSinVerificar
            }

            val perfil = try {
                firestore.collection(COL_USUARIOS).document(usuario.uid).get().await()
            } catch (e: FirebaseNetworkException) {
                auth.signOut()
                return@withContext ResultadoSesion.SinConexion
            } catch (e: Exception) {
                null
            }

            // Perfil sin documento o con otro estado: la cuenta no puede usar la app.
            val estado = perfil?.getString(CAMPO_ESTADO) ?: return@withContext ResultadoSesion.CuentaDesactivada
            if (estado != EstadoUsuario.ACTIVO.name) {
                auth.signOut()
                return@withContext ResultadoSesion.CuentaDesactivada
            }

            val rol = perfil?.getString(CAMPO_ROL)
                ?.let { nombre -> UserRole.entries.firstOrNull { it.name == nombre } }
                ?: UserRole.JUGADOR

            ResultadoSesion.Exito(usuario.uid, rol)
        }

    override suspend fun reenviarCorreoVerificacion(
        correo: String,
        password: String,
    ): ResultadoEnvioVerificacion = withContext(Dispatchers.IO) {
        val usuario = try {
            auth.signInWithEmailAndPassword(correo.trim(), password).await().user
        } catch (e: FirebaseAuthException) {
            return@withContext when (e.errorCode) {
                CODIGO_CREDENCIALES_INVALIDAS, CODIGO_PASSWORD_INCORRECTO, CODIGO_USUARIO_NO_EXISTE ->
                    ResultadoEnvioVerificacion.CredencialesIncorrectas
                CODIGO_SIN_RED -> ResultadoEnvioVerificacion.SinConexion
                else -> ResultadoEnvioVerificacion.Error
            }
        } catch (e: FirebaseNetworkException) {
            return@withContext ResultadoEnvioVerificacion.SinConexion
        } catch (e: Exception) {
            return@withContext ResultadoEnvioVerificacion.Error
        } ?: return@withContext ResultadoEnvioVerificacion.Error

        val enviado = runCatching { usuario.sendEmailVerification().await() }.isSuccess

        // El usuario sigue sin verificar: se cierra la sesión para no dejarlo adentro.
        auth.signOut()

        if (enviado) ResultadoEnvioVerificacion.Enviado else ResultadoEnvioVerificacion.Error
    }

    override suspend fun restablecerContrasena(correo: String) {
        withContext(Dispatchers.IO) {
            // Los errores se ignoran a propósito: la pantalla siempre confirma el envío para no
            // revelar si el correo está registrado.
            runCatching { auth.sendPasswordResetEmail(correo.trim()).await() }
        }
    }

    override fun haySesionActiva(): Boolean = auth.currentUser != null

    override suspend fun cerrarSesion() {
        withContext(Dispatchers.IO) { auth.signOut() }
    }

    companion object {
        private const val COL_USUARIOS = "usuarios"
        private const val CAMPO_ESTADO = "estado"
        private const val CAMPO_ROL = "rol"

        // Códigos literales: varias constantes FirebaseAuth.ERROR_* están ocultas por deprecación.
        private const val CODIGO_CREDENCIALES_INVALIDAS = "ERROR_INVALID_LOGIN_CREDENTIALS"
        private const val CODIGO_PASSWORD_INCORRECTO = "ERROR_WRONG_PASSWORD"
        private const val CODIGO_USUARIO_NO_EXISTE = "ERROR_USER_NOT_FOUND"
        private const val CODIGO_SIN_RED = "ERROR_NETWORK_REQUEST_FAILED"
    }
}