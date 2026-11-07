package com.esan.sportpro.domain.ia

import com.esan.sportpro.domain.cuentas.PerfilActual
import com.esan.sportpro.navigation.UserRole
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/**
 * Evaluación documentada de un resumen aprobado (US-025). Se guarda en `evaluacionesResumen` y queda
 * vinculada a la versión evaluada, al partido y al uid de quien la registró.
 */
data class EvaluacionResumen(
    val id: String,
    val partidoId: String,
    val resumenId: String,
    val version: Int,
    /** "Equipo vs Rival", copiado para el consolidado sin leer cada partido. */
    val partidoNombre: String,
    val fechaPartidoMs: Long?,
    val academiaId: String,
    /** Entrenador que aprobó el resumen; es quien puede consultar la evaluación junto con el administrador. */
    val aprobadoPor: String,
    val evaluadorUid: String,
    val utilidad: Int,
    val omisiones: Int,
    val errores: Int,
    val inventados: Int,
    val comentario: String,
    val correcciones: String,
    val creadoEnMs: Long?,
)

/** Valores del formulario tal como los escribe el entrenador. */
data class DatosEvaluacion(
    val utilidad: String = "",
    val omisiones: String = "",
    val errores: String = "",
    val inventados: String = "",
    val comentario: String = "",
    val correcciones: String = "",
)

enum class CampoEvaluacion { UTILIDAD, OMISIONES, ERRORES, INVENTADOS, COMENTARIO, CORRECCIONES }

/** Totales de todas las evaluaciones: lo que muestra la pantalla Evaluaciones (criterio 5). */
data class ConsolidadoEvaluaciones(
    val evaluaciones: Int,
    /** Promedio con un decimal; null si todavía no hay evaluaciones. */
    val utilidadPromedio: Double?,
    val omisiones: Int,
    val errores: Int,
    val inventados: Int,
)

/** Reglas de la evaluación del resumen (US-025). Funciones puras, sin Firebase ni Android. */
object ReglasEvaluacion {

    const val UTILIDAD_MIN = 1
    const val UTILIDAD_MAX = 5
    const val CANTIDAD_MAX = 99
    const val COMENTARIO_MIN = 20
    const val COMENTARIO_MAX = 500
    const val CORRECCIONES_MAX = 1500

    const val MSG_UTILIDAD = "Elige un valor de $UTILIDAD_MIN a $UTILIDAD_MAX"
    const val MSG_CANTIDAD = "Ingresa un número entre 0 y $CANTIDAD_MAX"
    const val MSG_COMENTARIO = "El comentario debe tener entre $COMENTARIO_MIN y $COMENTARIO_MAX caracteres"
    const val MSG_CORRECCIONES = "Las correcciones no pueden superar $CORRECCIONES_MAX caracteres"

    fun validar(datos: DatosEvaluacion): Map<CampoEvaluacion, String> {
        val errores = mutableMapOf<CampoEvaluacion, String>()
        val utilidad = datos.utilidad.trim().toIntOrNull()
        if (utilidad == null || utilidad !in UTILIDAD_MIN..UTILIDAD_MAX) errores[CampoEvaluacion.UTILIDAD] = MSG_UTILIDAD
        fun cantidad(texto: String, campo: CampoEvaluacion) {
            val valor = texto.trim().toIntOrNull()
            if (valor == null || valor !in 0..CANTIDAD_MAX) errores[campo] = MSG_CANTIDAD
        }
        cantidad(datos.omisiones, CampoEvaluacion.OMISIONES)
        cantidad(datos.errores, CampoEvaluacion.ERRORES)
        cantidad(datos.inventados, CampoEvaluacion.INVENTADOS)
        if (datos.comentario.trim().length !in COMENTARIO_MIN..COMENTARIO_MAX) errores[CampoEvaluacion.COMENTARIO] = MSG_COMENTARIO
        if (datos.correcciones.trim().length > CORRECCIONES_MAX) errores[CampoEvaluacion.CORRECCIONES] = MSG_CORRECCIONES
        return errores
    }

    /** Solo el entrenador que aprobó el resumen y el administrador registran o consultan la evaluación (criterio 7). */
    fun puedeEvaluar(perfil: PerfilActual?, resumen: ResumenPartido): Boolean {
        if (perfil == null || resumen.aprobadoPor == null) return false
        return perfil.rol == UserRole.ADMINISTRADOR ||
            (perfil.rol == UserRole.ENTRENADOR && resumen.aprobadoPor == perfil.uid)
    }

    /** Una evaluación queda pendiente mientras el resumen ya aprobado no tenga la suya (criterio 1). */
    fun pendiente(perfil: PerfilActual?, resumen: ResumenPartido?, evaluacion: EvaluacionResumen?): Boolean =
        resumen != null && evaluacion == null && puedeEvaluar(perfil, resumen)

    fun consolidar(evaluaciones: List<EvaluacionResumen>): ConsolidadoEvaluaciones = ConsolidadoEvaluaciones(
        evaluaciones = evaluaciones.size,
        utilidadPromedio = if (evaluaciones.isEmpty()) {
            null
        } else {
            BigDecimal(evaluaciones.sumOf { it.utilidad }.toDouble() / evaluaciones.size)
                .setScale(1, RoundingMode.HALF_UP).toDouble()
        },
        omisiones = evaluaciones.sumOf { it.omisiones },
        errores = evaluaciones.sumOf { it.errores },
        inventados = evaluaciones.sumOf { it.inventados },
    )

    /** "4.2": un decimal con punto, igual en todos los dispositivos. */
    fun formatearPromedio(valor: Double?): String = valor?.let { String.format(Locale.US, "%.1f", it) } ?: "–"

    /** Texto plano del consolidado para pegarlo en el informe final del curso (criterio 6). */
    fun textoExportable(evaluaciones: List<EvaluacionResumen>, formatoFecha: (Long) -> String): String {
        val total = consolidar(evaluaciones)
        return buildString {
            appendLine("EVALUACIÓN DEL RESUMEN NARRATIVO CON IA - SportPro")
            appendLine("Evaluaciones registradas: ${total.evaluaciones}")
            appendLine("Utilidad promedio: ${formatearPromedio(total.utilidadPromedio)} de $UTILIDAD_MAX")
            appendLine("Omisiones detectadas: ${total.omisiones}")
            appendLine("Errores factuales: ${total.errores}")
            appendLine("Datos inventados: ${total.inventados}")
            if (evaluaciones.isNotEmpty()) {
                appendLine()
                appendLine("Detalle por partido:")
            }
            evaluaciones.forEachIndexed { indice, e ->
                appendLine()
                val fecha = e.fechaPartidoMs?.let { " - ${formatoFecha(it)}" }.orEmpty()
                appendLine("${indice + 1}. ${e.partidoNombre} (versión ${e.version})$fecha")
                appendLine(
                    "   Utilidad: ${e.utilidad}/$UTILIDAD_MAX · Omisiones: ${e.omisiones} · " +
                        "Errores factuales: ${e.errores} · Datos inventados: ${e.inventados}",
                )
                appendLine("   Comentario: ${e.comentario}")
                if (e.correcciones.isNotBlank()) {
                    appendLine("   Correcciones realizadas:")
                    e.correcciones.lines().filter { it.isNotBlank() }.forEach { appendLine("   - ${it.trim()}") }
                }
            }
        }.trimEnd()
    }
}
