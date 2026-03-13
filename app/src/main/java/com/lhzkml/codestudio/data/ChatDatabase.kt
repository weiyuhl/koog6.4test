package com.lhzkml.codestudio.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.lhzkml.codestudio.data.dao.ChatMessageDao
import com.lhzkml.codestudio.data.dao.ChatSessionDao
import com.lhzkml.codestudio.data.entity.ChatMessageEntity
import com.lhzkml.codestudio.data.entity.ChatSessionEntity

@Database(
    entities = [ChatSessionEntity::class, ChatMessageEntity::class],
    version = 1,
    exportSchema = false
)
internal abstract class ChatDatabase : RoomDatabase() {
    abstract fun chatSessionDao(): ChatSessionDao
    abstract fun chatMessageDao(): ChatMessageDao
}
