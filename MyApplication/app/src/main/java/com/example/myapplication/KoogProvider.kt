package com.example.myapplication

data class AgentRequest(
    val provider: KoogProvider,
    val apiKey: String,
    val modelId: String,
    val baseUrl: String,
    val extraConfig: String,
    val runtimePreset: AgentRuntimePreset,
    val systemPrompt: String,
    val temperature: Double?,
    val maxIterations: Int?,
    val featureConfig: AgentFeatureConfig,
    val userPrompt: String,
)

data class AgentFeatureConfig(
    val localWriterEnabled: Boolean,
    val debuggerEnabled: Boolean,
    val debuggerPort: Int?,
    val debuggerWaitMillis: Long?,
    val remoteClientEnabled: Boolean,
    val remoteHost: String,
    val remotePort: Int?,
)

data class AgentExecutionResult(
    val answer: String,
    val events: List<String>,
    val runtimeSnapshot: AgentRuntimeSnapshot? = null,
)

data class AgentRuntimeSnapshot(
    val runId: String?,
    val agentId: String?,
    val strategyName: String?,
    val providerName: String,
    val modelId: String,
    val presetTitle: String,
    val nodeNames: List<String>,
    val subgraphNames: List<String>,
    val toolNames: List<String>,
    val llmModels: List<String>,
    val historyCount: Int,
    val historyPreview: List<AgentHistoryEntry>,
    val storageEntries: List<AgentStorageEntry>,
    val localWriterEnabled: Boolean,
    val debuggerEnabled: Boolean,
    val debuggerPort: Int?,
    val remoteClientEnabled: Boolean,
    val remoteClientTarget: String?,
    val remoteClientConnected: Boolean,
    val featureMessages: List<AgentFeatureMessageEntry>,
    val timeline: List<AgentTimelineEntry>,
    val finalResultPreview: String,
)

data class AgentHistoryEntry(
    val role: String,
    val contentPreview: String,
)

data class AgentStorageEntry(
    val key: String,
    val valuePreview: String,
)

data class AgentFeatureMessageEntry(
    val source: String,
    val type: String,
    val detail: String,
)

data class AgentTimelineEntry(
    val category: String,
    val name: String,
    val detail: String,
    val executionPath: String? = null,
)

enum class KoogProvider(
    val displayName: String,
    val defaultModelId: String,
    val defaultBaseUrl: String,
    val providerNote: String,
    val requiresApiKey: Boolean = true,
    val baseUrlLabel: String = "Base URL（可选）",
    val extraFieldLabel: String? = null,
    val extraFieldDefault: String = "",
    val isSupportedOnAndroid: Boolean = true,
) {
    OPENAI("OpenAI", "gpt-4o-mini", "https://api.openai.com", "官方 OpenAI。可自定义模型 ID，也可改为兼容 OpenAI 的自定义 Base URL。"),
    AZURE_OPENAI(
        "Azure OpenAI",
        "gpt-4o-mini",
        "https://your-resource.openai.azure.com/openai/deployments/your-deployment/",
        "Azure OpenAI 需要部署 Base URL 和 API Version。模型字段可填写模型名或部署关联模型名。",
        baseUrlLabel = "Azure Deployment Base URL",
        extraFieldLabel = "API Version",
        extraFieldDefault = "2024-10-21",
    ),
    ANTHROPIC("Anthropic", "claude-sonnet-4-5", "https://api.anthropic.com", "Anthropic Claude。已内置常见模型；自定义模型时会按输入 ID 直接映射。"),
    GOOGLE("Google", "gemini-2.5-flash", "https://generativelanguage.googleapis.com", "Google Gemini API。默认走 Gemini REST 端点，可自定义模型 ID。"),
    OPENROUTER("OpenRouter", "openai/gpt-4o-mini", "https://openrouter.ai", "OpenRouter 路由层。可切换不同上游模型，例如 OpenAI、DeepSeek、Qwen。"),
    OLLAMA(
        "Ollama",
        "llama3.2:latest",
        "http://10.0.2.2:11434",
        "本地 Ollama。Android 模拟器访问宿主机 Ollama 时默认建议用 10.0.2.2。",
        requiresApiKey = false,
        baseUrlLabel = "Ollama Base URL",
    ),
    DEEPSEEK("DeepSeek", "deepseek-chat", "https://api.deepseek.com", "DeepSeek 官方接口，默认模型为 deepseek-chat。"),
    MISTRAL("Mistral", "mistral-small-latest", "https://api.mistral.ai", "Mistral 官方接口，默认模型为 mistral-small-latest。"),
    DASHSCOPE("DashScope / Qwen", "qwen-plus", "https://dashscope-intl.aliyuncs.com/", "阿里 DashScope，默认国际站。也可以切换到中国大陆端点。"),
    BEDROCK(
        "AWS Bedrock",
        "anthropic.claude-3-5-sonnet",
        "",
        "Koog 当前 Bedrock 客户端实现主要位于 JVM source set。此 Android Demo 保留入口，但不会在端上直接执行。",
        requiresApiKey = false,
        isSupportedOnAndroid = false,
    );

    override fun toString(): String = displayName
}