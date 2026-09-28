package com.example.netboost

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import com.example.netboost.domain.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.example.netboost.ui.*

/** Destinations de la barre de navigation inférieure. */
private enum class Tab(val label: String, val icon: ImageVector) {
    Dashboard("Tableau de bord", Icons.Default.Home),
    Phone("Ce smartphone", Icons.Default.Phone),
    Search("Comparateur", Icons.Default.Search),
    Network("Réseau", Icons.AutoMirrored.Filled.List),
    Settings("Paramètres", Icons.Default.Settings)
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var settings: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val dark by settings.darkTheme.collectAsStateWithLifecycle(initialValue = null)
            val useDark = dark ?: isSystemInDarkTheme()
            val ctx = LocalContext.current
            val scheme = when {
                Build.VERSION.SDK_INT >= 31 -> if (useDark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
                useDark -> darkColorScheme() else -> lightColorScheme()
            }
            MaterialTheme(colorScheme = scheme) { AppRoot() }
        }
    }
}

@Composable
private fun AppRoot() {
    val nav = rememberNavController()
    val current = nav.currentBackStackEntryAsState().value?.destination?.route
    Scaffold(bottomBar = {
        NavigationBar {
            Tab.entries.forEach { tab ->
                NavigationBarItem(
                    selected = current == tab.name,
                    onClick = {
                        nav.navigate(tab.name) {
                            popUpTo(nav.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    },
                    icon = { Icon(tab.icon, tab.label) }, label = { Text(tab.label, maxLines = 1) }
                )
            }
        }
    }) { padding ->
        NavHost(nav, startDestination = Tab.Dashboard.name, modifier = Modifier.padding(padding)) {
            composable(Tab.Dashboard.name) { DashboardScreen() }
            composable(Tab.Phone.name) { PhoneInfoScreen() }
            composable(Tab.Search.name) { SearchScreen() }
            composable(Tab.Network.name) { AppsScreen() }
            composable(Tab.Settings.name) { SettingsScreen() }
        }
    }
}
