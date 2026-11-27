package com.esan.sportpro.ui.jugadores

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.esan.sportpro.core.theme.EstilosSportPro
import com.esan.sportpro.core.theme.SportProTema
import com.esan.sportpro.domain.entrenamientos.ReglasHistorial
import com.esan.sportpro.domain.entrenamientos.ResumenAsistencia
import kotlin.math.roundToInt

/**
 * Ficha del jugador en pestañas. Antes todo se mostraba de golpe en siete bloques; ahora cada
 * pestaña responde una sola pregunta:
 *  - Resumen: ¿cómo le va? (3 números clave y el resto en una fila discreta)
 *  - Asistencia: ¿viene a entrenar?
 *  - Ficha: ¿cuáles son sus datos? (secciones plegables, sin filas vacías)
 */
internal enum class PestanaFicha(val etiqueta: String) {
    RESUMEN("Resumen"),
    ASISTENCIA("Asistencia"),
    FICHA("Ficha"),
}

/** Selector de pestañas segmentado, con el acento lima en la opción elegida. */
@Composable
internal fun SelectorPestanasFicha(
    actual: PestanaFicha,
    onElegir: (PestanaFicha) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            PestanaFicha.entries.forEach { pestana ->
                val elegida = pestana == actual
                Surface(
                    onClick = { onElegir(pestana) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (elegida) MaterialTheme.colorScheme.surface else Color.Transparent,
                    shadowElevation = if (elegida) 1.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .semantics {
                            role = Role.Tab
                            selected = elegida
                        },
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                        Text(
                            pestana.etiqueta,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (elegida) FontWeight.Bold else FontWeight.Medium,
                            color = if (elegida) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Pestaña Resumen
// ---------------------------------------------------------------------------------------------

/**
 * Tres números protagonistas (partidos, goles y % de asistencia) y, debajo, una fila discreta con
 * minutos, asistencias y tarjetas. El color se reserva para las tarjetas, que sí significan algo.
 */
@Composable
internal fun ContenidoResumen(
    resumenDeportivo: ResumenDeportivo?,
    asistencia: ResumenAsistencia,
    onVerAsistencia: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val porcentaje = if (asistencia.registrados == 0) {
            null
        } else {
            ReglasHistorial.porcentaje(asistencia.participaciones, asistencia.registrados)
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SportProTema.colores.acento.copy(alpha = 0.22f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(vertical = 20.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NumeroClave(resumenDeportivo?.partidos?.toString() ?: "…", "Partidos", Modifier.weight(1f))
                SeparadorVertical()
                NumeroClave(resumenDeportivo?.goles?.toString() ?: "…", "Goles", Modifier.weight(1f))
                SeparadorVertical()
                NumeroClave(
                    porcentaje?.let { porcentajeCorto(it) } ?: "—",
                    "Asistencia",
                    Modifier.weight(1f),
                )
            }
        }

        if (resumenDeportivo != null && resumenDeportivo.partidos > 0) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DatoPequeno("${resumenDeportivo.minutos}'", "Minutos")
                    DatoPequeno("${resumenDeportivo.asistencias}", "Asist. de gol")
                    DatoTarjeta(resumenDeportivo.amarillas, SportProTema.colores.advertencia, "Amarillas")
                    DatoTarjeta(resumenDeportivo.rojas, SportProTema.colores.error, "Rojas")
                }
            }
        } else if (resumenDeportivo != null) {
            Text(
                "Todavía no juega partidos finalizados. Sus goles y minutos aparecerán aquí.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        if (porcentaje != null) {
            OutlinedButton(onClick = onVerAsistencia, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Ver detalle de asistencia")
            }
        }
    }
}

@Composable
private fun NumeroClave(valor: String, etiqueta: String, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(
            valor,
            style = EstilosSportPro.NumeroGrande,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            etiqueta,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SeparadorVertical() {
    Box(
        Modifier
            .width(1.dp)
            .height(40.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
    )
}

@Composable
private fun DatoPequeno(valor: String, etiqueta: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(etiqueta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Número de tarjetas junto a una tarjetita dibujada del color real (amarilla o roja). */
@Composable
private fun DatoTarjeta(cantidad: Int, color: Color, etiqueta: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "$cantidad tarjetas $etiqueta".lowercase() },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(width = 10.dp, height = 14.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color),
            )
            Spacer(Modifier.width(6.dp))
            Text("$cantidad", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Text(etiqueta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ---------------------------------------------------------------------------------------------
// Pestaña Asistencia
// ---------------------------------------------------------------------------------------------

/** Porcentaje grande con color según el nivel (bien, regular, bajo) y el desglose en una fila. */
@Composable
internal fun ContenidoAsistencia(resumen: ResumenAsistencia, onVerHistorial: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (resumen.registrados == 0) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "Aún no hay entrenamientos con asistencia tomada. Cuando el entrenador la registre, verás aquí su porcentaje.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        } else {
            val porcentaje = ReglasHistorial.porcentaje(resumen.participaciones, resumen.registrados)
            val nivel = porcentaje
            val color = when {
                nivel >= 80.0 -> SportProTema.colores.exito
                nivel >= 60.0 -> SportProTema.colores.advertencia
                else -> SportProTema.colores.error
            }
            val lectura = when {
                nivel >= 80.0 -> "Constante en los entrenamientos"
                nivel >= 60.0 -> "Asistencia irregular"
                else -> "Falta seguido: conviene hablar con él o su apoderado"
            }
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            porcentajeCorto(porcentaje),
                            style = EstilosSportPro.NumeroGrande,
                            color = color,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "${resumen.participaciones} de ${resumen.registrados} sesiones",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (nivel / 100.0).toFloat().coerceIn(0f, 1f) },
                        color = color,
                        trackColor = color.copy(alpha = 0.15f),
                        drawStopIndicator = {},
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    )
                    Text(lectura, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ConteoAsistencia(resumen.presentes, "Presente", SportProTema.colores.exito, Modifier.weight(1f))
                ConteoAsistencia(resumen.tardes, "Tarde", SportProTema.colores.advertencia, Modifier.weight(1f))
                ConteoAsistencia(resumen.justificados, "Justif.", SportProTema.colores.informacion, Modifier.weight(1f))
                ConteoAsistencia(resumen.ausentes, "Falta", SportProTema.colores.error, Modifier.weight(1f))
            }
        }

        OutlinedButton(onClick = onVerHistorial, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Ver historial de participación")
        }
    }
}

/** Conteo neutro con un punto de color: el color identifica, no decora. */
@Composable
private fun ConteoAsistencia(cantidad: Int, etiqueta: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
        ) {
            Text("$cantidad", style = EstilosSportPro.NumeroMediano, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(4.dp))
                Text(
                    etiqueta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Pestaña Ficha: secciones plegables y contacto con acciones
// ---------------------------------------------------------------------------------------------

/**
 * Sección que se abre y cierra. Muestra en el encabezado un resumen corto ([vistaPrevia]) para
 * que no haga falta abrirla si solo buscas un dato rápido.
 */
@Composable
internal fun SeccionPlegable(
    titulo: String,
    icono: ImageVector,
    modifier: Modifier = Modifier,
    vistaPrevia: String? = null,
    abiertaAlInicio: Boolean = false,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    var abierta by rememberSaveable(titulo) { mutableStateOf(abiertaAlInicio) }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            Surface(
                onClick = { abierta = !abierta },
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth().semantics { role = Role.Button },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SportProTema.colores.acento.copy(alpha = 0.25f)),
                    ) {
                        Icon(icono, contentDescription = null, tint = SportProTema.colores.textoAcento, modifier = Modifier.size(19.dp))
                    }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        if (!abierta && !vistaPrevia.isNullOrBlank()) {
                            Text(
                                vistaPrevia,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Icon(
                        if (abierta) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (abierta) "Cerrar $titulo" else "Abrir $titulo",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            AnimatedVisibility(visible = abierta, enter = expandVertically(), exit = shrinkVertically()) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    contenido()
                }
            }
        }
    }
}

/** Como [DatoJugador], pero no dibuja nada si el valor está vacío: sin filas de "—". */
@Composable
internal fun DatoSiExiste(etiqueta: String, valor: String?) {
    if (!valor.isNullOrBlank()) DatoJugador(etiqueta, valor)
}

/** Botones de acción para un teléfono y/o un correo. Solo aparecen los que tienen dato. */
@Composable
internal fun AccionesContacto(telefono: String?, correo: String?, modifier: Modifier = Modifier) {
    val contexto = LocalContext.current
    val tieneTelefono = !telefono.isNullOrBlank()
    val tieneCorreo = !correo.isNullOrBlank()
    if (!tieneTelefono && !tieneCorreo) return
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier.fillMaxWidth()) {
        if (tieneTelefono) {
            BotonContacto("Llamar", Icons.Default.Call, Modifier.weight(1f)) {
                abrir(contexto, Intent(Intent.ACTION_DIAL, Uri.parse("tel:${soloDigitos(telefono!!)}")))
            }
            BotonContacto("WhatsApp", Icons.AutoMirrored.Filled.Chat, Modifier.weight(1f)) {
                abrir(contexto, Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${numeroWhatsApp(telefono!!)}")))
            }
        }
        if (tieneCorreo) {
            BotonContacto("Correo", Icons.Default.Email, Modifier.weight(1f)) {
                abrir(contexto, Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${correo!!.trim()}")))
            }
        }
    }
}

@Composable
private fun BotonContacto(texto: String, icono: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = SportProTema.colores.acento.copy(alpha = 0.25f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.heightIn(min = 48.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
        ) {
            Icon(icono, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(texto, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, textAlign = TextAlign.Center)
        }
    }
}

/** "87%" en vez de "87.5 %": en los números grandes Anton se lee mejor y cabe en tres columnas. */
private fun porcentajeCorto(valor: Double): String = "${valor.roundToInt()}%"

private fun soloDigitos(telefono: String): String = telefono.filter { it.isDigit() || it == '+' }

/** wa.me necesita el número con código de país. Los celulares peruanos (9 dígitos, empiezan en 9) reciben 51. */
private fun numeroWhatsApp(telefono: String): String {
    val digitos = telefono.filter { it.isDigit() }
    return if (digitos.length == 9 && digitos.startsWith("9")) "51$digitos" else digitos
}

private fun abrir(contexto: Context, intent: Intent) {
    try {
        contexto.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // Sin app para esta acción (p. ej. emulador sin marcador): no hacemos nada.
    }
}
