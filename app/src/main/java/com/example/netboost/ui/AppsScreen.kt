package com.example.netboost.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

data class AppsState(val apps: List<AppNetUsage> = emptyList(), val loading: Boolean = true, val hasAccess: Boolean = true)

@HiltViewModel
class AppsViewModel @Inject constructor(private val getApps: GetAppsByNetworkUseCase) : ViewModel() {
    private val _state = MutableStateFlow(AppsState())
    val state = _state.asStateFlow()

    /** Chargement et tri sur un thread de fond (Dispatchers.IO dans le repository). */
    fun load() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        val overview = getApps()
        _state.value = AppsState(overview.apps, false, overview.hasAccess)
    }
}

@Composable
fun AppsScreen(vm: AppsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.load() }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!s.hasAccess) item {
            Text("Autorisez l'accès aux données d'utilisation (Paramètres) pour trier par consommation.",
                color = MaterialTheme.colorScheme.error)
        }
        if (s.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        items(s.apps, key = { it.packageName }) { app ->
            ListItem(
                headlineContent = { Text(app.label) },
                supportingContent = { Text("${formatBytes(app.bytes)} sur 7 jours") },
                trailingContent = {
                    // L'utilisateur restreint les données ou force l'arrêt depuis l'écran système officiel.
                    OutlinedButton(onClick = {
                        ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", app.packageName, null)))
                    }) { Text("Détails") }
                }
            )
        }
    }
}
