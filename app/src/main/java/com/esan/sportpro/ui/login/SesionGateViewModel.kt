package com.esan.sportpro.ui.login

import com.esan.sportpro.domain.cuentas.SesionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * US-002: control de la sesión en el grafo de navegación.
 *
 * [haySesionActiva] se consulta al construir el NavHost para decidir el destino inicial (si hay
 * sesión abierta se salta el login), y [cerrarSesion] cierra la sesión de Firebase Auth antes de
 * volver al login.
 */
@HiltViewModel
class SesionGateViewModel @Inject constructor(
    private val sesionRepository: SesionRepository,
) : androidx.lifecycle.ViewModel() {

    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    fun haySesionActiva(): Boolean = sesionRepository.haySesionActiva()

    /** Cierra la sesión y ejecuta [alTerminar] en el hilo principal, aunque Firebase falle. */
    fun cerrarSesion(alTerminar: () -> Unit) {
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { sesionRepository.cerrarSesion() } }
            alTerminar()
        }
    }
}