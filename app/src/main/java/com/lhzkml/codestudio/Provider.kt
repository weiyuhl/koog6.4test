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
    ANTHROPIC("Anthropic", "claude-3-5-sonnet-20241022", "https://api.anthropic.com", "Anthropic Claude。支持 Claude 3.5 Sonnet、Claude 3 Opus 等模型"),
    GOOGLE("Google", "gemini-2.0-flash-exp", "https://generativelanguage.googleapis.com", "Google Gemini API。支持 Gemini 2.0 Flash、Gemini 1.5 Pro 等模型"),
    OPENROUTER("OpenRouter", "openai/gpt-4o-mini", "https://openrouter.ai/api", "OpenRouter 路由层。可切换不同上游模型，例如 OpenAI、DeepSeek、Qwen"),
    DEEPSEEK("DeepSeek", "deepseek-chat", "https://api.deepseek.com", "DeepSeek 官方接口，支持 deepseek-chat、deepseek-reasoner 等模型"),
    SILICONFLOW("硅基流动", "deepseek-ai/DeepSeek-V3.2", "https://api.siliconflow.cn", "硅基流动 API，支持 Qwen、DeepSeek、GLM 等多种开源模型");

    override fun toString(): String = displayName
}
