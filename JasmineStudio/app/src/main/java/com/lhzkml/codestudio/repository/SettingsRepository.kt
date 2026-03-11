package com.lhzkml.codestudio.repository

import com.lhzkml.codestudio.LocalStore
import com.lhzkml.codestudio.Preset
import com.lhzkml.codestudio.Provider
import com.lhzkml.codestudio.StoredSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal interface SettingsRepository {
    val settingsFlow: StateFlow<StoredSettings>
    val presetIdFlow: StateFlow<String?>
    fun updateSettings(settings: StoredSettings)
    fun updatePresetId(presetId: String)
}

internal class SettingsRepositoryImpl(
    private val localStore: LocalStore
) : SettingsRepository {
    
    private val _settingsFlow = MutableStateFlow(localStore.loadState().settings)
    override val settingsFlow: StateFlow<StoredSettings> = _settingsFlow.asStateFlow()
    
    private val _presetIdFlow = MutableStateFlow(localStore.loadRuntimePresetId())
    override val presetIdFlow: StateFlow<String?> = _presetIdFlow.asStateFlow()
    
    override fun updateSettings(settings: StoredSettings) {
        localStore.saveSettings(settings)
        _settingsFlow.value = settings
    }
    
    override fun updatePresetId(presetId: String) {
        localStore.saveRuntimePresetId(presetId)
        _presetIdFlow.value = presetId
    }
}
