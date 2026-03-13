package com.lhzkml.codestudio.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lhzkml.codestudio.data.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface SettingsDao {
    
    @Query("SELECT * FROM settings WHERE id = 1")
    fun getSettingsFlow(): Flow<SettingsEntity?>
    
    @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun getSettings(): SettingsEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: SettingsEntity)
    
    @Query("UPDATE settings SET provider_name = :providerName WHERE id = 1")
    suspend fun updateProvider(providerName: String)
    
    @Query("UPDATE settings SET api_key = :apiKey WHERE id = 1")
    suspend fun updateApiKey(apiKey: String)
    
    @Query("UPDATE settings SET model_id = :modelId WHERE id = 1")
    suspend fun updateModelId(modelId: String)
    
    @Query("UPDATE settings SET base_url = :baseUrl WHERE id = 1")
    suspend fun updateBaseUrl(baseUrl: String)
    
    @Query("UPDATE settings SET extra_config = :extraConfig WHERE id = 1")
    suspend fun updateExtraConfig(extraConfig: String)
    
    @Query("UPDATE settings SET system_prompt = :systemPrompt WHERE id = 1")
    suspend fun updateSystemPrompt(systemPrompt: String)
    
    @Query("UPDATE settings SET temperature = :temperature WHERE id = 1")
    suspend fun updateTemperature(temperature: String)
    
    @Query("UPDATE settings SET max_iterations = :maxIterations WHERE id = 1")
    suspend fun updateMaxIterations(maxIterations: String)
    
    @Query("UPDATE settings SET runtime_preset_id = :presetId WHERE id = 1")
    suspend fun updatePresetId(presetId: String)
}
