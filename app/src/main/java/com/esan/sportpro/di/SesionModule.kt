package com.esan.sportpro.di

import com.esan.sportpro.data.cuentas.FirestoreSesionRepository
import com.esan.sportpro.domain.cuentas.SesionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** US-002: inyección de la capa de sesión. */
@Module
@InstallIn(SingletonComponent::class)
abstract class SesionModule {

    @Binds
    @Singleton
    abstract fun bindSesionRepository(impl: FirestoreSesionRepository): SesionRepository
}