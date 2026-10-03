package com.esan.sportpro.ui.registro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.navigation.UserRole
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FORMATO_FECHA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("es"))

/** Etiqueta en español de cada rol del enum [UserRole]. */
private fun etiquetaRol(rol: UserRole): String = when (rol) {
    UserRole.ENTRENADOR -> "Entrenador"
    UserRole.JUGADOR -> "Jugador"
    UserRole.PADRE_DE_FAMILIA -> "Padre de familia"
    UserRole.ADMINISTRADOR -> "Administrador"
}

/**
 * Pantalla de registro con selección de rol (US-001).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroUsuarioScreen(
    onCuentaCreada: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RegistroUsuarioViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var menuRolAbierto by remember { mutableStateOf(false) }
    var datePickerAbierto by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { evento ->
            when (evento) {
                is RegistroEvent.Snackbar -> snackbarHostState.showSnackbar(evento.mensaje)
                // Primero se muestra el mensaje y al terminar se redirige al login.
                RegistroEvent.CuentaCreada -> {
                    snackbarHostState.showSnackbar(MensajesRegistro.EXITO)
                    onCuentaCreada()
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = "Crear cuenta", style = MaterialTheme.typography.titleLarge)

            CampoTexto(
                valor = state.nombres,
                onValorCambiado = viewModel::onNombresChanged,
                etiqueta = "Nombres",
                error = state.errores[CampoRegistro.NOMBRES],
                teclado = KeyboardType.Text,
            )

            CampoTexto(
                valor = state.apellidos,
                onValorCambiado = viewModel::onApellidosChanged,
                etiqueta = "Apellidos",
                error = state.errores[CampoRegistro.APELLIDOS],
                teclado = KeyboardType.Text,
            )

            CampoTexto(
                valor = state.correo,
                onValorCambiado = viewModel::onCorreoChanged,
                etiqueta = "Correo",
                error = state.errores[CampoRegistro.CORREO],
                teclado = KeyboardType.Email,
            )

            CampoTexto(
                valor = state.password,
                onValorCambiado = viewModel::onPasswordChanged,
                etiqueta = "Contraseña",
                error = state.errores[CampoRegistro.PASSWORD],
                teclado = KeyboardType.Password,
                esPassword = true,
                passwordVisible = passwordVisible,
                onToggleVisibilidad = { passwordVisible = !passwordVisible },
            )

            CampoTexto(
                valor = state.confirmarPassword,
                onValorCambiado = viewModel::onConfirmarPasswordChanged,
                etiqueta = "Confirmar contraseña",
                error = state.errores[CampoRegistro.CONFIRMAR_PASSWORD],
                teclado = KeyboardType.Password,
                esPassword = true,
                passwordVisible = passwordVisible,
                onToggleVisibilidad = { passwordVisible = !passwordVisible },
            )

            CampoTexto(
                valor = state.telefono,
                onValorCambiado = viewModel::onTelefonoChanged,
                etiqueta = "Teléfono",
                error = state.errores[CampoRegistro.TELEFONO],
                teclado = KeyboardType.Phone,
            )

            // Fecha de nacimiento: abre el selector y de ahí se calcula si es menor de 18.
            val errorFecha = state.errores[CampoRegistro.FECHA_NACIMIENTO]
            OutlinedButton(
                onClick = { datePickerAbierto = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null)
                Text(
                    text = state.fechaNacimiento?.format(FORMATO_FECHA)
                        ?: "Selecciona tu fecha de nacimiento",
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            errorFecha?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                )
            }

            SelectorRol(
                rol = state.rol,
                error = state.errores[CampoRegistro.ROL],
                menuAbierto = menuRolAbierto,
                onMenuAbiertoChange = { menuRolAbierto = it },
                onRolSeleccionado = viewModel::onRolChanged,
            )

            // Solo para Administrador: código de invitación de 8 caracteres.
            if (state.mostrarCampoCodigoInvitacion) {
                CampoTexto(
                    valor = state.codigoInvitacion,
                    onValorCambiado = viewModel::onCodigoInvitacionChanged,
                    etiqueta = "Código de invitación",
                    error = state.errores[CampoRegistro.CODIGO_INVITACION],
                    teclado = KeyboardType.Text,
                )
            }

            // Solo para Jugador menor de 18 años.
            if (state.mostrarCampoApoderado) {
                CampoTexto(
                    valor = state.correoApoderado,
                    onValorCambiado = viewModel::onCorreoApoderadoChanged,
                    etiqueta = "Correo del apoderado",
                    error = state.errores[CampoRegistro.CORREO_APODERADO],
                    teclado = KeyboardType.Email,
                )
            }

            state.errorGeneral?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Button(
                onClick = viewModel::registrar,
                // Deshabilitado con indicador circular mientras guarda la cuenta.
                enabled = state.puedeEnviar,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("Crear cuenta")
                }
            }

            TextButton(
                onClick = onCuentaCreada,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Ya tengo cuenta")
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (datePickerAbierto) {
        val datePickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = state.fechaNacimiento
                ?.atStartOfDay(ZoneOffset.UTC)
                ?.toInstant()
                ?.toEpochMilli()
                ?: System.currentTimeMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { datePickerAbierto = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        // DatePicker trabaja en UTC: se convierte en esa zona antes de leer el día.
                        datePickerState.selectedDateMillis?.let { millis ->
                            val fecha = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            viewModel.onFechaNacimientoChanged(fecha)
                        }
                        datePickerAbierto = false
                    },
                ) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { datePickerAbierto = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/** Campo de texto con mensaje de error debajo (solo se muestra cuando hay error). */
@Composable
private fun CampoTexto(
    valor: String,
    onValorCambiado: (String) -> Unit,
    etiqueta: String,
    error: String?,
    teclado: KeyboardType,
    esPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onToggleVisibilidad: (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onValorCambiado,
        label = { Text(etiqueta) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { mensaje -> { Text(mensaje) } },
        visualTransformation = if (esPassword && !passwordVisible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        trailingIcon = if (esPassword) {
            {
                IconButton(onClick = { onToggleVisibilidad?.invoke() }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "Ocultar contraseña" else "Ver contraseña",
                    )
                }
            }
        } else {
            null
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    )
}

/** Selector de rol: es obligatorio, por eso muestra "Seleccione un rol" si no hay ninguno. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorRol(
    rol: UserRole?,
    error: String?,
    menuAbierto: Boolean,
    onMenuAbiertoChange: (Boolean) -> Unit,
    onRolSeleccionado: (UserRole) -> Unit,
) {
    ExposedDropdownMenuBox(
        expanded = menuAbierto,
        onExpandedChange = onMenuAbiertoChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        OutlinedTextField(
            value = rol?.let { etiquetaRol(it) } ?: "",
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            isError = error != null,
            supportingText = error?.let { mensaje -> { Text(mensaje) } },
            label = { Text("Rol") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuAbierto)
            },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = menuAbierto,
            onDismissRequest = { onMenuAbiertoChange(false) },
        ) {
            UserRole.entries.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(etiquetaRol(opcion)) },
                    onClick = {
                        onRolSeleccionado(opcion)
                        onMenuAbiertoChange(false)
                    },
                )
            }
        }
    }
}