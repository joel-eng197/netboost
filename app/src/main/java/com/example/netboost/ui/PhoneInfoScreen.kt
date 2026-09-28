package com.example.netboost.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.netboost.domain.model.*
import com.example.netboost.domain.repository.*
import com.example.netboost.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PhoneInfoViewModel @Inject constructor(repo: DeviceRepository) : ViewModel() {
    val info = repo.deviceInfo()
}

/** Liste de cartes (équivalent Compose de RecyclerView + CardView). */
@Composable
fun PhoneInfoScreen(vm: PhoneInfoViewModel = hiltViewModel()) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(vm.info) { (label, value) -> InfoCard(label, listOf("" to value)) }
    }
}
