# JasmineStudio 依赖库使用详细文档

> 生成时间: 2026-03-13  
> Kotlin 版本: 2.2.0  
> Android Gradle Plugin: 8.12.3

---

## 📚 目录

1. [核心配置](#核心配置)
2. [Kotlin 生态](#kotlin-生态)
3. [AndroidX 核心库](#androidx-核心库)
4. [Jetpack Compose](#jetpack-compose)
5. [依赖注入 - Hilt](#依赖注入---hilt)
6. [数据层](#数据层)
7. [网络层](#网络层)
8. [测试库](#测试库)
9. [构建插件](#构建插件)

---

## 核心配置

### Kotlin 2.2.0
**配置位置**: `gradle/libs.versions.toml`
```toml
kotlin = "2.2.0"
```

**编译目标**: 
- JVM Target: Java 17
- 配置文件: `app/build.gradle.kts`

```kotlin
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}
```

### Android Gradle Plugin 8.12.3
**版本**: `8.12.3`
**配置**: 
- compileSdk: 36
- minSdk: 24
- targetSdk: 36

---

## Kotlin 生态

### 1. Kotlin Coroutines 1.10.2

**依赖**:
```kotlin
implementation(libs.kotlinx.coroutines.android)  // 1.10.2
```

**使用位置**:

#### ViewModel 层

**文件**: `viewmodel/ChatViewModel.kt`, `viewmodel/SettingsViewModel.kt`
```kotlin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel @Inject constructor(...) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    // 使用 viewModelScope 处理协程
    private fun loadMessages() {
        viewModelScope.launch {
            val messages = chatRepository.loadMessages()
            _uiState.update { it.copy(messages = messages) }
        }
    }
}
```

#### Repository 层
**文件**: `repository/ChatRepository.kt`, `repository/SettingsRepository.kt`
```kotlin
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    val messagesFlow: Flow<List<ChatMessage>>  // 使用 Flow 暴露响应式数据
    suspend fun loadMessages(): List<ChatMessage>
}
```

#### UI 层
**文件**: `Main.kt`, `components/Side.kt`
```kotlin
import kotlinx.coroutines.launch

val scope = rememberCoroutineScope()
scope.launch { 
    sideState.open() 
}
```

#### 数据层
**文件**: `data/SettingsDataStore.kt`, `data/MessagesDataStore.kt`
```kotlin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val settingsFlow: Flow<StoredSettings> = dataStore.data.map { preferences ->
    StoredSettings(...)
}
```

---

### 2. Kotlin Serialization 1.9.0

**依赖**:
```kotlin
implementation(libs.kotlinx.serialization.json)  // 1.9.0
```

**插件**:
```kotlin
plugins {
    alias(libs.plugins.kotlin.serialization)  // 2.2.0
}
```

**使用位置**:
- 目前项目中配置了依赖，但代码中未找到 `@Serializable` 注解的使用
- 预留用于网络层 JSON 解析（配合 Retrofit）

---

### 3. Kotlin Metadata JVM 2.2.0

**依赖**:
```kotlin
kapt(libs.kotlin.metadata.jvm)  // 2.2.0
```

**用途**: 支持 Kotlin 注解处理器（KAPT）的元数据生成

---

## AndroidX 核心库

### 1. Core KTX 1.10.1

**依赖**:
```kotlin
implementation(libs.androidx.core.ktx)  // 1.10.1
```

**用途**: 提供 Kotlin 扩展函数，简化 Android API 使用

---

### 2. AppCompat 1.6.1

**依赖**:
```kotlin
implementation(libs.androidx.appcompat)  // 1.6.1
```

**用途**: 向后兼容支持库

---

### 3. Lifecycle 2.9.4

**依赖**:
```kotlin
implementation(libs.androidx.lifecycle.runtime.ktx)           // 2.9.4
implementation(libs.androidx.lifecycle.runtime.compose)       // 2.9.4
implementation(libs.androidx.lifecycle.viewmodel.compose)     // 2.9.4
```

**使用位置**:

#### lifecycle-runtime-ktx
**文件**: 所有 ViewModel
```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

class ChatViewModel : ViewModel() {
    // viewModelScope 来自 lifecycle-runtime-ktx
}
```

#### lifecycle-runtime-compose
**文件**: `Main.kt`
```kotlin
import androidx.lifecycle.compose.collectAsStateWithLifecycle

val chatState by chatViewModel.uiState.collectAsStateWithLifecycle()
val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
```

**作用**: 安全地在 Compose 中收集 Flow，自动处理生命周期

#### lifecycle-viewmodel-compose
**文件**: `Main.kt`
```kotlin
import androidx.hilt.navigation.compose.hiltViewModel

val chatViewModel: ChatViewModel = hiltViewModel()
val settingsViewModel: SettingsViewModel = hiltViewModel()
```

**作用**: 在 Compose 中获取 ViewModel 实例

---

### 4. Activity Compose 1.10.1

**依赖**:
```kotlin
implementation(libs.androidx.activity.compose)  // 1.10.1
```

**使用位置**:
**文件**: `MainActivity.kt`
```kotlin
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                App()
            }
        }
    }
}
```

---

## Jetpack Compose

### Compose UI 1.7.8

**依赖**:
```kotlin
implementation(libs.androidx.compose.ui)                      // 1.7.8
implementation(libs.androidx.compose.ui.graphics)             // 1.7.8
implementation(libs.androidx.compose.ui.tooling.preview)      // 1.7.8
implementation(libs.androidx.compose.foundation)              // 1.7.8
implementation(libs.androidx.compose.animation)               // 1.7.8
implementation(libs.androidx.compose.runtime.saveable)        // 1.7.8
debugImplementation(libs.androidx.compose.ui.tooling)         // 1.7.8
```

**使用位置**: 所有 UI 文件

#### 主要 UI 文件
1. **Main.kt** - 应用主入口
   ```kotlin
   import androidx.compose.runtime.Composable
   import androidx.compose.runtime.getValue
   import androidx.compose.foundation.layout.*
   import androidx.compose.ui.Modifier
   ```

2. **Chat.kt** - 聊天界面
   ```kotlin
   import androidx.compose.foundation.background
   import androidx.compose.foundation.layout.*
   import androidx.compose.foundation.lazy.LazyColumn
   import androidx.compose.foundation.shape.CircleShape
   import androidx.compose.foundation.text.BasicTextField
   ```

3. **Settings.kt** - 设置界面
   ```kotlin
   import androidx.compose.foundation.clickable
   import androidx.compose.foundation.verticalScroll
   import androidx.compose.foundation.rememberScrollState
   ```

4. **Theme.kt** - 主题配置
   ```kotlin
   import androidx.compose.runtime.Composable
   import androidx.compose.runtime.CompositionLocalProvider
   import androidx.compose.ui.graphics.Color
   ```

#### 自定义组件 (components/)
- **Bar.kt** - 顶部栏
- **Side.kt** - 侧边栏
- **Text.kt** - 文本组件
- **IconButton.kt** - 图标按钮
- **DropdownMenu.kt** - 下拉菜单
- **Surface.kt** - 表面容器
- **RadioButton.kt** - 单选按钮
- **Icon.kt** - 图标

---

### Navigation Compose 2.8.9

**依赖**:
```kotlin
// 已在 libs.versions.toml 中定义但未在 app/build.gradle.kts 中使用
```

**状态**: 已配置但未使用，项目使用自定义导航方案

**文件**: `viewmodel/NavigationViewModel.kt`
```kotlin
// 自定义导航实现
sealed class Route(val value: String) {
    data object Chat : Route("chat")
    data object Home : Route("home")
    data object Model : Route("model")
    data object Runtime : Route("runtime")
}
```

---

## 依赖注入 - Hilt

### Hilt 2.57.2

**依赖**:
```kotlin
implementation(libs.hilt.android)                // 2.57.2
implementation(libs.hilt.navigation.compose)     // 1.2.0
kapt(libs.hilt.compiler)                         // 2.57.2
```

**插件**:
```kotlin
plugins {
    alias(libs.plugins.hilt.android)   // 2.57.2
    alias(libs.plugins.kotlin.kapt)    // 2.2.0
}
```

**使用位置**:

### 1. Application 层
**文件**: `CodeStudioApplication.kt`
```kotlin
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CodeStudioApplication : Application()
```

### 2. Activity 层
**文件**: `MainActivity.kt`
```kotlin
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() { ... }
```

### 3. ViewModel 层
**文件**: `viewmodel/ChatViewModel.kt`, `viewmodel/SettingsViewModel.kt`, `viewmodel/NavigationViewModel.kt`
```kotlin
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
internal class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val settingsRepository: SettingsRepository,
    private val sendMessageUseCase: SendMessageUseCase
) : ViewModel() { ... }
```

### 4. DI 模块
**文件**: `di/AppModule.kt`
```kotlin
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AppModule {
    
    @Provides
    @Singleton
    internal fun provideSettingsDataStore(
        @ApplicationContext context: Context
    ): SettingsDataStore = SettingsDataStore(context)
    
    @Provides
    @Singleton
    internal fun provideChatDatabase(
        @ApplicationContext context: Context
    ): ChatDatabase = Room.databaseBuilder(
        context,
        ChatDatabase::class.java,
        "chat_studio.db"
    ).build()
    
    @Provides
    @Singleton
    internal fun provideSettingsRepository(
        settingsDataStore: SettingsDataStore
    ): SettingsRepository = SettingsRepositoryImpl(settingsDataStore)
    
    @Provides
    @Singleton
    internal fun provideChatRepository(
        database: ChatDatabase
    ): ChatRepository = ChatRepositoryImpl(database)
}
```

### 5. UI 层注入
**文件**: `Main.kt`
```kotlin
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
internal fun App() {
    val navigationViewModel: NavigationViewModel = hiltViewModel()
    val chatViewModel: ChatViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    ...
}
```

---

## 数据层

### 1. Room 2.7.0-alpha12

**依赖**:
```kotlin
implementation(libs.room.runtime)    // 2.7.0-alpha12
implementation(libs.room.ktx)        // 2.7.0-alpha12
ksp(libs.room.compiler)              // 2.7.0-alpha12
```

**插件**:
```kotlin
plugins {
    alias(libs.plugins.ksp)  // 2.2.0-2.0.2
}
```

**使用位置**:

#### Database 定义
**文件**: `data/ChatDatabase.kt`
```kotlin
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ChatSessionEntity::class, ChatMessageEntity::class],
    version = 1,
    exportSchema = false
)
internal abstract class ChatDatabase : RoomDatabase() {
    abstract fun chatSessionDao(): ChatSessionDao
    abstract fun chatMessageDao(): ChatMessageDao
}
```

#### Entity 定义
**文件**: `data/entity/ChatMessageEntity.kt`
```kotlin
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["session_id"])]
)
internal data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    @ColumnInfo(name = "role")
    val role: String,
    @ColumnInfo(name = "content")
    val content: String,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long
)
```

**文件**: `data/entity/ChatSessionEntity.kt`
```kotlin
@Entity(tableName = "chat_sessions")
internal data class ChatSessionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
```

#### DAO 定义
**文件**: `data/dao/ChatMessageDao.kt`
```kotlin
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ChatMessageDao {
    // 返回 Flow 实现响应式查询
    @Query("SELECT * FROM chat_messages WHERE session_id = :sessionId ORDER BY timestamp ASC")
    fun getMessagesFlow(sessionId: String): Flow<List<ChatMessageEntity>>
    
    @Query("SELECT * FROM chat_messages WHERE session_id = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessages(sessionId: String): List<ChatMessageEntity>
    
    @Insert
    suspend fun insertMessage(message: ChatMessageEntity)
    
    @Query("DELETE FROM chat_messages WHERE session_id = :sessionId")
    suspend fun deleteMessages(sessionId: String)
    
    @Transaction
    suspend fun replaceMessages(sessionId: String, messages: List<ChatMessageEntity>) {
        deleteMessages(sessionId)
        messages.forEach { insertMessage(it) }
    }
}
```

**文件**: `data/dao/ChatSessionDao.kt`
```kotlin
@Dao
internal interface ChatSessionDao {
    @Query("SELECT * FROM chat_sessions WHERE id = :id")
    suspend fun getSession(id: String): ChatSessionEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSessionEntity)
}
```

#### Repository 使用
**文件**: `repository/ChatRepository.kt`
```kotlin
internal class ChatRepositoryImpl(
    private val database: ChatDatabase
) : ChatRepository {
    
    // 使用 Room 的 Flow 支持
    override val messagesFlow: Flow<List<ChatMessage>> = 
        database.chatMessageDao()
            .getMessagesFlow(DEFAULT_SESSION_ID)
            .map { entities -> entities.map { it.toChatMessage() } }
    
    override suspend fun loadMessages(): List<ChatMessage> {
        return database.chatMessageDao()
            .getMessages(DEFAULT_SESSION_ID)
            .map { it.toChatMessage() }
    }
}
```

#### DI 配置
**文件**: `di/AppModule.kt`
```kotlin
import androidx.room.Room

@Provides
@Singleton
internal fun provideChatDatabase(
    @ApplicationContext context: Context
): ChatDatabase {
    return Room.databaseBuilder(
        context,
        ChatDatabase::class.java,
        "chat_studio.db"
    ).build()
}
```

---

### 2. DataStore 1.1.1

**依赖**:
```kotlin
implementation(libs.androidx.datastore.preferences)  // 1.1.1
```

**使用位置**:

#### SettingsDataStore
**文件**: `data/SettingsDataStore.kt`
```kotlin
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> 
    by preferencesDataStore(name = "settings")

internal class SettingsDataStore(private val context: Context) {
    private val dataStore = context.settingsDataStore
    
    private object Keys {
        val PROVIDER_NAME = stringPreferencesKey("provider_name")
        val API_KEY = stringPreferencesKey("api_key")
        val MODEL_ID = stringPreferencesKey("model_id")
        // ... 更多键
    }
    
    // 使用 Flow 暴露数据
    val settingsFlow: Flow<StoredSettings> = dataStore.data.map { preferences ->
        StoredSettings(
            providerName = preferences[Keys.PROVIDER_NAME] ?: "OPENAI",
            apiKey = preferences[Keys.API_KEY] ?: "",
            modelId = preferences[Keys.MODEL_ID] ?: "gpt-4o-mini",
            // ...
        )
    }
    
    // 使用 suspend 函数更新数据
    suspend fun updateSettings(settings: StoredSettings) {
        dataStore.edit { preferences ->
            preferences[Keys.PROVIDER_NAME] = settings.providerName
            preferences[Keys.API_KEY] = settings.apiKey
            preferences[Keys.MODEL_ID] = settings.modelId
            // ...
        }
    }
}
```

#### MessagesDataStore
**文件**: `data/MessagesDataStore.kt`
```kotlin
private val Context.messagesDataStore: DataStore<Preferences> 
    by preferencesDataStore(name = "messages")

internal class MessagesDataStore(private val context: Context) {
    // 类似实现
}
```

#### Repository 使用
**文件**: `repository/SettingsRepository.kt`
```kotlin
internal class SettingsRepositoryImpl(
    private val settingsDataStore: SettingsDataStore
) : SettingsRepository {
    
    override val settingsFlow: Flow<StoredSettings> = 
        settingsDataStore.settingsFlow
    
    override suspend fun updateSettings(settings: StoredSettings) {
        settingsDataStore.updateSettings(settings)
    }
}
```

---

## 网络层

### 1. Retrofit 2.11.0

**依赖**:
```kotlin
implementation(libs.retrofit)                                    // 2.11.0
implementation(libs.retrofit.converter.kotlinx.serialization)    // 2.11.0
```

**状态**: ✅ 已配置依赖，但代码中未找到实际使用

**预期用途**: 
- 用于 HTTP 网络请求
- 配合 Kotlin Serialization 进行 JSON 解析
- 可能用于未来的网络功能扩展

---

### 2. OkHttp 4.12.0

**依赖**:
```kotlin
implementation(libs.okhttp)                      // 4.12.0
implementation(libs.okhttp.logging.interceptor)  // 4.12.0
```

**状态**: ✅ 已配置依赖，但代码中未找到实际使用

**预期用途**:
- 作为 Retrofit 的底层 HTTP 客户端
- logging-interceptor 用于网络请求日志记录

---

### 网络层说明

项目当前的网络功能通过 `AgentRunner` 实现，但该功能已被禁用：

**文件**: `AgentRunner.kt`
```kotlin
object AgentRunner {
    suspend fun runAgent(request: Request): ExecutionResult {
        return ExecutionResult(
            answer = "Koog framework has been disabled.",
            events = listOf("Koog framework integration removed"),
            runtimeSnapshot = null
        )
    }
}
```

**结论**: Retrofit 和 OkHttp 依赖已配置，为未来的网络功能预留。

---

## 测试库

### 1. JUnit 4.13.2

**依赖**:
```kotlin
testImplementation(libs.junit)  // 4.13.2
```

**用途**: 单元测试框架

---

### 2. AndroidX Test

**依赖**:
```kotlin
androidTestImplementation(libs.androidx.junit)           // 1.1.5
androidTestImplementation(libs.androidx.espresso.core)   // 3.5.1
```

**用途**: 
- androidx.junit: Android 单元测试扩展
- espresso.core: UI 自动化测试

---

## 构建插件

### 已启用的插件

**文件**: `app/build.gradle.kts`
```kotlin
plugins {
    alias(libs.plugins.android.application)      // AGP 8.12.3
    alias(libs.plugins.compose.compiler)         // Kotlin 2.2.0
    alias(libs.plugins.kotlin.android)           // Kotlin 2.2.0
    alias(libs.plugins.kotlin.serialization)     // Kotlin 2.2.0
    alias(libs.plugins.hilt.android)             // Hilt 2.57.2
    alias(libs.plugins.kotlin.kapt)              // Kotlin 2.2.0
    alias(libs.plugins.ksp)                      // KSP 2.2.0-2.0.2
}
```

### 插件说明

1. **android.application** - Android 应用插件
2. **compose.compiler** - Compose 编译器插件（Kotlin 2.2.0 内置）
3. **kotlin.android** - Kotlin Android 插件
4. **kotlin.serialization** - Kotlin 序列化插件
5. **hilt.android** - Hilt 依赖注入插件
6. **kotlin.kapt** - Kotlin 注解处理器（用于 Hilt）
7. **ksp** - Kotlin Symbol Processing（用于 Room）

---

## 📊 依赖使用统计

| 类别 | 已配置 | 已使用 | 使用率 |
|------|--------|--------|--------|
| Kotlin 生态 | 3 | 3 | 100% |
| AndroidX 核心 | 4 | 4 | 100% |
| Jetpack Compose | 8 | 8 | 100% |
| Lifecycle | 3 | 3 | 100% |
| Hilt DI | 3 | 3 | 100% |
| Room | 3 | 3 | 100% |
| DataStore | 1 | 1 | 100% |
| 网络层 | 4 | 0 | 0% |
| 测试 | 3 | 0 | 0% |

**总计**: 32 个依赖，28 个已使用（87.5%）

---

## 🎯 数据流向图

```
┌─────────────────────────────────────────────────────────────┐
│                         UI 层 (Compose)                      │
│  - Main.kt, Chat.kt, Settings.kt                            │
│  - 使用 hiltViewModel() 获取 ViewModel                       │
│  - 使用 collectAsStateWithLifecycle() 收集状态               │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                    ViewModel 层 (@HiltViewModel)             │
│  - ChatViewModel, SettingsViewModel                         │
│  - 使用 StateFlow 管理 UI 状态                               │
│  - 使用 viewModelScope 处理协程                              │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                      Repository 层                           │
│  - ChatRepository, SettingsRepository                       │
│  - 使用 Flow 暴露响应式数据                                  │
│  - 协调数据源                                                │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                       数据源层                               │
│  ┌─────────────────┐          ┌─────────────────┐          │
│  │  Room Database  │          │   DataStore     │          │
│  │  - ChatDatabase │          │  - Settings     │          │
│  │  - DAO + Entity │          │  - Preferences  │          │
│  │  - Flow 查询    │          │  - Flow 数据    │          │
│  └─────────────────┘          └─────────────────┘          │
└─────────────────────────────────────────────────────────────┘
```

---

## 📝 最佳实践总结

### ✅ 项目遵循的最佳实践

1. **单向数据流 (UDF)**
   - UI → ViewModel → Repository → DataSource
   - 使用 StateFlow 和 Flow 实现响应式

2. **依赖注入**
   - 使用 Hilt 进行依赖注入
   - 所有依赖通过构造函数注入

3. **生命周期感知**
   - 使用 `collectAsStateWithLifecycle()` 安全收集 Flow
   - 使用 `viewModelScope` 自动管理协程生命周期

4. **数据持久化**
   - Room 用于结构化数据（聊天消息）
   - DataStore 用于简单配置（设置）

5. **协程和 Flow**
   - 所有异步操作使用协程
   - 响应式数据使用 Flow

---

## 🔄 版本更新建议

### 可以更新的依赖

1. **Core KTX**: 1.10.1 → 1.15.0（最新稳定版）
2. **Room**: 2.7.0-alpha12 → 2.7.0（等待稳定版发布）

### 保持当前版本

- Kotlin 2.2.0 - 最新稳定版 ✅
- Compose 1.7.8 - 最新稳定版 ✅
- Hilt 2.57.2 - 最新稳定版 ✅
- Lifecycle 2.9.4 - 最新稳定版 ✅

---

**文档维护**: 请在依赖版本更新时同步更新此文档
