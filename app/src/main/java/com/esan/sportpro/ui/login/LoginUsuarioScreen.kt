package com.esan.sportpro.ui.login

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.esan.sportpro.R
import com.esan.sportpro.core.theme.EstilosSportPro
import com.esan.sportpro.core.theme.PaletaSportPro
import com.esan.sportpro.core.theme.SportProTema
import com.esan.sportpro.navigation.UserRole
import com.esan.sportpro.ui.common.BotonApariencia
import com.esan.sportpro.ui.common.BotonPrincipalSportPro
import com.esan.sportpro.ui.common.coloresCampoSportPro

/**
 * Login de US-002, con bloqueo por intentos fallidos y recuperación de contraseña.
 *
 * Primero se ve una bienvenida a pantalla completa (foto, cinta "Entrena, juega, crece" y título);
 * al tocar "Ingresar" sube una hoja con el formulario. La lógica es la misma de siempre: todo
 * sale de [LoginViewModel].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginUsuarioScreen(
    onSesionIniciada: (UserRole) -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToRecuperar: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var hojaAbierta by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LoginEvent.SesionIniciada -> onSesionIniciada(event.rol)
                is LoginEvent.Snackbar -> snackbarHostState.showSnackbar(event.mensaje)
            }
        }
    }

    Scaffold(
        snackbarHost = { if (!hojaAbierta) SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Bienvenida(
            onIngresar = { hojaAbierta = true },
            onCrearCuenta = onNavigateToRegister,
            modifier = Modifier.padding(padding),
        )
    }

    if (hojaAbierta) {
        ModalBottomSheet(
            onDismissRequest = { hojaAbierta = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            FormularioIngreso(
                state = state,
                viewModel = viewModel,
                onNavigateToRecuperar = onNavigateToRecuperar,
                onNavigateToRegister = onNavigateToRegister,
            )
            // Dentro de la hoja, para que los avisos no queden debajo de ella.
            SnackbarHost(snackbarHostState)
        }
    }
}

/** Bienvenida: marca y apariencia arriba, el arte en el centro y el título con las acciones abajo. */
@Composable
private fun Bienvenida(onIngresar: () -> Unit, onCrearCuenta: () -> Unit, modifier: Modifier = Modifier) {
    val colores = SportProTema.colores
    // Brillo muy tenue detrás del arte: le da profundidad sin cargar la pantalla.
    val brillo = if (colores.esOscuro) PaletaSportPro.Musgo.copy(alpha = 0.22f) else PaletaSportPro.Lima.copy(alpha = 0.16f)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.45f to brillo,
                    0.75f to Color.Transparent,
                ),
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp),
        ) {
            Text(
                "SportPro",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            BotonApariencia(sobreFoto = false)
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .heightIn(min = 260.dp),
        ) {
            ArteBienvenida()
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
        ) {
            Text(
                "Tu academia\nen la cancha",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 52.sp, lineHeight = 50.sp),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                "Equipos, entrenamientos y partidos de tu academia en un solo lugar.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp, bottom = 22.dp),
            )
            BotonPrincipalSportPro(
                texto = "Ingresar",
                onClick = onIngresar,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            )
            TextButton(
                onClick = onCrearCuenta,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .heightIn(min = 52.dp),
            ) {
                Text(
                    "Crear cuenta",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}

private val SinColor = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/**
 * Arte de la bienvenida: un marco en arco (claro: degradado lima; oscuro: foto del estadio tenue),
 * la cinta "Entrena, juega, crece" cruzándolo, el jugador recortado saliendo del marco y la nota a mano.
 */
@Composable
private fun BoxScope.ArteBienvenida() {
    val oscuro = SportProTema.colores.esOscuro
    Box(
        modifier = Modifier
            .matchParentSize()
            .padding(start = 58.dp, end = 58.dp, top = 26.dp, bottom = 22.dp)
            .clip(RoundedCornerShape(topStart = 120.dp, topEnd = 120.dp, bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(
                if (oscuro) {
                    Brush.verticalGradient(listOf(PaletaSportPro.CarbonSuperficie, PaletaSportPro.Carbon))
                } else {
                    Brush.verticalGradient(listOf(PaletaSportPro.LimaSuave, PaletaSportPro.Lima))
                },
            ),
    ) {
        if (oscuro) {
            Image(
                painter = painterResource(R.drawable.foto_estadio),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = SinColor,
                alpha = 0.38f,
                modifier = Modifier.matchParentSize(),
            )
        }
    }

    CintaBienvenida(modifier = Modifier.matchParentSize())

    Image(
        painter = painterResource(R.drawable.jugador_bienvenida),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        colorFilter = SinColor,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .offset(x = 10.dp)
            .fillMaxHeight(0.98f),
    )

    Text(
        "Cada entreno\ncuenta",
        style = EstilosSportPro.NotaMano,
        color = SportProTema.colores.textoAcento,
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(start = 22.dp, top = 4.dp)
            .rotate(-8f),
    )
}

/**
 * La cinta: una banda gruesa con texto que corre por su curva y una franja delgada paralela.
 * Claro: banda carbón con texto lima. Oscuro: banda lima con texto carbón. La franja es musgo en ambos.
 * Un borde del color del fondo la separa de la foto.
 */
@Composable
private fun CintaBienvenida(modifier: Modifier = Modifier) {
    val contexto = LocalContext.current
    val oscuro = SportProTema.colores.esOscuro
    val fondo = MaterialTheme.colorScheme.background
    val banda = if (oscuro) PaletaSportPro.Lima else PaletaSportPro.Carbon
    val textoBanda = if (oscuro) PaletaSportPro.Carbon else PaletaSportPro.Lima
    val franja = PaletaSportPro.Musgo
    val tipografia = remember { ResourcesCompat.getFont(contexto, R.font.bricolage_grotesque_extrabold) }

    Canvas(modifier = modifier) {
        // Curva en S pensada sobre un lienzo de 390 × 420 y escalada al tamaño real.
        val ex = size.width / 390f
        val ey = size.height / 420f
        val curva = Path().apply {
            moveTo(-60f * ex, 300f * ey)
            cubicTo(40f * ex, 420f * ey, 150f * ex, 120f * ey, 230f * ex, 190f * ey)
            cubicTo(290f * ex, 240f * ey, 340f * ex, 260f * ey, 450f * ex, 110f * ey)
        }
        val grosor = 58.dp.toPx()

        translate(top = 46.dp.toPx()) {
            drawPath(curva, franja, style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round))
        }
        drawPath(curva, fondo, style = Stroke(width = grosor + 6.dp.toPx(), cap = StrokeCap.Round))
        drawPath(curva, banda, style = Stroke(width = grosor, cap = StrokeCap.Round))

        val pincel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = tipografia
            textSize = 21.sp.toPx()
            letterSpacing = 0.1f
            color = textoBanda.toArgb()
        }
        drawIntoCanvas { lienzo ->
            lienzo.nativeCanvas.drawTextOnPath(
                "ENTRENA     JUEGA     CRECE     ENTRENA     JUEGA     CRECE     ENTRENA",
                curva.asAndroidPath(),
                40.dp.toPx(),
                7.dp.toPx(),
                pincel,
            )
        }
    }
}

/** Hoja con el formulario de ingreso. */
@Composable
private fun FormularioIngreso(
    state: LoginUiState,
    viewModel: LoginViewModel,
    onNavigateToRecuperar: () -> Unit,
    onNavigateToRegister: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
    ) {
        Text(
            "Inicia sesión",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            "Entra con el correo de tu cuenta en la academia.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = state.correo,
            onValueChange = viewModel::onCorreoChanged,
            label = { Text("Correo") },
            singleLine = true,
            isError = state.errores.containsKey(CampoLogin.CORREO),
            supportingText = state.errores[CampoLogin.CORREO]?.let { mensaje ->
                { Text(mensaje) }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            shape = MaterialTheme.shapes.small,
            colors = coloresCampoSportPro(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )

        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChanged,
            label = { Text("Contraseña") },
            singleLine = true,
            isError = state.errores.containsKey(CampoLogin.PASSWORD),
            supportingText = state.errores[CampoLogin.PASSWORD]?.let { mensaje ->
                { Text(mensaje) }
            },
            visualTransformation = if (state.mostrarPassword) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = viewModel::onToggleMostrarPassword) {
                    Icon(
                        imageVector = if (state.mostrarPassword) {
                            Icons.Default.VisibilityOff
                        } else {
                            Icons.Default.Visibility
                        },
                        contentDescription = if (state.mostrarPassword) {
                            "Ocultar contraseña"
                        } else {
                            "Mostrar contraseña"
                        },
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            shape = MaterialTheme.shapes.small,
            colors = coloresCampoSportPro(),
            modifier = Modifier.fillMaxWidth(),
        )

        state.errorGeneral?.let { mensaje ->
            Text(
                text = mensaje,
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.bloqueado) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
        }

        // El correo existe pero sin verificar: se ofrece reenviar el mensaje.
        if (state.correoSinVerificar) {
            TextButton(
                onClick = viewModel::reenviarVerificacion,
                enabled = !state.isLoading,
            ) {
                Text("Reenviar correo de verificación", fontWeight = FontWeight.Bold)
            }
        }

        BotonPrincipalSportPro(
            texto = "Ingresar",
            onClick = viewModel::login,
            habilitado = !state.isLoading && !state.bloqueado,
            cargando = state.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .heightIn(min = 56.dp),
        )

        if (state.bloqueado) {
            Text(
                text = "Puedes volver a intentarlo en ${state.segundosRestantesBloqueo} segundos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        TextButton(onClick = onNavigateToRecuperar) {
            Text("¿Olvidaste tu contraseña?", fontWeight = FontWeight.Bold)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "¿No tienes cuenta?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp),
            )
            TextButton(onClick = onNavigateToRegister) {
                Text("Regístrate", fontWeight = FontWeight.Bold)
            }
        }
    }
}
