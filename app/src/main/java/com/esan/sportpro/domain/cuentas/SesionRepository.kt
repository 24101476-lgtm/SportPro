package com.esan.sportpro.domain.cuentas

import com.esan.sportpro.navigation.UserRole

/**
 * Resultado de un intento de inicio de sesión (US-002).
 *
 * [CredencialesIncorrectas] nunca distingue entre "el correo no existe" y "la contraseña no
 * coincide": ese detalle se lo lleva Firebase, no la app.
 */
sealed interface ResultadoSesion {
    data class Exito(val uid: String, val rol: UserRole) : ResultadoSesion
    data object CredencialesIncorrectas : ResultadoSesion
    data object CorreoSinVerificar : ResultadoSesion
    data object CuentaDesactivada : ResultadoSesion
    data object SinConexion : ResultadoSesion
    data class Error(val mensaje: String) : ResultadoSesion
}

/** US-002: sesión de Firebase Auth, verificación de correo y recuperación de contraseña. */
interface SesionRepository {

    /**
     * Inicia sesión y valida el estado de la cuenta. Si el correo no está verificado o la cuenta
     * está desactivada, cierra la sesión antes de devolver el resultado.
     */
    suspend fun iniciarSesion(correo: String, password: String): ResultadoSesion

    /**
     * Reenvía el correo de verificación. Como al detectar el correo sin verificar la sesión ya
     * quedó cerrada, primero vuelve a autenticar con las credenciales, envía el correo y vuelve a
     * cerrar sesión: el usuario sigue sin poder entrar hasta confirmar el enlace.
     */
    suspend fun reenviarCorreoVerificacion(correo: String, password: String): ResultadoEnvioVerificacion

    /**
     * Envía el correo de restablecimiento. No falla si el correo no está registrado: la respuesta
     * al usuario es siempre la misma para no revelar qué cuentas existen.
     */
    suspend fun restablecerContrasena(correo: String)

    /** `true` si hay una sesión abierta al abrir la app, para saltar el login. */
    fun haySesionActiva(): Boolean

    /** Cierra la sesión de Firebase Auth. */
    suspend fun cerrarSesion()
}

/** Resultado de pedir que se reenvíe el correo de verificación. */
sealed interface ResultadoEnvioVerificacion {
    data object Enviado : ResultadoEnvioVerificacion
    data object CredencialesIncorrectas : ResultadoEnvioVerificacion
    data object SinConexion : ResultadoEnvioVerificacion
    data object Error : ResultadoEnvioVerificacion
}