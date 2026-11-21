package com.esan.sportpro.ui.partido

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

data class JugadorCanchaUi(
    val id: String,
    val nombre: String,
    val dorsal: Int?,
    val posicion: String?,
    val titular: Boolean,
    val x: Float,
    val y: Float,
)

@Composable
fun CanchaTactica(
    titulares: List<JugadorCanchaUi>,
    suplentes: List<JugadorCanchaUi>,
    editable: Boolean,
    modifier: Modifier = Modifier,
    onJugadorClick: (JugadorCanchaUi) -> Unit = {},
    onPosicionJugadorChange: (jugadorId: String, x: Float, y: Float) -> Unit = { _, _, _ -> },
) {
    val posiciones = remember(titulares) {
        mutableStateMapOf<String, Offset>().apply {
            titulares.forEach { jugador ->
                put(jugador.id, Offset(jugador.x.coerceIn(0f, 1f), jugador.y.coerceIn(0f, 1f)))
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .background(Color(0xFF2F6B24), RoundedCornerShape(20.dp))
                .border(2.dp, Color.White.copy(alpha = 0.88f), RoundedCornerShape(20.dp))
                .padding(10.dp)
        ) {
            Column(Modifier.fillMaxSize()) {
                repeat(10) { index ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(
                                if (index % 2 == 0) Color(0xFF347A2B)
                                else Color(0xFF2C6B25)
                            )
                    )
                }
            }

            LineasCancha()

            var boardWidth by remember { mutableIntStateOf(1) }
            var boardHeight by remember { mutableIntStateOf(1) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged {
                        boardWidth = it.width.coerceAtLeast(1)
                        boardHeight = it.height.coerceAtLeast(1)
                    }
            ) {
                titulares.forEach { jugador ->
                    val actual = posiciones[jugador.id] ?: Offset(jugador.x, jugador.y)

                    FichaJugadorCancha(
                        jugador = jugador,
                        editable = editable,
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = (actual.x * boardWidth).roundToInt() - 36.dp.roundToPx(),
                                    y = (actual.y * boardHeight).roundToInt() - 43.dp.roundToPx()
                                )
                            }
                            .width(72.dp)
                            .pointerInput(jugador.id, editable, boardWidth, boardHeight) {
                                if (!editable) return@pointerInput

                                detectDragGestures(
                                    onDragEnd = {
                                        val final = posiciones[jugador.id]
                                        if (final != null) {
                                            onPosicionJugadorChange(
                                                jugador.id,
                                                final.x.coerceIn(0.05f, 0.95f),
                                                final.y.coerceIn(0.06f, 0.94f)
                                            )
                                        }
                                    }
                                ) { change, dragAmount ->
                                    change.consume()
                                    val anterior = posiciones[jugador.id] ?: Offset(jugador.x, jugador.y)
                                    posiciones[jugador.id] = Offset(
                                        x = (anterior.x + dragAmount.x / boardWidth).coerceIn(0.05f, 0.95f),
                                        y = (anterior.y + dragAmount.y / boardHeight).coerceIn(0.06f, 0.94f),
                                    )
                                }
                            },
                        onClick = { onJugadorClick(jugador) }
                    )
                }
            }
        }

        if (suplentes.isNotEmpty()) {
            Text(
                text = "SUPLENTES (${suplentes.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                suplentes.take(5).forEach { jugador ->
                    FichaSuplente(
                        jugador = jugador,
                        modifier = Modifier.weight(1f),
                        onClick = { onJugadorClick(jugador) }
                    )
                }
            }

            if (suplentes.size > 5) {
                Text(
                    text = "+${suplentes.size - 5} suplente(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text(
            text = if (editable) {
                "Mantén y arrastra las fichas para ubicar a tus 11 titulares."
            } else {
                "Toca una ficha para ver los datos del jugador."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LineasCancha() {
    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(1.5.dp)
                .background(Color.White.copy(alpha = 0.90f))
        )

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(88.dp)
                .border(1.5.dp, Color.White.copy(alpha = 0.90f), CircleShape)
        )

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(5.dp)
                .background(Color.White, CircleShape)
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(0.58f)
                .height(92.dp)
                .border(1.5.dp, Color.White.copy(alpha = 0.90f))
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.58f)
                .height(92.dp)
                .border(1.5.dp, Color.White.copy(alpha = 0.90f))
        )
    }
}

@Composable
private fun FichaJugadorCancha(
    jugador: JugadorCanchaUi,
    editable: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    /*
     * La ficha mantiene el mismo tamaño para no alterar
     * las coordenadas tácticas. El dorsal gana contraste:
     * círculo azul oscuro + borde blanco + número más grande.
     */
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(13.dp),
        color = Color(0xFFF3C74F),
        tonalElevation = 5.dp,
        shadowElevation = 5.dp,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(
                horizontal = 5.dp,
                vertical = 6.dp,
            ),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        color = Color(0xFF173A67),
                        shape = CircleShape,
                    )
                    .border(
                        width = 2.dp,
                        color = Color.White,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = jugador.dorsal?.toString() ?: "?",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                )
            }

            Text(
                text = jugador.nombre
                    .substringBefore(" ")
                    .uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.padding(top = 3.dp),
            )

            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFFB63D2F),
                modifier = Modifier.padding(top = 2.dp),
            ) {
                Text(
                    text = jugador.posicion ?: "—",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier.padding(
                        horizontal = 5.dp,
                        vertical = 2.dp,
                    ),
                )
            }

            if (editable) {
                Text(
                    text = "⋮⋮",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Black.copy(alpha = 0.55f),
                )
            }
        }
    }
}

@Composable
private fun FichaSuplente(
    jugador: JugadorCanchaUi,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = jugador.dorsal?.toString() ?: "?",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            Text(
                text = jugador.nombre.substringBefore(" "),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

fun posiciones433(jugadores: List<JugadorCanchaUi>): List<JugadorCanchaUi> {
    val posiciones = listOf(
        Offset(0.50f, 0.90f),
        Offset(0.14f, 0.72f),
        Offset(0.38f, 0.76f),
        Offset(0.62f, 0.76f),
        Offset(0.86f, 0.72f),
        Offset(0.30f, 0.52f),
        Offset(0.50f, 0.57f),
        Offset(0.70f, 0.52f),
        Offset(0.18f, 0.27f),
        Offset(0.50f, 0.19f),
        Offset(0.82f, 0.27f),
    )

    return jugadores.take(11).mapIndexed { index, jugador ->
        val p = posiciones[index]
        jugador.copy(x = p.x, y = p.y)
    }
}

