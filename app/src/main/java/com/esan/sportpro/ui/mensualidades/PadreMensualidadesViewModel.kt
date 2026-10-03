package com.esan.sportpro.ui.mensualidades

import androidx.lifecycle.viewModelScope
import com.esan.sportpro.core.viewmodel.BaseViewModel
import com.esan.sportpro.data.mensualidades.MensualidadRepository
import com.esan.sportpro.domain.mensualidades.EstadoMensualidad
import com.esan.sportpro.domain.mensualidades.Mensualidad
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class PadreMensualidadesUiState(
    val mensualidades: List<MensualidadConEstadoEfectivo> = emptyList(),
    val isLoading: Boolean = true,
    val sinJugadoresVinculados: Boolean = false,
)

/**
 * US-008 — Frame 40_Padre_Mensualidades. El padre de familia ve exclusivamente las mensualidades
 * de los jugadores vinculados a su cuenta (criterio de aceptación 8).
 *
 * TODO(US-007): reemplazar la lectura directa de "vinculaciones" por el repositorio del módulo
 * de vinculación padre-jugador cuando esté disponible; por ahora se consulta la colección
 * directamente para no bloquear el avance de este módulo.
 */
@HiltViewModel
class PadreMensualidadesViewModel @Inject constructor(
    private val repository: MensualidadRepository,
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
) : BaseViewModel<PadreMensualidadesUiState, Unit>(PadreMensualidadesUiState()) {

    init {
        cargar()
    }

    private fun cargar() {
        val padreUid = firebaseAuth.currentUser?.uid
        if (padreUid == null) {
            setState { copy(isLoading = false, sinJugadoresVinculados = true) }
            return
        }
        viewModelScope.launch {
            val jugadorIds = runCatching {
                firestore.collection("vinculaciones")
                    .whereEqualTo("padreUid", padreUid)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { it.getString("jugadorId") }
            }.getOrDefault(emptyList())

            if (jugadorIds.isEmpty()) {
                setState { copy(isLoading = false, sinJugadoresVinculados = true) }
                return@launch
            }

            repository.observarMensualidadesDeJugadores(jugadorIds).collectLatest { lista ->
                val hoy = LocalDate.now()
                val resueltas = lista.map { m ->
                    val diaCorte = 10.coerceAtMost(YearMonth.of(m.anio, m.mes).lengthOfMonth())
                    val corte = LocalDate.of(m.anio, m.mes, diaCorte)
                    MensualidadConEstadoEfectivo(m, EstadoMensualidad.calcular(m.estadoRegistrado, corte, hoy))
                }
                setState { copy(mensualidades = resueltas, isLoading = false, sinJugadoresVinculados = false) }
            }
        }
    }
}
