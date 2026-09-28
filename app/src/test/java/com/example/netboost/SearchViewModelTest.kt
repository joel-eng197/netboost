package com.example.netboost

import com.example.netboost.data.Catalog
import com.example.netboost.domain.usecase.*
import com.example.netboost.ui.SearchViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun TestScope.newVm(): SearchViewModel {
        val vm = SearchViewModel(SearchPhonesUseCase(), ResolvePriceUseCase())
        // stateIn(WhileSubscribed) ne calcule que s'il y a un collecteur.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.price.collect {} }
        return vm
    }

    @Test fun price_followsRamAndStorageSelection() = runTest {
        val vm = newVm()
        vm.select(Catalog.phones.first { it.model == "Galaxy S24" })
        assertEquals(809, vm.price.value)
        vm.pickStorage(512)
        assertEquals(989, vm.price.value)
    }

    @Test fun changingRam_correctsInvalidStorage() = runTest {
        val vm = newVm()
        vm.select(Catalog.phones.first { it.model == "14" }) // 12 Go / 256 Go par défaut
        vm.pickRam(16)                                        // 16 Go n'existe qu'en 512 Go
        assertEquals(512, vm.storage.value)
        assertEquals(1149, vm.price.value)
    }
}
