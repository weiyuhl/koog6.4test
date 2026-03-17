package com.lhzkml.jasmine.core.assistant.scheduler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.lhzkml.jasmine.core.assistant.email.*

/**
 * 助手任务调度引擎（深度对齐版）
 * 负责后台轮询并触发到期任务与心跳。
 */
class TaskScheduler(
    private val taskStore: TaskStore,
    private val emailStore: EmailStore? = null,
    private val onTaskDue: suspend (String) -> String, // 执行 Prompts
    private val onAssistantNotification: (String) -> Unit // 向用户展示通知
) {
    private companion object {
        const val POLL_INTERVAL_MS = 60_000L // 每分钟轮询一次
    }

    private var activeJob: Job? = null

    /**
     * 开启后台轮询循环
     */
    fun start(scope: CoroutineScope) {
        if (activeJob?.isActive == true) return
        activeJob = scope.launch {
            while (true) {
                delay(POLL_INTERVAL_MS)
                processDueTasks()
                checkNewEmails()
            }
        }
    }

    private suspend fun processDueTasks() {
        val dueTasks = taskStore.getDueTasks()
        for (task in dueTasks) {
            try {
                // 1. 调用回调执行 AI 任务
                val result = onTaskDue(task.prompt)
                
                // 2. 将提醒内容主动推送到通知/UI（闹钟核心逻辑修复）
                if (result.isNotBlank() && result != "No response") {
                    onAssistantNotification(result)
                }
                
                // 3. 处理执行后状态
                handleTaskCompletion(task, result)
            } catch (e: Exception) {
                // 失败则保留 PENDING，等待下一次轮询重试
                taskStore.updateTask(task.copy(lastResult = "Execution failed: ${e.message}"))
            }
        }
    }

    private suspend fun checkNewEmails() {
        val store = emailStore ?: return
        val accounts = store.getAccounts()
        
        for (acc in accounts) {
            val syncState = store.getSyncState(acc.id)
            // 每 15 分钟检查一次或对齐 AppSettings（此处简化为逻辑演示）
            if (System.currentTimeMillis() - syncState.lastSyncEpochMs < 15 * 60 * 1000) continue

            try {
                val imap = ImapClient(acc.imapHost, acc.imapPort)
                imap.connect()
                imap.login(acc.username.ifEmpty { acc.email }, store.getPassword(acc.id))
                imap.selectInbox()
                
                val unseenUids = imap.searchUnseen()
                val newUids = unseenUids.filter { it > syncState.lastSeenUid }
                
                if (newUids.isNotEmpty()) {
                    val messages = imap.fetchHeaders(newUids.takeLast(5), acc.id)
                    
                    // 构建智能分拣提示词 (Email Triage)
                    val triagePrompt = buildString {
                        appendLine("[EMAIL_TRIAGE] New emails for ${acc.email}. Score relevance 1-5.")
                        appendLine("Only report emails rated 4-5. For others, respond exactly: EMAIL_TRIAGE_OK")
                        messages.forEach { appendLine("- From: ${it.from} | Subject: ${it.subject} | Preview: ${it.preview}") }
                    }
                    
                    val response = onTaskDue(triagePrompt)
                    if (response.trim() != "EMAIL_TRIAGE_OK") {
                        onAssistantNotification(response)
                    }
                    
                    store.setSyncState(syncState.copy(
                        lastSeenUid = newUids.maxByOrNull { it } ?: syncState.lastSeenUid,
                        lastSyncEpochMs = System.currentTimeMillis(),
                        unreadCount = unseenUids.size
                    ))
                }
                imap.logout()
            } catch (_: Exception) { }
        }
    }

    private fun handleTaskCompletion(task: ScheduledTask, result: String) {
        val now = System.currentTimeMillis()
        if (task.cron != null) {
            // 周期性任务：计算下次执行时间，保持 PENDING
            val nextExecution = try {
                CronExpression(task.cron).nextAfter(now)
            } catch (_: Exception) {
                null
            }

            if (nextExecution != null) {
                taskStore.updateTask(task.copy(
                    scheduledAtEpochMs = nextExecution,
                    lastResult = "Executed at $now: $result",
                    status = TaskStatus.PENDING
                ))
            } else {
                // 无效的下次时间则标记完成
                taskStore.updateTask(task.copy(
                    status = TaskStatus.COMPLETED,
                    lastResult = "Executed at $now: $result (no future schedule)"
                ))
            }
        } else {
            // 一次性任务：标记完成
            taskStore.updateTask(task.copy(
                status = TaskStatus.COMPLETED,
                lastResult = "Executed at $now: $result"
            ))
        }
    }

    fun stop() {
        activeJob?.cancel()
        activeJob = null
    }
}
