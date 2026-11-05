package com.esan.sportpro.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.academia.EquipoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Escudos subidos por la academia, por nombre de equipo normalizado ([normalizarNombreEquipo]).
 * [EscudoEquipo] lo consulta, así cualquier pantalla (Inicio, Partidos, Academia...) muestra el
 * escudo real de un equipo solo con su nombre, sin cambiar sus ViewModels.
 */
val LocalEscudosEquipos = staticCompositionLocalOf<Map<String, String>> { emptyMap() }

/** Observa los equipos de la academia y arma el mapa nombre → URL del escudo. */
@HiltViewModel
class EscudosEquiposViewModel @Inject constructor(
    private val equipoRepository: EquipoRepository,
) : ViewModel() {

    private val _escudos = MutableStateFlow<Map<String, String>>(emptyMap())
    val escudos: StateFlow<Map<String, String>> = _escudos.asStateFlow()

    private var academia: String? = null

    fun iniciar(academiaId: String) {
        if (academia == academiaId) return
        academia = academiaId
        viewModelScope.launch {
            equipoRepository.observarEquipos(academiaId)
                .catch { /* los escudos son decorativos: si falla, se muestran los generados */ }
                .collect { lista ->
                    _escudos.value = lista
                        .filter { it.escudoUrl.isNotBlank() }
                        .associate { normalizarNombreEquipo(it.nombre) to it.escudoUrl }
                }
        }
    }
}

/** Envuelve el contenido para que [EscudoEquipo] encuentre los escudos subidos de la academia. */
@Composable
fun ProveedorEscudos(
    academiaId: String,
    viewModel: EscudosEquiposViewModel = hiltViewModel(),
    contenido: @Composable () -> Unit,
) {
    LaunchedEffect(academiaId) { viewModel.iniciar(academiaId) }
    val escudos by viewModel.escudos.collectAsState()
    CompositionLocalProvider(LocalEscudosEquipos provides escudos) {
        contenido()
    }
}
