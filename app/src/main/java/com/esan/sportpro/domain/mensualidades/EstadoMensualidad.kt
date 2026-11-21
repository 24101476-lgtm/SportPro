package com.esan.sportpro.domain.mensualidades

/**
 * Estados posibles de una mensualidad (US-008).
 * [VENCIDO] se calcula automáticamente cuando la fecha actual supera el día de corte
 * configurado por la academia y la mensualidad seguía [PENDIENTE] — nunca se persiste
 * como transición manual, ver [EstadoMensualidad.calcular].
 */
enum class EstadoMensualidad(val etiqueta: String) {
    PAGADO("Pagado"),
    PENDIENTE("Pendiente"),
    VENCIDO("Vencido"),
    EXONERADO("Exonerado");

    companion object {
        /**
         * Día de corte por defecto. Cada academia lo configura (US-004, `academias/{id}.diaCorte`);
         * este valor se usa mientras no lo haya configurado.
         */
        const val DIA_CORTE = com.esan.sportpro.domain.academia.ConfiguracionAcademia.DIA_CORTE_POR_DEFECTO

        /** Fecha de corte de un mes: el [diaCorte] o el último día si el mes es más corto. */
        fun fechaCorte(mes: Int, anio: Int, diaCorte: Int = DIA_CORTE): java.time.LocalDate {
            val ym = java.time.YearMonth.of(anio, mes)
            return ym.atDay(minOf(diaCorte, ym.lengthOfMonth()))
        }

        /** Estado efectivo de un registro de [mes]/[anio] (Vencido calculado, nunca persistido). */
        fun efectivo(
            estadoRegistrado: EstadoMensualidad,
            mes: Int,
            anio: Int,
            hoy: java.time.LocalDate = java.time.LocalDate.now(),
            diaCorte: Int = DIA_CORTE,
        ): EstadoMensualidad = calcular(estadoRegistrado, fechaCorte(mes, anio, diaCorte), hoy)

        /**
         * Resuelve el estado efectivo de una mensualidad a mostrar en la grilla: si el registro
         * más reciente está [PENDIENTE] y ya pasó el día de corte del mes de la mensualidad,
         * se muestra como [VENCIDO] sin que esto implique un nuevo registro en Firestore.
         */
        fun calcular(
            estadoRegistrado: EstadoMensualidad,
            fechaCorte: java.time.LocalDate,
            hoy: java.time.LocalDate = java.time.LocalDate.now(),
        ): EstadoMensualidad {
            return if (estadoRegistrado == PENDIENTE && hoy.isAfter(fechaCorte)) {
                VENCIDO
            } else {
                estadoRegistrado
            }
        }
    }
}
