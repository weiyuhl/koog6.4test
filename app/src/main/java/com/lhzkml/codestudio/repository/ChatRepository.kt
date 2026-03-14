package com.lhzkml.codestudio.repository

import com.lhzkml.codestudio.data.ChatDatabase
import com.lhzkml.codestudio.data.entity.ChatMessageEntity
import com.lhzkml.codestudio.data.entity.ChatSessionEntity
import com.lhzkml.codestudio.ChatMessage
import com.lhzkml.codestudio.ChatSession
import com.lhzkml.codestudio.MessageRole
import kotlinx.coroutines.flow.*


internal interface ChatRepository {
    val messagesFlow: Flow<List<ChatMessage>>
    val sessionsFlow: Flow<List<ChatSession>>
    suspend fun loadMessages(): List<ChatMessage>
    suspend fun saveMessages(messages: List<ChatMessage>)
    suspend fun loadSessions(): List<ChatSession>
    suspend fun createNewSession(): String
    suspend fun switchSession(sessionId: String)
    suspend fun deleteSession(sessionId: String)
    suspend fun getCurrentSessionId(): String
}

internal class ChatRepositoryImpl(
    private val database: ChatDatabase
) : ChatRepository {

    private var currentSessionId = "default_session"
    
    override val messagesFlow: Flow<List<ChatMessage>> = database.chatMessageDao()
        .getMessagesFlow(currentSessionId)
        .map { entities -> entities.map { it.toChatMessage() } }
    
    override val sessionsFlow: Flow<List<ChatSession>> = database.chatSessionDao()
        .getAllSessionsFlow()
        .map { entities -> entities.map { it.toChatSession() } }

    init {
        // Ensure session exists on initialization
    }

    override suspend fun loadMessages(): List<ChatMessage> {
        ensureSessionExists(currentSessionId)
        return database.chatMessageDao()
            .getMessages(currentSessionId)
            .map { it.toChatMessage() }
    }

    override suspend fun saveMessages(messages: List<ChatMessage>) {
        ensureSessionExists(currentSessionId)
        val entities = messages.map { it.toEntity(currentSessionId) }
        database.chatMessageDao().replaceMessages(currentSessionId, entities)
    }
    
    override suspend fun loadSessions(): List<ChatSession> {
        return database.chatSessionDao()
            .getAllSessions()
            .map { it.toChatSession() }
    }
    
    override suspend fun createNewSession(): String {
        val sessionId = "session_${System.currentTimeMillis()}"
        val title = "新对话"
        database.chatSessionDao().insertSession(
            ChatSessionEntity(
                id = sessionId,
                title = title,
                createdAt = System.currentTimeMillis()
            )
        )
        currentSessionId = sessionId
        return sessionId
    }
    
    override suspend fun switchSession(sessionId: String) {
        ensureSessionExists(sessionId)
        currentSessionId = sessionId
    }
    
    override suspend fun deleteSession(sessionId: String) {
        database.chatSessionDao().deleteSession(sessionId)
        // 如果删除的是当前会话，切换到默认会话或创建新会话
        if (currentSessionId == sessionId) {
            val sessions = loadSessions()
            currentSessionId = sessions.firstOrNull()?.id ?: run {
                createNewSession()
            }
        }
    }
    
    override suspend fun getCurrentSessionId(): String {
        return currentSessionId
    }
    
    private suspend fun ensureSessionExists(sessionId: String) {
        if (database.chatSessionDao().getSession(sessionId) == null) {
            database.chatSessionDao().insertSession(
                ChatSessionEntity(
                    id = sessionId,
                    title = if (sessionId == "default_session") "默认对话" else "新对话",
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }
    
    private fun ChatMessageEntity.toChatMessage(): ChatMessage {
        return ChatMessage(
            id = id,
            role = runCatching { MessageRole.valueOf(role) }.getOrDefault(MessageRole.System),
            text = content
        )
    }
    
    private fun ChatMessage.toEntity(sessionId: String): ChatMessageEntity {
        return ChatMessageEntity(
            id = if (id > 0) id else 0,
            sessionId = sessionId,
            role = role.name,
            content = text,
            timestamp = System.currentTimeMillis()
        )
    }
    
    private fun ChatSessionEntity.toChatSession(): ChatSession {
        return ChatSession(
            id = id,
            title = title,
            createdAt = createdAt
        )
    }
}

