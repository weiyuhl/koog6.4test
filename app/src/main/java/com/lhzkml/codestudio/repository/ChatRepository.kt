package com.lhzkml.codestudio.repository

import com.lhzkml.codestudio.data.ChatDatabase
import com.lhzkml.codestudio.data.entity.ChatMessageEntity
import com.lhzkml.codestudio.data.entity.ChatSessionEntity
import com.lhzkml.codestudio.ChatMessage
import com.lhzkml.codestudio.MessageRole
import kotlinx.coroutines.flow.*


internal interface ChatRepository {
    val messagesFlow: Flow<List<ChatMessage>>
    suspend fun loadMessages(): List<ChatMessage>
    suspend fun saveMessages(messages: List<ChatMessage>)
}

internal class ChatRepositoryImpl(
    private val database: ChatDatabase
) : ChatRepository {

    private val DEFAULT_SESSION_ID = "default_session"
    
    override val messagesFlow: Flow<List<ChatMessage>> = database.chatMessageDao()
        .getMessagesFlow(DEFAULT_SESSION_ID)
        .map { entities -> entities.map { it.toChatMessage() } }

    init {
        // Ensure session exists on initialization
    }

    override suspend fun loadMessages(): List<ChatMessage> {
        ensureSessionExists()
        return database.chatMessageDao()
            .getMessages(DEFAULT_SESSION_ID)
            .map { it.toChatMessage() }
    }

    override suspend fun saveMessages(messages: List<ChatMessage>) {
        ensureSessionExists()
        val entities = messages.map { it.toEntity(DEFAULT_SESSION_ID) }
        database.chatMessageDao().replaceMessages(DEFAULT_SESSION_ID, entities)
    }
    
    private suspend fun ensureSessionExists() {
        if (database.chatSessionDao().getSession(DEFAULT_SESSION_ID) == null) {
            database.chatSessionDao().insertSession(
                ChatSessionEntity(
                    id = DEFAULT_SESSION_ID,
                    title = "Main Chat",
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
}

