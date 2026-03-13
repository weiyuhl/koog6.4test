package com.lhzkml.codestudio.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.lhzkml.codestudio.data.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE session_id = :sessionId ORDER BY timestamp ASC")
    fun getMessagesFlow(sessionId: String): Flow<List<ChatMessageEntity>>
    
    @Query("SELECT * FROM chat_messages WHERE session_id = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessages(sessionId: String): List<ChatMessageEntity>
    
    @Insert
    suspend fun insertMessage(message: ChatMessageEntity)
    
    @Query("DELETE FROM chat_messages WHERE session_id = :sessionId")
    suspend fun deleteMessages(sessionId: String)
    
    @Transaction
    suspend fun replaceMessages(sessionId: String, messages: List<ChatMessageEntity>) {
        deleteMessages(sessionId)
        messages.forEach { insertMessage(it) }
    }
}
