import java.util.concurrent.ConcurrentHashMap
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 助手任务存储库（深度对齐版）
 * 负责任务的持久化状态管理与线程安全操作。
 */
class TaskStore {
    private val tasks = ConcurrentHashMap<String, ScheduledTask>()
    private val mutex = Mutex()

    /**
     * 添加新任务，支持 Cron 初始时间计算
     */
    suspend fun addTask(
        description: String,
        prompt: String,
        scheduledAtEpochMs: Long,
        cron: String? = null
    ): ScheduledTask = mutex.withLock {
        val now = System.currentTimeMillis()
        val effectiveScheduledAt = if (cron != null && scheduledAtEpochMs == 0L) {
            try {
                CronExpression(cron).nextAfter(now) ?: now
            } catch (_: Exception) {
                now
            }
        } else {
            scheduledAtEpochMs
        }

        val task = ScheduledTask(
            id = UUID.randomUUID().toString(),
            description = description,
            prompt = prompt,
            scheduledAtEpochMs = effectiveScheduledAt,
            createdAtEpochMs = now,
            cron = cron,
            status = TaskStatus.PENDING
        )
        tasks[task.id] = task
        return task
    }

    fun getTask(id: String): ScheduledTask? = tasks[id]

    fun getAllTasks(): List<ScheduledTask> = tasks.values.toList()

    fun getPendingTasks(): List<ScheduledTask> = tasks.values.filter { it.status == TaskStatus.PENDING }

    /**
     * 获取所有超过执行时间且处于 PENDING 状态的任务
     */
    fun getDueTasks(): List<ScheduledTask> {
        val now = System.currentTimeMillis()
        return tasks.values.filter { it.scheduledAtEpochMs <= now && it.status == TaskStatus.PENDING }
    }

    fun updateTask(task: ScheduledTask) {
        tasks[task.id] = task
    }

    suspend fun removeTask(id: String): Boolean = mutex.withLock {
        tasks.remove(id) != null
    }

    fun clear() {
        tasks.clear()
    }
}
