package com.example.netboost

import com.example.netboost.data.Catalog
import com.example.netboost.domain.model.BatteryInfo
import com.example.netboost.domain.usecase.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class UseCasesTest {
    private val s24 = Catalog.phones.first { it.model == "Galaxy S24" }

    @Test fun search_ignoresCaseAndSpaces() {
        val names = SearchPhonesUseCase()(Catalog.phones, "  galaxy ").map { it.fullName }
        assertEquals(listOf("Samsung Galaxy S24"), names)
    }

    @Test fun price_existingCombination() = assertEquals(869, ResolvePriceUseCase()(s24, 8, 256))

    @Test fun price_unknownCombinationIsNull() = assertNull(ResolvePriceUseCase()(s24, 12, 128))

    @Test fun battery_alertAtThreshold() = runTest {
        val uc = ObserveBatteryUseCase(FakeDevice(flowOf(BatteryInfo(50, 40f))), FakeSettings(tempInit = 40))
        assertTrue(uc().first().overheating)
    }

    @Test fun battery_noAlertBelowThreshold() = runTest {
        val uc = ObserveBatteryUseCase(FakeDevice(flowOf(BatteryInfo(50, 35f))), FakeSettings(tempInit = 40))
        assertFalse(uc().first().overheating)
    }
}
