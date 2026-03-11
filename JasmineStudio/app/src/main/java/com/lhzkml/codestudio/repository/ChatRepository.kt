package com.lhzkml.codestudio.repository

import com.lhzkml.codestudio.ChatMessage
import com.lhzkml.codestudio.LocalStore
import com.lhzkml.codestudio.StoredChatMessage
import com.lhzkml.codestudio.toChatMessage
import com.lhzkml.codestudio.toStoredMessage

internal interface ChatRepository {
    fun loadMessages(): List<ChatMessage>
    fun saveMessages(messages: List<ChatMessage>)
}

internal class ChatRepositoryImpl(
    private val localStore: LocalStore
) : ChatRepository {
    
    override fun loadMessages(): List<ChatMessage> {
        return localStore.loadState().messages.map { it.toChatMessage() }
    }
    
    override fun saveMessages(messages: List<ChatMessage>) {
        localStore.saveMessages(messages.map { it.toStoredMessage() })
    }
}
