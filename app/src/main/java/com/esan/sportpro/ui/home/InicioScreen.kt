package com.esan.sportpro.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups2
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.core.fecha.FechasSportPro
import com.esan.sportpro.domain.cuentas.PerfilActual
import com.esan.sportpro.domain.entrenamientos.Entrenamiento
import com.esan.sportpro.domain.partido.Partido
import com.esan.sportpro.navigation.HomeModule
import com.esan.sportpro.ui.common.ChipEstado
import com.esan.sportpro.ui.common.EncabezadoModulo
import com.esan.sportpro.ui.common.IconoCircular
import com.esan.sportpro.ui.common.TarjetaSportPro
import com.esan.sportpro.ui.common.TituloSeccion
import com.esan.sportpro.ui.common.TonoEstado

/**
 * Pantalla de Inicio de cada rol: saludo, indicadores del plantel (personal), próximo partido,
 * próximos entrenamientos y accesos rápidos a todos los módulos que el rol puede usar.
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

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            EncabezadoModulo(
                titulo = "Hola, ${perfil.nombre.substringBefore(' ').ifBlank { "bienvenido" }}",
                subtitulo = "${etiquetaRol(perfil.rol)} · ${FechasSportPro.fechaLarga(hoy).replaceFirstChar { it.uppercase() }}",
                icono = Icons.Default.SportsSoccer,
            )
        }

        if (perfil.esStaff) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Indicador("Equipos", state.equiposActivos, Icons.Default.Groups2, Modifier.weight(1f))
                    Indicador("Jugadores", state.jugadoresActivos, Icons.Default.Group, Modifier.weight(1f))
                }
            }
            val pendientes = state.pendientesApoderado ?: 0
            if (pendientes > 0) {
                item {
                    TarjetaSportPro(onClick = { onAbrirModulo(HomeModule.JUGADORES) }) {
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

        if (HomeModule.PARTIDO in modulos) {
            item { TituloSeccion("Próximo partido") }
            item {
                val partido = state.proximoPartido
                if (partido == null) {
                    TextoVacio("No hay partidos programados.")
                } else {
                    TarjetaPartido(partido, onClick = { onAbrirModulo(HomeModule.PARTIDO) })
                }
            }
        }

        if (HomeModule.ENTRENAMIENTOS in modulos) {
            item { TituloSeccion("Próximos entrenamientos") }
            if (state.proximosEntrenamientos.isEmpty()) {
                item { TextoVacio("No hay entrenamientos programados.") }
            } else {
                items(state.proximosEntrenamientos, key = { it.id }) { entrenamiento ->
                    TarjetaEntrenamiento(entrenamiento, onClick = { onAbrirModulo(HomeModule.ENTRENAMIENTOS) })
                }
            }
        }

        item { TituloSeccion("Accesos rápidos") }
        items(modulos.chunked(2)) { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                fila.forEach { modulo ->
                    TarjetaSportPro(modifier = Modifier.weight(1f), onClick = { onAbrirModulo(modulo) }) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            IconoCircular(iconoDe(modulo))
                            Text(
                                etiquetaModulo(modulo, perfil.rol),
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                if (fila.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Indicador(titulo: String, valor: Int?, icono: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    TarjetaSportPro(modifier = modifier) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconoCircular(icono)
            Column {
                Text(valor?.toString() ?: "–", style = MaterialTheme.typography.headlineSmall)
                Text(titulo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TarjetaPartido(partido: Partido, onClick: () -> Unit) {
    TarjetaSportPro(onClick = onClick) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconoCircular(Icons.Default.SportsSoccer)
            Column(modifier = Modifier.weight(1f)) {
                Text("${partido.nombreEquipo} vs ${partido.nombreRival}", style = MaterialTheme.typography.titleSmall)
                val cuando = listOfNotNull(
                    partido.fechaMs?.let { FechasSportPro.fecha(it) },
                    partido.hora?.takeIf { it.isNotBlank() },
                    partido.lugar?.takeIf { it.isNotBlank() },
                ).joinToString(" · ")
                Text(
                    cuando.ifBlank { "Fecha por confirmar" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (partido.estado == com.esan.sportpro.domain.partido.EstadoPartido.EN_CURSO) {
                ChipEstado("En vivo", TonoEstado.ERROR)
            }
        }
    }
}

@Composable
private fun TarjetaEntrenamiento(entrenamiento: Entrenamiento, onClick: () -> Unit) {
    TarjetaSportPro(onClick = onClick) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconoCircular(Icons.Default.EventAvailable)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${FechasSportPro.fechaIso(entrenamiento.fecha)} · ${entrenamiento.hora}",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    "${entrenamiento.equipoNombre} — ${entrenamiento.objetivo}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun TextoVacio(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}
