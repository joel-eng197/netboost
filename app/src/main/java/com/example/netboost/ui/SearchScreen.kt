package com.example.netboost.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
import com.example.netboost.data.Catalog
import kotlinx.coroutines.flow.*

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchPhones: SearchPhonesUseCase,
    private val resolvePrice: ResolvePriceUseCase
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()
    /** Filtrage temps réel du catalogue à chaque frappe. */
    val results = _query.map { q -> searchPhones(Catalog.phones, q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Catalog.phones)

    private val _selected = MutableStateFlow<Phone?>(null)
    private val _ram = MutableStateFlow<Int?>(null)
    private val _storage = MutableStateFlow<Int?>(null)
    val selected = _selected.asStateFlow(); val ram = _ram.asStateFlow(); val storage = _storage.asStateFlow()

    /** Prix recalculé instantanément à chaque changement de sélection. */
    val price = combine(_selected, _ram, _storage) { p, r, s ->
        p?.let { resolvePrice(it, r, s) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onQuery(q: String) { _query.value = q }
    fun select(p: Phone) { _selected.value = p; _ram.value = p.variants.first().ramGb; _storage.value = p.variants.first().storageGb }
    fun pickRam(r: Int) {
        _ram.value = r
        val options = _selected.value?.variants?.filter { it.ramGb == r }?.map { it.storageGb } ?: return
        if (_storage.value !in options) _storage.value = options.first() // évite une combinaison inexistante
    }
    fun pickStorage(s: Int) { _storage.value = s }
}

@Composable
fun SearchScreen(vm: SearchViewModel = hiltViewModel()) {
    val query by vm.query.collectAsStateWithLifecycle()
    val results by vm.results.collectAsStateWithLifecycle()
    val phone by vm.selected.collectAsStateWithLifecycle()
    val ram by vm.ram.collectAsStateWithLifecycle()
    val storage by vm.storage.collectAsStateWithLifecycle()
    val price by vm.price.collectAsStateWithLifecycle()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            OutlinedTextField(query, vm::onQuery, Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("Rechercher un smartphone") }, leadingIcon = { Icon(Icons.Default.Search, null) })
        }
        items(results, key = { it.fullName }) { p ->
            ListItem(headlineContent = { Text(p.fullName) }, supportingContent = { Text(p.cpu) },
                modifier = Modifier.clickable { vm.select(p) })
        }
        phone?.let { p ->
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(p.fullName, style = MaterialTheme.typography.titleLarge)
                        Text("Écran : ${p.screen}"); Text("CPU : ${p.cpu}")
                        Text("Photo : ${p.camera}"); Text("Batterie : ${p.battery}")
                        Text("RAM", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            p.variants.map { it.ramGb }.distinct().forEach {
                                FilterChip(ram == it, { vm.pickRam(it) }, label = { Text("$it Go") })
                            }
                        }
                        Text("Stockage", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            p.variants.filter { it.ramGb == ram }.map { it.storageGb }.forEach {
                                FilterChip(storage == it, { vm.pickStorage(it) }, label = { Text("$it Go") })
                            }
                        }
                        Text(price?.let { "$it €" } ?: "Indisponible", style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
