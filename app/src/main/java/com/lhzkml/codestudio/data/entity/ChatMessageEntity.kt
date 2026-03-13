package com.lhzkml.codestudio.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["session_id"])]
)
internal data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    
    @ColumnInfo(name = "role")
    val role: String,
    
    @ColumnInfo(name = "content")
    val content: String,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Long
)
