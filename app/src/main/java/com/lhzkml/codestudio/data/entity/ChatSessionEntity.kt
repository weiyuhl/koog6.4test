package com.lhzkml.codestudio.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_sessions")
internal data class ChatSessionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
