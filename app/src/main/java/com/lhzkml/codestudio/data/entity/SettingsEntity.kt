package com.lhzkml.codestudio.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
internal data class SettingsEntity(
    @PrimaryKey
    @ColumnInfo(name = "provider_name")
    val providerName: String, // 使用供应商名称作为主键，每个供应商独立存储
    
    @ColumnInfo(name = "api_key")
    val apiKey: String,
    
    @ColumnInfo(name = "model_id")
    val modelId: String,
    
    @ColumnInfo(name = "base_url")
    val baseUrl: String,
    
    @ColumnInfo(name = "extra_config")
    val extraConfig: String,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

// 全局设置（不依赖供应商）
@Entity(tableName = "global_settings")
internal data class GlobalSettingsEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = 1,
    
    @ColumnInfo(name = "current_provider")
    val currentProvider: String,
    
    @ColumnInfo(name = "enabled_providers")
    val enabledProviders: String, // 逗号分隔的供应商名称列表
    
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
