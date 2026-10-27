package com.esan.sportpro.ui.entrenamientos

import com.esan.sportpro.domain.jugadores.ReglasApoderado
import androidx.lifecycle.viewModelScope
import com.esan.sportpro.core.red.ConectividadObserver
import com.esan.sportpro.core.viewmodel.BaseViewModel
import com.esan.sportpro.data.partido.describirErrorFirestore
import com.esan.sportpro.domain.cuentas.PerfilActual
import com.esan.sportpro.domain.cuentas.SesionRepository
import com.esan.sportpro.domain.entrenamientos.CampoMarca
import com.esan.sportpro.domain.entrenamientos.Entrenamiento
import com.esan.sportpro.domain.entrenamientos.EntrenamientoRepository
import com.esan.sportpro.domain.entrenamientos.EstadoAsistencia
import com.esan.sportpro.domain.entrenamientos.MarcaAsistencia
import com.esan.sportpro.domain.entrenamientos.MarcaJugador
import com.esan.sportpro.domain.entrenamientos.RegistroHistorialAsistencia
import com.esan.sportpro.domain.entrenamientos.ReglasAsistencia
import com.esan.sportpro.domain.jugadores.Jugador
import com.esan.sportpro.domain.jugadores.JugadorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/** Indicador de sincronización (US-011, criterios 6 y 7). */
sealed interface IndicadorSync {
    data object SinConexion : IndicadorSync
    data object Sincronizando : IndicadorSync
    data class Sincronizado(val hora: LocalTime) : IndicadorSync

    val texto: String
        get() = when (this) {
            SinConexion -> "Sin conexión. Los cambios se sincronizarán automáticamente"
            Sincronizando -> "Sincronizando cambios…"
            is Sincronizado -> "Sincronizado a las ${hora.format(FORMATO_HORA)}"
        }

    companion object {
        private val FORMATO_HORA: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}

/** Diálogo para los estados que piden un dato adicional: Tarde (minutos) y Falta justificada (motivo). */
data class DialogoMarca(
    val jugadorId: String,
    val jugadorNombre: String,
    val estado: EstadoAsistencia,
    val minutos: String = "",
    val motivo: String = "",
    val errores: Map<CampoMarca, String> = emptyMap(),
)

/** Acción que espera confirmación. */
sealed interface ConfirmacionAsistencia {
    /** Marcar todos como presentes reemplazaría [reemplazadas] marcas distintas de Presente. */
    data class TodosPresentes(val reemplazadas: Int) : ConfirmacionAsistencia
    data class CerrarSesion(val porcentaje: Double) : ConfirmacionAsistencia
}

data class AsistenciaUiState(
    val perfil: PerfilActual? = null,
    val entrenamiento: Entrenamiento? = null,
    val jugadores: List<Jugador> = emptyList(),
    val historial: List<RegistroHistorialAsistencia> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null,
    val conectado: Boolean = true,
    val cambiosPendientes: Boolean = false,
    val ultimaSincronizacion: LocalTime? = null,
    val dialogo: DialogoMarca? = null,
    val confirmacion: ConfirmacionAsistencia? = null,
    val mostrarHistorial: Boolean = false,
    /** Se activa al intentar cerrar con jugadores sin marcar, para resaltarlos. */
    val resaltarSinMarcar: Boolean = false,
    val cerrando: Boolean = false,
) {
    /**
     * Jugadores activos del equipo, más los que ya tienen una marca aunque se hayan desactivado
     * después (para no perder su registro). Ordenados por dorsal y luego por apellido.
     */
    val plantel: List<Jugador>
        get() {
            val sesion = entrenamiento ?: return emptyList()
            return jugadores
                // Un menor pendiente de apoderado todavía no está registrado (ReglasApoderado).
                .filter { (ReglasApoderado.participa(it) && it.equipoId == sesion.equipoId) || it.id in sesion.asistencia }
                .sortedWith(compareBy<Jugador>({ it.dorsal ?: Int.MAX_VALUE }, { it.apellidos.lowercase() }))
        }

    val plantelIds: List<String> get() = plantel.map { it.id }

    /** Marcar es posible el día de la sesión o después, mientras no esté Realizada ni Cancelada. */
    val editable: Boolean
        get() = perfil?.esStaff == true && entrenamiento?.let { ReglasAsistencia.puedeTomarAsistencia(it) } == true

    val sinMarcar: List<String>
        get() = ReglasAsistencia.sinMarcar(plantelIds, entrenamiento?.asistencia.orEmpty())

    val marcados: Int get() = plantelIds.size - sinMarcar.size

    val porcentaje: Double
        get() = entrenamiento?.porcentajeAsistencia
            ?: ReglasAsistencia.porcentajeEquipo(plantelIds, entrenamiento?.asistencia.orEmpty())

    val indicador: IndicadorSync?
        get() = when {
            !conectado -> IndicadorSync.SinConexion
            cambiosPendientes -> IndicadorSync.Sincronizando
            ultimaSincronizacion != null -> IndicadorSync.Sincronizado(ultimaSincronizacion)
            else -> null
        }

    fun estadoDe(jugadorId: String): EstadoAsistencia? = EstadoAsistencia.desde(entrenamiento?.asistencia?.get(jugadorId))

    fun detalleDe(jugadorId: String): MarcaAsistencia? = entrenamiento?.asistenciaDetalle?.get(jugadorId)

    val historialOrdenado: List<RegistroHistorialAsistencia>
        get() = ReglasAsistencia.ordenarHistorial(historial)

    val conConflicto: Set<String> get() = ReglasAsistencia.jugadoresConConflicto(historial)
}

sealed interface AsistenciaEvent {
    data class Mensaje(val texto: String) : AsistenciaEvent
}

/** US-011: registro de asistencia de una sesión, con guardado inmediato y soporte sin conexión. */
@HiltViewModel
class AsistenciaViewModel @Inject constructor(
    private val sesionRepository: SesionRepository,
    private val entrenamientoRepository: EntrenamientoRepository,
    private val jugadorRepository: JugadorRepository,
    private val conectividad: ConectividadObserver,
) : BaseViewModel<AsistenciaUiState, AsistenciaEvent>(AsistenciaUiState()) {

    private var entrenamientoId: String? = null

    /** Se llama al abrir la pantalla; es idempotente para sobrevivir a recomposiciones. */
    fun iniciar(id: String) {
        if (entrenamientoId == id) return
        entrenamientoId = id
        viewModelScope.launch {
            val perfil = sesionRepository.perfilActual()
            if (perfil == null) {
                setState { copy(cargando = false, error = "No se pudo leer tu perfil. Vuelve a iniciar sesión.") }
                return@launch
            }
            if (!perfil.esStaff) {
                setState { copy(cargando = false, error = "Solo el entrenador o el administrador registran la asistencia.") }
                return@launch
            }
            setState { copy(perfil = perfil) }

            launch {
                entrenamientoRepository.observarSesion(id)
                    .catch { setState { copy(cargando = false, error = describirErrorFirestore(it)) } }
                    .collect { observada ->
                        setState {
                            // Criterio 7: cuando las escrituras locales quedan confirmadas por el
                            // servidor, se muestra "Sincronizado" con la hora.
                            val recienSincronizado = cambiosPendientes && !observada.cambiosPendientes && !observada.desdeCache
                            copy(
                                entrenamiento = observada.entrenamiento,
                                cambiosPendientes = observada.cambiosPendientes,
                                ultimaSincronizacion = if (recienSincronizado) LocalTime.now() else ultimaSincronizacion,
                                cargando = false,
                                error = if (observada.entrenamiento == null) "No se encontró el entrenamiento." else null,
                            )
                        }
                    }
            }
            launch {
                jugadorRepository.observarJugadores(perfil.academiaId)
                    .catch { setState { copy(error = describirErrorFirestore(it)) } }
                    .collect { lista -> setState { copy(jugadores = lista) } }
            }
            launch {
                entrenamientoRepository.observarHistorialAsistencia(id)
                    .catch { /* el historial es informativo */ }
                    .collect { lista -> setState { copy(historial = lista) } }
            }
            launch {
                conectividad.conectado.collect { conectado -> setState { copy(conectado = conectado) } }
            }
        }
    }

    // ---- Marcar ----------------------------------------------------------------------------

    fun onEstado(jugador: Jugador, estado: EstadoAsistencia) {
        if (!currentState.editable) return
        if (ReglasAsistencia.requiereDetalle(estado)) {
            val actual = currentState.detalleDe(jugador.id)?.takeIf { it.estado == estado.name }
            setState {
                copy(
                    dialogo = DialogoMarca(
                        jugadorId = jugador.id,
                        jugadorNombre = jugador.nombreCompleto,
                        estado = estado,
                        minutos = actual?.minutosRetraso?.toString().orEmpty(),
                        motivo = actual?.motivo.orEmpty(),
                    ),
                )
            }
        } else if (currentState.estadoDe(jugador.id) != estado) {
            guardar(listOf(marcaDe(jugador.id, jugador.nombreCompleto, estado)))
        }
    }

    fun onDialogoChanged(cambio: DialogoMarca.() -> DialogoMarca) = setState { copy(dialogo = dialogo?.cambio()) }

    fun cerrarDialogo() = setState { copy(dialogo = null) }

    fun confirmarDialogo() {
        val dialogo = currentState.dialogo ?: return
        val errores = ReglasAsistencia.validarMarca(dialogo.estado, dialogo.minutos, dialogo.motivo)
        if (errores.isNotEmpty()) {
            setState { copy(dialogo = dialogo.copy(errores = errores)) }
            return
        }
        setState { copy(dialogo = null) }
        guardar(listOf(marcaDe(dialogo.jugadorId, dialogo.jugadorNombre, dialogo.estado, dialogo.minutos, dialogo.motivo)))
    }

    /** Criterio 5: registro rápido. Si reemplaza marcas distintas de Presente, pide confirmación. */
    fun marcarTodosPresentes() {
        val estado = currentState
        if (!estado.editable) return
        val reemplazadas = estado.plantelIds.count {
            val actual = estado.estadoDe(it)
            actual != null && actual != EstadoAsistencia.PRESENTE
        }
        if (reemplazadas > 0) {
            setState { copy(confirmacion = ConfirmacionAsistencia.TodosPresentes(reemplazadas)) }
        } else {
            aplicarTodosPresentes()
        }
    }

    private fun aplicarTodosPresentes() {
        val estado = currentState
        val marcas = estado.plantel
            .filter { estado.estadoDe(it.id) != EstadoAsistencia.PRESENTE }
            .map { marcaDe(it.id, it.nombreCompleto, EstadoAsistencia.PRESENTE) }
        guardar(marcas)
    }

    private fun marcaDe(
        jugadorId: String,
        nombre: String,
        estado: EstadoAsistencia,
        minutos: String = "",
        motivo: String = "",
    ): MarcaJugador {
        val perfil = currentState.perfil
        return MarcaJugador(
            jugadorId = jugadorId,
            jugadorNombre = nombre,
            marca = ReglasAsistencia.crearMarca(estado, minutos, motivo, perfil?.uid.orEmpty(), perfil?.nombre.orEmpty()),
        )
    }

    /**
     * Guarda sin bloquear la pantalla: Firestore aplica el cambio en la caché local al instante (la
     * lista se actualiza sola por el listener) y lo envía al servidor cuando hay conexión.
     */
    private fun guardar(marcas: List<MarcaJugador>) {
        val id = entrenamientoId ?: return
        if (marcas.isEmpty()) return
        viewModelScope.launch {
            try {
                val confirmado = entrenamientoRepository.marcarAsistencia(id, marcas)
                if (confirmado && currentState.conectado) setState { copy(ultimaSincronizacion = LocalTime.now()) }
            } catch (e: Exception) {
                sendEvent(AsistenciaEvent.Mensaje(describirErrorFirestore(e)))
            }
        }
    }

    // ---- Cerrar la sesión --------------------------------------------------------------------

    /** Criterios 2 y 9: no se cierra con jugadores sin marcar; al cerrar pasa a Realizada. */
    fun solicitarCierre() {
        val estado = currentState
        if (!estado.editable) return
        if (estado.plantelIds.isEmpty()) {
            sendEvent(AsistenciaEvent.Mensaje("El equipo no tiene jugadores activos."))
            return
        }
        val pendientes = estado.sinMarcar.size
        if (pendientes > 0) {
            setState { copy(resaltarSinMarcar = true) }
            sendEvent(AsistenciaEvent.Mensaje(ReglasAsistencia.mensajeSinMarcar(pendientes)))
            return
        }
        val porcentaje = ReglasAsistencia.porcentajeEquipo(estado.plantelIds, estado.entrenamiento?.asistencia.orEmpty())
        setState { copy(confirmacion = ConfirmacionAsistencia.CerrarSesion(porcentaje)) }
    }

    fun cancelarConfirmacion() = setState { copy(confirmacion = null) }

    fun confirmar() {
        when (val confirmacion = currentState.confirmacion) {
            is ConfirmacionAsistencia.TodosPresentes -> {
                setState { copy(confirmacion = null) }
                aplicarTodosPresentes()
            }
            is ConfirmacionAsistencia.CerrarSesion -> cerrar(confirmacion.porcentaje)
            null -> Unit
        }
    }

    private fun cerrar(porcentaje: Double) {
        val estado = currentState
        val entrenamiento = estado.entrenamiento ?: return
        if (estado.cerrando) return
        // Se vuelve a comprobar por si otro usuario cambió algo mientras el diálogo estaba abierto.
        if (estado.sinMarcar.isNotEmpty()) {
            setState { copy(confirmacion = null, resaltarSinMarcar = true) }
            sendEvent(AsistenciaEvent.Mensaje(ReglasAsistencia.mensajeSinMarcar(estado.sinMarcar.size)))
            return
        }
        setState { copy(cerrando = true) }
        viewModelScope.launch {
            try {
                val confirmado = entrenamientoRepository.cerrarSesion(entrenamiento, estado.plantelIds, porcentaje)
                setState { copy(cerrando = false, confirmacion = null) }
                val texto = "Sesión realizada. Asistencia del equipo: ${ReglasAsistencia.formatearPorcentaje(porcentaje)}"
                sendEvent(AsistenciaEvent.Mensaje(if (confirmado) texto else "$texto. Se sincronizará cuando vuelva la conexión."))
            } catch (e: Exception) {
                setState { copy(cerrando = false, confirmacion = null) }
                sendEvent(AsistenciaEvent.Mensaje(describirErrorFirestore(e)))
            }
        }
    }

    // ---- Historial ----------------------------------------------------------------------------

    fun abrirHistorial() = setState { copy(mostrarHistorial = true) }

    fun cerrarHistorial() = setState { copy(mostrarHistorial = false) }
}
