package com.lhzkml.codestudio.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lhzkml.codestudio.data.entity.SettingsEntity
import com.lhzkml.codestudio.data.entity.GlobalSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface SettingsDao {
    
    // 供应商配置
    @Query("SELECT * FROM settings WHERE provider_name = :providerName")
    fun getProviderSettingsFlow(providerName: String): Flow<SettingsEntity?>
    
    @Query("SELECT * FROM settings WHERE provider_name = :providerName")
    suspend fun getProviderSettings(providerName: String): SettingsEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProviderSettings(settings: SettingsEntity)
    
    @Query("SELECT * FROM settings")
    suspend fun getAllProviderSettings(): List<SettingsEntity>
    
    // 全局设置
    @Query("SELECT * FROM global_settings WHERE id = 1")
    fun getGlobalSettingsFlow(): Flow<GlobalSettingsEntity?>
    
    @Query("SELECT * FROM global_settings WHERE id = 1")
    suspend fun getGlobalSettings(): GlobalSettingsEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGlobalSettings(settings: GlobalSettingsEntity)
}
