package com.esan.sportpro.domain.comunidad

/** US-030, criterio 1: motivo elegido al reportar contenido. */
enum class MotivoReporte(val etiqueta: String) {
    CONTENIDO_OFENSIVO("Contenido ofensivo"),
    ACOSO("Acoso"),
    DATOS_PERSONALES_EXPUESTOS("Datos personales expuestos"),
    CONVOCATORIA_ENGANOSA("Convocatoria engañosa"),
    SUPLANTACION("Suplantación"),
    OTRO("Otro"),
}
