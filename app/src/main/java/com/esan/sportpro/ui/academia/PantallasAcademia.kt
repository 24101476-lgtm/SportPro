package com.esan.sportpro.ui.academia

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.esan.sportpro.core.fecha.FechasSportPro
import com.esan.sportpro.core.theme.EstilosSportPro
import com.esan.sportpro.core.theme.PaletaSportPro
import com.esan.sportpro.core.theme.SportProTema
import com.esan.sportpro.domain.academia.CampoAcademia
import com.esan.sportpro.domain.academia.ConfiguracionAcademia
import com.esan.sportpro.domain.academia.EstadoCodigo
import com.esan.sportpro.navigation.UserRole
import com.esan.sportpro.ui.common.BarraSuperiorSportPro
import com.esan.sportpro.ui.common.ChipEstado
import com.esan.sportpro.ui.common.EscudoEquipo
import com.esan.sportpro.ui.common.FondoFotoSportPro
import com.esan.sportpro.ui.common.FotoSportPro
import kotlinx.coroutines.launch

// =============================================================================================
// US-004 · Datos de la academia (pantalla propia, antes era un diálogo)
// =============================================================================================

/**
 * Registro y edición de los datos de la academia. Arriba, una vista previa de cómo se verá la
 * academia en la app (escudo y nombre en vivo); debajo, los datos agrupados en bloques y el día de
 * corte con − / +. El botón Guardar queda fijo abajo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PantallaDatosAcademia(
    formulario: FormularioConfiguracion,
    snackbarHostState: SnackbarHostState,
    onCampo: (CampoAcademia, String) -> Unit,
    onEscudo: (String?) -> Unit,
    onGuardar: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selector = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { onEscudo(it.toString()) }
    }
    val elegirEscudo = {
        selector.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    val d = formulario.datos

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            BarraSuperiorSportPro(
                title = { Text(if (formulario.esRegistro) "Registrar academia" else "Datos de la academia") },
                navigationIcon = {
                    IconButton(onClick = onVolver, enabled = !formulario.guardando) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().imePadding().padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    TextButton(onClick = onVolver, enabled = !formulario.guardando) { Text("Cancelar") }
                    Button(
                        onClick = onGuardar,
                        enabled = !formulario.guardando,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = PaletaSportPro.Musgo, contentColor = Color.White),
                        modifier = Modifier.weight(1f).padding(start = 8.dp).heightIn(min = 52.dp),
                    ) {
                        if (formulario.guardando) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                            Text("Guardando…", modifier = Modifier.padding(start = 10.dp))
                        } else {
                            Text(if (formulario.esRegistro) "Registrar academia" else "Guardar cambios", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Vista previa: escudo con botón de cámara y el nombre tal como se verá.
            item(key = "vista") {
                FondoFotoSportPro(
                    foto = FotoSportPro.ESTADIO,
                    forma = RoundedCornerShape(28.dp),
                    velo = Brush.verticalGradient(
                        0f to PaletaSportPro.Musgo.copy(alpha = 0.80f),
                        1f to PaletaSportPro.Musgo.copy(alpha = 0.95f),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 22.dp, horizontal = 16.dp),
                    ) {
                        Box {
                            Surface(
                                onClick = elegirEscudo,
                                enabled = !formulario.guardando,
                                shape = CircleShape,
                                color = Color.White,
                                modifier = Modifier.size(96.dp),
                            ) {
                                val visible = formulario.escudoVisible
                                if (visible != null) {
                                    AsyncImage(
                                        model = visible,
                                        contentDescription = "Escudo de la academia",
                                        contentScale = ContentScale.Crop,
                                    )
                                } else {
                                    Box(contentAlignment = Alignment.Center) {
                                        EscudoEquipo(nombre = d.nombre.ifBlank { "Academia" }, tamano = 72.dp)
                                    }
                                }
                            }
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 4.dp, y = 4.dp)
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(PaletaSportPro.Lima)
                                    .clickable(enabled = !formulario.guardando, onClick = elegirEscudo),
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = "Cambiar escudo", tint = PaletaSportPro.Carbon, modifier = Modifier.size(18.dp))
                            }
                        }
                        Text(
                            d.nombre.ifBlank { "Tu academia" },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 14.dp),
                        )
                        val lugar = listOf(d.distrito, d.ciudad).filter { it.isNotBlank() }.joinToString(", ")
                        Text(
                            listOfNotNull(
                                lugar.ifBlank { null },
                                formulario.anioFundacionTexto.takeIf { it.length == 4 }?.let { "Desde $it" },
                            ).joinToString(" · ").ifBlank { "Así verán tu academia en la app" },
                            style = MaterialTheme.typography.bodySmall,
                            color = PaletaSportPro.MusgoClaro,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            formulario.errores[CampoAcademia.ESCUDO] ?: "Escudo: máximo 2 MB, se recorta en cuadrado",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (CampoAcademia.ESCUDO in formulario.errores) PaletaSportPro.ErrorClaro else PaletaSportPro.MusgoClaro,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }

            item(key = "identidad") {
                BloqueDatos(Icons.Default.Shield, "Identidad") {
                    CampoTextoAcademia("Nombre de la academia", d.nombre, CampoAcademia.NOMBRE, formulario, onCampo, icono = Icons.Default.SportsSoccer)
                    CampoTextoAcademia(
                        "Año de fundación", formulario.anioFundacionTexto, CampoAcademia.ANIO_FUNDACION, formulario, onCampo,
                        teclado = KeyboardType.Number, icono = Icons.Default.Flag,
                    )
                }
            }
            item(key = "ubicacion") {
                BloqueDatos(Icons.Default.Place, "Ubicación") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.weight(1f)) {
                            CampoTextoAcademia("Ciudad", d.ciudad, CampoAcademia.CIUDAD, formulario, onCampo, icono = Icons.Default.LocationCity)
                        }
                        Box(Modifier.weight(1f)) {
                            CampoTextoAcademia("Distrito", d.distrito, CampoAcademia.DISTRITO, formulario, onCampo)
                        }
                    }
                    CampoTextoAcademia("Dirección", d.direccion, CampoAcademia.DIRECCION, formulario, onCampo, icono = Icons.Default.Home)
                }
            }
            item(key = "contacto") {
                BloqueDatos(Icons.Default.Phone, "Contacto") {
                    CampoTextoAcademia(
                        "Teléfono", d.telefono, CampoAcademia.TELEFONO, formulario, onCampo,
                        teclado = KeyboardType.Phone, icono = Icons.Default.Phone,
                    )
                    CampoTextoAcademia(
                        "Correo", d.correo, CampoAcademia.CORREO, formulario, onCampo,
                        teclado = KeyboardType.Email, icono = Icons.Default.Email,
                    )
                }
            }
            item(key = "mensualidades") {
                BloqueDatos(Icons.Default.Payments, "Mensualidades") {
                    DiaDeCorte(
                        texto = formulario.diaCorteTexto,
                        error = formulario.errores[CampoAcademia.DIA_CORTE],
                        habilitado = !formulario.guardando,
                        onCambio = { onCampo(CampoAcademia.DIA_CORTE, it) },
                    )
                }
            }
        }
    }
}

/** Bloque blanco con un ícono en círculo lima suave y su título. */
@Composable
private fun BloqueDatos(icono: ImageVector, titulo: String, contenido: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(PaletaSportPro.LimaSuave),
                ) {
                    Icon(icono, contentDescription = null, tint = PaletaSportPro.Musgo, modifier = Modifier.size(18.dp))
                }
                Text(
                    titulo,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
            contenido()
        }
    }
}

/** Día de corte con − / +: "Día 10" en grande y qué significa. Guarda el valor como texto. */
@Composable
private fun DiaDeCorte(texto: String, error: String?, habilitado: Boolean, onCambio: (String) -> Unit) {
    val dia = texto.trim().toIntOrNull()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Vencen el", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.Bottom) {
                Text("Día ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(dia?.toString() ?: "—", style = EstilosSportPro.NumeroGrande, color = SportProTema.colores.textoAcento)
            }
            Text("de cada mes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        BotonDia("−", "Un día antes", habilitado && (dia ?: 1) > 1) {
            onCambio(((dia ?: 1) - 1).coerceAtLeast(1).toString())
        }
        Box(Modifier.width(10.dp))
        BotonDia("+", "Un día después", habilitado && (dia ?: 0) < ConfiguracionAcademia.DIA_CORTE_MAX) {
            onCambio(((dia ?: 0) + 1).coerceAtMost(ConfiguracionAcademia.DIA_CORTE_MAX).toString())
        }
    }
    Text(
        error ?: "Entre 1 y ${ConfiguracionAcademia.DIA_CORTE_MAX}. Después de ese día, lo no pagado figura como vencido.",
        style = MaterialTheme.typography.bodySmall,
        color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun BotonDia(simbolo: String, descripcion: String, habilitado: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = habilitado,
        shape = CircleShape,
        color = if (habilitado) PaletaSportPro.Musgo else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (habilitado) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(44.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(simbolo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun CampoTextoAcademia(
    etiqueta: String,
    valor: String,
    campo: CampoAcademia,
    formulario: FormularioConfiguracion,
    onCampo: (CampoAcademia, String) -> Unit,
    teclado: KeyboardType = KeyboardType.Text,
    ayuda: String? = null,
    icono: ImageVector? = null,
) {
    val error = formulario.errores[campo]
    OutlinedTextField(
        value = valor,
        onValueChange = { onCampo(campo, it) },
        label = { Text(etiqueta) },
        singleLine = true,
        enabled = !formulario.guardando,
        isError = error != null,
        shape = RoundedCornerShape(16.dp),
        leadingIcon = if (icono != null) {
            { Icon(icono, contentDescription = null, modifier = Modifier.size(20.dp)) }
        } else {
            null
        },
        supportingText = if (error != null || ayuda != null) {
            { Text(error ?: ayuda.orEmpty()) }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        modifier = Modifier.fillMaxWidth(),
    )
}

// =============================================================================================
// US-004 · Códigos de invitación (pantalla propia)
// =============================================================================================

/** Para qué sirve cada rol: se muestra en la tarjeta para elegir a quién invitar. */
private fun descripcionRol(rol: UserRole): String = when (rol) {
    UserRole.ADMINISTRADOR -> "Gestiona toda la academia"
    UserRole.ENTRENADOR -> "Lleva sus equipos y partidos"
    UserRole.JUGADOR -> "Ve su ficha, partidos y avisos"
    UserRole.PADRE_DE_FAMILIA -> "Sigue a su hijo y sus pagos"
}

private fun iconoRol(rol: UserRole): ImageVector = when (rol) {
    UserRole.ADMINISTRADOR -> Icons.Default.AdminPanelSettings
    UserRole.ENTRENADOR -> Icons.Default.Sports
    UserRole.JUGADOR -> Icons.Default.Badge
    UserRole.PADRE_DE_FAMILIA -> Icons.Default.FamilyRestroom
}

/**
 * Códigos de invitación: se elige a quién invitar (cuatro tarjetas con lo que puede hacer cada
 * rol), se genera el código y cada código se ve como una entrada con su estado y vencimiento.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PantallaCodigos(
    state: AcademiaUiState,
    snackbarHostState: SnackbarHostState,
    onGenerar: (UserRole) -> Unit,
    onRevocar: (String) -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var rol by rememberSaveable { mutableStateOf(UserRole.ENTRENADOR) }
    val portapapeles = LocalClipboardManager.current
    val alcance = rememberCoroutineScope()
    val vigentes = state.codigos.count { it.vigente() }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            BarraSuperiorSportPro(
                title = { Text("Códigos de invitación") },
                navigationIcon = {
                    IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "intro") {
                Surface(shape = RoundedCornerShape(28.dp), color = PaletaSportPro.Musgo, contentColor = Color.White) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Invita a tu equipo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                "Cada código sirve para crear una cuenta con el rol elegido y vence a los 7 días.",
                                style = MaterialTheme.typography.bodySmall,
                                color = PaletaSportPro.MusgoClaro,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(start = 12.dp)) {
                            Text(vigentes.toString(), style = EstilosSportPro.NumeroGrande, color = PaletaSportPro.Lima)
                            Text(if (vigentes == 1) "vigente" else "vigentes", style = MaterialTheme.typography.labelSmall, color = PaletaSportPro.MusgoClaro)
                        }
                    }
                }
            }

            item(key = "titulo-rol") {
                Text("¿A quién vas a invitar?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
            }
            item(key = "roles") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    UserRole.entries.chunked(2).forEach { par ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            par.forEach { opcion ->
                                TarjetaRol(opcion, elegido = rol == opcion, onClick = { rol = opcion }, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            item(key = "generar") {
                Button(
                    onClick = { onGenerar(rol) },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = PaletaSportPro.Lima, contentColor = PaletaSportPro.Carbon),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        "Generar código de ${etiquetaRol(rol).lowercase()}",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }

            item(key = "titulo-codigos") {
                Text("Tus códigos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            }
            if (state.codigos.isEmpty()) {
                item(key = "sin-codigos") {
                    Text(
                        "Todavía no generaste códigos. Elige un rol y toca Generar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(state.codigos, key = { it.codigo }) { codigo ->
                val estado = codigo.estado()
                EntradaCodigo(
                    codigo = codigo.codigo,
                    rol = codigo.rol,
                    estado = estado,
                    vence = codigo.expiraEn?.let { FechasSportPro.fecha(it) } ?: "—",
                    onCopiar = {
                        portapapeles.setText(AnnotatedString(codigo.codigo))
                        alcance.launch { snackbarHostState.showSnackbar("Código ${codigo.codigo} copiado") }
                    },
                    onRevocar = { onRevocar(codigo.codigo) },
                )
            }
        }
    }
}

@Composable
private fun TarjetaRol(rol: UserRole, elegido: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = if (elegido) PaletaSportPro.Musgo else MaterialTheme.colorScheme.surface,
        contentColor = if (elegido) Color.White else MaterialTheme.colorScheme.onSurface,
        border = if (elegido) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (elegido) PaletaSportPro.Lima else PaletaSportPro.LimaSuave),
            ) {
                Icon(iconoRol(rol), contentDescription = null, tint = if (elegido) PaletaSportPro.Carbon else PaletaSportPro.Musgo, modifier = Modifier.size(20.dp))
            }
            Text(etiquetaRol(rol), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
            Text(
                descripcionRol(rol),
                style = MaterialTheme.typography.bodySmall,
                color = if (elegido) PaletaSportPro.MusgoClaro else MaterialTheme.colorScheme.onSurfaceVariant,
                minLines = 2,
            )
        }
    }
}

/** Un código como entrada: el código grande a la izquierda, línea punteada y las acciones a la derecha. */
@Composable
private fun EntradaCodigo(
    codigo: String,
    rol: UserRole,
    estado: EstadoCodigo,
    vence: String,
    onCopiar: () -> Unit,
    onRevocar: () -> Unit,
) {
    val vigente = estado == EstadoCodigo.VIGENTE
    val linea = MaterialTheme.colorScheme.outlineVariant
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(6.dp).fillMaxHeight().background(if (vigente) PaletaSportPro.Lima else linea))
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp, top = 14.dp, bottom = 14.dp, end = 10.dp)) {
                Text(
                    codigo,
                    style = EstilosSportPro.NumeroMediano.copy(fontSize = 24.sp, letterSpacing = 3.sp),
                    color = if (vigente) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                    ChipEstado(estado.etiqueta, tonoDe(estado))
                    Text(
                        "  ${etiquetaRol(rol)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                    Text(
                        " Vence el $vence",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (vigente) {
                // Línea punteada, como el talón de una entrada.
                Canvas(Modifier.width(1.dp).fillMaxHeight().padding(vertical = 10.dp)) {
                    drawLine(
                        color = linea,
                        start = Offset(0f, 0f),
                        end = Offset(0f, size.height),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
                    )
                }
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxHeight().padding(horizontal = 6.dp),
                ) {
                    IconButton(onClick = onCopiar) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copiar código $codigo", tint = PaletaSportPro.Musgo)
                    }
                    IconButton(onClick = onRevocar) {
                        Icon(Icons.Default.Block, contentDescription = "Revocar código $codigo", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
