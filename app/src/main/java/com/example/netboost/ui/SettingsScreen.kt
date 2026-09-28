package com.example.netboost.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.netboost.domain.model.*
import com.example.netboost.domain.repository.*
import com.example.netboost.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val store: SettingsRepository,
    private val repo: DeviceRepository
) : ViewModel() {
    val dark = store.darkTheme.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val threshold = store.tempThreshold.stateIn(viewModelScope, SharingStarted.Eagerly, 40)
    private val _perms = MutableStateFlow(false to false)
    /** (accès aux stats d'utilisation, état du téléphone) */
    val perms = _perms.asStateFlow()

    fun refreshPermissions() { _perms.value = repo.hasUsageAccess() to repo.hasPhoneState() }
    fun setDark(v: Boolean) = viewModelScope.launch { store.setDarkTheme(v) }
    fun setThreshold(v: Int) = viewModelScope.launch { store.setTempThreshold(v) }
}

@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val ctx = LocalContext.current
    val dark by vm.dark.collectAsStateWithLifecycle()
    val threshold by vm.threshold.collectAsStateWithLifecycle()
    val (usageOk, phoneOk) = vm.perms.collectAsStateWithLifecycle().value
    // Revérifie à chaque retour depuis l'écran système : statut « temps réel ».
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshPermissions() }
    val phoneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.refreshPermissions() }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Apparence", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Thème sombre")
            Switch(dark ?: isSystemInDarkTheme(), vm::setDark)
        }
        Text("Alerte température batterie : $threshold °C", style = MaterialTheme.typography.titleMedium)
        Slider(threshold.toFloat(), { vm.setThreshold(it.toInt()) }, valueRange = 30f..55f)

        Text("Permissions", style = MaterialTheme.typography.titleMedium)
        PermissionRow("Statistiques d'utilisation", usageOk) {
            ctx.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:${ctx.packageName}")))
        }
        PermissionRow("État du téléphone", phoneOk) { phoneLauncher.launch(Manifest.permission.READ_PHONE_STATE) }
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, onGrant: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text(label)
            Text(if (granted) "✔ Accordée" else "✖ Manquante",
                color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        }
        if (!granted) Button(onGrant) { Text("Accorder") }
    }
}
