package com.esan.sportpro.ui.partido

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.esan.sportpro.core.red.ConectividadObserver
import com.esan.sportpro.core.viewmodel.BaseViewModel
import com.esan.sportpro.data.partido.PartidoEnCursoStore
import com.esan.sportpro.data.partido.describirErrorFirestore
import com.esan.sportpro.domain.partido.BorradorEvento
import com.esan.sportpro.domain.partido.CatalogoBase
import com.esan.sportpro.domain.partido.Campos
import com.esan.sportpro.domain.partido.EquipoEvento
import com.esan.sportpro.domain.partido.EstadoPartido
import com.esan.sportpro.domain.partido.EventoPartido
import com.esan.sportpro.domain.partido.JugadorConvocado
import com.esan.sportpro.domain.partido.JugadorEspecial
import com.esan.sportpro.domain.partido.Partido
import com.esan.sportpro.domain.partido.PartidoRepository
import com.esan.sportpro.domain.partido.ReglasPartido
import com.esan.sportpro.domain.partido.ResultadoPenal
import com.esan.sportpro.domain.partido.TipoEvento
import com.esan.sportpro.domain.partido.TiposEvento
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Datos editables de la hoja inferior de registro / corrección. */
data class HojaEvento(
    val tipo: TipoEvento,
    val editando: EventoPartido? = null,
    val minutoTexto: String,
    val equipo: EquipoEvento?,
    val jugadorId: String? = null,
    val jugadorNombre: String? = null,
    val secundarioId: String? = null,
    val secundarioNombre: String? = null,
    val resultado: ResultadoPenal? = null,
    val observacion: String = "",
    val motivo: String = "",
    val error: String? = null,
)

sealed interface DialogoPartido {
    data class ExcesoCambios(val borrador: BorradorEvento) : DialogoPartido
    data class FaltaFinPrimerTiempo(val borrador: BorradorEvento) : DialogoPartido
    data class Anular(val evento: EventoPartido, val motivo: String = "", val error: String? = null) : DialogoPartido
    data class Historial(val grupoId: String) : DialogoPartido
    data class CerrarActa(val incompletos: Int) : DialogoPartido
    data object Duplicados : DialogoPartido
    data object Pendientes : DialogoPartido
}

data class RegistroUiState(
    val cargando: Boolean = true,
    val partido: Partido? = null,
    val eventos: List<EventoPartido> = emptyList(),
    val convocados: List<JugadorConvocado> = emptyList(),
    val catalogo: List<TipoEvento> = CatalogoBase.tipos,
    val conectado: Boolean = true,
    val ahoraMs: Long = System.currentTimeMillis(),
    val rol: String? = null,
    val uid: String? = null,
    val hoja: HojaEvento? = null,
    val dialogo: DialogoPartido? = null,
    /** Error de lectura de Firestore (permisos, base inexistente, etc.). */
    val error: String? = null,
) {
    val cronologia: List<EventoPartido> get() = ReglasPartido.cronologia(eventos)
    val marcador: ReglasPartido.Marcador get() = ReglasPartido.marcador(eventos, catalogo)
    val incompletos: List<EventoPartido> get() = ReglasPartido.incompletos(eventos)
    val pendientesSync: Int get() = ReglasPartido.pendientesDeSincronizar(eventos)
    val duplicados: List<List<EventoPartido>> get() = ReglasPartido.duplicados(eventos)
    val reloj: ReglasPartido.Reloj
        get() = partido?.let { ReglasPartido.reloj(it, ahoraMs) } ?: ReglasPartido.Reloj(0, 0, 0, false, 0f)
    val minutoActual: Int get() = reloj.minuto

    /** Solo el operador asignado, el entrenador responsable y el administrador corrigen o anulan. */
    val puedeCorregir: Boolean
        get() {
            val p = partido ?: return false
            if (p.estado == EstadoPartido.ACTA_CERRADA) return false
            return rol == ROL_ADMIN || (uid != null && uid in listOfNotNull(p.operadorUid, p.entrenadorUid, p.creadoPor))
        }

    /** Tipos que se muestran en la paleta: el inicio se lanza con su propio botón. */
    val paleta: List<TipoEvento>
        get() {
            val p = partido ?: return emptyList()
            if (p.estado != EstadoPartido.EN_CURSO) return emptyList()
            return catalogo.filter { it.id != TiposEvento.INICIO_PARTIDO }
                .filterNot { it.id == TiposEvento.FIN_PRIMER_TIEMPO && (p.enDescanso || p.inicioSegundoTiempoMs != null) }
                .filterNot { it.id == TiposEvento.INICIO_SEGUNDO_TIEMPO && p.inicioSegundoTiempoMs != null }
        }

    companion object {
        const val ROL_ADMIN = "ADMINISTRADOR"
    }
}

sealed interface RegistroEvento {
    data class Mensaje(val texto: String) : RegistroEvento
}

@HiltViewModel
class RegistroPartidoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: PartidoRepository,
    private val conectividad: ConectividadObserver,
    private val partidoEnCurso: PartidoEnCursoStore,
) : BaseViewModel<RegistroUiState, RegistroEvento>(RegistroUiState(uid = repo.uidActual)) {

    val partidoId: String = checkNotNull(savedStateHandle["partidoId"])

    /** Pico de eventos pendientes acumulados mientras no había conexión. */
    private var pendientesSinConexion = 0

    init {
        viewModelScope.launch {
            repo.observarPartido(partidoId)
                .catch { e -> setState { copy(cargando = false, error = describirErrorFirestore(e)) } }
                .collect { p ->
                setState { copy(partido = p, cargando = false, error = null) }
                when (p?.estado) {
                    EstadoPartido.EN_CURSO -> partidoEnCurso.partidoId = partidoId
                    EstadoPartido.FINALIZADO, EstadoPartido.ACTA_CERRADA -> partidoEnCurso.limpiarSi(partidoId)
                    else -> Unit
                }
            }
        }
        viewModelScope.launch {
            repo.observarEventos(partidoId)
                .catch { e -> setState { copy(error = describirErrorFirestore(e)) } }
                .collect { lista ->
                setState { copy(eventos = lista) }
                revisarSincronizacion()
            }
        }
        viewModelScope.launch {
            repo.observarConvocados(partidoId)
                .catch { e -> setState { copy(error = describirErrorFirestore(e)) } }
                .collect { setState { copy(convocados = it) } }
        }
        viewModelScope.launch {
            repo.observarCatalogo().collect { setState { copy(catalogo = it) } }
        }
        viewModelScope.launch {
            repo.observarRolActual()
                .catch { /* sin perfil o sin permisos: se usa solo el uid */ }
                .collect { setState { copy(rol = it) } }
        }
        viewModelScope.launch {
            // Si el servidor rechaza una escritura (p. ej. reglas), Firestore revierte el cambio local.
            repo.erroresEscritura.collect { sendEvent(RegistroEvento.Mensaje(it)) }
        }
        viewModelScope.launch {
            delay(8_000)
            if (currentState.cargando) {
                setState {
                    copy(
                        cargando = false,
                        error = "Firestore no responde. Revisa tu conexión a internet e inténtalo de nuevo.",
                    )
                }
            }
        }
        viewModelScope.launch {
            conectividad.conectado.collect {
                setState { copy(conectado = it) }
                revisarSincronizacion()
            }
        }
        viewModelScope.launch {
            while (isActive) {
                setState { copy(ahoraMs = System.currentTimeMillis()) }
                revisarFinReglamentario()
                delay(1_000)
            }
        }
    }

    private var finAutomaticoEnviado = false

    /**
     * Al cumplirse el tiempo reglamentario del 2.º tiempo (90' por defecto) el partido se
     * finaliza automáticamente. Solo lo hace el dispositivo del operador (o del creador si no hay
     * operador asignado) para no generar eventos duplicados desde varios teléfonos.
     */
    private fun revisarFinReglamentario() {
        val st = currentState
        val p = st.partido ?: return
        if (finAutomaticoEnviado || p.estado != EstadoPartido.EN_CURSO || !st.reloj.tiempoCumplido) return
        val responsable = p.operadorUid ?: p.creadoPor
        if (responsable != null && responsable != st.uid) return
        if (ReglasPartido.existeVigente(TiposEvento.FIN_PARTIDO, st.eventos)) return
        finAutomaticoEnviado = true
        registrar(
            BorradorEvento(
                tipo = tipoPorId(TiposEvento.FIN_PARTIDO),
                minuto = 2 * p.duracionTiempoMin,
                equipo = null,
                jugadorId = null,
                jugadorNombre = null,
                secundarioId = null,
                secundarioNombre = null,
                resultadoPenal = null,
                observacion = "Finalizado automáticamente al cumplirse el tiempo reglamentario",
            ),
            excedeCambios = false,
        )
    }

    private fun revisarSincronizacion() {
        val pendientes = currentState.pendientesSync
        if (!currentState.conectado && pendientes > 0) {
            pendientesSinConexion = maxOf(pendientesSinConexion, pendientes)
        } else if (currentState.conectado && pendientes == 0 && pendientesSinConexion > 0) {
            sendEvent(RegistroEvento.Mensaje("Sincronización completa. $pendientesSinConexion eventos enviados"))
            pendientesSinConexion = 0
        }
    }

    // ------------------------------------------------------------ Hoja de registro

    fun abrirInicioPartido() {
        val p = currentState.partido ?: return
        if (!p.alineacionConfirmada) {
            sendEvent(RegistroEvento.Mensaje("Debes confirmar la alineación antes de iniciar el partido"))
            return
        }
        val tipo = tipoPorId(TiposEvento.INICIO_PARTIDO)
        setState { copy(hoja = HojaEvento(tipo = tipo, minutoTexto = "0", equipo = null)) }
    }

    fun abrirHoja(tipo: TipoEvento) {
        val equipo = if (requiereEquipo(tipo)) EquipoEvento.PROPIO else null
        setState {
            copy(hoja = HojaEvento(tipo = tipo, minutoTexto = minutoActual.toString(), equipo = equipo))
        }
    }

    fun editarEvento(evento: EventoPartido) {
        val tipo = currentState.catalogo.firstOrNull { it.id == evento.tipoId }
            ?: CatalogoBase.porId(evento.tipoId)
            ?: return
        setState {
            copy(
                hoja = HojaEvento(
                    tipo = tipo,
                    editando = evento,
                    minutoTexto = evento.minuto.toString(),
                    equipo = evento.equipo,
                    jugadorId = evento.jugadorId,
                    jugadorNombre = evento.jugadorNombre,
                    secundarioId = evento.jugadorSecundarioId,
                    secundarioNombre = evento.jugadorSecundarioNombre,
                    resultado = evento.resultadoPenal,
                    observacion = evento.observacion.orEmpty(),
                ),
            )
        }
    }

    fun cerrarHoja() = setState { copy(hoja = null) }

    private fun actualizarHoja(cambio: HojaEvento.() -> HojaEvento) =
        setState { copy(hoja = hoja?.cambio()?.copy(error = null)) }

    fun onMinuto(texto: String) = actualizarHoja { copy(minutoTexto = texto.filter { it.isDigit() }.take(3)) }

    fun onEquipo(equipo: EquipoEvento) = actualizarHoja {
        // Al cambiar de equipo se limpia el jugador para no mezclar planteles.
        copy(equipo = equipo, jugadorId = null, jugadorNombre = null, secundarioId = null, secundarioNombre = null)
    }

    fun onJugador(id: String, nombre: String) = actualizarHoja {
        val nuevoEquipo = when {
            id == JugadorEspecial.RIVAL -> EquipoEvento.RIVAL
            !JugadorEspecial.esEspecial(id) -> EquipoEvento.PROPIO
            else -> equipo
        }
        copy(jugadorId = id, jugadorNombre = nombre, equipo = nuevoEquipo)
    }

    fun onSecundario(id: String?, nombre: String?) = actualizarHoja { copy(secundarioId = id, secundarioNombre = nombre) }

    fun onResultado(resultado: ResultadoPenal) = actualizarHoja { copy(resultado = resultado) }

    fun onObservacion(texto: String) = actualizarHoja { copy(observacion = texto.take(300)) }

    fun onMotivo(texto: String) = actualizarHoja { copy(motivo = texto.take(ReglasPartido.MOTIVO_MAX)) }

    fun guardarHoja() {
        val hoja = currentState.hoja ?: return
        val minuto = hoja.minutoTexto.toIntOrNull()
        if (minuto == null || minuto !in ReglasPartido.MINUTO_MIN..ReglasPartido.MINUTO_MAX) {
            setState { copy(hoja = hoja.copy(error = "Minuto inválido")) }
            return
        }
        val borrador = BorradorEvento(
            tipo = hoja.tipo,
            minuto = minuto,
            equipo = hoja.equipo,
            jugadorId = hoja.jugadorId,
            jugadorNombre = hoja.jugadorNombre,
            secundarioId = hoja.secundarioId,
            secundarioNombre = hoja.secundarioNombre,
            resultadoPenal = hoja.resultado,
            observacion = hoja.observacion.trim().ifBlank { null },
        )
        val base = hoja.editando?.let { ed -> currentState.eventos.filter { it.grupoId != ed.grupoId } }
            ?: currentState.eventos
        ReglasPartido.validar(borrador, currentState.convocados, base)?.let { error ->
            setState { copy(hoja = hoja.copy(error = error)) }
            return
        }

        val editando = hoja.editando
        if (editando != null) {
            ReglasPartido.validarMotivo(hoja.motivo)?.let { error ->
                setState { copy(hoja = hoja.copy(error = error)) }
                return
            }
            val campos = ReglasPartido.camposModificados(editando, borrador)
            if (campos.isEmpty()) {
                setState { copy(hoja = hoja.copy(error = "No hay cambios para guardar")) }
                return
            }
            repo.corregirEvento(partidoId, editando, borrador, hoja.motivo, campos, ReglasPartido.esIncompleto(borrador))
            setState { copy(hoja = null) }
            sendEvent(RegistroEvento.Mensaje("Evento corregido. La versión anterior quedó en el historial"))
            return
        }

        setState { copy(hoja = null) }
        when {
            borrador.tipo.id == TiposEvento.INICIO_SEGUNDO_TIEMPO &&
                !ReglasPartido.existeVigente(TiposEvento.FIN_PRIMER_TIEMPO, currentState.eventos) ->
                setState { copy(dialogo = DialogoPartido.FaltaFinPrimerTiempo(borrador)) }

            borrador.tipo.id == TiposEvento.CAMBIO && borrador.equipo == EquipoEvento.PROPIO &&
                ReglasPartido.cambiosRealizados(currentState.eventos) >= (currentState.partido?.cambiosPermitidos ?: Int.MAX_VALUE) ->
                setState { copy(dialogo = DialogoPartido.ExcesoCambios(borrador)) }

            else -> registrar(borrador, excedeCambios = false)
        }
    }

    private fun registrar(borrador: BorradorEvento, excedeCambios: Boolean) {
        val eventos = currentState.eventos
        val maximo = ReglasPartido.minutoMaximo(eventos)
        val fueraDeOrden = maximo != null && borrador.minuto < maximo
        val incompleto = ReglasPartido.esIncompleto(borrador)
        val relacionados = mutableListOf<BorradorEvento>()
        val avisos = mutableListOf<String>()

        // Penal convertido: genera además el gol asociado.
        if (borrador.tipo.id == TiposEvento.PENAL && borrador.resultadoPenal == ResultadoPenal.CONVERTIDO) {
            relacionados += borrador.copy(
                tipo = tipoPorId(TiposEvento.GOL),
                resultadoPenal = null,
                secundarioId = null,
                secundarioNombre = null,
                observacion = "Gol de penal",
            )
        }
        // Segunda amarilla para el mismo jugador: roja automática por doble amonestación.
        val jugadorId = borrador.jugadorId
        if (borrador.tipo.id == TiposEvento.TARJETA_AMARILLA && jugadorId != null &&
            !JugadorEspecial.esEspecial(jugadorId) && ReglasPartido.amarillasDe(jugadorId, eventos) >= 1
        ) {
            relacionados += borrador.copy(
                tipo = tipoPorId(TiposEvento.TARJETA_ROJA),
                observacion = "Tarjeta roja por doble amonestación",
            )
            avisos += "Segunda amarilla: se registró tarjeta roja por doble amonestación"
        }

        repo.registrarEvento(partidoId, borrador, incompleto, fueraDeOrden, excedeCambios, relacionados)

        if (borrador.tipo.id == TiposEvento.INICIO_PARTIDO) partidoEnCurso.partidoId = partidoId
        if (fueraDeOrden) avisos += "Registro fuera de orden"
        if (incompleto) avisos += "Evento guardado como Incompleto"
        if (excedeCambios) avisos += "Superaste el número de cambios permitidos"
        sendEvent(RegistroEvento.Mensaje(avisos.joinToString(" · ").ifBlank { "${borrador.tipo.nombre} registrado" }))
    }

    // ------------------------------------------------------------ Diálogos

    fun cerrarDialogo() = setState { copy(dialogo = null) }

    fun confirmarExcesoCambios() {
        val d = currentState.dialogo as? DialogoPartido.ExcesoCambios ?: return
        setState { copy(dialogo = null) }
        registrar(d.borrador, excedeCambios = true)
    }

    /** Inserta el fin del primer tiempo con el minuto estimado y luego registra el inicio del segundo. */
    fun insertarFinPrimerTiempo() {
        val d = currentState.dialogo as? DialogoPartido.FaltaFinPrimerTiempo ?: return
        setState { copy(dialogo = null) }
        val duracion = currentState.partido?.duracionTiempoMin ?: 45
        val estimado = minOf(duracion, d.borrador.minuto)
        registrar(
            BorradorEvento(
                tipo = tipoPorId(TiposEvento.FIN_PRIMER_TIEMPO),
                minuto = estimado,
                equipo = null,
                jugadorId = null,
                jugadorNombre = null,
                secundarioId = null,
                secundarioNombre = null,
                resultadoPenal = null,
                observacion = "Insertado con minuto estimado",
            ),
            excedeCambios = false,
        )
        registrar(d.borrador, excedeCambios = false)
    }

    fun continuarSinFinPrimerTiempo() {
        val d = currentState.dialogo as? DialogoPartido.FaltaFinPrimerTiempo ?: return
        setState { copy(dialogo = null) }
        registrar(d.borrador, excedeCambios = false)
    }

    fun pedirAnulacion(evento: EventoPartido) = setState { copy(dialogo = DialogoPartido.Anular(evento)) }

    fun onMotivoAnulacion(texto: String) {
        val d = currentState.dialogo as? DialogoPartido.Anular ?: return
        setState { copy(dialogo = d.copy(motivo = texto.take(ReglasPartido.MOTIVO_MAX), error = null)) }
    }

    fun confirmarAnulacion() {
        val d = currentState.dialogo as? DialogoPartido.Anular ?: return
        ReglasPartido.validarMotivo(d.motivo)?.let { error ->
            setState { copy(dialogo = d.copy(error = error)) }
            return
        }
        // También se anulan los eventos generados automáticamente a partir de este.
        val relacionados = ReglasPartido.vigentes(currentState.eventos)
            .filter { it.eventoOrigenId == d.evento.grupoId }
        repo.anularEvento(partidoId, d.evento, relacionados, d.motivo)
        setState { copy(dialogo = null) }
        sendEvent(RegistroEvento.Mensaje("Evento anulado"))
    }

    fun verHistorial(evento: EventoPartido) = setState { copy(dialogo = DialogoPartido.Historial(evento.grupoId)) }

    fun verDuplicados() = setState { copy(dialogo = DialogoPartido.Duplicados) }

    fun verPendientes() = setState { copy(dialogo = DialogoPartido.Pendientes) }

    /** Asigna el jugador faltante de un evento incompleto creando una nueva versión del evento. */
    fun completarPendiente(evento: EventoPartido, jugador: JugadorConvocado) {
        val tipo = currentState.catalogo.firstOrNull { it.id == evento.tipoId }
            ?: CatalogoBase.porId(evento.tipoId)
            ?: return
        val faltaPrincipal = evento.jugadorId == null || evento.jugadorId == JugadorEspecial.NO_IDENTIFICADO
        val borrador = BorradorEvento(
            tipo = tipo,
            minuto = evento.minuto,
            equipo = evento.equipo ?: EquipoEvento.PROPIO,
            jugadorId = if (faltaPrincipal) jugador.id else evento.jugadorId,
            jugadorNombre = if (faltaPrincipal) jugador.nombre else evento.jugadorNombre,
            secundarioId = if (faltaPrincipal) evento.jugadorSecundarioId else jugador.id,
            secundarioNombre = if (faltaPrincipal) evento.jugadorSecundarioNombre else jugador.nombre,
            resultadoPenal = evento.resultadoPenal,
            observacion = evento.observacion,
        )
        val campos = ReglasPartido.camposModificados(evento, borrador)
        repo.corregirEvento(
            partidoId,
            evento,
            borrador,
            "Se completó el jugador faltante",
            campos,
            ReglasPartido.esIncompleto(borrador),
        )
        // Los eventos generados a partir de este (gol de penal, roja) heredan el jugador.
        ReglasPartido.vigentes(currentState.eventos)
            .filter { it.eventoOrigenId == evento.grupoId && it.incompleto }
            .forEach { rel ->
                val tipoRel = CatalogoBase.porId(rel.tipoId) ?: return@forEach
                val b = borrador.copy(tipo = tipoRel, minuto = rel.minuto, resultadoPenal = null, observacion = rel.observacion)
                repo.corregirEvento(
                    partidoId, rel, b, "Se completó el jugador faltante",
                    ReglasPartido.camposModificados(rel, b), ReglasPartido.esIncompleto(b),
                )
            }
        sendEvent(RegistroEvento.Mensaje("Evento completado"))
    }

    fun pedirCierreActa() {
        val p = currentState.partido ?: return
        if (p.estado != EstadoPartido.FINALIZADO) {
            sendEvent(RegistroEvento.Mensaje("Registra el fin del partido antes de cerrar el acta"))
            return
        }
        val n = currentState.incompletos.size
        if (n > 0) {
            setState { copy(dialogo = DialogoPartido.CerrarActa(n)) }
        } else {
            repo.cerrarActa(partidoId, 0)
            sendEvent(RegistroEvento.Mensaje("Acta cerrada"))
        }
    }

    fun confirmarCierreActa() {
        val d = currentState.dialogo as? DialogoPartido.CerrarActa ?: return
        repo.cerrarActa(partidoId, d.incompletos)
        setState { copy(dialogo = null) }
        sendEvent(RegistroEvento.Mensaje("Acta cerrada con ${d.incompletos} eventos incompletos"))
    }

    // ------------------------------------------------------------ Utilidades

    private fun tipoPorId(id: String): TipoEvento =
        currentState.catalogo.firstOrNull { it.id == id } ?: checkNotNull(CatalogoBase.porId(id))

    private fun requiereEquipo(tipo: TipoEvento) =
        tipo.requiere(Campos.EQUIPO) || tipo.requiere(Campos.JUGADOR) || tipo.requiere(Campos.JUGADOR_SALE)
}
