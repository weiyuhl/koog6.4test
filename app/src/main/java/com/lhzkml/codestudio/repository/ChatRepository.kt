package com.lhzkml.codestudio.repository

import com.lhzkml.codestudio.data.ChatDatabase
import com.lhzkml.codestudio.data.entity.ChatMessageEntity
import com.lhzkml.codestudio.data.entity.ChatSessionEntity
import com.lhzkml.codestudio.ChatMessage
import com.lhzkml.codestudio.ChatSession
import com.lhzkml.codestudio.MessageRole
import kotlinx.coroutines.flow.*


/**
 * 聊天仓库接口
 * 负责管理会话和消息的数据操作
 */
internal interface ChatRepository {
    /**
     * 会话列表流 - 实时监听数据库中的所有会话
     */
    val sessionsFlow: Flow<List<ChatSession>>
    
    /**
     * 加载消息 - 应用启动时调用，返回空列表并设置为空白状态
     */
    suspend fun loadMessages(): List<ChatMessage>
    
    /**
     * 加载指定会话的消息 - 用于切换到历史会话时加载消息
     */
    suspend fun loadMessagesForSession(sessionId: String): List<ChatMessage>
    
    /**
     * 保存消息 - 如果是空白状态则创建新会话，然后保存消息到数据库
     */
    suspend fun saveMessages(messages: List<ChatMessage>)
    
    /**
     * 重置为空白状态 - 将当前会话 ID 设为 null
     */
    suspend fun resetToBlankState()
    
    /**
     * 切换会话 - 设置当前会话 ID 为指定的会话 ID
     */
    suspend fun switchSession(sessionId: String)
    
    /**
     * 删除会话 - 从数据库删除会话，如果是当前会话则重置为空白状态
     */
    suspend fun deleteSession(sessionId: String)
}

internal class ChatRepositoryImpl(
    private val database: ChatDatabase
) : ChatRepository {

    /**
     * 当前会话 ID
     * null = 空白状态（没有会话）
     * 非 null = 有会话（已创建或已切换到历史会话）
     */
    private var currentSessionId: String? = null
    
    /**
     * 会话列表流
     * 从数据库实时获取所有会话，自动转换为 ChatSession 对象
     */
    override val sessionsFlow: Flow<List<ChatSession>> = database.chatSessionDao()
        .getAllSessionsFlow()
        .map { entities -> entities.map { it.toChatSession() } }

    /**
     * 加载消息
     * 应用启动时调用，设置为空白状态（currentSessionId = null）
     * 返回空列表，不加载任何消息
     */
    override suspend fun loadMessages(): List<ChatMessage> {
        currentSessionId = null
        return emptyList()
    }
    
    /**
     * 加载指定会话的消息
     * 从数据库查询指定会话 ID 的所有消息
     * 转换为 ChatMessage 对象返回
     * 用于：切换到历史会话时加载该会话的消息
     */
    override suspend fun loadMessagesForSession(sessionId: String): List<ChatMessage> {
        return database.chatMessageDao()
            .getMessages(sessionId)
            .map { it.toChatMessage() }
    }

    /**
     * 保存消息
     * 1. 如果 currentSessionId 为 null（空白状态）：
     *    - 生成新的会话 ID（格式：session_时间戳）
     *    - 在数据库中插入新会话记录（标题："新对话"）
     *    - 设置 currentSessionId 为新生成的 ID
     * 2. 将消息列表转换为数据库实体
     * 3. 替换数据库中该会话的所有消息（先删除旧消息，再插入新消息）
     */
    override suspend fun saveMessages(messages: List<ChatMessage>) {
        // 如果是空白状态，创建新会话
        if (currentSessionId == null) {
            currentSessionId = "session_${System.currentTimeMillis()}"
            database.chatSessionDao().insertSession(
                ChatSessionEntity(
                    id = currentSessionId!!,
                    title = "新对话",
                    createdAt = System.currentTimeMillis()
                )
            )
        }
        
        // 保存消息到数据库
        val entities = messages.map { it.toEntity(currentSessionId!!) }
        database.chatMessageDao().replaceMessages(currentSessionId!!, entities)
    }
    
    /**
     * 重置为空白状态
     * 将 currentSessionId 设为 null
     * 不删除数据库中的任何数据
     * 用于：点击"新建对话"按钮后，从有会话状态切换到空白状态
     */
    override suspend fun resetToBlankState() {
        currentSessionId = null
    }
    
    /**
     * 切换会话
     * 将 currentSessionId 设为指定的会话 ID
     * 不进行任何数据库操作
     * 用于：点击历史会话列表项，切换到该会话
     */
    override suspend fun switchSession(sessionId: String) {
        currentSessionId = sessionId
    }
    
    /**
     * 删除会话
     * 1. 从数据库删除指定的会话（级联删除该会话的所有消息）
     * 2. 如果删除的是当前会话，将 currentSessionId 设为 null（空白状态）
     */
    override suspend fun deleteSession(sessionId: String) {
        database.chatSessionDao().deleteSession(sessionId)
        if (currentSessionId == sessionId) {
            currentSessionId = null
        }
    }
    
    /**
     * 数据库实体转换为领域模型
     * ChatMessageEntity -> ChatMessage
     */
    private fun ChatMessageEntity.toChatMessage(): ChatMessage {
        return ChatMessage(
            id = id,
            role = runCatching { MessageRole.valueOf(role) }.getOrDefault(MessageRole.System),
            text = content
        )
    }
    
    /**
     * 领域模型转换为数据库实体
     * ChatMessage -> ChatMessageEntity
     */
    private fun ChatMessage.toEntity(sessionId: String): ChatMessageEntity {
        return ChatMessageEntity(
            id = if (id > 0) id else 0,
            sessionId = sessionId,
            role = role.name,
            content = text,
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * 会话实体转换为领域模型
     * ChatSessionEntity -> ChatSession
     */
    private fun ChatSessionEntity.toChatSession(): ChatSession {
        return ChatSession(
            id = id,
            title = title,
            createdAt = createdAt
        )
    }
}
