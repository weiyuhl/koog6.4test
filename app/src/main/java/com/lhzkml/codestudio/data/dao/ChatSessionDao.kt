package com.lhzkml.codestudio.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lhzkml.codestudio.data.entity.ChatSessionEntity

@Dao
internal interface ChatSessionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSession(session: ChatSessionEntity)
    
    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId")
    suspend fun getSession(sessionId: String): ChatSessionEntity?
    
    @Query("SELECT * FROM chat_sessions ORDER BY created_at DESC")
    suspend fun getAllSessions(): List<ChatSessionEntity>
    
    @Query("SELECT * FROM chat_sessions ORDER BY created_at DESC")
    fun getAllSessionsFlow(): kotlinx.coroutines.flow.Flow<List<ChatSessionEntity>>
    
    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: String)
    
    @Query("UPDATE chat_sessions SET title = :title WHERE id = :sessionId")
    suspend fun updateSessionTitle(sessionId: String, title: String)
}
