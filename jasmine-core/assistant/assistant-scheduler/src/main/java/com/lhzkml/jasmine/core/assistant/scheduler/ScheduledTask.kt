package com.lhzkml.jasmine.core.assistant.scheduler

import kotlinx.serialization.Serializable

/**
 * 任务状态枚举
 */
@Serializable
enum class TaskStatus { 
    PENDING,    // 待处理
    COMPLETED   // 已执行
}

/**
 * 助手调度任务定义（增强版）
 * 对齐 Kai 原生模型，支持任务指令存储与执行状态维护。
 */
@Serializable
data class ScheduledTask(
    val id: String,
    val description: String,
    val prompt: String,                    // AI 到期执行的具体指令
    val scheduledAtEpochMs: Long,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val cron: String? = null,              // 周期性任务 Cron 表达式
    val status: TaskStatus = TaskStatus.PENDING,
    val lastResult: String? = null         // 最近一次执行的结果快照
)
