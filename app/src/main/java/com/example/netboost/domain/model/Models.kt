package com.example.netboost.domain.model

/** Informations batterie issues du sticky broadcast ACTION_BATTERY_CHANGED. */
data class BatteryInfo(val levelPct: Int = 0, val tempC: Float = 0f, val voltageMv: Int = 0, val health: String = "—")

/** Utilisation d'une ressource (RAM ou stockage) en octets. */
data class ResourceUsage(val used: Long = 0, val total: Long = 1) {
    val fraction: Float get() = (used.toFloat() / total).coerceIn(0f, 1f)
    val available: Long get() = total - used
}

data class NetworkStatus(val type: String = "Hors ligne", val ssid: String? = null, val signalDbm: Int? = null, val ip: String = "—")

data class AppNetUsage(val label: String, val packageName: String, val bytes: Long)

/** Formate un nombre d'octets en unité lisible (Ko, Mo, Go…). */
fun formatBytes(bytes: Long): String {
    val units = listOf("o", "Ko", "Mo", "Go", "To")
    var v = bytes.toDouble(); var i = 0
    while (v >= 1024 && i < units.lastIndex) { v /= 1024; i++ }
    return "%.1f %s".format(v, units[i])
}

/** Variante commerciale d'un modèle : combinaison RAM / stockage et son prix. */
data class Variant(val ramGb: Int, val storageGb: Int, val priceEur: Int)

data class Phone(
    val brand: String, val model: String,
    val screen: String, val cpu: String, val camera: String, val battery: String,
    val variants: List<Variant>
) { val fullName get() = "$brand $model" }

/** Batterie + indicateur de surchauffe calculé par la couche domain. */
data class BatteryStatus(val info: BatteryInfo, val overheating: Boolean)

data class AppsOverview(val apps: List<AppNetUsage>, val hasAccess: Boolean)
