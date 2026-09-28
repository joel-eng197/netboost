package com.example.netboost.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.netboost.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("settings")

/** Persistance des préférences via DataStore. */
@Singleton
class SettingsRepositoryImpl @Inject constructor(@ApplicationContext private val context: Context) : SettingsRepository {
    private val darkKey = booleanPreferencesKey("dark_theme")
    private val tempKey = intPreferencesKey("temp_threshold")

    override val darkTheme: Flow<Boolean?> = context.dataStore.data.map { it[darkKey] }
    override val tempThreshold: Flow<Int> = context.dataStore.data.map { it[tempKey] ?: 40 }

    override suspend fun setDarkTheme(value: Boolean) { context.dataStore.edit { it[darkKey] = value } }
    override suspend fun setTempThreshold(value: Int) { context.dataStore.edit { it[tempKey] = value } }
}
