package com.esan.sportpro.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups2
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.cuentas.PerfilActual
import com.esan.sportpro.navigation.HomeModule
import com.esan.sportpro.navigation.MenuPrincipal
import com.esan.sportpro.navigation.PestanaNavegacion
import com.esan.sportpro.navigation.TipoPestana
import com.esan.sportpro.navigation.UserRole
import com.esan.sportpro.ui.academia.AcademiaScreen
import com.esan.sportpro.ui.common.AvatarIniciales
import com.esan.sportpro.ui.common.BarraSuperiorSportPro
import com.esan.sportpro.ui.common.CargandoSportPro
import com.esan.sportpro.ui.common.EncabezadoModulo
import com.esan.sportpro.ui.common.EstadoVacioModulo
import com.esan.sportpro.ui.common.IconoCircular
import com.esan.sportpro.ui.common.TarjetaSportPro
import com.esan.sportpro.ui.common.TituloSeccion
import com.esan.sportpro.ui.comunidad.ComunidadScreen
import com.esan.sportpro.ui.entrenamientos.EntrenamientosScreen
import com.esan.sportpro.ui.estadisticas.EstadisticasComunicacionEntryScreen
import com.esan.sportpro.ui.ia.IaScreen
import com.esan.sportpro.ui.jugadores.JugadoresScreen
import com.esan.sportpro.ui.mensualidades.MensualidadesEntryScreen
import com.esan.sportpro.ui.partido.PartidoScreen

/**
 * Punto de entrada tras el login (US-003: navegación principal diferenciada por rol).
 *
 * Barra inferior de **4 pestañas** por rol ([MenuPrincipal]): Inicio, una sección propia del rol
 * (con pestañas internas si agrupa varios módulos), Partidos y Más. Cada módulo vuelve a validar
 * el rol por su cuenta y `firestore.rules` lo exige en el servidor: ocultar una pestaña no es la
 * única barrera.
 */
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsState()

    when (val actual = estado) {
        is HomeUiState.Cargando -> CargandoSportPro(mensaje = "Cargando tu perfil…")

        is HomeUiState.SinPerfil -> PerfilNoDisponible(
            onReintentar = viewModel::cargarPerfil,
            onLogout = onLogout,
        )

        is HomeUiState.Listo -> HomeConPerfil(perfil = actual.perfil, onLogout = onLogout)
    }
}

@Composable
private fun HomeConPerfil(perfil: PerfilActual, onLogout: () -> Unit) {
    val pestanas = MenuPrincipal.para(perfil.rol)
    var pestanaId by rememberSaveable { mutableStateOf(pestanas.first().id) }
    // Módulo elegido dentro de cada sección con pestañas internas: "idPestana" -> nombre del módulo.
    var moduloPorSeccion by rememberSaveable { mutableStateOf(mapOf<String, String>()) }
    // Módulo abierto desde "Más" (null = se ve la lista de accesos).
    var moduloEnMas by rememberSaveable { mutableStateOf<String?>(null) }

    // Si cambió el rol (otra cuenta en el mismo dispositivo) y la pestaña guardada ya no existe.
    val pestana = pestanas.firstOrNull { it.id == pestanaId } ?: pestanas.first()

    fun abrirModulo(modulo: HomeModule) {
        val destino = MenuPrincipal.pestanaDe(perfil.rol, modulo) ?: return
        pestanaId = destino.id
        when (destino.tipo) {
            TipoPestana.SECCION -> moduloPorSeccion = moduloPorSeccion + (destino.id to modulo.name)
            TipoPestana.MAS -> moduloEnMas = modulo.name
            TipoPestana.INICIO -> Unit
        }
    }

    // Atrás desde cualquier pestaña vuelve a Inicio antes de salir de la app.
    BackHandler(enabled = pestana.tipo != TipoPestana.INICIO && !(pestana.tipo == TipoPestana.MAS && moduloEnMas != null)) {
        pestanaId = pestanas.first().id
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
                pestanas.forEach { item ->
                    NavigationBarItem(
                        selected = item.id == pestana.id,
                        onClick = {
                            // Tocar "Más" estando en "Más" vuelve a la lista de accesos.
                            if (item.id == pestana.id && item.tipo == TipoPestana.MAS) moduloEnMas = null
                            pestanaId = item.id
                        },
                        icon = { Icon(iconoPestana(item, perfil.rol), contentDescription = null) },
                        label = { Text(item.label, maxLines = 1) },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        val contenido = Modifier.padding(padding)
        when (pestana.tipo) {
            TipoPestana.INICIO -> InicioScreen(
                perfil = perfil,
                modulos = pestanas.flatMap { it.modulos },
                onAbrirModulo = ::abrirModulo,
                modifier = contenido,
            )

            TipoPestana.SECCION -> Seccion(
                pestana = pestana,
                perfil = perfil,
                moduloElegido = moduloPorSeccion[pestana.id],
                onElegir = { modulo -> moduloPorSeccion = moduloPorSeccion + (pestana.id to modulo.name) },
                modifier = contenido,
            )

            TipoPestana.MAS -> {
                val abierto = pestana.modulos.firstOrNull { it.name == moduloEnMas }
                if (abierto == null) {
                    MasScreen(
                        perfil = perfil,
                        modulos = pestana.modulos,
                        onAbrir = { moduloEnMas = it.name },
                        onLogout = onLogout,
                        modifier = contenido,
                    )
                } else {
                    BackHandler { moduloEnMas = null }
                    Column(modifier = contenido.fillMaxSize()) {
                        BarraSuperiorSportPro(
                            title = { Text(etiquetaModulo(abierto, perfil.rol)) },
                            navigationIcon = {
                                IconButton(onClick = { moduloEnMas = null }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver a Más")
                                }
                            },
                        )
                        ContenidoModulo(abierto, perfil, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** Sección de la barra inferior: un módulo, o varios con pestañas internas desplazables. */
@Composable
private fun Seccion(
    pestana: PestanaNavegacion,
    perfil: PerfilActual,
    moduloElegido: String?,
    onElegir: (HomeModule) -> Unit,
    modifier: Modifier = Modifier,
) {
    val modulos = pestana.modulos
    val actual = modulos.firstOrNull { it.name == moduloElegido } ?: modulos.first()
    if (modulos.size == 1) {
        ContenidoModulo(actual, perfil, modifier)
        return
    }
    Column(modifier = modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = modulos.indexOf(actual),
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            modulos.forEach { modulo ->
                Tab(
                    selected = modulo == actual,
                    onClick = { onElegir(modulo) },
                    text = { Text(etiquetaModulo(modulo, perfil.rol), maxLines = 1) },
                    icon = { Icon(iconoDe(modulo), contentDescription = null) },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        ContenidoModulo(actual, perfil, Modifier.weight(1f))
    }
}

@Composable
private fun ContenidoModulo(modulo: HomeModule, perfil: PerfilActual, modifier: Modifier) {
    when (modulo) {
        HomeModule.ACADEMIA -> AcademiaScreen(modifier)
        HomeModule.JUGADORES -> JugadoresScreen(modifier)
        HomeModule.ENTRENAMIENTOS -> EntrenamientosScreen(modifier)
        HomeModule.PARTIDO -> PartidoScreen(modifier)
        HomeModule.MENSUALIDADES -> MensualidadesEntryScreen(modifier = modifier, rolActual = perfil.rol)
        HomeModule.ESTADISTICAS -> EstadisticasComunicacionEntryScreen(modifier = modifier)
        HomeModule.COMUNIDAD -> ComunidadScreen(modifier)
        HomeModule.IA -> IaScreen(modifier)
    }
}

/** Pestaña "Más": perfil del usuario, módulos secundarios y cierre de sesión. */
@Composable
private fun MasScreen(
    perfil: PerfilActual,
    modulos: List<HomeModule>,
    onAbrir: (HomeModule) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            EncabezadoModulo(titulo = "Más opciones", subtitulo = "Módulos y cuenta", icono = Icons.Default.Menu)
        }
        item {
            TarjetaSportPro {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val iniciales = perfil.nombre.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1).uppercase() }
                    AvatarIniciales(iniciales, tamano = 52)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(perfil.nombre, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(etiquetaRol(perfil.rol), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item { TituloSeccion("Módulos") }
        items(modulos) { modulo ->
            TarjetaSportPro(onClick = { onAbrir(modulo) }) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    IconoCircular(iconoDe(modulo))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(etiquetaModulo(modulo, perfil.rol), style = MaterialTheme.typography.titleSmall)
                        Text(descripcionModulo(modulo), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { TituloSeccion("Cuenta") }
        item {
            OutlinedButton(onClick = onLogout, modifier = Modifier.padding(top = 4.dp)) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Text("Cerrar sesión", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

/** El usuario autenticado no tiene perfil legible: se le deja reintentar o salir, nunca bloqueado. */
@Composable
private fun PerfilNoDisponible(onReintentar: () -> Unit, onLogout: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        EstadoVacioModulo(
            titulo = "No pudimos cargar tu perfil",
            mensaje = "Revisa tu conexión e inténtalo de nuevo.",
            icono = Icons.Default.Warning,
        )
        Button(onClick = onReintentar) { Text("Reintentar") }
        OutlinedButton(onClick = onLogout, modifier = Modifier.padding(top = 8.dp)) { Text("Cerrar sesión") }
    }
}

internal fun etiquetaRol(rol: UserRole): String = when (rol) {
    UserRole.ADMINISTRADOR -> "Administrador"
    UserRole.ENTRENADOR -> "Entrenador"
    UserRole.JUGADOR -> "Jugador"
    UserRole.PADRE_DE_FAMILIA -> "Padre de familia"
}

/** Nombre del módulo para el rol: para el padre, "Jugadores" son sus hijos. */
internal fun etiquetaModulo(modulo: HomeModule, rol: UserRole): String =
    if (modulo == HomeModule.JUGADORES && rol == UserRole.PADRE_DE_FAMILIA) "Mis hijos" else modulo.label

internal fun descripcionModulo(modulo: HomeModule): String = when (modulo) {
    HomeModule.ACADEMIA -> "Equipos y categorías"
    HomeModule.JUGADORES -> "Perfiles y apoderados"
    HomeModule.ENTRENAMIENTOS -> "Sesiones, ejercicios y asistencia"
    HomeModule.PARTIDO -> "Programación, convocatoria y partido en vivo"
    HomeModule.MENSUALIDADES -> "Registro simulado de pagos"
    HomeModule.ESTADISTICAS -> "Estadísticas y anuncios"
    HomeModule.COMUNIDAD -> "Publicaciones y avisos de pruebas"
    HomeModule.IA -> "Resumen narrativo del partido"
}

internal fun iconoDe(modulo: HomeModule): ImageVector = when (modulo) {
    HomeModule.ACADEMIA -> Icons.Default.School
    HomeModule.JUGADORES -> Icons.Default.Group
    HomeModule.ENTRENAMIENTOS -> Icons.Default.FitnessCenter
    HomeModule.PARTIDO -> Icons.Default.SportsSoccer
    HomeModule.MENSUALIDADES -> Icons.Default.Payments
    HomeModule.ESTADISTICAS -> Icons.Default.BarChart
    HomeModule.COMUNIDAD -> Icons.Default.Newspaper
    HomeModule.IA -> Icons.Default.Psychology
}

private fun iconoPestana(pestana: PestanaNavegacion, rol: UserRole): ImageVector = when (pestana.tipo) {
    TipoPestana.INICIO -> Icons.Default.Home
    TipoPestana.MAS -> Icons.Default.Menu
    TipoPestana.SECCION -> when {
        pestana.modulos == listOf(HomeModule.PARTIDO) -> Icons.Default.SportsSoccer
        rol == UserRole.PADRE_DE_FAMILIA -> Icons.Default.FamilyRestroom
        pestana.modulos.size == 1 -> iconoDe(pestana.modulos.first())
        else -> Icons.Default.Groups2
    }
}
