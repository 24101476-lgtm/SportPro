package com.esan.sportpro.ui.jugadores

import com.esan.sportpro.ui.common.BarraSuperiorSportPro
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.academia.Equipo
import com.esan.sportpro.domain.comunidad.PosicionJugador
import com.esan.sportpro.domain.entrenamientos.ResumenAsistencia
import com.esan.sportpro.domain.jugadores.CampoJugador
import com.esan.sportpro.domain.jugadores.EstadoRegistro
import com.esan.sportpro.domain.jugadores.Jugador
import com.esan.sportpro.domain.jugadores.ReglasApoderado
import com.esan.sportpro.domain.jugadores.ValidacionJugador
import com.esan.sportpro.core.fecha.FechasSportPro
import com.esan.sportpro.ui.common.AvatarIniciales
import com.esan.sportpro.ui.common.CampoFecha
import com.esan.sportpro.ui.common.CampoFormulario
import com.esan.sportpro.ui.common.ChipEstado
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

    // US-007: el padre de familia ve aquí a sus hijos vinculados (con su propio ViewModel).
    if (state.esPadre) {
        MisHijosScreen(modifier = modifier)
        return
    }

    when {
        state.cargando -> Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) { CircularProgressIndicator() }

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
                            onEditar = { viewModel.editarJugador(jugador) },
                            onCambiarEstado = { viewModel.cambiarEstado(jugador) },
                            modifier = Modifier.padding(padding),
                            apoderados = { SeccionApoderados(state = state, jugador = jugador, viewModel = viewModel) },
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
            titulo = "Jugadores",
            subtitulo = "$registrados registrados" +
                if (pendientesApoderado > 0) " · $pendientesApoderado pendientes de apoderado" else "",
            icono = Icons.Default.Group,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
        )
        // US-007: aviso dentro de la app de las solicitudes pendientes (hasta que exista el push).
        val pendientes = state.solicitudesPendientes.size
        OutlinedButton(
            onClick = viewModel::abrirSolicitudes,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp),
        ) {
            Icon(Icons.Default.FamilyRestroom, contentDescription = null)
            Text(
                if (pendientes > 0) "  Solicitudes de vinculación ($pendientes pendientes)" else "  Solicitudes de vinculación",
            )
        }
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.filtroEquipoId == null,
                    onClick = { viewModel.onFiltroEquipo(null) },
                    label = { Text("Todos") },
                )
                state.equiposActivos.forEach { equipo ->
                    FilterChip(
                        selected = state.filtroEquipoId == equipo.id,
                        onClick = { viewModel.onFiltroEquipo(equipo.id) },
                        label = { Text(equipo.etiqueta) },
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Mostrar inactivos", style = MaterialTheme.typography.labelMedium)
                Switch(
                    checked = state.mostrarInactivos,
                    onCheckedChange = viewModel::onMostrarInactivos,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
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
    TarjetaSportPro(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AvatarIniciales(jugador.iniciales)
            Column(modifier = Modifier.weight(1f)) {
                Text(jugador.nombreCompleto, style = MaterialTheme.typography.titleMedium)
                Text(
                    listOfNotNull(jugador.dorsal?.let { "#$it" }, jugador.posicion, jugador.equipoNombre).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    if (ReglasApoderado.pendiente(jugador)) ChipEstado("Pendiente de apoderado", TonoEstado.ADVERTENCIA)
                    if (jugador.esMenor()) ChipEstado("Menor", TonoEstado.INFORMACION)
                    if (!jugador.activo) ChipEstado("Inactivo", TonoEstado.ERROR)
                }
            }
        }
    }
}

@Composable
private fun DetalleJugador(
    jugador: Jugador,
    resumen: ResumenAsistencia,
    onEditar: () -> Unit,
    onCambiarEstado: () -> Unit,
    modifier: Modifier = Modifier,
    apoderados: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (ReglasApoderado.pendiente(jugador)) {
            AvisoRegistroPendiente(jugador)
        }
        Seccion("Datos deportivos")
        FilaDato("Equipo", jugador.equipoNombre)
        FilaDato("Posición", jugador.posicion)
        FilaDato("Dorsal", jugador.dorsal?.toString())

        Seccion("Datos personales y físicos")
        FilaDato("Fecha de nacimiento", FechasSportPro.fechaIso(jugador.fechaNacimiento))
        FilaDato("Edad", jugador.edad()?.let { "$it años" })
        FilaDato("Estatura", jugador.estaturaCm?.let { "$it cm" })
        FilaDato("Peso", jugador.pesoKg?.let { "$it kg" })

        Seccion("Contacto")
        FilaDato("Teléfono", jugador.telefono.ifBlank { null })
        FilaDato("Correo", jugador.correo.ifBlank { null })

        Seccion(if (jugador.esMenor()) "Apoderado y contacto de emergencia" else "Contacto de emergencia")
        FilaDato("Nombre", jugador.contactoEmergenciaNombre)
        FilaDato("Parentesco", jugador.contactoEmergenciaParentesco)
        FilaDato("Teléfono", jugador.contactoEmergenciaTelefono)
        if (jugador.esMenor() || jugador.apoderadoCorreo.isNotBlank()) {
            FilaDato("Correo del apoderado", jugador.apoderadoCorreo)
        }

        Seccion("Asistencia a entrenamientos")
        if (resumen.registrados == 0) {
            Text("Todavía no hay entrenamientos con asistencia registrada.", style = MaterialTheme.typography.bodyMedium)
        } else {
            FilaDato("Participación", "${resumen.porcentaje}% (${resumen.participaciones} de ${resumen.registrados})")
            FilaDato("Presente / Tarde", "${resumen.presentes} / ${resumen.tardes}")
            FilaDato("Justificado / Ausente", "${resumen.justificados} / ${resumen.ausentes}")
        }

        apoderados()

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = onCambiarEstado, modifier = Modifier.weight(1f)) {
                Text(if (jugador.activo) "Desactivar" else "Activar")
            }
            Button(onClick = onEditar, modifier = Modifier.weight(1f)) { Text("Editar") }
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Seccion("Datos personales")
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

        Seccion("Equipo y posición")
        Text("Equipo", style = MaterialTheme.typography.labelLarge)
        if (state.equiposActivos.isEmpty()) {
            Text("No hay equipos activos. Crea uno en Academia.", style = MaterialTheme.typography.bodySmall)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.equiposActivos.forEach { equipo: Equipo ->
                FilterChip(
                    selected = datos.equipoId == equipo.id,
                    onClick = { viewModel.onCampoChanged(CampoJugador.EQUIPO) { copy(equipoId = equipo.id) } },
                    label = { Text(equipo.etiqueta) },
                )
            }
        }
        errores[CampoJugador.EQUIPO]?.let { ErrorTexto(it) }

        Text("Posición", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PosicionJugador.entries.forEach { posicion ->
                FilterChip(
                    selected = datos.posicion == posicion.etiqueta,
                    onClick = { viewModel.onCampoChanged(CampoJugador.POSICION) { copy(posicion = posicion.etiqueta) } },
                    label = { Text(posicion.etiqueta) },
                )
            }
        }
        errores[CampoJugador.POSICION]?.let { ErrorTexto(it) }

        CampoFormulario(
            valor = datos.dorsal,
            onCambio = { v -> viewModel.onCampoChanged(CampoJugador.DORSAL) { copy(dorsal = v) } },
            etiqueta = "Dorsal (opcional)",
            error = errores[CampoJugador.DORSAL],
            teclado = KeyboardType.Number,
        )

        Seccion("Datos físicos (opcionales)")
        CampoFormulario(
            valor = datos.estatura,
            onCambio = { v -> viewModel.onCampoChanged(CampoJugador.ESTATURA) { copy(estatura = v) } },
            etiqueta = "Estatura (cm)",
            error = errores[CampoJugador.ESTATURA],
            teclado = KeyboardType.Number,
        )
        CampoFormulario(
            valor = datos.peso,
            onCambio = { v -> viewModel.onCampoChanged(CampoJugador.PESO) { copy(peso = v) } },
            etiqueta = "Peso (kg)",
            error = errores[CampoJugador.PESO],
            teclado = KeyboardType.Decimal,
        )

        Seccion("Contacto (opcional)")
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

        val esMenor = ValidacionJugador.esMenor(datos.fechaNacimiento)
        Seccion(if (esMenor) "Apoderado (obligatorio para menores de edad)" else "Contacto de emergencia")
        if (esMenor) {
            Text(
                "El jugador quedará registrado cuando su apoderado se vincule con el código que genera la app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        CampoFormulario(
            valor = datos.emergenciaNombre,
            onCambio = { v -> viewModel.onCampoChanged(CampoJugador.EMERGENCIA_NOMBRE) { copy(emergenciaNombre = v) } },
            etiqueta = "Nombre completo",
            error = errores[CampoJugador.EMERGENCIA_NOMBRE],
        )
        if (esMenor) {
            Text("Parentesco", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReglasApoderado.PARENTESCOS_APODERADO.forEach { parentesco ->
                    FilterChip(
                        selected = datos.emergenciaParentesco == parentesco,
                        onClick = { viewModel.onCampoChanged(CampoJugador.EMERGENCIA_PARENTESCO) { copy(emergenciaParentesco = parentesco) } },
                        label = { Text(parentesco) },
                    )
                }
            }
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

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = viewModel::cancelarFormulario,
                enabled = !formulario.guardando,
                modifier = Modifier.weight(1f),
            ) { Text("Cancelar") }
            Button(
                onClick = viewModel::guardar,
                enabled = !formulario.guardando,
                modifier = Modifier.weight(1f),
            ) {
                if (formulario.guardando) {
                    CircularProgressIndicator(modifier = Modifier.padding(2.dp), strokeWidth = 2.dp)
                } else {
                    Text("Guardar")
                }
            }
        }
    }
}

@Composable
private fun ErrorTexto(texto: String) {
    Text(texto, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}
