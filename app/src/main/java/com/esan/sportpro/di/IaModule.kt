package com.esan.sportpro.di

import com.esan.sportpro.data.ia.FirestoreResumenRepository
import com.esan.sportpro.data.ia.SupabaseGeneradorResumen
import com.esan.sportpro.domain.ia.GeneradorResumen
import com.esan.sportpro.domain.ia.ResumenRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Inyección del módulo de IA: generador (Edge Function) y versiones del resumen (Firestore). */
@Module
@InstallIn(SingletonComponent::class)
abstract class IaModule {

    @Binds
    @Singleton
    abstract fun bindGeneradorResumen(impl: SupabaseGeneradorResumen): GeneradorResumen

    @Binds
    @Singleton
    abstract fun bindResumenRepository(impl: FirestoreResumenRepository): ResumenRepository
}
