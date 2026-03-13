package com.lhzkml.codestudio.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lhzkml.codestudio.StoredSettings
import com.lhzkml.codestudio.StoreCodec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

internal class SettingsDataStore(
    private val context: Context
) {
    private val dataStore = context.settingsDataStore
    
    private object Keys {
        val PROVIDER_NAME = stringPreferencesKey("provider_name")
        val API_KEY = stringPreferencesKey("api_key")
        val MODEL_ID = stringPreferencesKey("model_id")
        val BASE_URL = stringPreferencesKey("base_url")
        val EXTRA_CONFIG = stringPreferencesKey("extra_config")
        val SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        val TEMPERATURE = stringPreferencesKey("temperature")
        val MAX_ITERATIONS = stringPreferencesKey("max_iterations")
        val RUNTIME_PRESET_ID = stringPreferencesKey("runtime_preset_id")
    }
    
    val settingsFlow: Flow<StoredSettings> = dataStore.data.map { preferences ->
        StoredSettings(
            providerName = preferences[Keys.PROVIDER_NAME] ?: "OPENAI",
            apiKey = preferences[Keys.API_KEY] ?: "",
            modelId = preferences[Keys.MODEL_ID] ?: "gpt-4o-mini",
            baseUrl = preferences[Keys.BASE_URL] ?: "https://api.openai.com",
            extraConfig = preferences[Keys.EXTRA_CONFIG] ?: "",
            systemPrompt = preferences[Keys.SYSTEM_PROMPT] ?: "",
            temperature = preferences[Keys.TEMPERATURE] ?: "0.2",
            maxIterations = preferences[Keys.MAX_ITERATIONS] ?: "50"
        )
    }
    
    val presetIdFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[Keys.RUNTIME_PRESET_ID]
    }
    
    suspend fun updateSettings(settings: StoredSettings) {
        dataStore.edit { preferences ->
            preferences[Keys.PROVIDER_NAME] = settings.providerName
            preferences[Keys.API_KEY] = settings.apiKey
            preferences[Keys.MODEL_ID] = settings.modelId
            preferences[Keys.BASE_URL] = settings.baseUrl
            preferences[Keys.EXTRA_CONFIG] = settings.extraConfig
            preferences[Keys.SYSTEM_PROMPT] = settings.systemPrompt
            preferences[Keys.TEMPERATURE] = settings.temperature
            preferences[Keys.MAX_ITERATIONS] = settings.maxIterations
        }
    }
    
    suspend fun updatePresetId(presetId: String) {
        dataStore.edit { preferences ->
            preferences[Keys.RUNTIME_PRESET_ID] = presetId
        }
    }
}
