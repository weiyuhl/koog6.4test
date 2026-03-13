package com.lhzkml.codestudio.repository

import com.lhzkml.codestudio.StoredSettings
import com.lhzkml.codestudio.data.SettingsDataStore
import kotlinx.coroutines.flow.Flow

internal interface SettingsRepository {
    val settingsFlow: Flow<StoredSettings>
    val presetIdFlow: Flow<String?>
    suspend fun updateSettings(settings: StoredSettings)
    suspend fun updatePresetId(presetId: String)
}

internal class SettingsRepositoryImpl(
    private val settingsDataStore: SettingsDataStore
) : SettingsRepository {
    
    override val settingsFlow: Flow<StoredSettings> = settingsDataStore.settingsFlow
    
    override val presetIdFlow: Flow<String?> = settingsDataStore.presetIdFlow
    
    override suspend fun updateSettings(settings: StoredSettings) {
        settingsDataStore.updateSettings(settings)
    }
    
    override suspend fun updatePresetId(presetId: String) {
        settingsDataStore.updatePresetId(presetId)
    }
}
