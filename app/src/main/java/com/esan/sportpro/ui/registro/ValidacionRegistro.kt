package com.esan.sportpro.ui.registro

import java.time.LocalDate
import java.time.Period

/**
 * Mensajes de error de US-001, tal cual están escritos en los criterios de aceptación.
 * Se centralizan aquí para que la pantalla y las pruebas usen exactamente las mismas cadenas.
 */
object MensajesRegistro {
    const val MINIMO_CARACTERES = "Mínimo 2 caracteres"
    const val MAXIMO_CARACTERES = "Máximo 50 caracteres"
    const val SOLO_LETRAS = "Solo se permiten letras"
    const val CORREO_INVALIDO = "Correo inválido"
    const val CORREO_YA_REGISTRADO = "Correo ya registrado"
    const val PASSWORD_DEBE =
        "La contraseña debe tener mínimo 8 caracteres, una mayúscula, un número y un carácter especial"
    const val PASSWORDS_NO_COINCIDEN = "Las contraseñas no coinciden"
    const val ROL_REQUERIDO = "Seleccione un rol"
    const val CODIGO_INVALIDO = "Código de invitación inválido"
    const val APODERADO_REQUERIDO = "Ingresa el correo del apoderado"
    const val FECHA_NACIMIENTO_REQUERIDA = "Selecciona tu fecha de nacimiento"
    const val FECHA_NACIMIENTO_FUTURA = "La fecha de nacimiento no puede ser futura"
    const val SIN_CONEXION = "Sin conexión. Verifica tu red e inténtalo nuevamente"
    const val EXITO = "Cuenta creada. Verifica tu correo para continuar"
}

/** Campos del formulario de registro, para poder mostrar el error en el campo que corresponde. */
enum class CampoRegistro {
    NOMBRES,
    APELLIDOS,
    CORREO,
    PASSWORD,
    CONFIRMAR_PASSWORD,
    TELEFONO,
    FECHA_NACIMIENTO,
    ROL,
    CODIGO_INVITACION,
    CORREO_APODERADO,
}

/**
 * Reglas de validación del formulario de registro (US-001). Son funciones puras sin dependencias
 * de Android para poder ejercitarse sin emulador.
 */
object ValidacionRegistro {

    const val MIN_LETRAS = 2
    const val MAX_LETRAS = 50
    const val MIN_CORREO = 5
    const val MAX_CORREO = 100
    const val MIN_PASSWORD = 8
    const val MAX_PASSWORD = 50
    const val LARGO_CODIGO_INVITACION = 8
    const val EDAD_MAYORIDAD = 18

    private val REGEX_CORREO =
        Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$")

    private const val CARACTERES_ESPECIALES = "!@#$%^&*()_+-=[]{}|;:'\",.<>?/\\~`"

    /** 2 a 50 letras. Devuelve el mensaje exacto o `null` si está bien. */
    fun validarNombre(valor: String): String? {
        val texto = valor.trim()
        if (texto.isEmpty()) return MensajesRegistro.MINIMO_CARACTERES
        if (texto.length > MAX_LETRAS) return MensajesRegistro.MAXIMO_CARACTERES
        // Se admiten espacios, apóstrofos y guiones para nombres compuestos ("Ana María", "D'Angelo").
        val soloLetras = texto.all { it.isLetter() || it == ' ' || it == '\'' || it == '-' }
        if (!soloLetras) return MensajesRegistro.SOLO_LETRAS
        return null
    }

    /** 5 a 100 caracteres y con formato de correo válido. */
    fun validarCorreo(valor: String): String? {
        val texto = valor.trim()
        if (texto.length < MIN_CORREO || texto.length > MAX_CORREO) return MensajesRegistro.CORREO_INVALIDO
        if (!REGEX_CORREO.matches(texto)) return MensajesRegistro.CORREO_INVALIDO
        return null
    }

    /** 8 a 50 caracteres, con al menos una mayúscula, un número y un carácter especial. */
    fun validarPassword(valor: String): String? {
        if (valor.length < MIN_PASSWORD || valor.length > MAX_PASSWORD) return MensajesRegistro.PASSWORD_DEBE
        val tieneMayuscula = valor.any { it.isUpperCase() }
        val tieneNumero = valor.any { it.isDigit() }
        val tieneEspecial = valor.any { it in CARACTERES_ESPECIALES }
        if (!tieneMayuscula || !tieneNumero || !tieneEspecial) return MensajesRegistro.PASSWORD_DEBE
        return null
    }

    /** La confirmación debe coincidir con la contraseña. */
    fun validarConfirmacion(password: String, confirmacion: String): String? =
        if (password != confirmacion) MensajesRegistro.PASSWORDS_NO_COINCIDEN else null

    fun validarFechaNacimiento(fecha: LocalDate?): String? {
        if (fecha == null) return MensajesRegistro.FECHA_NACIMIENTO_REQUERIDA
        if (fecha.isAfter(LocalDate.now())) return MensajesRegistro.FECHA_NACIMIENTO_FUTURA
        return null
    }

    fun validarCodigoInvitacion(codigo: String): String? =
        if (codigo.trim().length != LARGO_CODIGO_INVITACION) MensajesRegistro.CODIGO_INVALIDO else null

    /** Un jugador es menor si no cumplió 18 años a la fecha de hoy. */
    fun esMenor(fechaNacimiento: LocalDate): Boolean =
        Period.between(fechaNacimiento, LocalDate.now()).years < EDAD_MAYORIDAD

    fun esCorreoValido(valor: String): Boolean = validarCorreo(valor) == null
}