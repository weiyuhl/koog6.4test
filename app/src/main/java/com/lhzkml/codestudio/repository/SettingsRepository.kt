package com.lhzkml.codestudio.repository

import com.lhzkml.codestudio.Provider
import com.lhzkml.codestudio.StoredSettings
import com.lhzkml.codestudio.data.dao.SettingsDao
import com.lhzkml.codestudio.data.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal interface SettingsRepository {
    val settingsFlow: Flow<StoredSettings>
    val presetIdFlow: Flow<String?>
    suspend fun updateSettings(settings: StoredSettings)
    suspend fun updatePresetId(presetId: String)
}

internal class SettingsRepositoryImpl(
    private val settingsDao: SettingsDao
) : SettingsRepository {
    
    override val settingsFlow: Flow<StoredSettings> = settingsDao.getSettingsFlow().map { entity ->
        entity?.toStoredSettings() ?: getDefaultSettings()
    }
    
    override val presetIdFlow: Flow<String?> = settingsDao.getSettingsFlow().map { entity ->
        entity?.runtimePresetId
    }
    
    override suspend fun updateSettings(settings: StoredSettings) {
        val existing = settingsDao.getSettings()
        val entity = SettingsEntity(
            id = 1,
            providerName = settings.providerName,
            apiKey = settings.apiKey,
            modelId = settings.modelId,
            baseUrl = settings.baseUrl,
            extraConfig = settings.extraConfig,
            systemPrompt = settings.systemPrompt,
            temperature = settings.temperature,
            maxIterations = settings.maxIterations,
            runtimePresetId = existing?.runtimePresetId ?: "graph-tools-sequential",
            updatedAt = System.currentTimeMillis()
        )
        settingsDao.insertOrUpdate(entity)
    }
    
    override suspend fun updatePresetId(presetId: String) {
        // 确保有设置记录
        val existing = settingsDao.getSettings()
        if (existing == null) {
            settingsDao.insertOrUpdate(getDefaultSettingsEntity().copy(runtimePresetId = presetId))
        } else {
            settingsDao.updatePresetId(presetId)
        }
    }
    
    private fun getDefaultSettings(): StoredSettings {
        return StoredSettings(
            providerName = Provider.SILICONFLOW.name,
            apiKey = "",
            modelId = Provider.SILICONFLOW.defaultModelId,
            baseUrl = Provider.SILICONFLOW.defaultBaseUrl,
            extraConfig = "",
            systemPrompt = "",
            temperature = "0.7",
            maxIterations = "50"
        )
    }
    
    private fun getDefaultSettingsEntity(): SettingsEntity {
        return SettingsEntity(
            id = 1,
            providerName = Provider.SILICONFLOW.name,
            apiKey = "",
            modelId = Provider.SILICONFLOW.defaultModelId,
            baseUrl = Provider.SILICONFLOW.defaultBaseUrl,
            extraConfig = "",
            systemPrompt = "",
            temperature = "0.7",
            maxIterations = "50",
            runtimePresetId = "graph-tools-sequential",
            updatedAt = System.currentTimeMillis()
        )
    }
}

private fun SettingsEntity.toStoredSettings(): StoredSettings {
    return StoredSettings(
        providerName = providerName,
        apiKey = apiKey,
        modelId = modelId,
        baseUrl = baseUrl,
        extraConfig = extraConfig,
        systemPrompt = systemPrompt,
        temperature = temperature,
        maxIterations = maxIterations
    )
}
