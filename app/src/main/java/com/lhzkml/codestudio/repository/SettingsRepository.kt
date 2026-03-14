package com.lhzkml.codestudio.repository

import com.lhzkml.codestudio.Provider
import com.lhzkml.codestudio.data.dao.SettingsDao
import com.lhzkml.codestudio.data.entity.GlobalSettingsEntity
import com.lhzkml.codestudio.data.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

internal data class StoredSettings(
    val providerName: String,
    val apiKey: String,
    val modelId: String,
    val baseUrl: String,
    val extraConfig: String,
    val systemPrompt: String,
    val temperature: String,
    val maxIterations: String
)

@Singleton
internal class SettingsRepository @Inject constructor(
    private val settingsDao: SettingsDao
) {
    
    // 当前选中的供应商配置
    val settingsFlow: Flow<StoredSettings> = combine(
        settingsDao.getGlobalSettingsFlow(),
        settingsDao.getGlobalSettingsFlow().map { it?.currentProvider ?: Provider.SILICONFLOW.name }
    ) { globalSettings, currentProvider ->
        val providerSettings = settingsDao.getProviderSettings(currentProvider)
        
        StoredSettings(
            providerName = currentProvider,
            apiKey = providerSettings?.apiKey ?: "",
            modelId = providerSettings?.modelId ?: getDefaultModelId(currentProvider),
            baseUrl = providerSettings?.baseUrl ?: getDefaultBaseUrl(currentProvider),
            extraConfig = providerSettings?.extraConfig ?: "",
            systemPrompt = globalSettings?.systemPrompt ?: "",
            temperature = globalSettings?.temperature ?: "0.2",
            maxIterations = globalSettings?.maxIterations ?: "50"
        )
    }
    
    val presetIdFlow: Flow<String> = settingsDao.getGlobalSettingsFlow()
        .map { it?.runtimePresetId ?: "graph-tools-sequential" }
    
    // 启用的供应商列表
    val enabledProvidersFlow: Flow<Set<String>> = settingsDao.getGlobalSettingsFlow()
        .map { globalSettings ->
            val enabledStr = globalSettings?.enabledProviders ?: ""
            if (enabledStr.isBlank()) {
                // 默认启用所有供应商
                Provider.entries.filter { it.isSupportedOnAndroid }.map { it.name }.toSet()
            } else {
                enabledStr.split(",").filter { it.isNotBlank() }.toSet()
            }
        }
    
    suspend fun updateSettings(settings: StoredSettings) {
        // 更新供应商配置
        settingsDao.insertProviderSettings(
            SettingsEntity(
                providerName = settings.providerName,
                apiKey = settings.apiKey,
                modelId = settings.modelId,
                baseUrl = settings.baseUrl,
                extraConfig = settings.extraConfig
            )
        )
        
        // 更新全局配置
        val globalSettings = settingsDao.getGlobalSettings() ?: GlobalSettingsEntity(
            id = 1,
            currentProvider = settings.providerName,
            enabledProviders = Provider.entries.filter { it.isSupportedOnAndroid }.joinToString(",") { it.name },
            systemPrompt = settings.systemPrompt,
            temperature = settings.temperature,
            maxIterations = settings.maxIterations,
            runtimePresetId = "graph-tools-sequential"
        )
        
        settingsDao.insertGlobalSettings(
            globalSettings.copy(
                systemPrompt = settings.systemPrompt,
                temperature = settings.temperature,
                maxIterations = settings.maxIterations
            )
        )
    }
    
    suspend fun updateCurrentProvider(providerName: String) {
        val globalSettings = settingsDao.getGlobalSettings() ?: GlobalSettingsEntity(
            id = 1,
            currentProvider = providerName,
            enabledProviders = Provider.entries.filter { it.isSupportedOnAndroid }.joinToString(",") { it.name },
            systemPrompt = "",
            temperature = "0.2",
            maxIterations = "50",
            runtimePresetId = "graph-tools-sequential"
        )
        
        settingsDao.insertGlobalSettings(
            globalSettings.copy(currentProvider = providerName)
        )
    }
    
    suspend fun updatePresetId(presetId: String) {
        val globalSettings = settingsDao.getGlobalSettings() ?: GlobalSettingsEntity(
            id = 1,
            currentProvider = Provider.SILICONFLOW.name,
            enabledProviders = Provider.entries.filter { it.isSupportedOnAndroid }.joinToString(",") { it.name },
            systemPrompt = "",
            temperature = "0.2",
            maxIterations = "50",
            runtimePresetId = presetId
        )
        
        settingsDao.insertGlobalSettings(
            globalSettings.copy(runtimePresetId = presetId)
        )
    }
    
    suspend fun updateEnabledProviders(enabledProviders: Set<String>) {
        val globalSettings = settingsDao.getGlobalSettings() ?: GlobalSettingsEntity(
            id = 1,
            currentProvider = Provider.SILICONFLOW.name,
            enabledProviders = enabledProviders.joinToString(","),
            systemPrompt = "",
            temperature = "0.2",
            maxIterations = "50",
            runtimePresetId = "graph-tools-sequential"
        )
        
        settingsDao.insertGlobalSettings(
            globalSettings.copy(enabledProviders = enabledProviders.joinToString(","))
        )
    }
    
    private fun getDefaultModelId(providerName: String): String {
        return Provider.entries.firstOrNull { it.name == providerName }?.defaultModelId ?: ""
    }
    
    private fun getDefaultBaseUrl(providerName: String): String {
        return Provider.entries.firstOrNull { it.name == providerName }?.defaultBaseUrl ?: ""
    }
}
