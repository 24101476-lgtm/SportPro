package com.esan.sportpro.ui.mensualidades

import androidx.lifecycle.viewModelScope
import com.esan.sportpro.core.viewmodel.BaseViewModel
import com.esan.sportpro.data.mensualidades.MensualidadRepository
import com.esan.sportpro.domain.mensualidades.EstadoMensualidad
import com.esan.sportpro.domain.mensualidades.MedioPago
import com.esan.sportpro.domain.mensualidades.Mensualidad
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Date
import javax.inject.Inject

data class MensualidadRegistrarUiState(
    val jugadorId: String = "",
    val jugadorNombre: String = "",
    val equipoId: String = "",
    val equipoNombre: String = "",
    val mes: Int = LocalDate.now().monthValue,
    val anio: Int = LocalDate.now().year,
    val montoTexto: String = "",
    val fechaRegistro: Date = Date(),
    val medioDeclarado: MedioPago = MedioPago.EFECTIVO,
    val observacion: String = "",
    val estadoSeleccionado: EstadoMensualidad = EstadoMensualidad.PAGADO,
    val isSaving: Boolean = false,
    val montoError: String? = null,
    val observacionError: String? = null,
    val errorMessage: String? = null,
)

sealed interface MensualidadRegistrarEvent {
    data object RegistroGuardado : MensualidadRegistrarEvent
}

/**
 * US-008 — Frame 38_Mensualidad_Registrar. Solo disponible para el rol Administrador
 * (criterio de aceptación 7); la verificación de rol vive en la navegación / capa de UI superior.
 */
@HiltViewModel
class MensualidadRegistrarViewModel @Inject constructor(
    private val repository: MensualidadRepository,
    private val firebaseAuth: FirebaseAuth,
) : BaseViewModel<MensualidadRegistrarUiState, MensualidadRegistrarEvent>(MensualidadRegistrarUiState()) {

    // TODO(academia): reemplazar por el academiaId real del usuario autenticado.
    private val academiaIdActual = "academia-demo"

    fun onJugadorSeleccionado(jugadorId: String, jugadorNombre: String, equipoId: String, equipoNombre: String) {
        setState {
            copy(
                jugadorId = jugadorId,
                jugadorNombre = jugadorNombre,
                equipoId = equipoId,
                equipoNombre = equipoNombre,
            )
        }
    }

    fun onMesAnioChanged(mes: Int, anio: Int) = setState { copy(mes = mes, anio = anio) }

    fun onMontoChanged(texto: String) = setState { copy(montoTexto = texto, montoError = null) }

    fun onFechaRegistroChanged(fecha: Date) = setState { copy(fechaRegistro = fecha) }

    fun onMedioChanged(medio: MedioPago) = setState { copy(medioDeclarado = medio) }

    fun onObservacionChanged(texto: String) {
        val error = if (texto.length > 200) "Máximo 200 caracteres" else null
        setState { copy(observacion = texto, observacionError = error) }
    }

    fun onEstadoChanged(estado: EstadoMensualidad) = setState { copy(estadoSeleccionado = estado) }

    /** Criterio de aceptación 4: 0..5000 con dos decimales; fuera de rango → "Monto inválido". */
    private fun validarMonto(texto: String): Pair<Double?, String?> {
        val valor = texto.replace(",", ".").toDoubleOrNull()
        val redondeado = valor?.let { Math.round(it * 100) / 100.0 }
        return if (redondeado == null || redondeado < 0.0 || redondeado > 5000.0) {
            null to "Monto inválido"
        } else {
            redondeado to null
        }
    }

    fun guardar() {
        val estado = currentState
        if (estado.jugadorId.isBlank()) {
            setState { copy(errorMessage = "Selecciona un jugador") }
            return
        }
        val (monto, montoError) = validarMonto(estado.montoTexto)
        val observacionError = if (estado.observacion.length > 200) "Máximo 200 caracteres" else null
        if (monto == null || observacionError != null) {
            setState { copy(montoError = montoError, observacionError = observacionError) }
            return
        }

        val uidAdmin = firebaseAuth.currentUser?.uid.orEmpty()
        viewModelScope.launch {
            setState { copy(isSaving = true, errorMessage = null) }
            try {
                repository.registrarPago(
                    nuevo = Mensualidad(
                        academiaId = academiaIdActual,
                        equipoId = estado.equipoId,
                        equipoNombre = estado.equipoNombre,
                        jugadorId = estado.jugadorId,
                        jugadorNombre = estado.jugadorNombre,
                        mes = estado.mes,
                        anio = estado.anio,
                        montoReferencial = monto,
                        fechaRegistro = estado.fechaRegistro,
                        medioDeclarado = estado.medioDeclarado,
                        observacion = estado.observacion.ifBlank { null },
                        estadoRegistrado = estado.estadoSeleccionado,
                    ),
                    uidAdministrador = uidAdmin,
                )
                setState { copy(isSaving = false) }
                sendEvent(MensualidadRegistrarEvent.RegistroGuardado)
            } catch (e: Exception) {
                setState { copy(isSaving = false, errorMessage = e.localizedMessage ?: "No se pudo registrar el pago") }
            }
        }
    }
}
