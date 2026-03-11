package com.lhzkml.codestudio.repository

import com.lhzkml.codestudio.LocalStore
import com.lhzkml.codestudio.Preset
import com.lhzkml.codestudio.Provider
import com.lhzkml.codestudio.StoredSettings

internal interface SettingsRepository {
    fun loadSettings(): StoredSettings
    fun saveSettings(settings: StoredSettings)
    fun loadRuntimePresetId(): String?
    fun saveRuntimePresetId(presetId: String)
}

internal class SettingsRepositoryImpl(
    private val localStore: LocalStore
) : SettingsRepository {
    
    override fun loadSettings(): StoredSettings {
        return localStore.loadState().settings
    }
    
    override fun saveSettings(settings: StoredSettings) {
        localStore.saveSettings(settings)
    }
    
    override fun loadRuntimePresetId(): String? {
        return localStore.loadRuntimePresetId()
    }
    
    override fun saveRuntimePresetId(presetId: String) {
        localStore.saveRuntimePresetId(presetId)
    }
}
