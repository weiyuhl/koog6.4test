package com.lhzkml.codestudio.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.lhzkml.codestudio.data.dao.ChatMessageDao
import com.lhzkml.codestudio.data.dao.ChatSessionDao
import com.lhzkml.codestudio.data.dao.SettingsDao
import com.lhzkml.codestudio.data.entity.ChatMessageEntity
import com.lhzkml.codestudio.data.entity.ChatSessionEntity
import com.lhzkml.codestudio.data.entity.SettingsEntity
import com.lhzkml.codestudio.data.entity.GlobalSettingsEntity

@Database(
    entities = [
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        SettingsEntity::class,
        GlobalSettingsEntity::class
    ],
    version = 3,
    exportSchema = false
)
internal abstract class ChatDatabase : RoomDatabase() {
    abstract fun chatSessionDao(): ChatSessionDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun settingsDao(): SettingsDao
}
