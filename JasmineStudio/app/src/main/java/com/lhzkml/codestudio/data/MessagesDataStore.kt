package com.lhzkml.codestudio.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lhzkml.codestudio.StoredChatMessage
import com.lhzkml.codestudio.StoreCodec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.messagesDataStore: DataStore<Preferences> by preferencesDataStore(name = "messages")

internal class MessagesDataStore(
    private val context: Context
) {
    private val dataStore = context.messagesDataStore
    
    private object Keys {
        val MESSAGES = stringPreferencesKey("messages")
    }
    
    val messagesFlow: Flow<List<StoredChatMessage>> = dataStore.data.map { preferences ->
        val raw = preferences[Keys.MESSAGES]
        StoreCodec.decodeMessages(raw).ifEmpty {
            listOf(
                StoredChatMessage(
                    id = 0L,
                    role = "System",
                    label = "欢迎",
                    text = "欢迎使用。请先点左上角菜单，在抽屉底部进入设置完成模型配置，然后开始聊天"
                )
            )
        }
    }
    
    suspend fun saveMessages(messages: List<StoredChatMessage>) {
        dataStore.edit { preferences ->
            preferences[Keys.MESSAGES] = StoreCodec.encodeMessages(messages)
        }
    }
}
