package com.esan.sportpro.ui.ia

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Módulo IA: reúne las evaluaciones del resumen narrativo (US-025). El resumen en sí se genera,
 * revisa y aprueba desde cada partido con el acta cerrada (US-023 y US-024), con el botón «Resumen con IA».
 */
@Composable
fun IaScreen(modifier: Modifier = Modifier) {
    EvaluacionesScreen(modifier = modifier)
}
