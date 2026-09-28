package com.example.netboost

import com.example.netboost.domain.model.*
import com.example.netboost.domain.repository.*
import kotlinx.coroutines.flow.*

class FakeDevice(private val battery: Flow<BatteryInfo> = emptyFlow()) : DeviceRepository {
    override fun batteryFlow() = battery
    override fun ram() = ResourceUsage()
    override suspend fun storage() = ResourceUsage()
    override suspend fun optimize() = 0L
    override suspend fun network() = NetworkStatus()
    override fun deviceInfo() = emptyList<Pair<String, String>>()
    override suspend fun appsByNetwork() = emptyList<AppNetUsage>()
    override fun hasUsageAccess() = false
    override fun hasPhoneState() = false
}

class FakeSettings(tempInit: Int = 40) : SettingsRepository {
    val temp = MutableStateFlow(tempInit)
    override val darkTheme: Flow<Boolean?> = flowOf(null)
    override val tempThreshold: Flow<Int> = temp
    override suspend fun setDarkTheme(value: Boolean) {}
    override suspend fun setTempThreshold(value: Int) { temp.value = value }
}
