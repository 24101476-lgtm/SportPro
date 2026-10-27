package com.esan.sportpro.domain.cuentas

/** Resultado de crear la cuenta en Firebase Auth, para que el repositorio no lance excepciones. */
sealed interface ResultadoAuth {
    data class Exito(val uid: String) : ResultadoAuth

    /** Firebase ya tiene una cuenta con ese correo. */
    data object CorreoYaRegistrado : ResultadoAuth

    data class Error(val mensaje: String) : ResultadoAuth
}

/**
 * Operations de la cuenta del usuario (US-001). La implementación con Firestore/Auth está en
 * `data/cuentas/FirestoreUsuarioRepository.kt`.
 */
interface UsuarioRepository {

    /** True si ya existe un documento en `usuarios` con ese correo. */
    suspend fun correoYaRegistrado(correo: String): Boolean

    /**
     * True si el código de invitación existe en `codigosInvitacion/{codigo}`, no está revocado,
     * no fue usado y no expiró (7 días desde su emisión, US-004).
     */
    suspend fun codigoInvitacionValido(codigo: String): Boolean

    /** Crea el usuario en Firebase Auth y devuelve su uid. */
    suspend fun crearCuentaEnAuth(correo: String, password: String): ResultadoAuth

    /**
     * Elimina de Firebase Auth una cuenta recién creada. Se usa cuando el registro se crea antes
     * de validar el código de invitación y este resulta inválido: así no queda una cuenta huérfana.
     */
    suspend fun eliminarCuentaEnAuth(uid: String)

    /**
     * Cuenta de Auth que quedó sin perfil (un registro anterior falló al guardar `usuarios/{uid}`).
     * Inicia sesión con esas credenciales y devuelve el uid solo si no existe su perfil, para
     * completar el registro. En cualquier otro caso cierra la sesión y devuelve null.
     */
    suspend fun recuperarCuentaSinPerfil(correo: String, password: String): String?

    /** Envía el correo de verificación al usuario recién creado. */
    suspend fun enviarCorreoVerificacion()

    /** Escribe el perfil en `usuarios/{uid}`. */
    /**
     * [codigoInvitacion] es obligatorio para un Administrador: las reglas de Firestore solo
     * aceptan ese rol si el perfil trae un código de invitación válido y sin usar.
     */
    suspend fun guardarUsuario(usuario: Usuario, codigoInvitacion: String? = null)

    /** Marca el código de invitación como usado para que no se pueda reutilizar. */
    suspend fun marcarCodigoInvitacionUsado(codigo: String)
}