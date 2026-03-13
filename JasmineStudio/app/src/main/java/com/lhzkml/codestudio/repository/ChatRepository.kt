package com.lhzkml.codestudio.repository

import com.lhzkml.codestudio.ChatMessage
import com.lhzkml.codestudio.data.MessagesDataStore
import com.lhzkml.codestudio.toChatMessage
import com.lhzkml.codestudio.toStoredMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal interface ChatRepository {
    val messagesFlow: Flow<List<ChatMessage>>
    suspend fun loadMessages(): List<ChatMessage>
    suspend fun saveMessages(messages: List<ChatMessage>)
}

internal class ChatRepositoryImpl(
    private val messagesDataStore: MessagesDataStore
) : ChatRepository {
    
    override val messagesFlow: Flow<List<ChatMessage>> = 
        messagesDataStore.messagesFlow.map { storedMessages ->
            storedMessages.map { it.toChatMessage() }
        }
    
    override suspend fun loadMessages(): List<ChatMessage> {
        return messagesDataStore.messagesFlow.first().map { it.toChatMessage() }
    }
    
    override suspend fun saveMessages(messages: List<ChatMessage>) {
        messagesDataStore.saveMessages(messages.map { it.toStoredMessage() })
    }
}
