package com.esan.sportpro.ui.registro

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.core.theme.PaletaSportPro
import com.esan.sportpro.core.theme.SportProTema
import com.esan.sportpro.navigation.UserRole
import com.esan.sportpro.ui.common.BotonApariencia
import com.esan.sportpro.ui.common.BotonPrincipalSportPro
import com.esan.sportpro.ui.common.ChipEstado
import com.esan.sportpro.ui.common.TarjetaSportPro
import com.esan.sportpro.ui.common.TonoEstado
import com.esan.sportpro.ui.common.TrazoSportPro
import com.esan.sportpro.ui.common.coloresCampoSportPro
import java.time.Instant
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

/** Qué hace cada rol en la app, en una línea. */
private fun descripcionRol(rol: UserRole): String = when (rol) {
    UserRole.ENTRENADOR -> "Dirige equipos y entrenamientos"
    UserRole.JUGADOR -> "Juega en un equipo de la academia"
    UserRole.PADRE_DE_FAMILIA -> "Sigue a tus hijos"
    UserRole.ADMINISTRADOR -> "Gestiona la academia, con código"
}

private fun iconoRol(rol: UserRole): ImageVector = when (rol) {
    UserRole.ENTRENADOR -> Icons.Default.Sports
    UserRole.JUGADOR -> Icons.Default.SportsSoccer
    UserRole.PADRE_DE_FAMILIA -> Icons.Default.FamilyRestroom
    UserRole.ADMINISTRADOR -> Icons.Default.AdminPanelSettings
}

/**
 * Pantalla de registro con selección de rol (US-001).
 * Título grande con el trazo de la marca, datos agrupados por sección y el rol como tarjetas.
 * La lógica es la misma: todo sale de [RegistroUsuarioViewModel].
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

    // Brillo muy tenue arriba, detrás del título.
    val brillo = if (SportProTema.colores.esOscuro) {
        PaletaSportPro.Musgo.copy(alpha = 0.30f)
    } else {
        PaletaSportPro.Lima.copy(alpha = 0.22f)
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0f to brillo, 0.28f to Color.Transparent))
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                IconButton(onClick = onCuentaCreada, modifier = Modifier.padding(start = 0.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver a iniciar sesión")
                }
                Spacer(modifier = Modifier.weight(1f))
                BotonApariencia(sobreFoto = false)
            }

            Text(
                "Crea tu\ncuenta",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 16.dp),
            )
            TrazoSportPro(
                color = if (SportProTema.colores.esOscuro) SportProTema.colores.acento else MaterialTheme.colorScheme.onBackground,
                grosor = 6.dp,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .width(150.dp)
                    .height(18.dp),
            )
            Text(
                "Completa tus datos para unirte a tu academia.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )

            Seccion("Tus datos")
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
                valor = state.telefono,
                onValorCambiado = viewModel::onTelefonoChanged,
                etiqueta = "Teléfono",
                error = state.errores[CampoRegistro.TELEFONO],
                teclado = KeyboardType.Phone,
            )
            // Fecha de nacimiento: abre el selector y de ahí se calcula si es menor de 18.
            CampoFechaNacimiento(
                texto = state.fechaNacimiento?.format(FORMATO_FECHA),
                error = state.errores[CampoRegistro.FECHA_NACIMIENTO],
                onClick = { datePickerAbierto = true },
            )

            Seccion("Acceso")
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

            Seccion("Tu rol")
            SelectorRol(
                rol = state.rol,
                error = state.errores[CampoRegistro.ROL],
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

            // Jugador menor de 18: no puede registrarse solo (regla del apoderado).
            if (state.mostrarCampoApoderado) {
                TarjetaSportPro(modifier = Modifier.padding(top = 12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ChipEstado("Menor de edad", TonoEstado.ADVERTENCIA)
                        Text(
                            "Los jugadores menores de 18 años no crean su cuenta. Tu academia registra tu ficha y " +
                                "quedas registrado cuando tu apoderado crea su cuenta de Padre de familia y se " +
                                "vincula con el código de jugador que le entrega la academia.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }

            state.errorGeneral?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            BotonPrincipalSportPro(
                texto = "Crear cuenta",
                onClick = viewModel::registrar,
                // Deshabilitado con indicador circular mientras guarda la cuenta.
                habilitado = state.puedeEnviar,
                cargando = state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .heightIn(min = 56.dp),
            )

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    "¿Ya tienes cuenta?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp),
                )
                TextButton(onClick = onCuentaCreada) {
                    Text("Inicia sesión", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (datePickerAbierto) {
        // Solo fechas entre hace 100 años y hoy: no se puede elegir un año imposible.
        val hoyUtc = java.time.LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val datePickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = state.fechaNacimiento
                ?.atStartOfDay(ZoneOffset.UTC)
                ?.toInstant()
                ?.toEpochMilli(),
            yearRange = ValidacionRegistro.anioMinimoNacimiento()..java.time.LocalDate.now().year,
            selectableDates = object : androidx.compose.material3.SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= hoyUtc
            },
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

/** Título de un grupo de campos. */
@Composable
private fun Seccion(titulo: String) {
    Text(
        titulo,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 28.dp, bottom = 4.dp),
    )
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
        shape = MaterialTheme.shapes.small,
        colors = coloresCampoSportPro(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    )
}

/** Campo de fecha que se ve como los demás campos y abre el calendario al tocarlo. */
@Composable
private fun CampoFechaNacimiento(texto: String?, error: String?, onClick: () -> Unit) {
    val colorBorde = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, colorBorde),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .heightIn(min = 56.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Fecha de nacimiento",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    texto ?: "Elige una fecha",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (texto != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    error?.let {
        Text(
            text = it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp),
        )
    }
}

/** El rol como 4 tarjetas en una grilla de 2 × 2; la elegida se rellena con el acento. */
@Composable
private fun SelectorRol(
    rol: UserRole?,
    error: String?,
    onRolSeleccionado: (UserRole) -> Unit,
) {
    val colores = SportProTema.colores
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
        UserRole.entries.chunked(2).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                fila.forEach { opcion ->
                    val elegido = opcion == rol
                    Surface(
                        onClick = { onRolSeleccionado(opcion) },
                        shape = MaterialTheme.shapes.small,
                        color = if (elegido) colores.acento else MaterialTheme.colorScheme.surface,
                        contentColor = if (elegido) colores.sobreAcento else MaterialTheme.colorScheme.onSurface,
                        border = if (elegido) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 112.dp)
                            .semantics { selected = elegido },
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(14.dp),
                        ) {
                            Icon(iconoRol(opcion), contentDescription = null, modifier = Modifier.size(24.dp))
                            Text(etiquetaRol(opcion), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                descripcionRol(opcion),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (elegido) colores.sobreAcento else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (fila.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}
