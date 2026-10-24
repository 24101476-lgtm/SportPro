package com.esan.sportpro.di

import com.esan.sportpro.data.academia.FirestoreEquipoRepository
import com.esan.sportpro.data.entrenamientos.FirestoreEntrenamientoRepository
import com.esan.sportpro.data.jugadores.FirestoreJugadorRepository
import com.esan.sportpro.domain.academia.EquipoRepository
import com.esan.sportpro.domain.entrenamientos.EntrenamientoRepository
import com.esan.sportpro.domain.jugadores.JugadorRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Inyección de los repositorios de gestión: equipos, jugadores y entrenamientos. */
@Module
@InstallIn(SingletonComponent::class)
abstract class GestionModule {

    @Binds
    @Singleton
    abstract fun bindEquipoRepository(impl: FirestoreEquipoRepository): EquipoRepository

    @Binds
    @Singleton
    abstract fun bindJugadorRepository(impl: FirestoreJugadorRepository): JugadorRepository

    @Binds
    @Singleton
    abstract fun bindEntrenamientoRepository(impl: FirestoreEntrenamientoRepository): EntrenamientoRepository
}
