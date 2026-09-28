package com.example.netboost.data

import com.example.netboost.domain.model.*
import com.example.netboost.domain.repository.DeviceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

import android.Manifest
import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.*
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.*
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.net.Inet4Address

/** Couche « Domain/Data » : accès unique aux API système. Toutes les méthodes lourdes sont des suspend. */
@Singleton
class DeviceRepositoryImpl @Inject constructor(@ApplicationContext context: Context) : DeviceRepository {
    private val ctx = context.applicationContext
    private val tag = "DeviceRepository"

    // ---------- Batterie (BroadcastReceiver -> Flow) ----------
    override fun batteryFlow(): Flow<BatteryInfo> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) { trySend(i.toBatteryInfo()) }
        }
        // registerReceiver renvoie l'intent « sticky » : première valeur immédiate.
        ctx.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.let { trySend(it.toBatteryInfo()) }
        awaitClose { ctx.unregisterReceiver(receiver) }
    }

    private fun Intent.toBatteryInfo(): BatteryInfo {
        val level = getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
        val scale = getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        val health = when (getIntExtra(BatteryManager.EXTRA_HEALTH, 0)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Bonne"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Surchauffe"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Défaillante"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Surtension"
            BatteryManager.BATTERY_HEALTH_COLD -> "Froide"
            else -> "Inconnue"
        }
        return BatteryInfo(level * 100 / scale, getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f,
            getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0), health)
    }

    // ---------- RAM / stockage ----------
    override fun ram(): ResourceUsage = try {
        val info = ActivityManager.MemoryInfo()
        ctx.getSystemService(ActivityManager::class.java).getMemoryInfo(info)
        ResourceUsage(info.totalMem - info.availMem, info.totalMem)
    } catch (e: Exception) { Log.e(tag, "ram", e); ResourceUsage() }

    override suspend fun storage(): ResourceUsage = withContext(Dispatchers.IO) {
        try {
            val s = StatFs(Environment.getDataDirectory().path)
            ResourceUsage(s.totalBytes - s.availableBytes, s.totalBytes)
        } catch (e: Exception) { Log.e(tag, "storage", e); ResourceUsage() }
    }

    /**
     * Demande au système de terminer les processus d'arrière-plan des applis tierces.
     * Android peut relancer certains services : le gain réel varie. Retourne les octets libérés.
     */
    override suspend fun optimize(): Long = withContext(Dispatchers.Default) {
        try {
            val am = ctx.getSystemService(ActivityManager::class.java)
            val before = ram().available
            ctx.packageManager.getInstalledApplications(0)
                .filter { it.packageName != ctx.packageName && it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
                .forEach { am.killBackgroundProcesses(it.packageName) }
            delay(1500) // laisse le temps au système de récupérer la mémoire
            (ram().available - before).coerceAtLeast(0)
        } catch (e: Exception) { Log.e(tag, "optimize", e); 0L }
    }

    // ---------- Réseau ----------
    @Suppress("DEPRECATION")
    override suspend fun network(): NetworkStatus = withContext(Dispatchers.IO) {
        try {
            val cm = ctx.getSystemService(ConnectivityManager::class.java)
            val net = cm.activeNetwork ?: return@withContext NetworkStatus()
            val caps = cm.getNetworkCapabilities(net)
            val ip = cm.getLinkProperties(net)?.linkAddresses?.map { it.address }
                ?.firstOrNull { it is Inet4Address }?.hostAddress ?: "—"
            when {
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> {
                    val w = ctx.getSystemService(WifiManager::class.java).connectionInfo
                    val ssid = w?.ssid?.trim('"')?.takeIf { it != WifiManager.UNKNOWN_SSID }
                    NetworkStatus("Wi-Fi", ssid ?: "Masqué (permission localisation requise)", w?.rssi, ip)
                }
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> {
                    val dbm = try {
                        ctx.getSystemService(TelephonyManager::class.java).signalStrength
                            ?.cellSignalStrengths?.firstOrNull()?.dbm
                    } catch (e: SecurityException) { null }
                    NetworkStatus("Données mobiles", null, dbm, ip)
                }
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> NetworkStatus("Ethernet", null, null, ip)
                else -> NetworkStatus("Autre", null, null, ip)
            }
        } catch (e: Exception) { Log.e(tag, "network", e); NetworkStatus() }
    }

    // ---------- Infos appareil ----------
    override fun deviceInfo(): List<Pair<String, String>> = listOf(
        "Modèle" to Build.MODEL, "Marque" to Build.BRAND.replaceFirstChar { it.uppercase() },
        "Version Android" to Build.VERSION.RELEASE, "Niveau d'API" to Build.VERSION.SDK_INT.toString(),
        "Correctif de sécurité" to Build.VERSION.SECURITY_PATCH,
        "Architectures CPU" to Build.SUPPORTED_ABIS.joinToString(", "),
        "Cœurs CPU" to Runtime.getRuntime().availableProcessors().toString(),
        "Matériel / carte" to "${Build.HARDWARE} / ${Build.BOARD}"
    )

    // ---------- Applications triées par consommation réseau (7 jours) ----------
    override suspend fun appsByNetwork(): List<AppNetUsage> = withContext(Dispatchers.IO) {
        val bytesByUid = mutableMapOf<Int, Long>()
        if (hasUsageAccess()) try {
            val nsm = ctx.getSystemService(NetworkStatsManager::class.java)
            val end = System.currentTimeMillis(); val start = end - 7 * 24 * 3600 * 1000L
            for (type in listOf(ConnectivityManager.TYPE_WIFI, ConnectivityManager.TYPE_MOBILE)) {
                val stats = nsm.querySummary(type, null, start, end)
                val bucket = NetworkStats.Bucket()
                while (stats.hasNextBucket()) { stats.getNextBucket(bucket); bytesByUid.merge(bucket.uid, bucket.rxBytes + bucket.txBytes, Long::plus) }
                stats.close()
            }
        } catch (e: Exception) { Log.e(tag, "netstats", e) }
        val pm = ctx.packageManager
        try {
            pm.getInstalledApplications(0).filter { pm.getLaunchIntentForPackage(it.packageName) != null }
                .map { AppNetUsage(pm.getApplicationLabel(it).toString(), it.packageName, bytesByUid[it.uid] ?: 0L) }
                .sortedByDescending { it.bytes }
        } catch (e: Exception) { Log.e(tag, "apps", e); emptyList() }
    }

    // ---------- Permissions ----------
    override fun hasUsageAccess(): Boolean = try {
        val ops = ctx.getSystemService(AppOpsManager::class.java)
        ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName) == AppOpsManager.MODE_ALLOWED
    } catch (e: Exception) { false }

    override fun hasPhoneState(): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
}
