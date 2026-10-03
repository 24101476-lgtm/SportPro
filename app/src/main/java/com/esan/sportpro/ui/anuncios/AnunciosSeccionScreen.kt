package com.esan.sportpro.ui.anuncios

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.anuncios.Anuncio
import com.google.firebase.auth.FirebaseAuth

private sealed interface AnunciosDestino {
    data object Lista : AnunciosDestino
    data object Nuevo : AnunciosDestino
    data object Preferencias : AnunciosDestino
    data class Detalle(val anuncioId: String) : AnunciosDestino
}

/**
 * Sección "Anuncios" del tab Estadísticas y Comunicación (US-027, frames 126 a 130).
 * El [AnunciosViewModel] se comparte entre las pantallas internas vía [hiltViewModel].
 */
@Composable
fun AnunciosSeccionScreen(modifier: Modifier = Modifier) {
    val viewModel: AnunciosViewModel = hiltViewModel()
    var destino by rememberSaveable(stateSaver = DestinoSaver) { mutableStateOf<AnunciosDestino>(AnunciosDestino.Lista) }
    val state by viewModel.uiState.collectAsState()
    val uidActual = remember { FirebaseAuth.getInstance().currentUser?.uid }

    when (val actual = destino) {
        is AnunciosDestino.Lista -> AnunciosListaScreen(
            onNuevoAnuncio = { destino = AnunciosDestino.Nuevo },
            onAbrirAnuncio = { anuncio: Anuncio -> destino = AnunciosDestino.Detalle(anuncio.id) },
            onAbrirPreferencias = { destino = AnunciosDestino.Preferencias },
            modifier = modifier,
            viewModel = viewModel,
        )
        is AnunciosDestino.Nuevo -> AnuncioNuevoScreen(
            onPublicado = { destino = AnunciosDestino.Lista },
            onCancelar = { destino = AnunciosDestino.Lista },
            modifier = modifier,
            viewModel = viewModel,
        )
        is AnunciosDestino.Preferencias -> PreferenciasNotificacionesScreen(
            onVolver = { destino = AnunciosDestino.Lista },
            modifier = modifier,
            viewModel = viewModel,
        )
        is AnunciosDestino.Detalle -> {
            val anuncio = (state.anuncios + state.anunciosUrgentesNoLeidos).firstOrNull { it.id == actual.anuncioId }
            if (anuncio != null) {
                AnuncioDetalleScreen(
                    anuncio = anuncio,
                    esAutor = anuncio.autorUid == uidActual,
                    onVolver = { destino = AnunciosDestino.Lista },
                    modifier = modifier,
                    viewModel = viewModel,
                )
            } else {
                androidx.compose.runtime.LaunchedEffect(actual.anuncioId) { destino = AnunciosDestino.Lista }
            }
        }
    }
}

private val DestinoSaver = androidx.compose.runtime.saveable.Saver<AnunciosDestino, String>(
    save = { destino ->
        when (destino) {
            is AnunciosDestino.Lista -> "lista"
            is AnunciosDestino.Nuevo -> "nuevo"
            is AnunciosDestino.Preferencias -> "preferencias"
            is AnunciosDestino.Detalle -> "detalle|${destino.anuncioId}"
        }
    },
    restore = { guardado ->
        when {
            guardado == "lista" -> AnunciosDestino.Lista
            guardado == "nuevo" -> AnunciosDestino.Nuevo
            guardado == "preferencias" -> AnunciosDestino.Preferencias
            guardado.startsWith("detalle|") -> AnunciosDestino.Detalle(guardado.removePrefix("detalle|"))
            else -> AnunciosDestino.Lista
        }
    },
)
