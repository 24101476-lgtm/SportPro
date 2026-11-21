package com.esan.sportpro.ui.academia

import androidx.lifecycle.viewModelScope
import com.esan.sportpro.core.viewmodel.BaseViewModel
import com.esan.sportpro.data.partido.describirErrorFirestore
import com.esan.sportpro.domain.academia.CampoEquipo
import com.esan.sportpro.domain.academia.EntrenadorCuenta
import com.esan.sportpro.domain.academia.Equipo
import com.esan.sportpro.domain.academia.EquipoRepository
import com.esan.sportpro.domain.academia.ValidacionEquipo
import com.esan.sportpro.domain.cuentas.PerfilActual
import com.esan.sportpro.domain.cuentas.SesionRepository
import com.esan.sportpro.navigation.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Formulario de alta/edición de equipo. [id] es `null` al crear. */
data class FormularioEquipo(
    val id: String? = null,
    val nombre: String = "",
    val categoria: String = "",
    val entrenador: String = "",
    /** Cuenta del entrenador elegido; vacía = sin asignar (o equipo antiguo solo con nombre). */
    val entrenadorUid: String = "",
    val errores: Map<CampoEquipo, String> = emptyMap(),
    val guardando: Boolean = false,
    /** Escudo ya guardado (al editar). */
    val escudoActual: String = "",
    /** URI local de un escudo nuevo (galería), todavía sin subir. */
    val escudoNuevo: String? = null,
) {
    val escudoVisible: String? get() = escudoNuevo ?: escudoActual.takeIf { it.isNotBlank() }
}

data class AcademiaUiState(
    val perfil: PerfilActual? = null,
    val equipos: List<Equipo> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null,
    val mostrarInactivos: Boolean = false,
    val formulario: FormularioEquipo? = null,
    /** Cuentas con rol Entrenador de la academia, para asignar a un equipo. */
    val entrenadores: List<EntrenadorCuenta> = emptyList(),
    /** Cuentas de entrenador que esperan aprobación (solo las ve el administrador). */
    val entrenadoresPendientes: List<EntrenadorCuenta> = emptyList(),
    /** US-004: nombre de la academia y día de corte de las mensualidades. */
    val configuracion: com.esan.sportpro.domain.academia.ConfiguracionAcademia =
        com.esan.sportpro.domain.academia.ConfiguracionAcademia(),
    /** US-004: códigos de invitación para cuentas de administrador. */
    val codigos: List<com.esan.sportpro.domain.academia.CodigoInvitacion> = emptyList(),
    val editandoConfiguracion: FormularioConfiguracion? = null,
) {
    /** Solo el administrador crea y edita equipos; el entrenador los consulta. */
    val puedeEditar: Boolean get() = perfil?.rol == UserRole.ADMINISTRADOR

    val equiposVisibles: List<Equipo>
        get() = if (mostrarInactivos) equipos else equipos.filter { it.activo }
}

/** Edición del nombre y el día de corte de la academia. */
data class FormularioConfiguracion(
    val nombre: String,
    val diaCorteTexto: String,
    val error: String? = null,
    val guardando: Boolean = false,
)

sealed interface AcademiaEvent {
    data class Mensaje(val texto: String) : AcademiaEvent
}

/** US-004/US-005: gestión de equipos de la academia por categoría. */
@HiltViewModel
class AcademiaViewModel @Inject constructor(
    private val sesionRepository: SesionRepository,
    private val equipoRepository: EquipoRepository,
    private val usuarioRepository: com.esan.sportpro.domain.cuentas.UsuarioRepository,
    private val academiaRepository: com.esan.sportpro.domain.academia.AcademiaRepository,
) : BaseViewModel<AcademiaUiState, AcademiaEvent>(AcademiaUiState()) {

    init {
        viewModelScope.launch {
            val perfil = sesionRepository.perfilActual()
            if (perfil == null) {
                setState { copy(cargando = false, error = "No se pudo leer tu perfil. Vuelve a iniciar sesión.") }
                return@launch
            }
            setState { copy(perfil = perfil) }
            launch {
                academiaRepository.observarConfiguracion(perfil.academiaId)
                    .collect { setState { copy(configuracion = it) } }
            }
            if (perfil.rol == UserRole.ADMINISTRADOR) {
                launch {
                    academiaRepository.observarCodigos(perfil.academiaId)
                        .catch { }
                        .collect { setState { copy(codigos = it) } }
                }
                launch {
                    equipoRepository.observarEntrenadores(perfil.academiaId)
                        .catch { }
                        .collect { setState { copy(entrenadores = it) } }
                }
                launch {
                    usuarioRepository.observarEntrenadoresPendientes(perfil.academiaId)
                        .catch { }
                        .collect { setState { copy(entrenadoresPendientes = it) } }
                }
            }
            equipoRepository.observarEquipos(perfil.academiaId)
                .catch { setState { copy(cargando = false, error = describirErrorFirestore(it)) } }
                .collect { lista -> setState { copy(equipos = lista, cargando = false, error = null) } }
        }
    }

    fun onMostrarInactivos(valor: Boolean) = setState { copy(mostrarInactivos = valor) }

    fun nuevoEquipo() {
        if (!currentState.puedeEditar) return
        setState { copy(formulario = FormularioEquipo()) }
    }

    fun editarEquipo(equipo: Equipo) {
        if (!currentState.puedeEditar) return
        setState {
            copy(
                formulario = FormularioEquipo(
                    id = equipo.id,
                    nombre = equipo.nombre,
                    categoria = equipo.categoria,
                    entrenador = equipo.entrenadorNombre,
                    // Equipos antiguos: si el nombre coincide con una cuenta, se propone esa cuenta.
                    entrenadorUid = equipo.entrenadorUid.ifBlank {
                        currentState.entrenadores.firstOrNull { equipo.copy(entrenadorUid = "").dirigidoPor(it.uid, it.nombre) }?.uid.orEmpty()
                    },
                    escudoActual = equipo.escudoUrl,
                ),
            )
        }
    }

    fun onEscudoElegido(uri: String) = editarFormulario { copy(escudoNuevo = uri) }

    fun quitarEscudoNuevo() = editarFormulario { copy(escudoNuevo = null) }

    fun cerrarFormulario() = setState { copy(formulario = null) }

    fun onNombreChanged(valor: String) =
        editarFormulario { copy(nombre = valor, errores = errores - CampoEquipo.NOMBRE) }

    fun onCategoriaChanged(valor: String) =
        editarFormulario { copy(categoria = valor, errores = errores - CampoEquipo.CATEGORIA) }

    fun onEntrenadorChanged(valor: String) =
        editarFormulario { copy(entrenador = valor, errores = errores - CampoEquipo.ENTRENADOR) }

    /** Asigna una cuenta de entrenador (o ninguna con `null`). */
    fun onEntrenadorElegido(cuenta: EntrenadorCuenta?) = editarFormulario {
        copy(
            entrenadorUid = cuenta?.uid.orEmpty(),
            entrenador = cuenta?.nombre.orEmpty(),
            errores = errores - CampoEquipo.ENTRENADOR,
        )
    }

    fun guardar() {
        val estado = currentState
        val formulario = estado.formulario ?: return
        val perfil = estado.perfil ?: return
        if (!estado.puedeEditar || formulario.guardando) return

        val errores = ValidacionEquipo.validar(
            nombre = formulario.nombre,
            categoria = formulario.categoria,
            entrenadorNombre = formulario.entrenador,
            existentes = estado.equipos,
            idEditando = formulario.id,
        )
        if (errores.isNotEmpty()) {
            editarFormulario { copy(errores = errores) }
            return
        }

        editarFormulario { copy(guardando = true) }
        viewModelScope.launch {
            try {
                val esNuevo = formulario.id == null
                // Para un equipo nuevo con escudo se reserva el id antes, así se sube justo después.
                val id = formulario.id ?: if (formulario.escudoNuevo != null) equipoRepository.nuevoId() else ""
                val confirmado = equipoRepository.guardar(
                    Equipo(
                        id = id,
                        academiaId = perfil.academiaId,
                        nombre = formulario.nombre,
                        categoria = formulario.categoria,
                        entrenadorNombre = formulario.entrenador,
                        entrenadorUid = formulario.entrenadorUid,
                        creadoPor = perfil.uid,
                    ),
                    esNuevo = esNuevo,
                )
                // El escudo se sube después de guardar el equipo. Si falla, el equipo queda sin escudo.
                val escudoFallido = formulario.escudoNuevo?.let { uri ->
                    id.isBlank() || equipoRepository.subirEscudo(id, uri) == null
                } ?: false
                setState { copy(formulario = null) }
                val texto = buildString {
                    append(mensajeGuardado(confirmado, "Equipo guardado"))
                    if (escudoFallido) append(". No se pudo subir el escudo; inténtalo de nuevo más tarde")
                }
                sendEvent(AcademiaEvent.Mensaje(texto))
            } catch (e: Exception) {
                editarFormulario { copy(guardando = false) }
                sendEvent(AcademiaEvent.Mensaje(describirErrorFirestore(e)))
            }
        }
    }

    // ---- US-004: configuración y códigos de invitación ---------------------------------------

    fun editarConfiguracion() {
        if (!currentState.puedeEditar) return
        val c = currentState.configuracion
        setState { copy(editandoConfiguracion = FormularioConfiguracion(c.nombre, c.diaCorte.toString())) }
    }

    fun cerrarConfiguracion() = setState { copy(editandoConfiguracion = null) }

    fun onConfiguracion(nombre: String, diaCorteTexto: String) = setState {
        copy(
            editandoConfiguracion = editandoConfiguracion?.copy(
                nombre = nombre,
                diaCorteTexto = diaCorteTexto.filter(Char::isDigit).take(2),
                error = null,
            ),
        )
    }

    fun guardarConfiguracion() {
        val formulario = currentState.editandoConfiguracion ?: return
        val perfil = currentState.perfil ?: return
        val dia = formulario.diaCorteTexto.toIntOrNull()
        com.esan.sportpro.domain.academia.ConfiguracionAcademia.validar(formulario.nombre, dia)?.let { error ->
            setState { copy(editandoConfiguracion = formulario.copy(error = error)) }
            return
        }
        setState { copy(editandoConfiguracion = formulario.copy(guardando = true)) }
        viewModelScope.launch {
            try {
                val confirmado = academiaRepository.guardarConfiguracion(
                    com.esan.sportpro.domain.academia.ConfiguracionAcademia(perfil.academiaId, formulario.nombre.trim(), dia!!),
                )
                setState { copy(editandoConfiguracion = null) }
                sendEvent(AcademiaEvent.Mensaje(mensajeGuardado(confirmado, "Configuración guardada")))
            } catch (e: Exception) {
                setState { copy(editandoConfiguracion = formulario.copy(guardando = false)) }
                sendEvent(AcademiaEvent.Mensaje(describirErrorFirestore(e)))
            }
        }
    }

    fun generarCodigo() {
        val perfil = currentState.perfil ?: return
        if (!currentState.puedeEditar) return
        viewModelScope.launch {
            try {
                val codigo = academiaRepository.generarCodigo(perfil.academiaId, perfil.uid)
                sendEvent(AcademiaEvent.Mensaje("Código $codigo generado: vale 7 días y un solo uso"))
            } catch (e: Exception) {
                sendEvent(AcademiaEvent.Mensaje(describirErrorFirestore(e)))
            }
        }
    }

    fun revocarCodigo(codigo: String) {
        if (!currentState.puedeEditar) return
        viewModelScope.launch {
            try {
                val confirmado = academiaRepository.revocarCodigo(codigo)
                sendEvent(AcademiaEvent.Mensaje(mensajeGuardado(confirmado, "Código revocado")))
            } catch (e: Exception) {
                sendEvent(AcademiaEvent.Mensaje(describirErrorFirestore(e)))
            }
        }
    }

    /** Aprueba (ACTIVO) o rechaza (DESACTIVADO) una cuenta de entrenador pendiente. */
    fun resolverEntrenador(cuenta: EntrenadorCuenta, aprobar: Boolean) {
        if (!currentState.puedeEditar) return
        viewModelScope.launch {
            try {
                val confirmado = usuarioRepository.resolverEntrenador(cuenta.uid, aprobar)
                val texto = if (aprobar) "Entrenador aprobado" else "Cuenta de entrenador rechazada"
                sendEvent(AcademiaEvent.Mensaje(mensajeGuardado(confirmado, texto)))
            } catch (e: Exception) {
                sendEvent(AcademiaEvent.Mensaje(describirErrorFirestore(e)))
            }
        }
    }

    fun cambiarEstado(equipo: Equipo) {
        if (!currentState.puedeEditar) return
        viewModelScope.launch {
            try {
                val confirmado = equipoRepository.cambiarEstado(equipo.id, !equipo.activo)
                val texto = if (equipo.activo) "Equipo desactivado" else "Equipo activado"
                sendEvent(AcademiaEvent.Mensaje(mensajeGuardado(confirmado, texto)))
            } catch (e: Exception) {
                sendEvent(AcademiaEvent.Mensaje(describirErrorFirestore(e)))
            }
        }
    }

    private fun editarFormulario(cambio: FormularioEquipo.() -> FormularioEquipo) =
        setState { copy(formulario = formulario?.cambio()) }

    private fun mensajeGuardado(confirmado: Boolean, texto: String) =
        if (confirmado) texto else "$texto. Se sincronizará cuando vuelva la conexión."
}
