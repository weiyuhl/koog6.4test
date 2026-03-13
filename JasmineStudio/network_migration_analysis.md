# JasmineStudio 网络层改造方案评估 (Ktor -> Retrofit)

## 📌 现状深度背景分析

经过对您整个工作区（特别是 `ai.koog` 核心模块）的源码深度扫描，我发现了一个至关重要的架构前提：

**您的项目不是一个简单的 Android 单端 App。**
App 本身（`app` 模块）其实**没有包含任何直接的 Ktor 网络调用代码**。所有的 LLM 提示词执行、大模型 API 调用（如 OpenAI/Ollama）实际上都被深层封装在 `ai.koog:prompt-executor-llms-all` 和相关子模块中（例如 `AbstractOpenAILLMClient.kt`）。

更关键的是，**`ai.koog` 是一套跨平台（Kotlin Multiplatform, KMP）架构**。在 KMP 生态中，**Ktor Client 是官方钦定且唯一的全平台异步网络标准**，它原生支持了协程（Coroutines）、流式传输（SSE / Flow）以及跨平台编译（JVM, iOS, JS, Wasm）。

---

## ⚠️ Retrofit 方案的可行性痛点

如果您坚持要将下层网络全部从 Ktor 替换为 Retrofit，我们会面临以下几个巨大的架构挑战：

1. **丧失跨平台特性**：
   Retrofit 强依赖于 Java 的反射以及 OkHttp（纯 JVM 库）。如果将底层的大模型执行器（Prompt Executor）改写为 Retrofit，那么您的 `ai.koog` SDK 将**永久失去编译到 iOS / JS 等非 JVM 平台的能力**，从 KMP 架构倒退回纯 Android/JVM 架构。
2. **SSE 流式输出的断层**：
   大模型对话最核心的功能是 "打字机效果" 的流式输出（Server-Sent Events）。Ktor Client 原生通过 `io.ktor.client.plugins.sse.SSE` 和 Kotlin `Flow` 的结合，完美实现了像流水一样的非阻塞数据流。而 Retrofit 默认**极不擅长处理长期保持连接的 SSE 流**，通常需要非常复杂且丑陋的 OkHttp 拦截器魔改或引入非官方的第三方补丁库。
3. **改造工作量惊人**：
   几乎要重写整个 `ai.koog:prompt-executor` 相关的七八个模块。所有的序列化器、超时配置、拦截器链条都得推倒重来。

---

## 🎯 我的终极架构建议

结合当前项目的 KMP 基因和架构愿景，我为您提供上、中、下三策：

### 【上策】保持原状，深化 Ktor (极度推荐 ⭐⭐⭐⭐⭐)
不要替换 Ktor。您当前的 `ai.koog.http.client.KoogHttpClient` 封装已经非常抽象和优秀。Ktor 在现代 Kotlin 协程和多平台领域的统治地位是 Retrofit 无法撼动的。如果您觉得 Ktor 语法啰嗦，我们可以在 `KoogHttpClient` 外层再封装一层类似 Retrofit 注解风格的扩展函数（DSL），既保住了 KMP，又提升了写代码的爽感。

### 【中策】双轨制：App 业务用 Retrofit，核心 LLM 用 Ktor (推荐 ⭐⭐⭐)
底层 `ai.koog` 跨平台模块里控制大模型对话的、需要 SSE 流的核心网络**保持 Ktor 不动**。
但是对于上层 Android App（如果未来有注册、登录、获取非流式 JSON 设定的普通业务接口），我们在 `d:\koog\JasmineStudio\app\` 模块里引入 **Retrofit + OkHttp** 专门来处理这些短连接 CRUD 请求。
- **优点**：互不干涉。Android 开发人员写常规业务依然能享受 Retrofit 的直观。
- **缺点**：APK 包体积略微增大（同时带了双网络引擎）。

### 【下策】全面暴力破除 KMP 拥抱 Retrofit (不推荐 ❌)
彻底放弃 `ai.koog` 的跨平台能力。将所有 commonMain 里的网络层重构，引入 Retrofit。强行用 OkHttp Callbacks 去拼凑流式数据的解析。这个方案会产生大量的破窗效应，引入很多历史技术债。

---

## 💬 下一步怎么走？

如果您需要：
1. **实施【中策】**：请回复我“在 App 模块里引入 Retrofit 处理普通请求”，我会在 AppModule 帮您配置好 Retrofit 单例架构。
2. **实施【上策】的语法糖封装**：请回复我“帮我优化 Ktor 调用的写法”，我会用高阶函数帮您隐藏掉冗余的构建代码。 

请您定夺！
