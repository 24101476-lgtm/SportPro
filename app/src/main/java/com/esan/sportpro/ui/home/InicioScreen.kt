package com.esan.sportpro.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.core.fecha.FechasSportPro
import com.esan.sportpro.core.theme.EstilosSportPro
import com.esan.sportpro.core.theme.PaletaSportPro
import com.esan.sportpro.core.theme.SportProTema
import com.esan.sportpro.domain.cuentas.PerfilActual
import com.esan.sportpro.domain.entrenamientos.Entrenamiento
import com.esan.sportpro.domain.partido.EstadoPartido
import com.esan.sportpro.domain.partido.Partido
import com.esan.sportpro.navigation.HomeModule
import com.esan.sportpro.ui.common.ChipEstado
import com.esan.sportpro.ui.common.DatoEstadistica
import com.esan.sportpro.ui.common.EscudoEquipo
import com.esan.sportpro.ui.common.FilaEstadisticas
import com.esan.sportpro.ui.common.FondoFotoSportPro
import com.esan.sportpro.ui.common.FotoSportPro
import com.esan.sportpro.ui.common.IconoCircular
import com.esan.sportpro.ui.common.TarjetaSportPro
import com.esan.sportpro.ui.common.TituloSeccion
import com.esan.sportpro.ui.common.TonoEstado
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Pantalla de Inicio de cada rol: encabezado con foto y saludo, el próximo partido con los escudos
 * frente a frente, indicadores del plantel (personal), próximos entrenamientos en tarjetas
 * deslizables y accesos rápidos en grilla asimétrica.
 * Solo cambia el aspecto: los datos salen de [InicioViewModel] como antes.
 */
@Composable
fun InicioScreen(
    perfil: PerfilActual,
    modulos: List<HomeModule>,
    onAbrirModulo: (HomeModule) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InicioViewModel = hiltViewModel(),
) {
    LaunchedEffect(perfil.uid) { viewModel.iniciar(perfil) }
    val state by viewModel.uiState.collectAsState()
    val hoy = FechasSportPro.hoy()
    val margen = Modifier.padding(horizontal = 20.dp)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            // El encabezado y la tarjeta del partido se superponen: la tarjeta sube sobre la foto.
            Box(modifier = Modifier.fillMaxWidth()) {
                Encabezado(perfil = perfil, fecha = FechasSportPro.fechaLarga(hoy).replaceFirstChar { it.uppercase() })
                if (HomeModule.PARTIDO in modulos) {
                    TarjetaProximoPartido(
                        partido = state.proximoPartido,
                        onClick = { onAbrirModulo(HomeModule.PARTIDO) },
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 236.dp),
                    )
                }
            }
        }

        if (perfil.esStaff) {
            item {
                FilaEstadisticas(
                    datos = listOf(
                        DatoEstadistica(state.equiposActivos?.toString() ?: "–", "Equipos"),
                        DatoEstadistica(state.jugadoresActivos?.toString() ?: "–", "Jugadores"),
                        DatoEstadistica(state.proximosEntrenamientos.size.toString(), "Próximos entrenos"),
                    ),
                    modifier = margen,
                )
            }
            val pendientes = state.pendientesApoderado ?: 0
            if (pendientes > 0) {
                item {
                    TarjetaSportPro(onClick = { onAbrirModulo(HomeModule.JUGADORES) }, modifier = margen) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            IconoCircular(Icons.Default.FamilyRestroom)
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Registros pendientes", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "$pendientes ${if (pendientes == 1) "menor espera" else "menores esperan"} la vinculación de su apoderado.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            ChipEstado("Pendiente", TonoEstado.ADVERTENCIA)
                        }
                    }
                }
            }
        }

        if (HomeModule.ENTRENAMIENTOS in modulos) {
            item { TituloSeccion("Próximos entrenamientos", modifier = margen) }
            item {
                if (state.proximosEntrenamientos.isEmpty()) {
                    TextoVacio("No hay entrenamientos programados. Créalos desde Entrenamientos.", modifier = margen)
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        itemsIndexed(state.proximosEntrenamientos, key = { _, e -> e.id }) { indice, entrenamiento ->
                            TarjetaEntrenamiento(
                                entrenamiento = entrenamiento,
                                conFoto = indice == 0,
                                onClick = { onAbrirModulo(HomeModule.ENTRENAMIENTOS) },
                            )
                        }
                    }
                }
            }
        }

        if (modulos.isNotEmpty()) {
            item { TituloSeccion("Accesos rápidos", modifier = margen) }
            item {
                AccesosRapidos(modulos = modulos, perfil = perfil, onAbrirModulo = onAbrirModulo, modifier = margen)
            }
        }
    }
}

/** Foto del entrenador en B/N con velo musgo, avatar, saludo grande y la única nota a mano. */
@Composable
private fun Encabezado(perfil: PerfilActual, fecha: String) {
    val nombre = perfil.nombre.substringBefore(' ').ifBlank { "de nuevo" }
    val iniciales = perfil.nombre.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1).uppercase() }
    FondoFotoSportPro(
        foto = FotoSportPro.ENTRENADOR,
        forma = RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp),
        velo = Brush.verticalGradient(
            0f to PaletaSportPro.MusgoProfundo.copy(alpha = 0.40f),
            1f to PaletaSportPro.MusgoProfundo.copy(alpha = 0.90f),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
    ) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.18f), modifier = Modifier.size(44.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(iniciales.ifBlank { "?" }, color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                }
                Column {
                    Text(etiquetaRol(perfil.rol), color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(fecha, color = Color.White.copy(alpha = 0.80f), style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(
                "Hola,\n$nombre",
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 22.dp, end = 96.dp),
            )
        }
        Text(
            "A darle\ntodo hoy",
            style = EstilosSportPro.NotaMano,
            color = PaletaSportPro.Lima,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 104.dp, end = 24.dp)
                .rotate(-7f),
        )
    }
}

/**
 * Próximo partido: tarjeta lima con degradado (igual en ambos modos), los escudos frente a frente
 * y la fecha (o el marcador si está en vivo) al centro. El texto es carbón porque va sobre lima.
 */
@Composable
private fun TarjetaProximoPartido(partido: Partido?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val carbon = PaletaSportPro.Carbon
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        contentColor = carbon,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(PaletaSportPro.LimaSuave, PaletaSportPro.Lima)))
                .padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val enVivo = partido?.estado == EstadoPartido.EN_CURSO
                Surface(shape = CircleShape, color = carbon) {
                    Text(
                        if (enVivo) "En vivo" else "Próximo partido",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = PaletaSportPro.Lima,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                partido?.categoria?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }
            }

            if (partido == null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(top = 18.dp, bottom = 6.dp),
                ) {
                    Icon(Icons.Default.SportsSoccer, contentDescription = null, modifier = Modifier.size(40.dp))
                    Column {
                        Text("Sin partidos por ahora", style = MaterialTheme.typography.headlineSmall)
                        Text("Prográmalos desde Partidos.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 14.dp, bottom = 12.dp),
                ) {
                    EquipoPartido(nombre = partido.nombreEquipo, modifier = Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 6.dp)) {
                        if (partido.estado == EstadoPartido.EN_CURSO) {
                            Text("${partido.golesEquipo} – ${partido.golesRival}", style = EstilosSportPro.NumeroGrande)
                        } else {
                            val fecha = partido.fechaMs?.let { Instant.ofEpochMilli(it).atZone(FechasSportPro.ZONA).toLocalDate() }
                            Text(fecha?.let { "${diaSemana(it)} ${it.dayOfMonth}" } ?: "Por definir", style = EstilosSportPro.NumeroMediano.copy(fontSize = EstilosSportPro.NumeroGrande.fontSize))
                            partido.hora?.takeIf { it.isNotBlank() }?.let {
                                Text(it, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                    EquipoPartido(nombre = partido.nombreRival, modifier = Modifier.weight(1f))
                }
            }

            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(carbon.copy(alpha = 0.14f)),
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                Text(
                    partido?.lugar?.takeIf { it.isNotBlank() } ?: if (partido == null) "Ir a Partidos" else "Lugar por confirmar",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Surface(shape = RoundedCornerShape(12.dp), color = carbon, modifier = Modifier.size(42.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.NorthEast, contentDescription = "Abrir partidos", tint = PaletaSportPro.Lima, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EquipoPartido(nombre: String, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        EscudoEquipo(nombre = nombre, tamano = 72.dp)
        Text(
            nombre,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Entrenamiento en tarjeta deslizable; la primera lleva la foto de los conos a color. */
@Composable
private fun TarjetaEntrenamiento(entrenamiento: Entrenamiento, conFoto: Boolean, onClick: () -> Unit) {
    val fecha = FechasSportPro.desdeIso(entrenamiento.fecha)
    val titulo = entrenamiento.objetivo.ifBlank { "Entrenamiento" }
    val detalle = listOf(entrenamiento.equipoNombre, entrenamiento.hora).filter { it.isNotBlank() }.joinToString(", ")
    val tamano = Modifier.size(width = 250.dp, height = 150.dp)

    if (conFoto) {
        Surface(onClick = onClick, shape = MaterialTheme.shapes.medium, color = Color.Transparent, modifier = tamano) {
            FondoFotoSportPro(
                foto = FotoSportPro.CONOS,
                aColor = true,
                forma = MaterialTheme.shapes.medium,
                velo = Brush.verticalGradient(0.35f to Color.Transparent, 1f to PaletaSportPro.Carbon.copy(alpha = 0.85f)),
                modifier = Modifier.fillMaxSize(),
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp),
                ) {
                    BloqueFecha(fecha, fondo = PaletaSportPro.Lima, texto = PaletaSportPro.Carbon, secundario = PaletaSportPro.Carbon)
                    Column {
                        Text(titulo, color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(detalle, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    } else {
        Surface(onClick = onClick, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, modifier = tamano) {
            Column(modifier = Modifier.padding(14.dp)) {
                BloqueFecha(
                    fecha,
                    fondo = MaterialTheme.colorScheme.surfaceVariant,
                    texto = SportProTema.colores.textoAcento,
                    secundario = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(detalle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun BloqueFecha(fecha: LocalDate?, fondo: Color, texto: Color, secundario: Color) {
    Surface(shape = RoundedCornerShape(14.dp), color = fondo, modifier = Modifier.size(width = 54.dp, height = 60.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(fecha?.dayOfMonth?.toString()?.padStart(2, '0') ?: "–", style = EstilosSportPro.NumeroMediano.copy(fontSize = EstilosSportPro.NumeroMediano.fontSize * 0.86f), color = texto)
            Text(fecha?.let { diaSemana(it) } ?: "", style = MaterialTheme.typography.labelSmall, color = secundario)
        }
    }
}

/** Foto que acompaña a cada módulo en la tarjeta grande de accesos rápidos. */
private fun fotoDe(modulo: HomeModule): FotoSportPro = when (modulo) {
    HomeModule.JUGADORES -> FotoSportPro.VESTUARIO
    HomeModule.ENTRENAMIENTOS -> FotoSportPro.CONOS
    HomeModule.PARTIDO -> FotoSportPro.JUVENILES
    HomeModule.COMUNIDAD -> FotoSportPro.CELEBRACION
    HomeModule.MENSUALIDADES -> FotoSportPro.BOTINES
    HomeModule.ESTADISTICAS -> FotoSportPro.ESTADIO
    HomeModule.ACADEMIA -> FotoSportPro.CANCHA
    HomeModule.IA -> FotoSportPro.BALON
}

/**
 * Accesos rápidos en grilla asimétrica, todos con foto a color suave y velo oscuro abajo:
 * el primer módulo en una tarjeta alta, los dos siguientes apilados a su lado y el resto
 * en tarjetas más bajas de a dos.
 */
@Composable
private fun AccesosRapidos(
    modulos: List<HomeModule>,
    perfil: PerfilActual,
    onAbrirModulo: (HomeModule) -> Unit,
    modifier: Modifier = Modifier,
) {
    val destacados = modulos.take(3)
    val resto = modulos.drop(3)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().height(244.dp)) {
            TarjetaAcceso(
                modulo = destacados.first(),
                perfil = perfil,
                onAbrirModulo = onAbrirModulo,
                grande = true,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f).fillMaxHeight()) {
                destacados.drop(1).forEach { modulo ->
                    TarjetaAcceso(modulo, perfil, onAbrirModulo, modifier = Modifier.weight(1f).fillMaxWidth())
                }
                if (destacados.size < 3) Spacer(modifier = Modifier.weight((3 - destacados.size).toFloat()))
            }
        }
        resto.chunked(2).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().height(96.dp)) {
                fila.forEach { modulo ->
                    TarjetaAcceso(modulo, perfil, onAbrirModulo, modifier = Modifier.weight(1f).fillMaxHeight())
                }
                if (fila.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

/** Tarjeta de acceso con la foto del módulo, su ícono y el nombre en blanco sobre el velo. */
@Composable
private fun TarjetaAcceso(
    modulo: HomeModule,
    perfil: PerfilActual,
    onAbrirModulo: (HomeModule) -> Unit,
    modifier: Modifier = Modifier,
    grande: Boolean = false,
) {
    Surface(
        onClick = { onAbrirModulo(modulo) },
        shape = MaterialTheme.shapes.medium,
        color = Color.Transparent,
        modifier = modifier,
    ) {
        FondoFotoSportPro(
            foto = fotoDe(modulo),
            aColor = true,
            forma = MaterialTheme.shapes.medium,
            velo = Brush.verticalGradient(
                0f to PaletaSportPro.Carbon.copy(alpha = 0.10f),
                1f to PaletaSportPro.Carbon.copy(alpha = if (grande) 0.88f else 0.80f),
            ),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (!grande) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.18f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .size(34.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(iconoDe(modulo), contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Text(
                etiquetaModulo(modulo, perfil.rol),
                style = if (grande) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleSmall,
                fontWeight = if (grande) null else FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
            )
        }
    }
}

@Composable
private fun TextoVacio(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

private val FORMATO_DIA_SEMANA: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE", Locale.forLanguageTag("es-PE"))

/** "Sáb", "Jue"… sin el punto que agrega el formato abreviado. */
private fun diaSemana(fecha: LocalDate): String =
    fecha.format(FORMATO_DIA_SEMANA).removeSuffix(".").replaceFirstChar { it.uppercase() }
