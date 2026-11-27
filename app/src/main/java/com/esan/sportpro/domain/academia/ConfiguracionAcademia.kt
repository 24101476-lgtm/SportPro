package com.esan.sportpro.domain.academia

import com.esan.sportpro.navigation.UserRole
import kotlinx.coroutines.flow.Flow
import java.text.Normalizer
import java.time.LocalDate
import java.util.Date

/**
 * US-004: datos de la academia (colección `academias`, id = academiaId del perfil).
 * Si el documento todavía no existe se usan estos valores por defecto.
 */
data class ConfiguracionAcademia(
    val id: String = "",
    val nombre: String = "",
    /** Día del mes desde el que una mensualidad pendiente pasa a Vencido (US-008, criterio 5). */
    val diaCorte: Int = DIA_CORTE_POR_DEFECTO,
    val escudoUrl: String = "",
    val ciudad: String = "",
    val distrito: String = "",
    val direccion: String = "",
    val telefono: String = "",
    val correo: String = "",
    val anioFundacion: Int? = null,
    val adminUid: String = "",
) {
    /** Valores que se comparan para el registro de auditoría (campo → texto). */
    fun valoresAuditables(): Map<String, String> = mapOf(
        "nombre" to nombre.trim(),
        "ciudad" to ciudad.trim(),
        "distrito" to distrito.trim(),
        "direccion" to direccion.trim(),
        "telefono" to telefono.trim(),
        "correo" to correo.trim(),
        "anioFundacion" to (anioFundacion?.toString() ?: ""),
        "diaCorte" to diaCorte.toString(),
        "escudoUrl" to escudoUrl,
    )

    companion object {
        const val DIA_CORTE_POR_DEFECTO = 10
        const val DIA_CORTE_MAX = 28
        const val NOMBRE_MIN = 3
        const val NOMBRE_MAX = 80

        /** Mensaje de error o `null` si los datos son válidos (las reglas de Firestore exigen lo mismo). */
        fun validar(nombre: String, diaCorte: Int?): String? = when {
            nombre.trim().length !in NOMBRE_MIN..NOMBRE_MAX -> MensajesAcademia.NOMBRE_LARGO
            diaCorte == null || diaCorte !in 1..DIA_CORTE_MAX -> "El día de corte debe estar entre 1 y $DIA_CORTE_MAX"
            else -> null
        }
    }
}

/** Campos del formulario de la academia, para mostrar cada error debajo de su campo. */
enum class CampoAcademia { NOMBRE, CIUDAD, DISTRITO, DIRECCION, TELEFONO, CORREO, ANIO_FUNDACION, DIA_CORTE, ESCUDO }

/** Mensajes exactos de los criterios de US-004. */
object MensajesAcademia {
    const val NOMBRE_LARGO = "El nombre debe tener entre 3 y 80 caracteres"
    const val NOMBRE_DUPLICADO = "Ya existe una academia con ese nombre"
    const val ANIO_INVALIDO = "Año inválido"
    const val IMAGEN_GRANDE = "La imagen no debe superar 2 MB"
    const val CAMPO_OBLIGATORIO = "Campo obligatorio"
    const val TELEFONO_INVALIDO = "Ingresa un teléfono de 7 a 15 dígitos"
    const val CORREO_INVALIDO = "Correo inválido"
}

/** Reglas de validación de los datos de la academia (US-004). Sin Android ni Firebase. */
object ValidacionAcademia {
    const val ANIO_MINIMO = 1900
    const val TEXTO_MAX = 80
    const val DIRECCION_MAX = 120

    /** 2 MB: tamaño máximo del escudo antes de comprimirlo. */
    const val ESCUDO_MAX_BYTES = 2L * 1024 * 1024

    private val REGEX_CORREO = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$")

    fun validar(
        datos: ConfiguracionAcademia,
        anioFundacionTexto: String,
        diaCorteTexto: String,
        anioActual: Int = LocalDate.now().year,
    ): Map<CampoAcademia, String> {
        val errores = mutableMapOf<CampoAcademia, String>()
        if (datos.nombre.trim().length !in ConfiguracionAcademia.NOMBRE_MIN..ConfiguracionAcademia.NOMBRE_MAX) {
            errores[CampoAcademia.NOMBRE] = MensajesAcademia.NOMBRE_LARGO
        }
        obligatorio(datos.ciudad, TEXTO_MAX)?.let { errores[CampoAcademia.CIUDAD] = it }
        obligatorio(datos.distrito, TEXTO_MAX)?.let { errores[CampoAcademia.DISTRITO] = it }
        obligatorio(datos.direccion, DIRECCION_MAX)?.let { errores[CampoAcademia.DIRECCION] = it }
        validarTelefono(datos.telefono)?.let { errores[CampoAcademia.TELEFONO] = it }
        validarCorreo(datos.correo)?.let { errores[CampoAcademia.CORREO] = it }
        validarAnioFundacion(anioFundacionTexto, anioActual)?.let { errores[CampoAcademia.ANIO_FUNDACION] = it }
        val dia = diaCorteTexto.toIntOrNull()
        if (dia == null || dia !in 1..ConfiguracionAcademia.DIA_CORTE_MAX) {
            errores[CampoAcademia.DIA_CORTE] = "Entre 1 y ${ConfiguracionAcademia.DIA_CORTE_MAX}"
        }
        return errores
    }

    /** Entre 1900 y el año actual; vacío o fuera de rango → "Año inválido". */
    fun validarAnioFundacion(texto: String, anioActual: Int = LocalDate.now().year): String? {
        val anio = texto.trim().toIntOrNull() ?: return MensajesAcademia.ANIO_INVALIDO
        return if (anio in ANIO_MINIMO..anioActual) null else MensajesAcademia.ANIO_INVALIDO
    }

    fun validarTelefono(texto: String): String? {
        val digitos = texto.filter { it.isDigit() }
        return when {
            texto.isBlank() -> MensajesAcademia.CAMPO_OBLIGATORIO
            digitos.length !in 7..15 -> MensajesAcademia.TELEFONO_INVALIDO
            else -> null
        }
    }

    fun validarCorreo(texto: String): String? = when {
        texto.isBlank() -> MensajesAcademia.CAMPO_OBLIGATORIO
        texto.trim().length > 100 || !REGEX_CORREO.matches(texto.trim()) -> MensajesAcademia.CORREO_INVALIDO
        else -> null
    }

    /** El escudo no debe superar 2 MB. */
    fun validarEscudo(bytes: Long): String? = if (bytes > ESCUDO_MAX_BYTES) MensajesAcademia.IMAGEN_GRANDE else null

    /**
     * Clave para comprobar que el nombre es único sin importar mayúsculas, tildes ni espacios:
     * "Academia  Lima" y "academia lima" son el mismo nombre.
     */
    fun claveNombre(nombre: String): String =
        Normalizer.normalize(nombre.trim().lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')

    /** Cambios entre la versión anterior y la nueva: lo que se escribe en `auditoria`. */
    fun cambios(anterior: ConfiguracionAcademia, nueva: ConfiguracionAcademia): List<CambioAcademia> {
        val antes = anterior.valoresAuditables()
        return nueva.valoresAuditables().mapNotNull { (campo, valorNuevo) ->
            val valorAnterior = antes[campo].orEmpty()
            if (valorAnterior == valorNuevo) null else CambioAcademia(campo, valorAnterior, valorNuevo)
        }
    }

    private fun obligatorio(texto: String, maximo: Int): String? = when {
        texto.isBlank() -> MensajesAcademia.CAMPO_OBLIGATORIO
        texto.trim().length > maximo -> "Máximo $maximo caracteres"
        else -> null
    }
}

/** Una fila de la subcolección `academias/{id}/auditoria`. */
data class CambioAcademia(val campo: String, val valorAnterior: String, val valorNuevo: String)

/**
 * US-004: código de 8 caracteres con un rol asociado y vigencia de 7 días. Hoy la app usa los de
 * rol Administrador para crear esas cuentas; el rol queda guardado para los demás.
 */
data class CodigoInvitacion(
    val codigo: String,
    val creadoEn: Date?,
    val expiraEn: Date?,
    val usado: Boolean,
    val revocado: Boolean,
    val rol: UserRole = UserRole.ADMINISTRADOR,
) {
    fun vigente(ahora: Date = Date()): Boolean = estado(ahora) == EstadoCodigo.VIGENTE

    fun estado(ahora: Date = Date()): EstadoCodigo = when {
        usado -> EstadoCodigo.USADO
        revocado -> EstadoCodigo.REVOCADO
        expiraEn != null && !expiraEn.after(ahora) -> EstadoCodigo.EXPIRADO
        else -> EstadoCodigo.VIGENTE
    }
}

enum class EstadoCodigo(val etiqueta: String) {
    VIGENTE("Vigente"),
    USADO("Usado"),
    EXPIRADO("Expirado"),
    REVOCADO("Revocado"),
}

/** Datos de la academia y códigos de invitación. Implementación: `data/academia/FirestoreAcademiaRepository`. */
interface AcademiaRepository {
    fun observarConfiguracion(academiaId: String): Flow<ConfiguracionAcademia>

    /** @return `true` si el servidor confirmó, `false` si quedó pendiente por falta de conexión. */
    suspend fun guardarConfiguracion(configuracion: ConfiguracionAcademia): Boolean

    /**
     * US-004: guarda todos los datos, reserva el nombre (único) y registra en `auditoria` cada
     * campo que cambió con uid, valor anterior, valor nuevo y fecha.
     * @throws NombreAcademiaDuplicadoException si otra academia ya usa ese nombre.
     */
    suspend fun guardarDatos(anterior: ConfiguracionAcademia, nueva: ConfiguracionAcademia, uid: String): Boolean

    /**
     * Sube el escudo (máximo 2 MB, recortado 1:1) y devuelve su URL, o `null` si no se pudo.
     * @throws EscudoMuyGrandeException si la imagen pesa más de 2 MB.
     */
    suspend fun subirEscudo(academiaId: String, uriLocal: String): String?

    fun observarCodigos(academiaId: String): Flow<List<CodigoInvitacion>>

    /** Genera un código nuevo (sin caracteres ambiguos) para [rol] y devuelve su texto. */
    suspend fun generarCodigo(academiaId: String, adminUid: String, rol: UserRole = UserRole.ADMINISTRADOR): String

    suspend fun revocarCodigo(codigo: String): Boolean
}

class NombreAcademiaDuplicadoException : Exception(MensajesAcademia.NOMBRE_DUPLICADO)

class EscudoMuyGrandeException : Exception(MensajesAcademia.IMAGEN_GRANDE)
