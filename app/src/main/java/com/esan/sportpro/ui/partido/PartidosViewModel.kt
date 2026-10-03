package com.esan.sportpro.ui.partido

import androidx.lifecycle.viewModelScope
import com.esan.sportpro.core.viewmodel.BaseViewModel
import com.esan.sportpro.data.partido.PartidoEnCursoStore
import com.esan.sportpro.data.partido.describirErrorFirestore
import com.esan.sportpro.domain.partido.Partido
import com.esan.sportpro.domain.partido.PartidoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PartidosUiState(
    val cargando: Boolean = true,
    val partidos: List<Partido> = emptyList(),
    val mostrarCrear: Boolean = false,
    /** Partido que la pantalla debe abrir (restauración o recién creado). */
    val abrirPartidoId: String? = null,
    /** Error al leer de Firestore (permisos, base inexistente, etc.). */
    val error: String? = null,
    /** Aviso cuando el servidor tarda en responder. */
    val aviso: String? = null,
    val mensaje: String? = null,
)

@HiltViewModel
class PartidosViewModel @Inject constructor(
    private val repo: PartidoRepository,
    private val partidoEnCurso: PartidoEnCursoStore,
) : BaseViewModel<PartidosUiState, Unit>(PartidosUiState()) {

    private var restauracionRevisada = false
    private var escucha: Job? = null

    init {
        escuchar()
        viewModelScope.launch {
            repo.erroresEscritura.collect { setState { copy(mensaje = it) } }
        }
    }

    /** (Re)inicia la escucha en tiempo real de los partidos. */
    fun escuchar() {
        escucha?.cancel()
        setState { copy(cargando = true, error = null, aviso = null) }
        escucha = viewModelScope.launch {
            launch {
                delay(TIEMPO_ESPERA_MS)
                if (currentState.cargando) {
                    setState {
                        copy(
                            cargando = false,
                            aviso = "Firestore no responde todavía. Revisa tu conexión; " +
                                "los partidos aparecerán apenas lleguen.",
                        )
                    }
                }
            }
            repo.observarPartidos()
                .catch { e -> setState { copy(cargando = false, error = describirErrorFirestore(e)) } }
                .collect { lista -> setState { copy(partidos = lista, cargando = false, aviso = null, error = null) } }
        }
    }

    /**
     * Si la app se cerró en medio de un partido, se vuelve a abrir directamente su registro.
     * Se hace una sola vez por pantalla para no impedir que el usuario regrese a la lista.
     */
    fun revisarPartidoEnCurso() {
        if (restauracionRevisada) return
        restauracionRevisada = true
        partidoEnCurso.partidoId?.let { id -> setState { copy(abrirPartidoId = id) } }
    }

    fun mostrarCrear(mostrar: Boolean) = setState { copy(mostrarCrear = mostrar) }

    fun crearPartidoPractica(nombreEquipo: String, nombreRival: String, cambiosPermitidos: Int, duracionTiempoMin: Int) {
        if (repo.uidActual == null) {
            setState { copy(mensaje = "Inicia sesión para crear un partido") }
            return
        }
        val id = repo.crearPartidoPractica(
            nombreEquipo = nombreEquipo.trim().ifBlank { "Mi equipo" },
            nombreRival = nombreRival.trim().ifBlank { "Rival" },
            cambiosPermitidos = cambiosPermitidos.coerceIn(1, 15),
            duracionTiempoMin = duracionTiempoMin.coerceIn(10, 45),
        )
        setState { copy(mostrarCrear = false, abrirPartidoId = id) }
    }

    fun navegacionRealizada() = setState { copy(abrirPartidoId = null) }

    fun mensajeMostrado() = setState { copy(mensaje = null) }

    private companion object {
        const val TIEMPO_ESPERA_MS = 8_000L
    }
}
