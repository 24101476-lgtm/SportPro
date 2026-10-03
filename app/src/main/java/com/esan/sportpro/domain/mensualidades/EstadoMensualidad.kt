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
