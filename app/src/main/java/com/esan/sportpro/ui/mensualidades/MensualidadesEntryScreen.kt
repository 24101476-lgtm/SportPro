package com.esan.sportpro.ui.mensualidades

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.esan.sportpro.navigation.UserRole

/** Estados internos de la navegación anidada del módulo Mensualidades (US-008). */
private sealed interface MensualidadesDestino {
    data object Grilla : MensualidadesDestino
    data object Registrar : MensualidadesDestino
    data class Historial(val jugadorId: String, val jugadorNombre: String) : MensualidadesDestino
}

/**
 * Punto de entrada del módulo de Mensualidades (US-008, frames 37 a 40).
 *
 * Sigue el mismo patrón de navegación interna por estado que [com.esan.sportpro.ui.home.HomeScreen]
 * usa para alternar entre módulos, en vez de un grafo anidado de Navigation-Compose, para mantener
 * consistencia con el resto del proyecto.
 *
 * TODO(cuentas): [rolActual] debe venir del perfil del usuario autenticado una vez que el módulo
 * `cuentas` (US-001) exponga el rol real; por ahora se recibe como parámetro con valor por defecto.
 */
@Composable
fun MensualidadesEntryScreen(
    modifier: Modifier = Modifier,
    rolActual: UserRole = UserRole.ADMINISTRADOR,
) {
    if (rolActual == UserRole.PADRE_DE_FAMILIA) {
        PadreMensualidadesScreen(modifier = modifier)
        return
    }
    if (rolActual == UserRole.JUGADOR) {
        // Criterio de aceptación 7: el jugador no accede al módulo de mensualidades.
        com.esan.sportpro.ui.common.PlaceholderScreen(
            title = "Mensualidades",
            relatedUserStories = "Módulo no disponible para el rol Jugador (US-008)",
            modifier = modifier,
        )
        return
    }

    var destino by rememberSaveable(stateSaver = DestinoSaver) { mutableStateOf<MensualidadesDestino>(MensualidadesDestino.Grilla) }

    when (val actual = destino) {
        is MensualidadesDestino.Grilla -> MensualidadesGrillaScreen(
            onNuevoRegistro = { destino = MensualidadesDestino.Registrar },
            onVerHistorial = { jugadorId, jugadorNombre ->
                destino = MensualidadesDestino.Historial(jugadorId, jugadorNombre)
            },
            modifier = modifier,
        )
        is MensualidadesDestino.Registrar -> MensualidadRegistrarScreen(
            onGuardado = { destino = MensualidadesDestino.Grilla },
            onCancelar = { destino = MensualidadesDestino.Grilla },
            modifier = modifier,
        )
        is MensualidadesDestino.Historial -> MensualidadHistorialScreen(
            jugadorId = actual.jugadorId,
            jugadorNombre = actual.jugadorNombre,
            modifier = modifier,
        )
    }
}

/**
 * [MensualidadesDestino] no es serializable trivialmente por rememberSaveable (lleva datos de
 * navegación), así que se guarda solo lo esencial como texto y se reconstruye al restaurar.
 */
private val DestinoSaver = androidx.compose.runtime.saveable.Saver<MensualidadesDestino, String>(
    save = { destino ->
        when (destino) {
            is MensualidadesDestino.Grilla -> "grilla"
            is MensualidadesDestino.Registrar -> "registrar"
            is MensualidadesDestino.Historial -> "historial|${destino.jugadorId}|${destino.jugadorNombre}"
        }
    },
    restore = { guardado ->
        when {
            guardado == "grilla" -> MensualidadesDestino.Grilla
            guardado == "registrar" -> MensualidadesDestino.Registrar
            guardado.startsWith("historial|") -> {
                val partes = guardado.split("|")
                MensualidadesDestino.Historial(partes.getOrElse(1) { "" }, partes.getOrElse(2) { "" })
            }
            else -> MensualidadesDestino.Grilla
        }
    },
)
