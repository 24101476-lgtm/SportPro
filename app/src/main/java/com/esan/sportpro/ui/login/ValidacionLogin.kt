package com.esan.sportpro.ui.login

/** Mensajes y reglas de bloqueo del login (US-002). */
object MensajesLogin {
    const val CAMPO_OBLIGATORIO = "Campo obligatorio"
    const val CREDENCIALES_INCORRECTAS = "Correo o contraseña incorrectos"
    const val CORREO_SIN_VERIFICAR = "Debes verificar tu correo"
    const val CUENTA_DESACTIVADA = "Cuenta desactivada. Contacta al administrador de tu academia"
    const val SIN_CONEXION = "Sin conexión. Verifica tu red e inténtalo nuevamente"

    /**
     * Siempre la misma respuesta, exista o no la cuenta: no se revela qué correos están
     * registrados.
     */
    const val RECUPERACION_ENVIADA =
        "Si el correo está registrado, te enviaremos un enlace para restablecer tu contraseña"

    const val INTENTOS_AGOTADOS = "Demasiados intentos fallidos"

    const val VERIFICACION_REENVIADA = "Te enviamos un nuevo correo de verificación"
    const val SIN_CONTRASENA = "Ingresa tu contraseña para reenviar el correo de verificación"

    /** US-002: tras 3 intentos fallidos el login se bloquea temporalmente. */
    const val MAX_INTENTOS_FALLIDOS = 3
    const val SEGUNDOS_BLOQUEO = 30L

    /** Mensaje de bloqueo con la cuenta regresiva visible. */
    fun mensajeBloqueo(segundosRestantes: Int): String =
        "$INTENTOS_AGOTADOS. Espera $segundosRestantes segundos e inténtalo nuevamente"
}

/** Validaciones del formulario de login y de recuperación (US-002). */
object ValidacionLogin {

    /**
     * Valida los campos obligatorios en el orden de [CampoLogin]. Un campo vacío produce
     * "Campo obligatorio" y evita llegar al backend.
     */
    fun validarCampos(valores: List<String>): Map<CampoLogin, String> {
        val errores = mutableMapOf<CampoLogin, String>()
        valores.forEachIndexed { indice, valor ->
            if (valor.isBlank()) {
                errores[CampoLogin.entries[indice]] = MensajesLogin.CAMPO_OBLIGATORIO
            }
        }
        return errores
    }

    /** Valida solo el correo (pantalla de recuperación). */
    fun validarCorreo(correo: String): String? =
        if (correo.isBlank()) MensajesLogin.CAMPO_OBLIGATORIO else null
}

/** Campos del formulario de login, en el mismo orden que se validan. */
enum class CampoLogin { CORREO, PASSWORD }