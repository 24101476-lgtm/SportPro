package com.esan.sportpro.di

import com.esan.sportpro.data.partido.FirestorePartidoRepository
import com.esan.sportpro.domain.partido.PartidoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Enlaza la interfaz del repositorio del partido con su implementación en Firestore. */
@Module
@InstallIn(SingletonComponent::class)
abstract class PartidoModule {

    @Binds
    @Singleton
    abstract fun bindPartidoRepository(impl: FirestorePartidoRepository): PartidoRepository
}
