package com.esan.sportpro.domain

import com.esan.sportpro.domain.estadisticas.EstadisticaJugador
import com.esan.sportpro.domain.estadisticas.RankingsJugadores
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RankingsJugadoresTest {

    private fun j(id: String, goles: Int = 0, asist: Int = 0, ta: Int = 0, tr: Int = 0, min: Int = 0, sinAsignar: Boolean = false) =
        EstadisticaJugador(
            jugadorId = id, jugadorNombre = id, goles = goles, asistencias = asist,
            tarjetasAmarillas = ta, tarjetasRojas = tr, minutosJugados = min, sinAsignar = sinAsignar,
        )

    @Test
    fun `el puntaje suma goles y asistencias y resta tarjetas`() {
        assertEquals(3 * 2 + 2 * 1 - 1 - 3, RankingsJugadores.puntaje(j("a", goles = 2, asist = 1, ta = 1, tr = 1)))
    }

    @Test
    fun `el destacado es el de mayor puntaje y desempata por minutos`() {
        val lista = listOf(j("a", goles = 1, min = 90), j("b", goles = 1, min = 135), j("c", asist = 1))
        assertEquals("b", RankingsJugadores.destacado(lista)?.jugadorId)
    }

    @Test
    fun `sin puntos nadie es destacado y Sin asignar no cuenta`() {
        assertNull(RankingsJugadores.destacado(listOf(j("a"), j("x", goles = 5, sinAsignar = true))))
    }

    @Test
    fun `el top ordena de mayor a menor, omite ceros y respeta el limite`() {
        val lista = listOf(j("a", goles = 0), j("b", goles = 2), j("c", goles = 5), j("d", goles = 1), j("e", goles = 3))
        val top = RankingsJugadores.top(lista, 3) { it.goles.toDouble() }
        assertEquals(listOf("c", "e", "b"), top.map { it.jugadorId })
    }
}
