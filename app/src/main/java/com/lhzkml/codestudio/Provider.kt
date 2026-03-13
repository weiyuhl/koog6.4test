package com.lhzkml.codestudio

data class Request(
    val provider: Provider,
    val apiKey: String,
    val modelId: String,
    val baseUrl: String,
    val extraConfig: String,
    val runtimePreset: Preset,
    val systemPrompt: String,
    val temperature: Double?,
    val maxIterations: Int?,
    val userPrompt: String,
)

data class FeatureConfig(
    // 保留用于未来扩展
    val placeholder: Boolean = false,
)

data class ExecutionResult(
    val answer: String,
    val events: List<String>,
    val runtimeSnapshot: RuntimeSnapshot? = null,
)

data class RuntimeSnapshot(
    val runId: String?,
    val agentId: String?,
    val strategyName: String?,
    val providerName: String,
    val modelId: String,
    val presetTitle: String,
    val nodeNames: List<String>,
    val subgraphNames: List<String>,
    val availableToolNames: List<String>,
    val toolSourceSummaries: List<String>,
    val toolNames: List<String>,
    val llmModels: List<String>,
    val historyCount: Int,
    val historyPreview: List<HistoryEntry>,
    val storageEntries: List<StorageEntry>,
    val timeline: List<TimelineEntry>,
    val finalResultPreview: String,
)

data class HistoryEntry(
    val role: String,
    val contentPreview: String,
)

data class StorageEntry(
    val key: String,
    val valuePreview: String,
)

data class TimelineEntry(
    val category: String,
    val name: String,
    val detail: String,
    val executionPath: String? = null,
)

enum class Provider(
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
    OPENAI("OpenAI", "gpt-4o-mini", "https://api.openai.com", "官方 OpenAI。可自定义模型 ID，也可改为兼容 OpenAI 的自定义 Base URL"),
    AZURE_OPENAI(
        "Azure OpenAI",
        "gpt-4o-mini",
        "https://your-resource.openai.azure.com/openai/deployments/your-deployment/",
        "Azure OpenAI 需要部署 Base URL 和 API Version。模型字段可填写模型名或部署关联模型名",
        baseUrlLabel = "Azure Deployment Base URL",
        extraFieldLabel = "API Version",
        extraFieldDefault = "2024-10-21",
    ),
    ANTHROPIC("Anthropic", "claude-sonnet-4-5", "https://api.anthropic.com", "Anthropic Claude。已内置常见模型；自定义模型时会按输入 ID 直接映射"),
    GOOGLE("Google", "gemini-2.5-flash", "https://generativelanguage.googleapis.com", "Google Gemini API。默认走 Gemini REST 端点，可自定义模型 ID"),
    OPENROUTER("OpenRouter", "openai/gpt-4o-mini", "https://openrouter.ai", "OpenRouter 路由层。可切换不同上游模型，例如 OpenAI、DeepSeek、Qwen"),
    OLLAMA(
        "Ollama",
        "llama3.2:latest",
        "http://10.0.2.2:11434",
        "本地 Ollama。Android 模拟器访问宿主机 Ollama 时默认建议用 10.0.2.2",
        requiresApiKey = false,
        baseUrlLabel = "Ollama Base URL",
    ),
    DEEPSEEK("DeepSeek", "deepseek-chat", "https://api.deepseek.com", "DeepSeek 官方接口，默认模型为 deepseek-chat"),
    MISTRAL("Mistral", "mistral-small-latest", "https://api.mistral.ai", "Mistral 官方接口，默认模型为 mistral-small-latest"),
    DASHSCOPE("DashScope / Qwen", "qwen-plus", "https://dashscope-intl.aliyuncs.com/", "阿里 DashScope，默认国际站。也可以切换到中国大陆端点"),
    BEDROCK(
        "AWS Bedrock",
        "anthropic.claude-3-5-sonnet",
        "",
        "AWS Bedrock client implementation. This Android Demo retains the entry point but will not execute directly on the device.",
        requiresApiKey = false,
        isSupportedOnAndroid = false,
    );

    override fun toString(): String = displayName
}
