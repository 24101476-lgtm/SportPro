package com.esan.sportpro.domain.ia

import com.esan.sportpro.domain.cuentas.PerfilActual
import com.esan.sportpro.domain.partido.Partido
import com.esan.sportpro.navigation.UserRole

/** Los tres bloques editables del resumen (US-024, criterio 2). */
enum class CampoBloque { HECHOS, DESARROLLO, INTERPRETACION }

/**
 * Reglas de la revisión, la corrección y la aprobación del resumen (US-024). Funciones puras, sin
 * Firebase ni Android, para probarlas con pruebas unitarias.
 */
object ReglasRevision {

    /** Leyenda que acompaña de forma permanente a todo resumen publicado (criterio 8). */
    const val LEYENDA_IA = "Resumen generado con IA y revisado por el entrenador"

    /** Texto que el entrenador acepta al pulsar Aprobar y publicar (criterio 5). */
    const val TEXTO_CONFIRMACION =
        "Confirmo que revisé el resumen y sus datos corresponden a lo ocurrido en el partido"

    /** Mensaje cuando todavía queda alguna mención marcada (criterio 4). */
    const val MSG_MENCIONES_PENDIENTES = "Corrige las menciones no verificadas antes de aprobar"

    const val MSG_SIN_HECHOS = "Escribe al menos un hecho del partido"
    const val MSG_SIN_DESARROLLO = "Escribe el desarrollo del partido"
    const val MSG_LIMITE_PALABRAS = "Los hechos y el desarrollo no pueden superar ${ReglasResumen.MAX_PALABRAS} palabras"

    const val MAX_INTERPRETACION = 1000
    const val MSG_INTERPRETACION = "La interpretación no puede superar $MAX_INTERPRETACION caracteres"

    /** Máximo de caracteres de una publicación del muro (US-028, criterio 1). */
    private const val MAX_PUBLICACION = 1000

    /** Validación de forma de los tres bloques; el mapa vacío significa que se puede guardar. */
    fun validar(bloques: BloquesResumen): Map<CampoBloque, String> {
        val errores = mutableMapOf<CampoBloque, String>()
        if (bloques.hechos.isBlank()) errores[CampoBloque.HECHOS] = MSG_SIN_HECHOS
        if (bloques.desarrollo.isBlank()) errores[CampoBloque.DESARROLLO] = MSG_SIN_DESARROLLO
        if (errores.isEmpty() && !ReglasResumen.dentroDelLimite(bloques)) {
            errores[CampoBloque.DESARROLLO] = MSG_LIMITE_PALABRAS
        }
        if (bloques.interpretacion.length > MAX_INTERPRETACION) {
            errores[CampoBloque.INTERPRETACION] = MSG_INTERPRETACION
        }
        return errores
    }

    /**
     * Motivo por el que todavía no se puede aprobar, o null si se puede (criterios 4 y 5). Primero se
     * piden los datos que faltan y después las menciones sin verificar.
     */
    fun motivoNoAprobar(bloques: BloquesResumen, menciones: List<MencionNoVerificada>): String? =
        validar(bloques).values.firstOrNull()
            ?: MSG_MENCIONES_PENDIENTES.takeIf { menciones.isNotEmpty() }

    /**
     * Menciones que siguen sin resolverse en el texto que se está editando (criterio 4): las que marcó
     * la generación y todavía aparecen tal cual, más las que detecta la verificación sobre el texto
     * nuevo. Una mención corregida o eliminada deja de contar en cuanto el texto cambia.
     */
    fun mencionesVigentes(
        previas: List<MencionNoVerificada>,
        bloques: BloquesResumen,
        insumo: InsumoResumen,
    ): List<MencionNoVerificada> {
        val texto = bloques.hechos + "\n" + bloques.desarrollo
        val sinResolver = previas.filter { it.texto.isNotBlank() && texto.contains(it.texto) }
        return (sinResolver + ReglasResumen.verificarEdicion(bloques.hechos, bloques.desarrollo, insumo))
            .distinctBy { it.texto }
    }

    /**
     * Quien revisa y aprueba: el administrador, o el entrenador que opera el partido (operador,
     * entrenador responsable o creador). Es la misma regla que aplica Firestore.
     */
    fun puedeRevisar(perfil: PerfilActual?, partido: Partido?): Boolean {
        if (perfil == null || partido == null || !perfil.esStaff) return false
        if (perfil.rol == UserRole.ADMINISTRADOR) return true
        return perfil.uid in listOfNotNull(partido.operadorUid, partido.entrenadorUid, partido.creadoPor)
    }

    /** El resumen que ven jugadores y padres: la versión aprobada más reciente. */
    fun publicadoMasReciente(resumenes: List<ResumenPartido>): ResumenPartido? =
        resumenes.filter { it.publicado }.maxByOrNull { it.version }

    /**
     * Texto de la publicación del muro cuando el entrenador habilita la comunidad (criterio 7): el
     * marcador, los hechos que quepan y la leyenda de IA, sin pasar de 1000 caracteres.
     */
    fun textoParaComunidad(partido: Partido, resumen: ResumenPartido): String {
        val marcador = resumen.marcador.split("-").map { it.trim() }
            .takeIf { it.size == 2 }
            ?.let { (propio, rival) -> "${partido.nombreEquipo} $propio - $rival ${partido.nombreRival}" }
            ?: "${partido.nombreEquipo} vs ${partido.nombreRival}"
        val encabezado = "Resumen del partido: $marcador"
        val pie = LEYENDA_IA
        val presupuesto = MAX_PUBLICACION - encabezado.length - pie.length - 4
        val hechos = resumen.bloquesVigentes.hechos.trim()
        val cuerpo = if (hechos.length <= presupuesto) {
            hechos
        } else {
            val corte = hechos.take(presupuesto - 1)
            corte.substringBeforeLast('\n', corte).trimEnd() + "…"
        }
        return "$encabezado\n\n$cuerpo\n\n$pie"
    }
}

/**
 * Diferencias entre el texto generado por la IA y el texto aprobado (US-025, criterio 3). Compara
 * por frases: los hechos por línea y el resto por oración.
 */
object DiferenciasResumen {

    const val SIN_CORRECCIONES = "Sin correcciones: se aprobó el texto generado por la IA."

    fun correcciones(original: BloquesResumen, final: BloquesResumen): String {
        val lineas = mutableListOf<String>()
        lineas += comparar("Hechos", unidades(original.hechos, porLinea = true), unidades(final.hechos, porLinea = true))
        lineas += comparar("Desarrollo", unidades(original.desarrollo, porLinea = false), unidades(final.desarrollo, porLinea = false))
        lineas += comparar(
            "Interpretación",
            unidades(original.interpretacion, porLinea = false),
            unidades(final.interpretacion, porLinea = false),
        )
        return if (lineas.isEmpty()) SIN_CORRECCIONES else lineas.joinToString("\n")
    }

    private fun unidades(texto: String, porLinea: Boolean): List<String> {
        val partes = if (porLinea) texto.lines() else texto.split(Regex("(?<=[.!?])\\s+|\\n+"))
        return partes.map { it.trim().removePrefix("•").trim() }.filter { it.isNotEmpty() }
    }

    private sealed interface Paso {
        data class Igual(val texto: String) : Paso
        data class Quita(val texto: String) : Paso
        data class Agrega(val texto: String) : Paso
    }

    private fun comparar(bloque: String, antes: List<String>, despues: List<String>): List<String> {
        // Subsecuencia común más larga: lo que no está en ella se quitó o se agregó.
        val lcs = Array(antes.size + 1) { IntArray(despues.size + 1) }
        for (i in antes.indices.reversed()) {
            for (j in despues.indices.reversed()) {
                lcs[i][j] = if (antes[i] == despues[j]) lcs[i + 1][j + 1] + 1 else maxOf(lcs[i + 1][j], lcs[i][j + 1])
            }
        }
        val pasos = mutableListOf<Paso>()
        var i = 0
        var j = 0
        while (i < antes.size && j < despues.size) {
            when {
                antes[i] == despues[j] -> { pasos += Paso.Igual(antes[i]); i++; j++ }
                lcs[i + 1][j] >= lcs[i][j + 1] -> { pasos += Paso.Quita(antes[i]); i++ }
                else -> { pasos += Paso.Agrega(despues[j]); j++ }
            }
        }
        while (i < antes.size) pasos += Paso.Quita(antes[i++])
        while (j < despues.size) pasos += Paso.Agrega(despues[j++])

        // Una frase quitada seguida de una agregada es, en la práctica, una frase corregida.
        val resultado = mutableListOf<String>()
        var k = 0
        while (k < pasos.size) {
            val paso = pasos[k]
            val siguiente = pasos.getOrNull(k + 1)
            when {
                paso is Paso.Quita && siguiente is Paso.Agrega -> {
                    resultado += "$bloque, se corrigió: «${paso.texto}» por «${siguiente.texto}»"
                    k += 2
                }
                paso is Paso.Quita -> { resultado += "$bloque, se eliminó: «${paso.texto}»"; k++ }
                paso is Paso.Agrega -> { resultado += "$bloque, se agregó: «${paso.texto}»"; k++ }
                else -> k++
            }
        }
        return resultado
    }
}
