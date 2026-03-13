package com.lhzkml.codestudio.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
internal data class SettingsEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = 1, // 只有一条设置记录
    
    @ColumnInfo(name = "provider_name")
    val providerName: String,
    
    @ColumnInfo(name = "api_key")
    val apiKey: String,
    
    @ColumnInfo(name = "model_id")
    val modelId: String,
    
    @ColumnInfo(name = "base_url")
    val baseUrl: String,
    
    @ColumnInfo(name = "extra_config")
    val extraConfig: String,
    
    @ColumnInfo(name = "system_prompt")
    val systemPrompt: String,
    
    @ColumnInfo(name = "temperature")
    val temperature: String,
    
    @ColumnInfo(name = "max_iterations")
    val maxIterations: String,
    
    @ColumnInfo(name = "runtime_preset_id")
    val runtimePresetId: String,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
