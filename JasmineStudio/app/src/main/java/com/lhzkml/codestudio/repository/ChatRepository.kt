package com.lhzkml.codestudio.repository

import android.content.ContentValues
import com.lhzkml.codestudio.data.ChatDatabaseHelper
import com.lhzkml.codestudio.ChatMessage
import com.lhzkml.codestudio.MessageRole
import kotlinx.coroutines.flow.*


internal interface ChatRepository {
    val messagesFlow: Flow<List<ChatMessage>>
    suspend fun loadMessages(): List<ChatMessage>
    suspend fun saveMessages(messages: List<ChatMessage>)
}

internal class ChatRepositoryImpl(
    private val dbHelper: ChatDatabaseHelper
) : ChatRepository {

    private val DEFAULT_SESSION_ID = "default_session"
    private val _messagesFlow = MutableStateFlow<List<ChatMessage>>(emptyList())
    override val messagesFlow: Flow<List<ChatMessage>> = _messagesFlow.asStateFlow()

    init {
        // Initialize from DB
        val messages = loadFromDb(DEFAULT_SESSION_ID)
        _messagesFlow.value = messages
    }

    private fun loadFromDb(sessionId: String): List<ChatMessage> {
        val db = dbHelper.readableDatabase
        val messages = mutableListOf<ChatMessage>()
        
        // Ensure session exists
        ensureSessionExists(sessionId)

        val cursor = db.query(
            ChatDatabaseHelper.TABLE_MESSAGES,
            null,
            "${ChatDatabaseHelper.COLUMN_MESSAGE_SESSION_ID} = ?",
            arrayOf(sessionId),
            null,
            null,
            "${ChatDatabaseHelper.COLUMN_MESSAGE_TIMESTAMP} ASC"
        )

        with(cursor) {
            while (moveToNext()) {
                val roleStr = getString(getColumnIndexOrThrow(ChatDatabaseHelper.COLUMN_MESSAGE_ROLE))
                val content = getString(getColumnIndexOrThrow(ChatDatabaseHelper.COLUMN_MESSAGE_CONTENT))
                val id = getLong(getColumnIndexOrThrow(ChatDatabaseHelper.COLUMN_MESSAGE_ID))
                
                messages.add(
                    ChatMessage(
                        id = id,
                        role = runCatching { MessageRole.valueOf(roleStr) }.getOrDefault(MessageRole.System),
                        text = content
                    )
                )
            }
            close()
        }
        return messages
    }

    private fun ensureSessionExists(sessionId: String) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put(ChatDatabaseHelper.COLUMN_SESSION_ID, sessionId)
            put(ChatDatabaseHelper.COLUMN_SESSION_TITLE, "Main Chat")
            put(ChatDatabaseHelper.COLUMN_SESSION_CREATED_AT, System.currentTimeMillis())
        }
        db.insertWithOnConflict(
            ChatDatabaseHelper.TABLE_SESSIONS,
            null,
            cv,
            android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE
        )
    }

    override suspend fun loadMessages(): List<ChatMessage> {
        return loadFromDb(DEFAULT_SESSION_ID)
    }

    override suspend fun saveMessages(messages: List<ChatMessage>) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            // Replicating Room implementation's clear and re-insert behavior for compatibility
            db.delete(
                ChatDatabaseHelper.TABLE_MESSAGES,
                "${ChatDatabaseHelper.COLUMN_MESSAGE_SESSION_ID} = ?",
                arrayOf(DEFAULT_SESSION_ID)
            )

            messages.forEach { msg ->
                val cv = ContentValues().apply {
                    put(ChatDatabaseHelper.COLUMN_MESSAGE_SESSION_ID, DEFAULT_SESSION_ID)
                    put(ChatDatabaseHelper.COLUMN_MESSAGE_ROLE, msg.role.name)
                    put(ChatDatabaseHelper.COLUMN_MESSAGE_CONTENT, msg.text)
                    put(ChatDatabaseHelper.COLUMN_MESSAGE_TIMESTAMP, System.currentTimeMillis())
                }
                db.insert(ChatDatabaseHelper.TABLE_MESSAGES, null, cv)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        _messagesFlow.value = loadFromDb(DEFAULT_SESSION_ID)
    }
}

