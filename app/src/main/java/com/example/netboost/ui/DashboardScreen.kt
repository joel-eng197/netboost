package com.example.netboost.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.netboost.domain.model.*
import com.example.netboost.domain.repository.*
import com.example.netboost.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DashboardState(
    val battery: BatteryInfo = BatteryInfo(), val ram: ResourceUsage = ResourceUsage(),
    val storage: ResourceUsage = ResourceUsage(), val network: NetworkStatus = NetworkStatus(),
    val optimizing: Boolean = false, val freedBytes: Long? = null, val tempAlert: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repo: DeviceRepository,
    observeBattery: ObserveBatteryUseCase,
    private val optimizeMemory: OptimizeMemoryUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(DashboardState())
    val state = _state.asStateFlow()

    init {
        // Batterie en temps réel + comparaison au seuil d'alerte défini dans les paramètres.
        viewModelScope.launch {
            observeBattery().collect { s -> _state.update { it.copy(battery = s.info, tempAlert = s.overheating) } }
        }
        // Rafraîchissement périodique RAM / stockage / réseau (hors thread UI).
        viewModelScope.launch {
            while (true) {
                val storage = repo.storage(); val net = repo.network()
                _state.update { it.copy(ram = repo.ram(), storage = storage, network = net) }
                delay(5_000)
            }
        }
    }

    fun optimize() {
        if (_state.value.optimizing) return
        viewModelScope.launch {
            _state.update { it.copy(optimizing = true, freedBytes = null) }
            val freed = optimizeMemory()
            _state.update { it.copy(optimizing = false, freedBytes = freed, ram = repo.ram()) }
        }
    }
}

@Composable
fun DashboardScreen(vm: DashboardViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { OptimizeButton(s.optimizing, vm::optimize) }
        s.freedBytes?.let { item { Text("Mémoire libérée : ${formatBytes(it)}", style = MaterialTheme.typography.titleSmall) } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Gauge("RAM", s.ram, Modifier.weight(1f)); Gauge("Stockage", s.storage, Modifier.weight(1f))
            }
        }
        item {
            InfoCard("Batterie", listOf(
                "Niveau" to "${s.battery.levelPct} %", "Température" to "%.1f °C".format(s.battery.tempC),
                "Tension" to "${s.battery.voltageMv} mV", "Santé" to s.battery.health
            ), warning = if (s.tempAlert) "⚠ Température supérieure au seuil défini" else null)
        }
        item {
            InfoCard("Réseau", listOf(
                "Type" to s.network.type, "SSID" to (s.network.ssid ?: "—"),
                "Signal" to (s.network.signalDbm?.let { "$it dBm" } ?: "—"), "Adresse IP" to s.network.ip
            ))
        }
    }
}

@Composable
private fun OptimizeButton(busy: Boolean, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "scale")
    Button(onClick, enabled = !busy, interactionSource = source,
        modifier = Modifier.fillMaxWidth().height(80.dp).scale(scale)) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(28.dp), color = LocalContentColor.current, strokeWidth = 3.dp)
            Spacer(Modifier.width(12.dp)); Text("Analyse en cours…")
        } else Text("OPTIMISER LA CONNEXION", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun Gauge(label: String, usage: ResourceUsage, modifier: Modifier) {
    val progress by animateFloatAsState(usage.fraction, tween(800), label = "gauge")
    Card(modifier) {
        Column(Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(96.dp), strokeWidth = 8.dp)
                Text("${(progress * 100).toInt()} %", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text("${formatBytes(usage.used)} / ${formatBytes(usage.total)}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun InfoCard(title: String, rows: List<Pair<String, String>>, warning: String? = null) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            warning?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            rows.forEach { (k, v) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(k, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(v)
                }
            }
        }
    }
}
