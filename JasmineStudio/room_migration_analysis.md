# JasmineStudio 聊天存储改造方案评估 (DataStore -> Room)

## 📌 现状深度问题分析

当前 JasmineStudio 的历史聊天记录采取了极度简化的存储方案：
- **容器**：`androidx.datastore.preferences` (底层基于本地文件流)
- **形式**：整个巨大的聊天上下文被序列化为一个单一的 JSON 字符串（通过 `StoreCodec.encodeMessages`）。
- **读写逻辑**：每次发消息或收消息，App 都会将**所有的甚至数以千计的历史消息**在内存中转为 JSON 字符串，一次性全量覆盖写入本地。重新启动时也会全量解析整个巨型 JSON。

### ⚠️ DataStore 方案在中大型应用中的致命痛点
1. **OOM 内存溢出风险**：随着聊天轮数的堆积（尤其是大模型生成的长文本甚至代码块返回），反序列化和全量重写这一个巨大的 JSON 将导致严重的内存毛刺和 CPU 飙升。
2. **缺乏多会话（Session）支持**：目前全局只有一个 `messages` 键。如果想实现类似 ChatGPT 左侧的“历史会话列表”，在当前的 DataStore JSON 单一数组架构下，维护成本呈指数级上升。
3. **不可局部检索与分页**：目前无法针对历史记录进行搜索，也无法实现“下拉加载上一页”的懒加载分页，只能一次性把所有数据强塞进 UI 层。

---

## 🎯 Room 关系型数据库全案设计

**Room** (Android Jetpack 原生 SQLite ORM 框架) 是解决上述痛点的完美标准答案。

### 1. 实体架构分层设计 (Entities)
引入真正的关系型设计。我们将建立两张表：会话表和消息表，实现真正意义多轮独立会话。

**表1: `ChatSessionEntity` (会话表)**
- `sessionId` (主键, UUID)
- `sessionTitle` (会话标题，例如第一句话的概要)
- `createdAt` (创建时间戳)
- `updatedAt` (最后活跃时间)

**表2: `ChatMessageEntity` (消息详情表)**
- `messageId` (主键, 自增)
- `sessionId` (外键，关联到所属的 Session)
- `role` (角色: User / System / Assistant)
- `fullText` (回答的详细内容)
- `timestamp` (消息发出的时间戳)

### 2. 数据访问层设计 (DAO)
使用 Flow 和 Room 的极棒的响应式绑定特性，替代掉当前的整个 JSON 解析流：
```kotlin
@Dao
interface ChatMessageDao {
    // 按时间倒序分页拉取某个会话的消息，完美支撑懒加载
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sid ORDER BY timestamp ASC")
    fun getMessagesForSession(sid: String): Flow<List<ChatMessageEntity>>

    // 局部极速插入单条消息，从此告别全量复写！
    @Insert
    suspend fun insertMessage(message: ChatMessageEntity)
    
    // 全局关键词搜索支持
    @Query("SELECT * FROM chat_messages WHERE fullText LIKE '%' || :query || '%'")
    suspend fun searchMessages(query: String): List<ChatMessageEntity>
}
```

### 3. Repository 无缝过渡策略
在 `ChatRepositoryImpl` 中，我们将抛弃 `MessagesDataStore`，改为注入 `ChatMessageDao`。
对于顶层的 `ChatViewModel` 而言，数据的读写依然是靠 `val messagesFlow: Flow<List<ChatMessage>>` 和 `suspend fun save() ` 提供，**UI 层的逻辑一行都无需修改（只需稍加调整，支持按最新的 SessionID 加载）**。这正是我们之前剥离出 Repository 模式带来的巨大红利。

### 4. 依赖注入 (Koin)
```kotlin
// 在 AppModule.kt 中只需新增三行即可提供数据库能力
single { 
    Room.databaseBuilder(androidContext(), AppDatabase::class.java, "jasmine_database")
        .fallbackToDestructiveMigration() // 测试期方便改表
        .build()
}
single { get<AppDatabase>().chatMessageDao() }
```

---

## 🚀 迁移实施步骤 (Timeline)

如果您批准此方案，我接下来可以帮您一次性完成如下连贯操作：
1. **基建**：在 `libs.versions.toml` 引入 `room-runtime`、`room-ktx` 和 KSP 编译器支持。
2. **建模**：建立上述两张核心数据表 (`Entity`) 和对应的数据接口 (`Dao`)。
3. **改造 Repo**：在 `ChatRepository` 内无痛切换底层引擎从 JSON 为 Room。
4. **向下兼容（可选）**：写一个一次性的挂载器，如果检测到 DataStore 里面有老的旧 JSON，就在 App 第一次启动时一口气解析并清洗入库到 Room 中的默认 Session 里，保证您的既有对话记录不丢失！

您是否希望我立即为您**执行上述完整的 Room 改造工程**？如果确认，您只需说“实施 Room 方案”即可！
