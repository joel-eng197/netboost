package com.example.netboost.data

import com.example.netboost.domain.model.*

/** Catalogue local (prix indicatifs, à remplacer par une API ou une base Room). */
object Catalog {
    val phones = listOf(
        Phone("Google", "Pixel 9", "6,3\" OLED 120 Hz", "Tensor G4", "50 MP + 48 MP", "4700 mAh",
            listOf(Variant(12, 128, 899), Variant(12, 256, 999))),
        Phone("Samsung", "Galaxy S24", "6,2\" AMOLED 120 Hz", "Snapdragon 8 Gen 3 / Exynos 2400", "50 MP + 12 MP + 10 MP", "4000 mAh",
            listOf(Variant(8, 128, 809), Variant(8, 256, 869), Variant(8, 512, 989))),
        Phone("Xiaomi", "14", "6,36\" AMOLED 120 Hz", "Snapdragon 8 Gen 3", "Triple 50 MP Leica", "4610 mAh",
            listOf(Variant(12, 256, 999), Variant(12, 512, 1049), Variant(16, 512, 1149)))
    )
}
