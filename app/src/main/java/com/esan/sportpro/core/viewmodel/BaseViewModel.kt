package com.esan.sportpro.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Base para todos los ViewModel de los módulos (cuentas, academia, jugadores, entrenamientos,
 * partido, comunidad, ia). Expone estado observable vía [uiState] y eventos de un solo disparo
 * (navegación, snackbars) vía [events], para que cada equipo siga el mismo patrón MVVM.
 */
abstract class BaseViewModel<State, Event>(initialState: State) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<State> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<Event>()
    val events: SharedFlow<Event> = _events.asSharedFlow()

    protected val currentState: State
        get() = _uiState.value

    protected fun setState(reducer: State.() -> State) {
        _uiState.update(reducer)
    }

    protected fun sendEvent(event: Event) {
        viewModelScope.launch { _events.emit(event) }
    }
}
