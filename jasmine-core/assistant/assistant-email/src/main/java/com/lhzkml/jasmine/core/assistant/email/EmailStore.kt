package com.lhzkml.jasmine.core.assistant.email

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

/**
 * 助手邮件存储库（原子化版）
 * 负责账号、同步状态的持久化封装。
 * 注意：由于 AppSettings 属于 App 层，这里采用内存映射+回调持久化的简化模式，
 * 或要求 Runtime 注入初始数据。
 */
class EmailStore(
    initialAccounts: List<EmailAccount> = emptyList(),
    private val onSaveAccounts: (List<EmailAccount>) -> Unit = {},
    private val onSaveSyncState: (EmailSyncState) -> Unit = {}
) {
    private val accounts = ConcurrentHashMap<String, EmailAccount>().apply {
        initialAccounts.forEach { put(it.id, it) }
    }
    private val syncStates = ConcurrentHashMap<String, EmailSyncState>()
    private val passwords = ConcurrentHashMap<String, String>()
    private val mutex = Mutex()

    fun getAccounts(): List<EmailAccount> = accounts.values.toList()

    fun getAccount(id: String): EmailAccount? = accounts[id]

    suspend fun addAccount(account: EmailAccount, password: String) = mutex.withLock {
        accounts[account.id] = account
        passwords[account.id] = password
        onSaveAccounts(accounts.values.toList())
    }

    suspend fun removeAccount(id: String) = mutex.withLock {
        accounts.remove(id)
        passwords.remove(id)
        syncStates.remove(id)
        onSaveAccounts(accounts.values.toList())
    }

    fun getPassword(accountId: String): String = passwords[accountId] ?: ""

    fun setSyncState(state: EmailSyncState) {
        syncStates[state.accountId] = state
        onSaveSyncState(state)
    }

    fun getSyncState(accountId: String): EmailSyncState {
        return syncStates[accountId] ?: EmailSyncState(accountId)
    }
}
