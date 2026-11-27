package com.esan.sportpro.ui.jugadores

import com.esan.sportpro.ui.common.BarraSuperiorSportPro
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.academia.Equipo
import com.esan.sportpro.domain.comunidad.PosicionJugador
import com.esan.sportpro.domain.entrenamientos.ReglasAsistencia
import com.esan.sportpro.domain.entrenamientos.ReglasHistorial
import com.esan.sportpro.domain.entrenamientos.ResumenAsistencia
import com.esan.sportpro.domain.jugadores.CampoJugador
import com.esan.sportpro.domain.jugadores.EstadoRegistro
import com.esan.sportpro.domain.jugadores.Jugador
import com.esan.sportpro.domain.jugadores.PieHabil
import com.esan.sportpro.domain.jugadores.ReglasApoderado
import com.esan.sportpro.domain.jugadores.ValidacionJugador
import com.esan.sportpro.core.fecha.FechasSportPro
import com.esan.sportpro.ui.common.AvatarIniciales
import com.esan.sportpro.ui.common.CampoFecha
import com.esan.sportpro.ui.common.CampoFormulario
import com.esan.sportpro.ui.common.ChipEstado
import com.esan.sportpro.ui.common.CargandoSportPro
import com.esan.sportpro.ui.common.EncabezadoModulo
import com.esan.sportpro.ui.common.FilaDatoSportPro
import com.esan.sportpro.ui.common.TarjetaSportPro
import com.esan.sportpro.ui.common.TituloSeccion
import com.esan.sportpro.ui.common.TonoEstado
import com.esan.sportpro.ui.common.EmptyStateMessage

/**
 * Módulo Jugadores (US-006/US-007). Navegación interna por estado del ViewModel:
 * lista → detalle → formulario, y lista → solicitudes de vinculación (US-007). Administrador y
 * entrenador ven todos los perfiles; el padre de familia ve solo a sus hijos vinculados
 * ([MisHijosScreen]); el jugador no accede (privacidad de menores).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JugadoresScreen(
    modifier: Modifier = Modifier,
    viewModel: JugadoresViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { evento ->
            if (evento is JugadoresEvent.Mensaje) snackbarHostState.showSnackbar(evento.texto)
        }
    }

    BackHandler(enabled = state.pantalla == PantallaJugadores.Formulario) { viewModel.cancelarFormulario() }
    BackHandler(enabled = state.pantalla is PantallaJugadores.Detalle) { viewModel.volverALista() }
    BackHandler(enabled = state.pantalla == PantallaJugadores.Solicitudes) { viewModel.volverALista() }
    (state.pantalla as? PantallaJugadores.Historial)?.let { historial ->
        BackHandler { viewModel.cerrarHistorial(historial.jugadorId) }
    }

    // US-007: el padre de familia ve aquí a sus hijos vinculados (con su propio ViewModel).
    if (state.esPadre) {
        MisHijosScreen(modifier = modifier)
        return
    }
    // US-006: el jugador ve aquí su propio perfil (con su ViewModel).
    if (state.esJugador) {
        MiPerfilScreen(modifier = modifier)
        return
    }

    when {
        state.cargando -> CargandoSportPro(modifier = modifier)

        state.error != null -> EmptyStateMessage(mensaje = state.error.orEmpty(), modifier = modifier)

        !state.tieneAcceso -> EmptyStateMessage(
            mensaje = "Los perfiles de jugadores solo están disponibles para el administrador y el entrenador.",
            modifier = modifier,
            icono = Icons.Default.Group,
        )

        else -> when (val pantalla = state.pantalla) {
            is PantallaJugadores.Lista -> Scaffold(
                modifier = modifier,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                floatingActionButton = {
                    FloatingActionButton(onClick = viewModel::nuevoJugador) {
                        Icon(Icons.Default.Add, contentDescription = "Nuevo jugador")
                    }
                },
            ) { padding ->
                ListaJugadores(state = state, viewModel = viewModel, modifier = Modifier.padding(padding))
            }

            is PantallaJugadores.Solicitudes -> Scaffold(
                modifier = modifier,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    BarraSuperiorSportPro(
                        title = { Text("Solicitudes de vinculación") },
                        navigationIcon = {
                            IconButton(onClick = viewModel::volverALista) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                            }
                        },
                    )
                },
            ) { padding ->
                SolicitudesVinculacionContenido(state = state, viewModel = viewModel, modifier = Modifier.padding(padding))
            }

            is PantallaJugadores.Detalle -> {
                val jugador = state.jugador(pantalla.jugadorId)
                Scaffold(
                    modifier = modifier,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        BarraSuperiorSportPro(
                            title = { Text(jugador?.nombreCompleto ?: "Jugador") },
                            navigationIcon = {
                                IconButton(onClick = viewModel::volverALista) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                                }
                            },
                        )
                    },
                ) { padding ->
                    if (jugador == null) {
                        EmptyStateMessage("No se encontró el jugador.", Modifier.padding(padding))
                    } else {
                        DetalleJugador(
                            jugador = jugador,
                            resumen = state.asistenciaDe(jugador),
                            resumenDeportivo = state.resumenesDeportivos[jugador.id],
                            onEditar = { viewModel.editarJugador(jugador) },
                            onCambiarEstado = { viewModel.cambiarEstado(jugador) },
                            onVerHistorial = { viewModel.abrirHistorial(jugador.id) },
                            modifier = Modifier.padding(padding),
                            apoderados = { SeccionApoderados(state = state, jugador = jugador, viewModel = viewModel) },
                        )
                    }
                }
            }

            is PantallaJugadores.Historial -> {
                val jugador = state.jugador(pantalla.jugadorId)
                Scaffold(
                    modifier = modifier,
                    topBar = {
                        BarraSuperiorSportPro(
                            title = { Text("Historial · ${jugador?.nombreCompleto.orEmpty()}") },
                            navigationIcon = {
                                IconButton(onClick = { viewModel.cerrarHistorial(pantalla.jugadorId) }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                                }
                            },
                        )
                    },
                ) { padding ->
                    if (jugador == null || !state.puedeVerHistorial(jugador)) {
                        EmptyStateMessage("No tienes acceso al historial de este jugador.", Modifier.padding(padding))
                    } else {
                        HistorialParticipacion(
                            jugador = jugador,
                            entrenamientos = state.entrenamientos,
                            partidos = state.historialPartidos,
                            cargandoPartidos = state.cargandoPartidos,
                            modifier = Modifier.padding(padding),
                        )
                    }
                }
            }

            is PantallaJugadores.Formulario -> Scaffold(
                modifier = modifier,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    BarraSuperiorSportPro(
                        title = { Text(if (state.formulario.id == null) "Nuevo jugador" else "Editar jugador") },
                        navigationIcon = {
                            IconButton(onClick = viewModel::cancelarFormulario) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancelar")
                            }
                        },
                    )
                },
            ) { padding ->
                FormularioJugadorContenido(
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }

    DialogosVinculacion(state = state, viewModel = viewModel)

    // US-005: asignar un jugador fuera del rango de años de la categoría del equipo.
    if (state.formulario.avisoFueraDeRango) {
        val equipo = state.equipos.firstOrNull { it.id == state.formulario.datos.equipoId }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = viewModel::cancelarFueraDeRango,
            title = { Text(com.esan.sportpro.domain.academia.MensajesEquipo.FUERA_DE_RANGO) },
            text = {
                Text(
                    buildString {
                        append("${equipo?.nombre ?: "El equipo"} admite nacidos entre ${equipo?.rangoAnios.orEmpty()}.")
                        append(" Si lo asignas igual, quedará registrado como excepción de categoría.")
                    },
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = viewModel::asignarFueraDeRango) {
                    Text("Asignar de todas formas")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = viewModel::cancelarFueraDeRango) { Text("Cancelar") }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ListaJugadores(
    state: JugadoresUiState,
    viewModel: JugadoresViewModel,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        val registrados = state.jugadores.count { ReglasApoderado.participa(it) }
        val pendientesApoderado = state.jugadores.count { it.activo && ReglasApoderado.pendiente(it) }
        EncabezadoModulo(
            foto = com.esan.sportpro.ui.common.FotoSportPro.VESTUARIO,
            titulo = "Jugadores",
            subtitulo = "$registrados registrados" +
                if (pendientesApoderado > 0) " · $pendientesApoderado pendientes de apoderado" else "",
            icono = Icons.Default.Group,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
        )
        // US-007: aviso dentro de la app de las solicitudes pendientes (hasta que exista el push).
        val pendientes = state.solicitudesPendientes.size
        TarjetaSportPro(
            onClick = viewModel::abrirSolicitudes,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                com.esan.sportpro.ui.common.IconoCircular(Icons.Default.FamilyRestroom, tamano = 40)
                Text(
                    "Solicitudes de vinculación",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                if (pendientes > 0) {
                    androidx.compose.material3.Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = com.esan.sportpro.core.theme.SportProTema.colores.acento,
                        contentColor = com.esan.sportpro.core.theme.SportProTema.colores.sobreAcento,
                    ) {
                        Text(
                            "$pendientes",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
        // Equipo en un combo (una sola fila aunque la academia tenga muchos equipos) y, al lado,
        // el interruptor de inactivos.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            com.esan.sportpro.ui.common.SelectorEquipo(
                equipos = state.equiposActivos,
                seleccionadoId = state.filtroEquipoId,
                onSeleccionar = viewModel::onFiltroEquipo,
                modifier = Modifier.weight(1f),
            )
            Text(
                "Inactivos",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 12.dp),
            )
            Switch(
                checked = state.mostrarInactivos,
                onCheckedChange = viewModel::onMostrarInactivos,
                modifier = Modifier.padding(start = 6.dp),
            )
        }

        if (state.jugadoresVisibles.isEmpty()) {
            EmptyStateMessage(
                mensaje = if (state.equipos.isEmpty()) {
                    "Primero crea un equipo en Academia y luego registra a sus jugadores."
                } else {
                    "No hay jugadores para este filtro. Registra uno con el botón +."
                },
                icono = Icons.Default.Group,
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.jugadoresVisibles, key = { it.id }) { jugador ->
                    JugadorCard(jugador = jugador, onClick = { viewModel.abrirDetalle(jugador.id) })
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JugadorCard(jugador: Jugador, onClick: () -> Unit) {
    // Rediseño claro: foto con anillo del color de la posición, insignia de la posición y el
    // dorsal en un círculo lima suave. Los inactivos se ven atenuados.
    androidx.compose.material3.Surface(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (jugador.activo) Modifier else Modifier.alpha(0.6f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FotoConAnillo(jugador.fotoUrl, jugador.iniciales, jugador.posicion, tamano = 52.dp, nombre = jugador.nombreCompleto)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    jugador.nombreCompleto,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InsigniaPosicion(jugador.posicion)
                    Text(
                        listOf(jugador.posicion.ifBlank { "Sin posición" }, jugador.equipoNombre)
                            .filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
                if (ReglasApoderado.pendiente(jugador) || jugador.esMenor() || !jugador.activo) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (ReglasApoderado.pendiente(jugador)) ChipEstado("Pendiente de apoderado", TonoEstado.ADVERTENCIA)
                        if (jugador.esMenor()) ChipEstado("Menor", TonoEstado.INFORMACION)
                        if (!jugador.activo) ChipEstado("Inactivo", TonoEstado.ERROR)
                    }
                }
            }
            jugador.dorsal?.let { DorsalJugador(it) }
        }
    }
}

@Composable
private fun DetalleJugador(
    jugador: Jugador,
    resumen: ResumenAsistencia,
    resumenDeportivo: ResumenDeportivo?,
    onEditar: () -> Unit,
    onCambiarEstado: () -> Unit,
    modifier: Modifier = Modifier,
    onVerHistorial: () -> Unit = {},
    apoderados: @Composable () -> Unit = {},
) {
    // Antes la ficha mostraba 7 bloques seguidos; ahora se reparte en 3 pestañas (FichaJugador.kt).
    var pestana by rememberSaveable(jugador.id) { mutableStateOf(PestanaFicha.RESUMEN) }
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (ReglasApoderado.pendiente(jugador)) {
            AvisoRegistroPendiente(jugador)
        }
        EncabezadoFicha(jugador)
        SelectorPestanasFicha(actual = pestana, onElegir = { pestana = it })

        when (pestana) {
            PestanaFicha.RESUMEN -> ContenidoResumen(
                resumenDeportivo = resumenDeportivo,
                asistencia = resumen,
                onVerAsistencia = { pestana = PestanaFicha.ASISTENCIA },
            )

            PestanaFicha.ASISTENCIA -> ContenidoAsistencia(resumen = resumen, onVerHistorial = onVerHistorial)

            PestanaFicha.FICHA -> {
                SeccionPlegable(
                    titulo = "Datos deportivos",
                    icono = Icons.Default.SportsSoccer,
                    vistaPrevia = listOf(jugador.equipoNombre, jugador.posicion).filter { it.isNotBlank() }.joinToString(", "),
                    abiertaAlInicio = true,
                ) {
                    DatoSiExiste("Equipo", jugador.equipoNombre)
                    DatoJugador("Posición principal", jugador.posicion)
                    if (PosicionJugador.entries.none { it.etiqueta == jugador.posicion } && jugador.posicion.isNotBlank()) {
                        Text(
                            "Posición del catálogo anterior: edita la ficha para elegir una de las 9 posiciones actuales.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                    DatoSiExiste("Posición secundaria", jugador.posicionSecundaria)
                    DatoSiExiste("Pie hábil", jugador.pieHabil)
                    DatoSiExiste("Dorsal", jugador.dorsal?.toString())
                    if (jugador.excepcionCategoria) {
                        DatoJugador("Categoría", "Excepción: fuera del rango de años")
                    }
                }

                SeccionPlegable(
                    titulo = "Datos personales",
                    icono = Icons.Default.Badge,
                    vistaPrevia = jugador.edad()?.let { "$it años" },
                ) {
                    DatoSiExiste("Documento", jugador.documentoIdentidad)
                    DatoSiExiste("Nacimiento", FechasSportPro.fechaIso(jugador.fechaNacimiento))
                    DatoSiExiste("Edad", jugador.edad()?.let { "$it años" })
                    DatoSiExiste("Estatura", jugador.estaturaCm?.let { "${ValidacionJugador.centimetrosAMetros(it)} m" })
                    DatoSiExiste("Peso", jugador.pesoKg?.let { "$it kg" })
                }

                if (jugador.telefono.isNotBlank() || jugador.correo.isNotBlank()) {
                    SeccionPlegable(
                        titulo = "Contacto",
                        icono = Icons.Default.Phone,
                        vistaPrevia = jugador.telefono.ifBlank { jugador.correo },
                    ) {
                        DatoSiExiste("Teléfono", jugador.telefono)
                        DatoSiExiste("Correo", jugador.correo)
                        AccionesContacto(telefono = jugador.telefono, correo = jugador.correo)
                    }
                }

                SeccionPlegable(
                    titulo = if (jugador.esMenor()) "Apoderado y emergencia" else "Contacto de emergencia",
                    icono = Icons.Default.FamilyRestroom,
                    vistaPrevia = listOf(jugador.contactoEmergenciaNombre, jugador.contactoEmergenciaParentesco)
                        .filter { it.isNotBlank() }.joinToString(", ").ifBlank { "Sin registrar" },
                ) {
                    val sinDatos = jugador.contactoEmergenciaNombre.isBlank() && jugador.contactoEmergenciaTelefono.isBlank()
                    if (sinDatos) {
                        Text(
                            "No tiene contacto de emergencia. Agrégalo con Editar ficha.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DatoSiExiste("Nombre", jugador.contactoEmergenciaNombre)
                    DatoSiExiste("Parentesco", jugador.contactoEmergenciaParentesco)
                    DatoSiExiste("Teléfono", jugador.contactoEmergenciaTelefono)
                    DatoSiExiste("Correo del apoderado", jugador.apoderadoCorreo)
                    AccionesContacto(telefono = jugador.contactoEmergenciaTelefono, correo = jugador.apoderadoCorreo)
                    apoderados()
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(onClick = onCambiarEstado, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                        Text(if (jugador.activo) "Desactivar" else "Activar")
                    }
                    com.esan.sportpro.ui.common.BotonPrincipalSportPro(
                        texto = "Editar ficha",
                        onClick = onEditar,
                        icono = Icons.Default.Edit,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private fun colorPosicionOVerde(posicion: String): androidx.compose.ui.graphics.Color =
    if (posicion.isBlank()) com.esan.sportpro.ui.partido.ColoresPartido.Verde else com.esan.sportpro.ui.partido.colorPosicion(posicion)

/** Encabezado claro de la ficha: foto con anillo, nombre, posición, equipo y dorsal. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EncabezadoFicha(jugador: Jugador) {
    androidx.compose.material3.Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .background(fondoEncabezado(jugador.posicion))
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
        ) {
            androidx.compose.foundation.layout.Box {
                FotoConAnillo(jugador.fotoUrl, jugador.iniciales, jugador.posicion, tamano = 96.dp, nombre = jugador.nombreCompleto)
                jugador.dorsal?.let {
                    androidx.compose.foundation.layout.Box(Modifier.align(Alignment.BottomEnd)) { DorsalJugador(it, tamano = 40.dp) }
                }
            }
            Text(
                jugador.nombreCompleto,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InsigniaPosicion(jugador.posicion)
                Text(
                    listOf(jugador.posicion.ifBlank { "Sin posición" }, jugador.equipoNombre)
                        .filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (!jugador.activo) ChipEstado("Inactivo", TonoEstado.ERROR)
                jugador.edad()?.let { ChipEstado("$it años", TonoEstado.NEUTRO) }
                if (jugador.pieHabil.isNotBlank()) ChipEstado("Pie ${jugador.pieHabil.lowercase()}", TonoEstado.NEUTRO)
                if (jugador.esMenor()) ChipEstado("Menor", TonoEstado.INFORMACION)
            }
        }
    }
}

@Composable
private fun Seccion(titulo: String) = TituloSeccion(titulo)

@Composable
private fun FilaDato(etiqueta: String, valor: String?) = FilaDatoSportPro(etiqueta, valor)

/** Regla del apoderado: el registro del menor no está completo hasta que se vincule su apoderado. */
@Composable
private fun AvisoRegistroPendiente(jugador: Jugador) {
    TarjetaSportPro {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ChipEstado(EstadoRegistro.PENDIENTE_APODERADO.etiqueta, TonoEstado.ADVERTENCIA)
            Text(
                "Este jugador es menor de edad y aún no tiene un apoderado vinculado, así que su registro no está completo: " +
                    "no aparece en asistencia ni en convocatorias.",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (jugador.codigoVinculacion.isNotBlank()) {
                Text(
                    "Entrega el código ${jugador.codigoVinculacion} al apoderado. Cuando apruebes su solicitud, el registro se completará.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormularioJugadorContenido(
    state: JugadoresUiState,
    viewModel: JugadoresViewModel,
    modifier: Modifier = Modifier,
) {
    val formulario = state.formulario
    val datos = formulario.datos
    val errores = formulario.errores

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val verde = com.esan.sportpro.ui.partido.ColoresPartido.Verde
        BloqueJugador(
            titulo = "Foto y cuenta",
            icono = Icons.Default.PhotoCamera,
            color = verde,
            subtitulo = "Con la cuenta enlazada el jugador ve su perfil y sus partidos",
        ) {
            SelectorFotoJugador(
                fotoVisible = formulario.fotoVisible,
                iniciales = listOf(datos.nombres, datos.apellidos).mapNotNull { it.trim().firstOrNull()?.uppercaseChar() }.joinToString(""),
                onFotoElegida = viewModel::onFotoElegida,
                onQuitarFotoNueva = if (formulario.fotoNueva != null) viewModel::quitarFotoNueva else null,
            )
            // US-006: con la cuenta enlazada, el jugador ve su perfil, su historial, sus convocatorias
            // y el partido en vivo desde su propia cuenta.
            Etiqueta("Cuenta del jugador en la app")
            SelectorCuentaJugador(
                cuentas = state.cuentasDisponibles,
                seleccionada = formulario.usuarioUid,
                onElegir = viewModel::onCuentaElegida,
            )
        }

        BloqueJugador(
            titulo = "Datos personales",
            icono = Icons.Default.Badge,
            color = com.esan.sportpro.ui.partido.ColoresPartido.Morado,
        ) {
            CampoFormulario(
                valor = datos.nombres,
                onCambio = { v -> viewModel.onCampoChanged(CampoJugador.NOMBRES) { copy(nombres = v) } },
                etiqueta = "Nombres",
                error = errores[CampoJugador.NOMBRES],
            )
            CampoFormulario(
                valor = datos.apellidos,
                onCambio = { v -> viewModel.onCampoChanged(CampoJugador.APELLIDOS) { copy(apellidos = v) } },
                etiqueta = "Apellidos",
                error = errores[CampoJugador.APELLIDOS],
            )
            CampoFecha(
                valorIso = datos.fechaNacimiento,
                onCambio = { v -> viewModel.onCampoChanged(CampoJugador.FECHA_NACIMIENTO) { copy(fechaNacimiento = v) } },
                etiqueta = "Fecha de nacimiento",
                error = errores[CampoJugador.FECHA_NACIMIENTO],
                ayuda = "Entre ${ValidacionJugador.EDAD_MINIMA} y ${ValidacionJugador.EDAD_MAXIMA} años",
                fechaMinima = ValidacionJugador.fechaNacimientoMinima(),
                fechaMaxima = ValidacionJugador.fechaNacimientoMaxima(),
            )
            CampoFormulario(
                valor = datos.documento,
                onCambio = { v -> viewModel.onCampoChanged(CampoJugador.DOCUMENTO) { copy(documento = v) } },
                etiqueta = "Documento de identidad (DNI o CE)",
                error = errores[CampoJugador.DOCUMENTO],
            )
        }

        BloqueJugador(
            titulo = "Equipo y posición",
            icono = Icons.Default.SportsSoccer,
            color = colorPosicionOVerde(datos.posicion),
        ) {
            Etiqueta("Equipo")
            if (state.equiposActivos.isEmpty()) {
                Text("No hay equipos activos. Crea uno en Academia.", style = MaterialTheme.typography.bodySmall)
            }
            com.esan.sportpro.ui.common.SelectorEquipo(
                equipos = state.equiposActivos,
                seleccionadoId = datos.equipoId.ifBlank { null },
                onSeleccionar = { id -> id?.let { viewModel.onCampoChanged(CampoJugador.EQUIPO) { copy(equipoId = it) } } },
                permitirTodos = false,
                error = errores[CampoJugador.EQUIPO] != null,
            )
            errores[CampoJugador.EQUIPO]?.let { ErrorTexto(it) }

            // Posiciones: 9 opciones, mejor en un combo que en tres filas de chips.
            Etiqueta("Posición principal")
            if (datos.posicion.isNotBlank() && PosicionJugador.entries.none { it.etiqueta == datos.posicion }) {
                Text(
                    "Tenía \"${datos.posicion}\" (catálogo anterior). Elige una de las posiciones actuales.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            com.esan.sportpro.ui.common.SelectorOpciones(
                opciones = PosicionJugador.entries,
                seleccionada = PosicionJugador.entries.firstOrNull { it.etiqueta == datos.posicion },
                texto = { it.etiqueta },
                onSeleccionar = { p ->
                    p?.let {
                        viewModel.onCampoChanged(CampoJugador.POSICION) {
                            // Si la secundaria era la misma, se libera.
                            copy(
                                posicion = it.etiqueta,
                                posicionSecundaria = if (posicionSecundaria == it.etiqueta) "" else posicionSecundaria,
                            )
                        }
                    }
                },
                placeholder = "Elige la posición",
                error = errores[CampoJugador.POSICION] != null,
                icono = { InsigniaPosicion(it?.etiqueta) },
            )
            errores[CampoJugador.POSICION]?.let { ErrorTexto(it) }

            Etiqueta("Posición secundaria (opcional)")
            com.esan.sportpro.ui.common.SelectorOpciones(
                opciones = PosicionJugador.entries.filter { it.etiqueta != datos.posicion },
                seleccionada = PosicionJugador.entries.firstOrNull { it.etiqueta == datos.posicionSecundaria },
                texto = { it.etiqueta },
                onSeleccionar = { p ->
                    viewModel.onCampoChanged(CampoJugador.POSICION_SECUNDARIA) { copy(posicionSecundaria = p?.etiqueta.orEmpty()) }
                },
                opcionVacia = "Ninguna",
                error = errores[CampoJugador.POSICION_SECUNDARIA] != null,
                icono = { InsigniaPosicion(it?.etiqueta) },
            )
            errores[CampoJugador.POSICION_SECUNDARIA]?.let { ErrorTexto(it) }

            // Pie hábil: solo 3 opciones, se queda como botones (un toque).
            Etiqueta("Pie hábil")
            SelectorPieHabil(
                seleccionado = datos.pieHabil,
                onElegir = { pie -> viewModel.onCampoChanged(CampoJugador.PIE_HABIL) { copy(pieHabil = pie) } },
            )
            errores[CampoJugador.PIE_HABIL]?.let { ErrorTexto(it) }

            CampoFormulario(
                valor = datos.dorsal,
                onCambio = { v -> viewModel.onCampoChanged(CampoJugador.DORSAL) { copy(dorsal = v) } },
                etiqueta = "Número de camiseta (opcional)",
                error = errores[CampoJugador.DORSAL],
                ayuda = "De 1 a 99, único en el equipo",
                teclado = KeyboardType.Number,
            )
        }

        BloqueJugador(
            titulo = "Datos físicos",
            icono = Icons.Default.Straighten,
            color = com.esan.sportpro.ui.partido.ColoresPartido.Azul,
            subtitulo = "Opcionales",
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    CampoFormulario(
                        valor = datos.estatura,
                        onCambio = { v -> viewModel.onCampoChanged(CampoJugador.ESTATURA) { copy(estatura = v) } },
                        etiqueta = "Estatura (m)",
                        error = errores[CampoJugador.ESTATURA],
                        ayuda = "1,00 a 2,30",
                        teclado = KeyboardType.Decimal,
                    )
                }
                Column(Modifier.weight(1f)) {
                    CampoFormulario(
                        valor = datos.peso,
                        onCambio = { v -> viewModel.onCampoChanged(CampoJugador.PESO) { copy(peso = v) } },
                        etiqueta = "Peso (kg)",
                        error = errores[CampoJugador.PESO],
                        ayuda = "20 a 150",
                        teclado = KeyboardType.Decimal,
                    )
                }
            }
        }

        BloqueJugador(
            titulo = "Contacto",
            icono = Icons.Default.Phone,
            color = verde,
            subtitulo = "Opcional",
        ) {
            CampoFormulario(
                valor = datos.telefono,
                onCambio = { v -> viewModel.onCampoChanged(CampoJugador.TELEFONO) { copy(telefono = v) } },
                etiqueta = "Teléfono",
                error = errores[CampoJugador.TELEFONO],
                teclado = KeyboardType.Phone,
            )
            CampoFormulario(
                valor = datos.correo,
                onCambio = { v -> viewModel.onCampoChanged(CampoJugador.CORREO) { copy(correo = v) } },
                etiqueta = "Correo",
                error = errores[CampoJugador.CORREO],
                teclado = KeyboardType.Email,
            )
        }

        val esMenor = ValidacionJugador.esMenor(datos.fechaNacimiento)
        BloqueJugador(
            titulo = if (esMenor) "Apoderado" else "Contacto de emergencia",
            icono = Icons.Default.FamilyRestroom,
            color = com.esan.sportpro.ui.partido.ColoresPartido.Naranja,
            subtitulo = if (esMenor) {
                "Obligatorio: el jugador quedará registrado cuando su apoderado se vincule con el código que genera la app"
            } else {
                "Opcional"
            },
        ) {
            CampoFormulario(
                valor = datos.emergenciaNombre,
                onCambio = { v -> viewModel.onCampoChanged(CampoJugador.EMERGENCIA_NOMBRE) { copy(emergenciaNombre = v) } },
                etiqueta = "Nombre completo",
                error = errores[CampoJugador.EMERGENCIA_NOMBRE],
            )
            if (esMenor) {
                Etiqueta("Parentesco")
                com.esan.sportpro.ui.common.SelectorOpciones(
                    opciones = ReglasApoderado.PARENTESCOS_APODERADO,
                    seleccionada = datos.emergenciaParentesco.takeIf { it in ReglasApoderado.PARENTESCOS_APODERADO },
                    texto = { it },
                    onSeleccionar = { p ->
                        p?.let { viewModel.onCampoChanged(CampoJugador.EMERGENCIA_PARENTESCO) { copy(emergenciaParentesco = it) } }
                    },
                    placeholder = "Elige el parentesco",
                    error = errores[CampoJugador.EMERGENCIA_PARENTESCO] != null,
                )
                errores[CampoJugador.EMERGENCIA_PARENTESCO]?.let { ErrorTexto(it) }
            } else {
                CampoFormulario(
                    valor = datos.emergenciaParentesco,
                    onCambio = { v -> viewModel.onCampoChanged(CampoJugador.EMERGENCIA_PARENTESCO) { copy(emergenciaParentesco = v) } },
                    etiqueta = "Parentesco (ej. Madre)",
                    error = errores[CampoJugador.EMERGENCIA_PARENTESCO],
                )
            }
            CampoFormulario(
                valor = datos.emergenciaTelefono,
                onCambio = { v -> viewModel.onCampoChanged(CampoJugador.EMERGENCIA_TELEFONO) { copy(emergenciaTelefono = v) } },
                etiqueta = if (esMenor) "Teléfono del apoderado" else "Teléfono de emergencia",
                error = errores[CampoJugador.EMERGENCIA_TELEFONO],
                teclado = KeyboardType.Phone,
            )
            CampoFormulario(
                valor = datos.apoderadoCorreo,
                onCambio = { v -> viewModel.onCampoChanged(CampoJugador.APODERADO_CORREO) { copy(apoderadoCorreo = v) } },
                etiqueta = if (esMenor) "Correo del apoderado" else "Correo del contacto (opcional)",
                error = errores[CampoJugador.APODERADO_CORREO],
                teclado = KeyboardType.Email,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = viewModel::cancelarFormulario,
                enabled = !formulario.guardando,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) { Text("Cancelar") }
            com.esan.sportpro.ui.common.BotonPrincipalSportPro(
                texto = "Guardar",
                onClick = viewModel::guardar,
                habilitado = !formulario.guardando,
                cargando = formulario.guardando,
                icono = Icons.Default.Check,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun Etiqueta(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ErrorTexto(texto: String) {
    Text(texto, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}

/** Lista desplegable con las cuentas de rol Jugador libres (o la ya enlazada a esta ficha). */
@Composable
private fun SelectorCuentaJugador(
    cuentas: List<com.esan.sportpro.domain.jugadores.CuentaJugador>,
    seleccionada: String,
    onElegir: (String?) -> Unit,
) {
    var abierto by remember { androidx.compose.runtime.mutableStateOf(false) }
    val actual = cuentas.firstOrNull { it.uid == seleccionada }
    Column {
        androidx.compose.material3.OutlinedButton(
            onClick = { abierto = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                when {
                    actual != null -> "${actual.nombre.ifBlank { actual.correo }} · ${actual.correo}"
                    seleccionada.isNotBlank() -> "Cuenta enlazada (no disponible en la lista)"
                    else -> "Sin cuenta enlazada"
                },
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        androidx.compose.material3.DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            androidx.compose.material3.DropdownMenuItem(
                text = { Text("Sin cuenta enlazada") },
                onClick = { abierto = false; onElegir(null) },
            )
            cuentas.forEach { cuenta ->
                androidx.compose.material3.DropdownMenuItem(
                    text = {
                        Column {
                            Text(cuenta.nombre.ifBlank { "Sin nombre" })
                            Text(cuenta.correo, style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    onClick = { abierto = false; onElegir(cuenta.uid) },
                )
            }
        }
        Text(
            if (cuentas.isEmpty()) "No hay cuentas de jugador libres en la academia. El jugador debe registrarse con el rol Jugador."
            else "Elige la cuenta con la que el jugador entra a la app.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
