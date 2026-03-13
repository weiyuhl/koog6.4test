package com.lhzkml.codestudio.di

import android.content.Context
import com.lhzkml.codestudio.data.ChatDatabaseHelper
import com.lhzkml.codestudio.data.SettingsDataStore
import com.lhzkml.codestudio.repository.ChatRepository
import com.lhzkml.codestudio.repository.ChatRepositoryImpl
import com.lhzkml.codestudio.repository.SettingsRepository
import com.lhzkml.codestudio.repository.SettingsRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AppModule {
    
    @Provides
    @Singleton
    internal fun provideSettingsDataStore(
        @ApplicationContext context: Context
    ): SettingsDataStore {
        return SettingsDataStore(context)
    }
    
    @Provides
    @Singleton
    internal fun provideChatDatabaseHelper(
        @ApplicationContext context: Context
    ): ChatDatabaseHelper {
        return ChatDatabaseHelper(context)
    }
    
    @Provides
    @Singleton
    internal fun provideSettingsRepository(
        settingsDataStore: SettingsDataStore
    ): SettingsRepository {
        return SettingsRepositoryImpl(settingsDataStore)
    }
    
    @Provides
    @Singleton
    internal fun provideChatRepository(
        dbHelper: ChatDatabaseHelper
    ): ChatRepository {
        return ChatRepositoryImpl(dbHelper)
    }
}
