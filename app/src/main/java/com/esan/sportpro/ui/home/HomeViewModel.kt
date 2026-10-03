package com.esan.sportpro.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esan.sportpro.domain.cuentas.PerfilActual
import com.esan.sportpro.domain.cuentas.SesionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HomeUiState {
    data object Cargando : HomeUiState
    data class Listo(val perfil: PerfilActual) : HomeUiState
    data object SinPerfil : HomeUiState
}

/** US-003: carga el perfil (rol y academia) del usuario autenticado para armar la navegación. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sesionRepository: SesionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Cargando)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        cargarPerfil()
    }

    fun cargarPerfil() {
        _uiState.value = HomeUiState.Cargando
        viewModelScope.launch {
            val perfil = sesionRepository.perfilActual()
            _uiState.value = if (perfil != null) HomeUiState.Listo(perfil) else HomeUiState.SinPerfil
        }
    }
}
