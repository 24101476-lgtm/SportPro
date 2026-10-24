package com.esan.sportpro.di

import com.esan.sportpro.data.cuentas.FirestoreUsuarioRepository
import com.esan.sportpro.domain.cuentas.UsuarioRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Enlaza la interfaz de cuenta de usuario con su implementación en Firestore (US-001). */
@Module
@InstallIn(SingletonComponent::class)
abstract class CuentasModule {

    @Binds
    @Singleton
    abstract fun bindUsuarioRepository(impl: FirestoreUsuarioRepository): UsuarioRepository
}