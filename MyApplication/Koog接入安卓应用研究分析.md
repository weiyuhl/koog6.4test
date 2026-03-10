# Koog 接入安卓应用研究分析

## 1. 研究范围

本文基于工作区内两部分真实代码进行分析：

- 安卓工程：`MyApplication/`
- Koog 框架源码：当前仓库根目录下各模块，重点查看了 `agents/`、`prompt/`、`examples/demo-compose-app/`、`examples/simple-examples/`

目标不是直接改代码，而是判断：**Koog 现在能不能接入这个 Android 应用、应该怎么接、有哪些前置条件和风险。**

## 2. 先说结论

### 可以接入，但当前 `MyApplication` 还只是一个很空的模板壳子

从代码看，Koog 本身是支持 Android 目标的，仓库里也有实际的 Android/KMP 消费样例；但当前 `MyApplication` 还没有 Activity、没有 Kotlin 业务代码、没有网络权限，也没有任何 UI 入口，所以它现在还不处于“把 Koog 塞进去就能跑”的状态。

### 我对接入难度的判断

- **技术上可行**：高
- **直接开工成功率**：中
- **先升级工程基线再接入的必要性**：高

原因是：Koog 的 Android 使用路径在仓库里是有代码证明的，但 `MyApplication` 当前的 Kotlin/Java 基线比仓库内的 Koog 消费样例低不少。

## 3. `MyApplication` 当前真实状态

### 3.1 工程结构

我检查到：

- 根设置：`MyApplication/settings.gradle.kts`
- 单模块应用：`MyApplication/app`
- 依赖非常少，只包含：`core-ktx`、`appcompat`、`material`、JUnit、androidTest

### 3.2 编译基线

来自 `MyApplication/app/build.gradle.kts` 与 `MyApplication/gradle/libs.versions.toml`：

- AGP：`8.13.1`
- Kotlin：`2.0.21`
- `minSdk = 24`
- `targetSdk = 36`
- Java 编译目标：`11`

### 3.3 入口与代码现状

从 `MyApplication/app/src/main/AndroidManifest.xml` 以及 `app/src/main` 的实际文件列表看：

- **没有 `MainActivity`**
- **没有 `src/main/java` 或 `src/main/kotlin` 业务代码**
- Manifest 中也**没有 launcher activity**
- 也**没有 `INTERNET` 权限**

这说明它目前更像一个“刚创建完成的 Android Studio 模板工程”，而不是已经有业务页面和架构的 APK 项目。

### 3.4 基线验证结果

我已执行：`MyApplication\gradlew.bat :app:assembleDebug`

结果：

- **BUILD SUCCESSFUL**
- 但有一个 SDK 路径告警：`local.properties` 里的 `sdk.dir` 指向目录不存在

这说明：

- 当前模板工程本身能组装 APK
- 但它只是“能构建”，**并不等于已经具备可启动、可交互的 App 入口**

## 4. Koog 对 Android 接入的代码级证据

### 4.1 Koog 本身有 Android target

在 `convention-plugin-ai/src/main/kotlin/ai.kotlin.multiplatform.gradle.kts` 中，Koog 的公共模块统一使用了：

- `androidTarget()`
- Android `compileSdk = 36`
- Java `sourceCompatibility/targetCompatibility = 17`

这说明 Koog 不是只面向 JVM server，它本身确实在发布 Android 变体。

### 4.2 仓库里已有 Android 消费样例

最关键证据是 `examples/demo-compose-app/`：

- `commonApp/build.gradle.kts` 中直接依赖了：
  - `ai.koog:agents-core`
  - `ai.koog:prompt-executor-llms-all`
- `androidMain.dependencies` 里额外加了：
  - `io.ktor:ktor-client-okhttp`
- `androidApp/src/main/AndroidManifest.xml` 里显式声明了：
  - `android.permission.INTERNET`

这说明：**Koog 在 Android 上的可行接入组合，仓库内已经有实际先例。**

### 4.3 Agent 的核心接入面

从 `examples/demo-compose-app/.../CalculatorAgentProvider.kt` 和 Koog 核心源码可确认，最小可用链路通常是：

1. 创建 `LLMClient`
2. 用 `SingleLLMPromptExecutor` 包装它
3. 准备 `ToolRegistry`
4. 准备 `AIAgentConfig`
5. 通过 `AIAgent(...)` 创建 agent
6. 在协程里执行 `agent.run(input)`

这条链路对 Android 是友好的，因为它主要依赖协程、Ktor、序列化和普通 Kotlin 对象。

### 4.4 LLM 客户端是可关闭资源

从 `prompt-executor-clients/.../LLMClient.kt` 与 `SingleLLMPromptExecutor.kt` 可见：

- `LLMClient : AutoCloseable`
- `SingleLLMPromptExecutor.close()` 会向下关闭 `llmClient`

这意味着放进 Android 后要考虑生命周期，不能无限制创建后不释放。

### 4.5 Koog 的网络层依赖 Ktor HttpClient

从 `AbstractOpenAILLMClient.kt`、`AnthropicLLMClient.kt`、`GoogleLLMClient.kt`、`KtorKoogHttpClient.kt` 可见：

- Koog 的 LLM client 内部通过 `HttpClient()` / `KoogHttpClient.fromKtorClient(...)` 发起请求
- 在 Android 场景下需要有 Ktor engine
- 仓库内 Android 样例使用的是 `ktor-client-okhttp`

因此，**如果在 `MyApplication` 里走 Koog 的模块化依赖方式，Android 端应补上 `ktor-client-okhttp`。**

### 4.6 Android 上不要优先依赖反射式 Tool 注册

从 `agents-tools` 模块可见：

- `ToolRegistry` 与 `Tool` 基类在 `commonMain`
- 但 `asTools()`、`ToolSet` 这套反射辅助主要在 `src/jvmMain`

对 Android 来说，最稳妥的方案不是反射扫描工具方法，而是像 `CalculatorTools.kt` 那样：

- 显式写 `Tool` 子类
- 然后手动注册到 `ToolRegistry`

这个方式在 Android 上更直接，也更容易控体积与可维护性。

### 4.7 Android 上不要依赖系统 secrets/config 读取器

从 `utils/src/androidMain/.../SystemSecretsReader.android.kt` 与 `SystemConfigReader.android.kt` 可见：

- Android 实现直接 `throw NotImplementedError(...)`

也就是说，**不要把 Android 端 token 读取寄希望于 Koog 内部的系统 secrets/config 机制**。Key 应该由 App 自己管理。

## 5. 对 `MyApplication` 的接入判断

### 5.1 可以接，但不建议“直接硬接”

如果现在马上往 `MyApplication` 加 Koog，我认为最容易先撞上的不是 Agent API，而是工程基线问题：

- `MyApplication` 现在是 Kotlin `2.0.21`、Java `11`
- Koog 仓库当前版本目录里使用 Kotlin `2.3.10`
- Koog 的 Android/KMP 消费样例使用 Kotlin `2.2.21`、Java `17`

因此，**当前工程基线与 Koog 实际使用基线存在明显落差**。我不建议在不升级的情况下直接进入正式接入阶段。

### 5.2 目前最适合 `MyApplication` 的接入方式

#### 方案 A：本地联调优先，使用 Composite Build（推荐）

理由：

- 你的 `MyApplication` 就放在 Koog 仓库工作区里
- 仓库内 `examples/simple-examples/settings.gradle.kts` 已证明可以通过 `includeBuild(...)` 消费当前源码
- 这样最适合研究、断点调试和源码联动

对于当前目录结构，后续可以评估在 `MyApplication/settings.gradle.kts` 中使用类似：

- `includeBuild("..")`

然后依赖 Koog 坐标，而不是先手动发布到 Maven。

#### 方案 B：后续独立化时再切到发布依赖

仓库内 `demo-compose-app` 使用的是外部坐标方式，说明这条路也成立；但对于你现在这个“就在仓库里分析接入”的场景，我认为不如 Composite Build 方便。

## 6. 我建议的接入架构

### 6.1 不建议第一步就上复杂图策略

Koog 的强项是图策略、节点、边、工具链和事件管线；但对 `MyApplication` 来说，第一阶段更适合做一个**最小聊天型 agent**：

- Activity 或 Fragment 提供输入框/发送按钮
- ViewModel 内通过协程调用 agent
- AgentFactory 负责创建 `LLMClient / PromptExecutor / AIAgent`
- Tool 先只接 0~2 个显式工具

这样能先把“应用 UI → Agent → LLM → 返回结果”这条主链打通。

### 6.2 推荐在 App 中拆成 4 层

1. **UI 层**
   - `MainActivity` / `ChatFragment` / Compose 页面
2. **ViewModel 层**
   - 发消息、持有 UI 状态、启动协程
3. **Agent 装配层**
   - `AgentFactory` 或 `KoogAgentProvider`
4. **配置与密钥层**
   - 本地存储、远程下发、或后端代理

## 7. 关键风险点

### 7.1 版本与工具链风险

这是当前第一风险：

- App：Kotlin 2.0.21 / Java 11
- Koog 示例：Kotlin 2.2.21 / Java 17
- Koog 仓库：Kotlin 2.3.10 / Java 17

我的建议是：**先把 `MyApplication` 升到 Java 17，并把 Kotlin 至少提升到接近 Koog 示例的区间，再谈正式接入。**

### 7.2 网络权限缺失

当前 Manifest 没有 `INTERNET`。只要 agent 要直连 LLM，就必须补。

### 7.3 当前没有 App 入口

没有 Activity，也没有 launcher 声明。就算依赖都接好，也没有页面承载 agent 交互。

### 7.4 API Key 暴露风险

从安全角度，我不建议把 OpenAI/Anthropic key 长期硬编码在 APK 里。Koog 在 Android 上可以跑，但**“能跑”不代表“适合直接把生产 key 放客户端”**。

更稳妥的做法：

- 调试阶段：临时本地输入或本地存储
- 正式阶段：走你自己的服务端代理

### 7.5 生命周期与资源释放

由于 `LLMClient` 是可关闭资源，后续若在 Activity/ViewModel 中频繁 new client，需要明确谁负责：

- 复用实例
- 取消协程
- 页面结束时关闭资源

## 8. 推荐落地顺序

### 第一阶段：把工程变成“可承载 Agent 的安卓应用”

1. 增加 `MainActivity`
2. 增加 launcher intent-filter
3. 补 `INTERNET` 权限
4. 升 Java/Kotlin 基线

### 第二阶段：以最小依赖方式接入 Koog

优先参考仓库内 Android 样例的依赖组合：

- `ai.koog:agents-core`
- `ai.koog:prompt-executor-llms-all`
- `io.ktor:ktor-client-okhttp`
- Android 协程依赖

### 第三阶段：先做最小聊天 Agent

只做：

- 用户输入文本
- 调用 `agent.run(...)`
- 展示回复

工具先从 0 个开始，或者只做一个简单显式 `Tool`。

### 第四阶段：再考虑复杂能力

比如：

- 多工具
- 图策略
- 事件追踪
- 流式输出
- 本地记忆/持久化

## 9. 最终建议

如果你问我“这个框架适不适合接到当前这个 Android 应用里”，我的判断是：

- **适合做研究型接入和 Demo 型接入**
- **不适合在当前这个空模板、低基线状态下直接一步到位做正式接入**

最现实的路线是：

1. 先把 `MyApplication` 升到能承载 Koog 的工程基线
2. 先按仓库现有 Android 先例做最小接入
3. 跑通后再决定是否继续做业务化封装

如果下一步需要，我可以继续基于这份分析，**直接给 `MyApplication` 设计一版最小可落地的目录结构与接入改造清单**。