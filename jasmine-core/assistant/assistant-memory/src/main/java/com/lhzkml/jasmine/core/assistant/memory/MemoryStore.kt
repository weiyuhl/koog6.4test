import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 助手记忆系统（强化版本）
 * 支持按分类管理与检索，为 LLM 提供结构化的长效历史记忆。
 */
class MemoryStore {
    
    private val memories = java.util.concurrent.ConcurrentHashMap<String, MemoryEntry>()
    private val mutex = Mutex()
    
    /**
     * 存储或更新一条记忆
     * 如果 key 已存在且内容相同，则增加 hitCount。
     * 如果 content 为空，则视为纯粹的“强化”现有记忆。
     */
    suspend fun store(key: String, content: String, category: String = MemoryEntry.CATEGORY_GENERAL, source: String? = null) = mutex.withLock {
        val existing = memories[key]
        val entry = if (existing != null) {
            val newContent = if (content.isNotEmpty()) content else existing.content
            if (newContent == existing.content) {
                existing.copy(hitCount = existing.hitCount + 1, updatedAt = System.currentTimeMillis(), source = source ?: existing.source)
            } else {
                // 内容变化，重置强化计数
                MemoryEntry(key, newContent, category, source = source)
            }
        } else {
            MemoryEntry(key, content, category, source = source)
        }
        memories[key] = entry
    }

    /**
     * 获取所有记忆条目
     */
    fun getAllMemories(): List<MemoryEntry> = memories.values.toList()

    /**
     * 获取推荐晋升的记忆（被多次强化过的模式）
     */
    fun getPromotionCandidates(threshold: Int = 3): List<MemoryEntry> {
        return memories.values.filter { it.hitCount >= threshold && it.category == MemoryEntry.CATEGORY_LEARNING }
    }

    /**
     * 获取增强版提示词片段，按分类结构化输出
     */
    fun getMemoriesAsPromptSection(): String {
        if (memories.isEmpty()) return ""
        
        val byCategory = memories.values.groupBy { it.category }
        
        return buildString {
            append("\n## Your Memories & Context\n")
            append("These are facts and patterns you have learned about the user and your own performance.\n")
            
            byCategory[MemoryEntry.CATEGORY_PREFERENCE]?.let { list ->
                append("\n### User Preferences\n")
                list.forEach { append("- **${it.key}**: ${it.content}\n") }
            }
            
            byCategory[MemoryEntry.CATEGORY_LEARNING]?.let { list ->
                append("\n### Learned Patterns\n")
                list.forEach { append("- ${it.content} (Reinforced ${it.hitCount}x)\n") }
            }
            
            byCategory[MemoryEntry.CATEGORY_ERROR]?.let { list ->
                append("\n### Past Errors & Resolutions\n")
                list.forEach { append("- **${it.key}**: ${it.content}\n") }
            }

            byCategory[MemoryEntry.CATEGORY_GENERAL]?.let { list ->
                append("\n### General Information\n")
                list.forEach { append("- ${it.key}: ${it.content}\n") }
            }
        }
    }

    /**
     * 获取关于如何使用和管理记忆的元指令
     */
    fun getMemoryInstructions(): String {
        return """
            ## Memory Management Instructions
            - Use `store_memory` for persistent facts (e.g., user's name).
            - Use `learn_memory` (category: PREFERENCE) when the user corrects you or expresses a style preference.
            - Use `learn_memory` (category: ERROR) when you make a mistake and find the resolution.
            - Periodically review memories to ensure they are still accurate.
            - If a memory is no longer true, use `forget_memory`.
        """.trimIndent()
    }

    /**
     * 遗忘一条记忆
     */
    suspend fun forget(key: String) = mutex.withLock {
        memories.remove(key)
    }

    fun clear() {
        memories.clear()
    }
}
