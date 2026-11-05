package com.esan.sportpro.data.partido

import com.esan.sportpro.domain.cuentas.PerfilActual
import com.esan.sportpro.domain.jugadores.JugadorRepository
import com.esan.sportpro.domain.jugadores.VinculacionRepository
import com.esan.sportpro.navigation.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Equipos cuyos partidos puede seguir el usuario (US-021, criterio 7): el padre, los de sus
 * jugadores con vínculo aprobado; el jugador, el de su propia ficha. Para administrador y
 * entrenador no hay restricción y se emite `null`.
 */
@Singleton
class AccesoEspectador @Inject constructor(
    private val vinculacionRepository: VinculacionRepository,
    private val jugadorRepository: JugadorRepository,
) {

    fun equiposPermitidos(perfil: PerfilActual): Flow<Set<String>?> = when (perfil.rol) {
        UserRole.PADRE_DE_FAMILIA ->
            vinculacionRepository.observarMisVinculaciones(perfil.uid).map { vinculaciones ->
                vinculaciones.filter { it.aprobada }.map { it.equipoId }.filter { it.isNotBlank() }.toSet()
            }

        UserRole.JUGADOR ->
            jugadorRepository.observarFichaPropia(perfil.uid).map { ficha ->
                setOfNotNull(ficha?.takeIf { it.activo }?.equipoId?.takeIf { it.isNotBlank() })
            }

        UserRole.ADMINISTRADOR, UserRole.ENTRENADOR -> flowOf(null)
    }
}
