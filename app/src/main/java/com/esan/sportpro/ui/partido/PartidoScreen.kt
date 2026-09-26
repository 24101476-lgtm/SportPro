package com.esan.sportpro.ui.partido

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.esan.sportpro.ui.common.PlaceholderScreen

@Composable
fun PartidoScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = "Partido",
        relatedUserStories = "US-014 a US-022 (incluye tiempo real y offline/resync)",
        modifier = modifier,
    )
}
