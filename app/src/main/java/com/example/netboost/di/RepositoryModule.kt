package com.example.netboost.di

import com.example.netboost.data.DeviceRepositoryImpl
import com.example.netboost.data.SettingsRepositoryImpl
import com.example.netboost.domain.repository.DeviceRepository
import com.example.netboost.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Associe chaque interface du domaine à son implémentation Android. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun bindDevice(impl: DeviceRepositoryImpl): DeviceRepository
    @Binds @Singleton abstract fun bindSettings(impl: SettingsRepositoryImpl): SettingsRepository
}
