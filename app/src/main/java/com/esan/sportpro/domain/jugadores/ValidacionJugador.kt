package com.esan.sportpro.domain.jugadores

import com.esan.sportpro.domain.comunidad.PosicionJugador
import java.time.LocalDate

enum class CampoJugador {
    NOMBRES,
    APELLIDOS,
    FECHA_NACIMIENTO,
    EQUIPO,
    POSICION,
    POSICION_SECUNDARIA,
    PIE_HABIL,
    DOCUMENTO,
    DORSAL,
    ESTATURA,
    PESO,
    TELEFONO,
    CORREO,
    EMERGENCIA_NOMBRE,
    EMERGENCIA_TELEFONO,
    EMERGENCIA_PARENTESCO,
    APODERADO_CORREO,
}

/** Valores del formulario del jugador tal como los escribe el usuario (todo texto). */
data class DatosJugador(
    val nombres: String = "",
    val apellidos: String = "",
    val fechaNacimiento: String = "",
    val equipoId: String = "",
    val posicion: String = "",
    val posicionSecundaria: String = "",
    val pieHabil: String = "",
    val documento: String = "",
    val dorsal: String = "",
    /** Estatura en metros, con coma o punto: "1,76". */
    val estatura: String = "",
    val peso: String = "",
    val telefono: String = "",
    val correo: String = "",
    val emergenciaNombre: String = "",
    val emergenciaTelefono: String = "",
    val emergenciaParentesco: String = "",
    /** Correo del apoderado: obligatorio si el jugador es menor de edad. */
    val apoderadoCorreo: String = "",
)

/** Reglas de validación del perfil del jugador. Sin dependencias de Android ni Firebase. */
object ValidacionJugador {
    private val REGEX_NOMBRE = Regex("^\\p{L}[\\p{L} .'-]*$")
    private val REGEX_TELEFONO = Regex("^\\d{9}$")
    /** DNI (8 dígitos) o carné de extranjería / pasaporte (hasta 12 caracteres alfanuméricos). */
    private val REGEX_DOCUMENTO = Regex("^[A-Za-z0-9]{8,12}$")

    const val ESTATURA_MIN_M = 1.00
    const val ESTATURA_MAX_M = 2.30
    const val PESO_MIN_KG = 20.0
    const val PESO_MAX_KG = 150.0

    const val MSG_FUERA_DE_RANGO = "Valor fuera de rango"
    const val MSG_DORSAL_TOMADO = "El número ya pertenece a otro jugador del equipo"
    const val MSG_OBLIGATORIO_MENOR = "Obligatorio para menores de edad"
    const val MSG_TELEFONO = "Ingrese un número de 9 dígitos"
    private val REGEX_CORREO = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    const val EDAD_MINIMA = 4
    const val EDAD_MAXIMA = 60

    /**
     * @param jugadoresDelEquipo jugadores activos de la academia, para comprobar que el dorsal sea
     * único dentro del equipo (US-006, criterio 3).
     * @param idEditando id del jugador que se edita, para no compararlo consigo mismo.
     */
    fun validar(
        datos: DatosJugador,
        hoy: LocalDate = LocalDate.now(),
        jugadoresDelEquipo: List<Jugador> = emptyList(),
        idEditando: String? = null,
    ): Map<CampoJugador, String> {
        val errores = mutableMapOf<CampoJugador, String>()

        validarNombre(datos.nombres, 2, 50)?.let { errores[CampoJugador.NOMBRES] = it }
        validarNombre(datos.apellidos, 2, 50)?.let { errores[CampoJugador.APELLIDOS] = it }

        val nacimiento = FechasJugador.parsear(datos.fechaNacimiento)
        when {
            nacimiento == null ->
                errores[CampoJugador.FECHA_NACIMIENTO] = "Elige la fecha de nacimiento."
            nacimiento.isAfter(hoy) ->
                errores[CampoJugador.FECHA_NACIMIENTO] = "La fecha de nacimiento no puede ser futura."
            FechasJugador.edad(nacimiento, hoy) !in EDAD_MINIMA..EDAD_MAXIMA ->
                errores[CampoJugador.FECHA_NACIMIENTO] = "La edad debe estar entre $EDAD_MINIMA y $EDAD_MAXIMA años."
        }

        if (datos.equipoId.isBlank()) errores[CampoJugador.EQUIPO] = "Selecciona un equipo."

        // US-006, criterio 2: selector con las 9 posiciones del catálogo.
        if (PosicionJugador.entries.none { it.etiqueta == datos.posicion }) {
            errores[CampoJugador.POSICION] = "Selecciona una posición."
        }
        if (datos.posicionSecundaria.isNotBlank()) {
            when {
                PosicionJugador.entries.none { it.etiqueta == datos.posicionSecundaria } ->
                    errores[CampoJugador.POSICION_SECUNDARIA] = "Selecciona una posición del catálogo."
                datos.posicionSecundaria == datos.posicion ->
                    errores[CampoJugador.POSICION_SECUNDARIA] = "Debe ser distinta de la posición principal."
            }
        }
        if (datos.pieHabil.isNotBlank() && PieHabil.entries.none { it.etiqueta == datos.pieHabil }) {
            errores[CampoJugador.PIE_HABIL] = "Selecciona el pie hábil."
        }
        if (datos.documento.isNotBlank() && !REGEX_DOCUMENTO.matches(datos.documento.trim())) {
            errores[CampoJugador.DOCUMENTO] = "Ingresa un DNI de 8 dígitos o un documento de hasta 12 caracteres."
        }

        // US-006, criterio 3: dorsal de 1 a 99 y único dentro del equipo.
        if (datos.dorsal.isNotBlank()) {
            val dorsal = datos.dorsal.trim().toIntOrNull()
            when {
                dorsal == null || dorsal !in 1..99 -> errores[CampoJugador.DORSAL] = "El dorsal debe estar entre 1 y 99."
                jugadoresDelEquipo.any {
                    it.id != idEditando && it.activo && it.equipoId == datos.equipoId && it.dorsal == dorsal
                } -> errores[CampoJugador.DORSAL] = MSG_DORSAL_TOMADO
            }
        }

        // US-006, criterio 4: estatura 1,00-2,30 m y peso 20-150 kg.
        if (datos.estatura.isNotBlank()) {
            val metros = parsearDecimal(datos.estatura)
            if (metros == null || metros < ESTATURA_MIN_M || metros > ESTATURA_MAX_M) {
                errores[CampoJugador.ESTATURA] = MSG_FUERA_DE_RANGO
            }
        }
        if (datos.peso.isNotBlank()) {
            val peso = parsearPeso(datos.peso)
            if (peso == null || peso < PESO_MIN_KG || peso > PESO_MAX_KG) {
                errores[CampoJugador.PESO] = MSG_FUERA_DE_RANGO
            }
        }

        if (datos.telefono.isNotBlank() && !REGEX_TELEFONO.matches(datos.telefono.trim())) {
            errores[CampoJugador.TELEFONO] = MSG_TELEFONO
        }

        if (datos.correo.isNotBlank() && !REGEX_CORREO.matches(datos.correo.trim())) {
            errores[CampoJugador.CORREO] = "Ingresa un correo válido."
        }

        // US-006, criterio 5: el contacto de emergencia es obligatorio para menores de edad (es su
        // apoderado, regla RN-08). Para mayores es opcional, pero si se llena debe ser válido.
        val menor = nacimiento != null && !nacimiento.isAfter(hoy) && FechasJugador.edad(nacimiento, hoy) < Jugador.EDAD_MAYORIA
        val nombreEmergencia = datos.emergenciaNombre.trim()
        val telefonoEmergencia = datos.emergenciaTelefono.trim()
        val parentesco = datos.emergenciaParentesco.trim()
        val algoDeEmergencia = nombreEmergencia.isNotEmpty() || telefonoEmergencia.isNotEmpty() || parentesco.isNotEmpty()

        if (menor || algoDeEmergencia) {
            when {
                nombreEmergencia.isEmpty() ->
                    errores[CampoJugador.EMERGENCIA_NOMBRE] = if (menor) MSG_OBLIGATORIO_MENOR else "Completa el nombre del contacto."
                else -> validarNombre(nombreEmergencia, 3, 60)?.let { errores[CampoJugador.EMERGENCIA_NOMBRE] = it }
            }
            when {
                telefonoEmergencia.isEmpty() ->
                    errores[CampoJugador.EMERGENCIA_TELEFONO] = if (menor) MSG_OBLIGATORIO_MENOR else MSG_TELEFONO
                !REGEX_TELEFONO.matches(telefonoEmergencia) -> errores[CampoJugador.EMERGENCIA_TELEFONO] = MSG_TELEFONO
            }
            when {
                parentesco.isEmpty() ->
                    errores[CampoJugador.EMERGENCIA_PARENTESCO] = if (menor) MSG_OBLIGATORIO_MENOR else "Indica el parentesco."
                menor && parentesco !in ReglasApoderado.PARENTESCOS_APODERADO ->
                    errores[CampoJugador.EMERGENCIA_PARENTESCO] = "Elige el parentesco del apoderado."
                !menor && parentesco.length !in 2..30 ->
                    errores[CampoJugador.EMERGENCIA_PARENTESCO] = "Indica el parentesco (2 a 30 caracteres)."
            }
        }
        val correoApoderado = datos.apoderadoCorreo.trim()
        when {
            menor && correoApoderado.isEmpty() ->
                errores[CampoJugador.APODERADO_CORREO] = "El correo del apoderado es obligatorio para menores de edad."
            correoApoderado.isNotEmpty() && !REGEX_CORREO.matches(correoApoderado) ->
                errores[CampoJugador.APODERADO_CORREO] = "Ingresa un correo válido."
        }
        return errores
    }

    /** El jugador es menor de edad según la fecha escrita en el formulario (falso si no es válida). */
    fun esMenor(fechaNacimiento: String, hoy: LocalDate = LocalDate.now()): Boolean {
        val nacimiento = FechasJugador.parsear(fechaNacimiento) ?: return false
        return !nacimiento.isAfter(hoy) && FechasJugador.edad(nacimiento, hoy) < Jugador.EDAD_MAYORIA
    }

    /** Rango que ofrece el calendario de la fecha de nacimiento (entre 4 y 60 años). */
    fun fechaNacimientoMinima(hoy: LocalDate = LocalDate.now()): LocalDate = hoy.minusYears(EDAD_MAXIMA.toLong() + 1).plusDays(1)

    fun fechaNacimientoMaxima(hoy: LocalDate = LocalDate.now()): LocalDate = hoy.minusYears(EDAD_MINIMA.toLong())

    /** Acepta coma o punto decimal ("45,5" o "45.5"). */
    fun parsearPeso(texto: String): Double? = parsearDecimal(texto)

    fun parsearDecimal(texto: String): Double? = texto.trim().replace(',', '.').toDoubleOrNull()

    /** "1,76" -> 176 cm (se guarda en centímetros, como los datos anteriores). */
    fun estaturaACentimetros(texto: String): Int? = parsearDecimal(texto)?.let { Math.round(it * 100).toInt() }

    /** 176 -> "1,76" para mostrar y editar en metros. */
    fun centimetrosAMetros(cm: Int?): String =
        cm?.let { "%d,%02d".format(it / 100, it % 100) }.orEmpty()

    private fun validarNombre(valor: String, min: Int, max: Int): String? {
        val limpio = valor.trim()
        return when {
            limpio.length < min -> "Debe tener al menos $min caracteres."
            limpio.length > max -> "No puede superar $max caracteres."
            !REGEX_NOMBRE.matches(limpio) -> "Solo se permiten letras y espacios."
            else -> null
        }
    }
}
