package com.esan.sportpro.ui.registro

import androidx.lifecycle.viewModelScope
import com.esan.sportpro.core.red.ConectividadObserver
import com.esan.sportpro.core.viewmodel.BaseViewModel
import com.esan.sportpro.domain.cuentas.EstadoUsuario
import com.esan.sportpro.domain.cuentas.ResultadoAuth
import com.esan.sportpro.domain.cuentas.Usuario
import com.esan.sportpro.domain.cuentas.UsuarioRepository
import com.esan.sportpro.navigation.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Estado del formulario de registro (US-001). Sobrevive a rotaciones porque vive en el ViewModel. */
data class RegistroUiState(
    val nombres: String = "",
    val apellidos: String = "",
    val correo: String = "",
    val password: String = "",
    val confirmarPassword: String = "",
    val telefono: String = "",
    val fechaNacimiento: LocalDate? = null,
    val rol: UserRole? = null,
    val codigoInvitacion: String = "",
    val correoApoderado: String = "",
    val esMenor: Boolean = false,
    /** Se muestra el campo "Correo del apoderado" cuando el rol es Jugador y es menor de 18. */
    val mostrarCampoApoderado: Boolean = false,
    /** El código de invitación solo se pide para el rol Administrador. */
    val mostrarCampoCodigoInvitacion: Boolean = false,
    val errores: Map<CampoRegistro, String> = emptyMap(),
    val errorGeneral: String? = null,
    val isLoading: Boolean = false,
    val conectado: Boolean = true,
) {
    val puedeEnviar: Boolean get() = !isLoading
}

/** Eventos de un solo disparo: navegación al login y mensajes para el Snackbar. */
sealed interface RegistroEvent {
    /** La cuenta se creó y el correo de verificación se envió: se vuelve al login (US-001). */
    data object CuentaCreada : RegistroEvent

    data class Snackbar(val mensaje: String) : RegistroEvent
}

/**
 * ViewModel del registro con rol (US-001). Reutiliza [BaseViewModel] (StateFlow + eventos) y
 * [ConectividadObserver] para el caso "sin conexión", sin copiar nada de los módulos existentes.
 */
@HiltViewModel
class RegistroUsuarioViewModel @Inject constructor(
    private val usuarioRepository: UsuarioRepository,
    conectividad: ConectividadObserver,
) : BaseViewModel<RegistroUiState, RegistroEvent>(RegistroUiState()) {

    init {
        viewModelScope.launch {
            conectividad.conectado.collect { hayConexion ->
                setState { copy(conectado = hayConexion) }
            }
        }
    }

    // ------------------------------------------------------------------ Cambios de campo

    fun onNombresChanged(valor: String) = limpiar(CampoRegistro.NOMBRES) { copy(nombres = valor) }

    fun onApellidosChanged(valor: String) = limpiar(CampoRegistro.APELLIDOS) { copy(apellidos = valor) }

    fun onCorreoChanged(valor: String) = limpiar(CampoRegistro.CORREO) { copy(correo = valor) }

    fun onPasswordChanged(valor: String) =
        limpiar(CampoRegistro.PASSWORD) { copy(password = valor) }

    fun onConfirmarPasswordChanged(valor: String) =
        limpiar(CampoRegistro.CONFIRMAR_PASSWORD) { copy(confirmarPassword = valor) }

    fun onTelefonoChanged(valor: String) = limpiar(CampoRegistro.TELEFONO) { copy(telefono = valor) }

    fun onCodigoInvitacionChanged(valor: String) =
        limpiar(CampoRegistro.CODIGO_INVITACION) { copy(codigoInvitacion = valor.uppercase()) }

    fun onCorreoApoderadoChanged(valor: String) =
        limpiar(CampoRegistro.CORREO_APODERADO) { copy(correoApoderado = valor) }

    fun onFechaNacimientoChanged(fecha: LocalDate) = setState {
        val menor = ValidacionRegistro.esMenor(fecha)
        copy(
            fechaNacimiento = fecha,
            esMenor = menor,
            mostrarCampoApoderado = rol == UserRole.JUGADOR && menor,
            errores = errores - CampoRegistro.FECHA_NACIMIENTO,
            errorGeneral = null,
        )
    }

    fun onRolChanged(rol: UserRole) = setState {
        val menor = fechaNacimiento?.let { ValidacionRegistro.esMenor(it) } ?: false
        copy(
            rol = rol,
            esMenor = menor,
            mostrarCampoApoderado = rol == UserRole.JUGADOR && menor,
            mostrarCampoCodigoInvitacion = rol == UserRole.ADMINISTRADOR,
            errores = errores - CampoRegistro.ROL,
            errorGeneral = null,
        )
    }

    /** Borra el error del campo apenas se corrige, sin pisar el resto del formulario. */
    private fun limpiar(campo: CampoRegistro, reducer: RegistroUiState.() -> RegistroUiState) {
        setState { reducer().copy(errores = errores - campo, errorGeneral = null) }
    }

    // ------------------------------------------------------------------ Registro

    fun registrar() {
        val formulario = currentState
        if (formulario.isLoading) return

        val errores = validarFormulario(formulario)
        if (errores.isNotEmpty()) {
            setState { copy(errores = errores, errorGeneral = "Revisa los campos marcados") }
            return
        }

        viewModelScope.launch {
            setState { copy(isLoading = true, errores = emptyMap(), errorGeneral = null) }

            // Sin conexión no se intenta nada: se avisa y el formulario conserva lo escrito.
            if (!currentState.conectado) {
                setState { copy(isLoading = false) }
                sendEvent(RegistroEvent.Snackbar(MensajesRegistro.SIN_CONEXION))
                return@launch
            }

            if (usuarioRepository.correoYaRegistrado(formulario.correo)) {
                setState {
                    copy(
                        isLoading = false,
                        errores = mapOf(CampoRegistro.CORREO to MensajesRegistro.CORREO_YA_REGISTRADO),
                    )
                }
                return@launch
            }

            val rol = formulario.rol

            when (val resultado = usuarioRepository.crearCuentaEnAuth(formulario.correo, formulario.password)) {
                is ResultadoAuth.CorreoYaRegistrado -> setState {
                    copy(
                        isLoading = false,
                        errores = mapOf(CampoRegistro.CORREO to MensajesRegistro.CORREO_YA_REGISTRADO),
                    )
                }

                is ResultadoAuth.Error -> {
                    setState { copy(isLoading = false) }
                    sendEvent(RegistroEvent.Snackbar(resultado.mensaje))
                }

                is ResultadoAuth.Exito -> {
                    val uid = resultado.uid

                    // La cuenta de Auth ya existe (el usuario quedó autenticado), así que recién
                    // ahora se puede leer el código de invitación. Si no sirve, la cuenta recién
                    // creada se elimina para no dejar un usuario huérfano.
                    if (rol == UserRole.ADMINISTRADOR) {
                        val codigoValido = runCatching {
                            usuarioRepository.codigoInvitacionValido(formulario.codigoInvitacion)
                        }.getOrDefault(false)
                        if (!codigoValido) {
                            usuarioRepository.eliminarCuentaEnAuth(uid)
                            setState {
                                copy(
                                    isLoading = false,
                                    errores = mapOf(
                                        CampoRegistro.CODIGO_INVITACION to MensajesRegistro.CODIGO_INVALIDO,
                                    ),
                                )
                            }
                            return@launch
                        }
                    }

                    val fecha = requireNotNull(formulario.fechaNacimiento)
                    val usuario = Usuario(
                        uid = uid,
                        nombres = formulario.nombres,
                        apellidos = formulario.apellidos,
                        correo = formulario.correo,
                        telefono = formulario.telefono,
                        fechaNacimiento = fecha,
                        rol = requireNotNull(rol),
                        // Solo los jugadores pueden quedar marcados como menores de edad.
                        esMenor = rol == UserRole.JUGADOR && ValidacionRegistro.esMenor(fecha),
                        estado = EstadoUsuario.ACTIVO,
                        clubId = null,
                    )

                    val guardado = runCatching { usuarioRepository.guardarUsuario(usuario) }
                    if (guardado.isFailure) {
                        setState { copy(isLoading = false) }
                        sendEvent(
                            RegistroEvent.Snackbar("No se pudo guardar el perfil. Inténtalo nuevamente"),
                        )
                        return@launch
                    }

                    // El código se consume recién cuando el perfil quedó guardado.
                    if (rol == UserRole.ADMINISTRADOR) {
                        runCatching { usuarioRepository.marcarCodigoInvitacionUsado(formulario.codigoInvitacion) }
                    }
                    // Si el envío del correo falla, la cuenta ya existe: no se bloquea el registro.
                    runCatching { usuarioRepository.enviarCorreoVerificacion() }

                    setState { copy(isLoading = false) }
                    sendEvent(RegistroEvent.CuentaCreada)
                }
            }
        }
    }

    private fun validarFormulario(f: RegistroUiState): Map<CampoRegistro, String> {
        val errores = mutableMapOf<CampoRegistro, String>()

        ValidacionRegistro.validarNombre(f.nombres)?.let { errores[CampoRegistro.NOMBRES] = it }
        ValidacionRegistro.validarNombre(f.apellidos)?.let { errores[CampoRegistro.APELLIDOS] = it }
        ValidacionRegistro.validarCorreo(f.correo)?.let { errores[CampoRegistro.CORREO] = it }
        ValidacionRegistro.validarPassword(f.password)?.let { errores[CampoRegistro.PASSWORD] = it }
        ValidacionRegistro.validarConfirmacion(f.password, f.confirmarPassword)?.let {
            errores[CampoRegistro.CONFIRMAR_PASSWORD] = it
        }
        ValidacionRegistro.validarFechaNacimiento(f.fechaNacimiento)?.let {
            errores[CampoRegistro.FECHA_NACIMIENTO] = it
        }

        val rol = f.rol
        if (rol == null) {
            errores[CampoRegistro.ROL] = MensajesRegistro.ROL_REQUERIDO
        }

        // Solo se pide el código de invitación al Administrador.
        if (rol == UserRole.ADMINISTRADOR) {
            ValidacionRegistro.validarCodigoInvitacion(f.codigoInvitacion)?.let {
                errores[CampoRegistro.CODIGO_INVITACION] = it
            }
        }

        // Solo se pide el apoderado si es Jugador y aún no cumplió 18 años.
        val necesitaApoderado = rol == UserRole.JUGADOR &&
            f.fechaNacimiento?.let { ValidacionRegistro.esMenor(it) } == true
        if (necesitaApoderado && !ValidacionRegistro.esCorreoValido(f.correoApoderado)) {
            errores[CampoRegistro.CORREO_APODERADO] = MensajesRegistro.APODERADO_REQUERIDO
        }

        return errores
    }
}