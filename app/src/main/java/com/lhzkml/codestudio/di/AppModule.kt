package com.lhzkml.codestudio.di

import android.content.Context
import androidx.room.Room
import com.lhzkml.codestudio.data.ChatDatabase
import com.lhzkml.codestudio.data.SettingsDataStore
import com.lhzkml.codestudio.repository.ChatRepository
import com.lhzkml.codestudio.repository.ChatRepositoryImpl
import com.lhzkml.codestudio.repository.SettingsRepository
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
    internal fun provideChatDatabase(
        @ApplicationContext context: Context
    ): ChatDatabase {
        return Room.databaseBuilder(
            context,
            ChatDatabase::class.java,
            "chat_studio.db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }
    
    @Provides
    @Singleton
    internal fun provideSettingsRepository(
        database: ChatDatabase
    ): SettingsRepository {
        return SettingsRepository(database.settingsDao())
    }
    
    @Provides
    @Singleton
    internal fun provideChatRepository(
        database: ChatDatabase
    ): ChatRepository {
        return ChatRepositoryImpl(database)
    }
}
