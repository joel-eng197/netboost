package com.example.netboost.domain.usecase

import com.example.netboost.domain.model.*
import com.example.netboost.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/** Libère la RAM et retourne le nombre d'octets récupérés. */
class OptimizeMemoryUseCase @Inject constructor(private val repo: DeviceRepository) {
    suspend operator fun invoke(): Long = repo.optimize()
}

/** Combine l'état de la batterie et le seuil d'alerte choisi par l'utilisateur. */
class ObserveBatteryUseCase @Inject constructor(
    private val repo: DeviceRepository, private val settings: SettingsRepository
) {
    operator fun invoke(): Flow<BatteryStatus> =
        combine(repo.batteryFlow(), settings.tempThreshold) { info, threshold ->
            BatteryStatus(info, overheating = info.tempC >= threshold)
        }
}

/** Applications triées par consommation réseau + état de l'autorisation nécessaire. */
class GetAppsByNetworkUseCase @Inject constructor(private val repo: DeviceRepository) {
    suspend operator fun invoke() = AppsOverview(repo.appsByNetwork(), repo.hasUsageAccess())
}

/** Filtre le catalogue sur le nom complet, sans tenir compte de la casse. */
class SearchPhonesUseCase @Inject constructor() {
    operator fun invoke(phones: List<Phone>, query: String): List<Phone> =
        phones.filter { it.fullName.contains(query.trim(), ignoreCase = true) }
}

/** Prix de la variante RAM/stockage choisie, ou null si la combinaison n'existe pas. */
class ResolvePriceUseCase @Inject constructor() {
    operator fun invoke(phone: Phone, ramGb: Int?, storageGb: Int?): Int? =
        phone.variants.firstOrNull { it.ramGb == ramGb && it.storageGb == storageGb }?.priceEur
}
