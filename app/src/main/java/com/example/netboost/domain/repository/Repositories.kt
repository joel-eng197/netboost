package com.example.netboost.domain.repository

import com.example.netboost.domain.model.*
import kotlinx.coroutines.flow.Flow

/** Contrat d'accès aux informations système (implémenté dans la couche data). */
interface DeviceRepository {
    fun batteryFlow(): Flow<BatteryInfo>
    fun ram(): ResourceUsage
    suspend fun storage(): ResourceUsage
    suspend fun optimize(): Long
    suspend fun network(): NetworkStatus
    fun deviceInfo(): List<Pair<String, String>>
    suspend fun appsByNetwork(): List<AppNetUsage>
    fun hasUsageAccess(): Boolean
    fun hasPhoneState(): Boolean
}

/** Contrat de persistance des préférences. */
interface SettingsRepository {
    /** null = suivre le thème du système. */
    val darkTheme: Flow<Boolean?>
    val tempThreshold: Flow<Int>
    suspend fun setDarkTheme(value: Boolean)
    suspend fun setTempThreshold(value: Int)
}
