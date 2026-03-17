package com.lhzkml.jasmine.core.assistant.memory

import kotlinx.serialization.Serializable

/**
 * 助手记忆条目（强化版本）
 * 包含命数统计与多维分类
 */
@Serializable
data class MemoryEntry(
    val key: String,
    val content: String,
    val category: String = CATEGORY_GENERAL,
    val hitCount: Int = 1,
    val source: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val CATEGORY_GENERAL = "general"     // 通用记忆
        const val CATEGORY_PREFERENCE = "preference" // 用户偏好
        const val CATEGORY_LEARNING = "learning"   // AI 习得的模式
        const val CATEGORY_ERROR = "error"         // 过往错误与修正方案
    }
}
