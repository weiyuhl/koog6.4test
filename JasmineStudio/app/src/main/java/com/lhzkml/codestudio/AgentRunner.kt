package com.lhzkml.codestudio

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.FunctionalAIAgent
import ai.koog.agents.core.agent.GraphAIAgent
import ai.koog.agents.core.agent.context.AIAgentContext
import ai.koog.agents.core.agent.ToolCalls
import ai.koog.agents.core.annotation.ExperimentalAgentsApi
import ai.koog.agents.core.agent.functionalStrategy
import ai.koog.agents.core.agent.singleRunStrategy
import ai.koog.agents.core.dsl.extension.asAssistantMessage
import ai.koog.agents.core.dsl.extension.containsToolCalls
import ai.koog.agents.core.dsl.extension.executeMultipleTools
import ai.koog.agents.core.dsl.extension.extractToolCalls
import ai.koog.agents.core.dsl.extension.requestLLMMultiple
import ai.koog.agents.core.dsl.extension.sendMultipleToolResults
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.EventHandler
import ai.koog.agents.features.eventHandler.feature.EventHandlerConfig
import ai.koog.prompt.executor.clients.anthropic.AnthropicClientSettings
import ai.koog.prompt.executor.clients.anthropic.AnthropicLLMClient
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.clients.dashscope.DashscopeClientSettings
import ai.koog.prompt.executor.clients.dashscope.DashscopeLLMClient
import ai.koog.prompt.executor.clients.dashscope.DashscopeModels
import ai.koog.prompt.executor.clients.deepseek.DeepSeekClientSettings
import ai.koog.prompt.executor.clients.deepseek.DeepSeekLLMClient
import ai.koog.prompt.executor.clients.deepseek.DeepSeekModels
import ai.koog.prompt.executor.clients.google.GoogleClientSettings
import ai.koog.prompt.executor.clients.google.GoogleLLMClient
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.clients.mistralai.MistralAIClientSettings
import ai.koog.prompt.executor.clients.mistralai.MistralAILLMClient
import ai.koog.prompt.executor.clients.mistralai.MistralAIModels
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.clients.openai.azure.AzureOpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.azure.AzureOpenAIServiceVersion
import ai.koog.prompt.executor.clients.openrouter.OpenRouterClientSettings
import ai.koog.prompt.executor.clients.openrouter.OpenRouterLLMClient
import ai.koog.prompt.executor.clients.openrouter.OpenRouterModels
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.executor.ollama.client.OllamaModels
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.prompt.streaming.StreamFrame
import kotlinx.coroutines.coroutineScope
import kotlin.time.Duration.Companion.milliseconds

// 工具装配结果数据类
internal data class RuntimeToolAssembly(
    val toolRegistry: ToolRegistry,
    val availableToolNames: List<String>,
    val toolSourceSummaries: List<String>,
    val promptAddendum: String,
)

object AgentRunner {
    suspend fun runAgent(request: Request): ExecutionResult = runAgentStreaming(request)

    suspend fun runAgentStreaming(
        request: Request,
        onTextDelta: (String) -> Unit = {},
        onEvent: (String) -> Unit = {},
    ): ExecutionResult = coroutineScope {
        val events = mutableListOf<String>()
        val runtimeCollector = RuntimeSnapshotCollector(request)

        fun recordEvent(text: String) {
            events += text
            onEvent(text)
        }

        recordEvent("Provider: ${request.provider.displayName}")
        recordEvent("Model: ${request.modelId}")
        recordEvent("Runtime preset: ${request.runtimePreset.title}")
        request.temperature?.let { recordEvent("Temperature: $it") }
        request.maxIterations?.let { recordEvent("Max iterations: $it") }
        request.systemPrompt.takeIf { it.isNotBlank() }?.let { recordEvent("Custom system prompt: ${it.take(120)}") }
        request.baseUrl.takeIf { it.isNotBlank() }?.let { recordEvent("Base URL: $it") }

        if (!request.provider.isSupportedOnAndroid) {
            throw UnsupportedOperationException(
                "AWS Bedrock is primarily implemented for JVM in the current version. This Android Demo retains the entry point but does not support direct execution on the device.",
            )
        }

        val model = resolveModel(request.provider, request.modelId)
        val agentTemperature = request.temperature ?: 0.2
        val agentMaxIterations = request.maxIterations ?: 50
        val runtimeTools = assembleRuntimeToolAssembly(request)
        runtimeCollector.captureAvailableTools(runtimeTools.availableToolNames, runtimeTools.toolSourceSummaries)
        recordEvent("Loaded tool sources: ${runtimeTools.toolSourceSummaries.joinToString(" · ")}")
        if (runtimeTools.availableToolNames.isNotEmpty()) {
            val preview = runtimeTools.availableToolNames.take(12).joinToString()
            val suffix = if (runtimeTools.availableToolNames.size > 12) " ..." else ""
            recordEvent("Available tools: $preview$suffix")
        }

        buildExecutor(request, model).use { executor ->
            when (request.runtimePreset) {
                Preset.BasicSingleRun -> {
                    val agent = AIAgent(
                        promptExecutor = executor,
                        llmModel = model,
                        systemPrompt = buildSystemPrompt(request, runtimeTools.promptAddendum),
                        temperature = agentTemperature,
                        toolRegistry = runtimeTools.toolRegistry,
                        maxIterations = agentMaxIterations,
                    ) { installGraphStudioFeatures(request, ::recordEvent, onTextDelta, runtimeCollector) }

                    val answer = agent.run(request.userPrompt)
                    ExecutionResult(answer = answer, events = events, runtimeSnapshot = runtimeCollector.build(answer))
                }

                Preset.GraphToolsSequential,
                Preset.GraphToolsParallel,
                -> {
                    val toolCallsMode = when (request.runtimePreset) {
                        Preset.GraphToolsParallel -> ToolCalls.PARALLEL
                        else -> ToolCalls.SEQUENTIAL
                    }

                    val agent = AIAgent(
                        promptExecutor = executor,
                        strategy = singleRunStrategy(toolCallsMode),
                        llmModel = model,
                        systemPrompt = buildSystemPrompt(request, runtimeTools.promptAddendum),
                        temperature = agentTemperature,
                        toolRegistry = runtimeTools.toolRegistry,
                        maxIterations = agentMaxIterations,
                    ) { installGraphStudioFeatures(request, ::recordEvent, onTextDelta, runtimeCollector) }

                    val answer = agent.run(request.userPrompt)
                    ExecutionResult(answer = answer, events = events, runtimeSnapshot = runtimeCollector.build(answer))
                }

                Preset.FunctionalToolsLoop -> {
                    val agent = AIAgent(
                        promptExecutor = executor,
                        llmModel = model,
                        systemPrompt = buildSystemPrompt(request, runtimeTools.promptAddendum),
                        temperature = agentTemperature,
                        toolRegistry = runtimeTools.toolRegistry,
                        maxIterations = agentMaxIterations,
                        strategy = functionalStrategy<String, String>(name = "functional_tools_loop") { input ->
                            var responses = requestLLMMultiple(input)
                            while (responses.containsToolCalls()) {
                                val tools = extractToolCalls(responses)
                                val results = executeMultipleTools(tools, parallelTools = false)
                                responses = sendMultipleToolResults(results)
                            }
                            responses.single().asAssistantMessage().content
                        },
                    ) { installFunctionalStudioFeatures(request, ::recordEvent, onTextDelta, runtimeCollector) }

                    val answer = agent.run(request.userPrompt)
                    ExecutionResult(answer = answer, events = events, runtimeSnapshot = runtimeCollector.build(answer))
                }
            }
        }
    }

    @OptIn(ExperimentalAgentsApi::class)
    private fun GraphAIAgent.FeatureContext.installGraphStudioFeatures(
        request: Request,
        recordEvent: (String) -> Unit,
        onTextDelta: (String) -> Unit,
        runtimeCollector: RuntimeSnapshotCollector,
    ) {
        install(EventHandler) {
            commonEventHandlers(recordEvent, onTextDelta, runtimeCollector)()
        }
    }

    @OptIn(ExperimentalAgentsApi::class)
    private fun FunctionalAIAgent.FeatureContext.installFunctionalStudioFeatures(
        request: Request,
        recordEvent: (String) -> Unit,
        onTextDelta: (String) -> Unit,
        runtimeCollector: RuntimeSnapshotCollector,
    ) {
        install(EventHandler) {
            commonEventHandlers(recordEvent, onTextDelta, runtimeCollector)()
        }
    }

    private fun commonEventHandlers(
        recordEvent: (String) -> Unit,
        onTextDelta: (String) -> Unit,
        runtimeCollector: RuntimeSnapshotCollector,
    ): EventHandlerConfig.() -> Unit = {
            onAgentStarting { ctx ->
                runtimeCollector.captureAgentStarting(ctx.runId, ctx.context.agentId, ctx.context.strategyName, ctx.executionInfo.path())
                recordEvent("Agent started")
            }
            onStrategyStarting { ctx ->
                runtimeCollector.captureStrategyStarting(
                    strategy = ctx.strategy.name,
                    runId = ctx.context.runId,
                    path = ctx.executionInfo.path(),
                )
            }
            onStrategyCompleted { ctx ->
                runtimeCollector.captureTimeline(
                    category = "strategy",
                    name = ctx.strategy.name,
                    detail = "completed",
                    executionPath = ctx.executionInfo.path(),
                )
            }
            onNodeExecutionStarting { ctx ->
                runtimeCollector.captureNode(name = ctx.node.name, state = "start", path = ctx.executionInfo.path())
            }
            onNodeExecutionCompleted { ctx ->
                runtimeCollector.captureNode(name = ctx.node.name, state = "done", path = ctx.executionInfo.path())
            }
            onSubgraphExecutionStarting { ctx ->
                runtimeCollector.captureSubgraph(name = ctx.subgraph.name, state = "start", path = ctx.executionInfo.path())
            }
            onSubgraphExecutionCompleted { ctx ->
                runtimeCollector.captureSubgraph(name = ctx.subgraph.name, state = "done", path = ctx.executionInfo.path())
            }
            onLLMCallStarting { ctx ->
                runtimeCollector.captureLlm(model = ctx.model.id, path = ctx.executionInfo.path())
            }
            onLLMStreamingStarting { ctx ->
                runtimeCollector.captureLlm(model = ctx.model.id, path = ctx.executionInfo.path(), detail = "stream")
            }
            onLLMStreamingFrameReceived { ctx ->
                (ctx.streamFrame as? StreamFrame.TextDelta)?.let { frame -> onTextDelta(frame.text) }
            }
            onLLMStreamingCompleted { recordEvent("Streaming completed") }
            onToolCallStarting { ctx ->
                runtimeCollector.captureTool(name = ctx.toolName, state = "start", path = ctx.executionInfo.path(), detail = ctx.toolArgs.toString())
                recordEvent("Tool call: ${ctx.toolName} args=${ctx.toolArgs}")
            }
            onToolCallCompleted { ctx ->
                runtimeCollector.captureTool(
                    name = ctx.toolName,
                    state = "done",
                    path = ctx.executionInfo.path(),
                    detail = ctx.toolResult?.toString().orEmpty(),
                )
            }
            onAgentCompleted { ctx ->
                runtimeCollector.captureContextSnapshot(ctx.context, ctx.executionInfo.path())
                runtimeCollector.captureTimeline(
                    category = "agent",
                    name = "completed",
                    detail = compactText(ctx.result),
                    executionPath = ctx.executionInfo.path(),
                )
                recordEvent("Agent completed")
            }
            onAgentExecutionFailed { ctx ->
                runtimeCollector.captureContextSnapshot(ctx.context, ctx.executionInfo.path())
                runtimeCollector.captureTimeline(
                    category = "agent",
                    name = "failed",
                    detail = ctx.throwable.message ?: ctx.throwable::class.simpleName.orEmpty(),
                    executionPath = ctx.executionInfo.path(),
                )
                recordEvent("Agent failed: ${ctx.throwable.message ?: ctx.throwable::class.simpleName}")
            }
    }

    // Simplified tool assembly function - only using basic demo tools
    private suspend fun assembleRuntimeToolAssembly(request: Request): RuntimeToolAssembly {
        val localTools = demoToolsCatalog()
        val availableToolNames = localTools.map { it.name }
        val sourceSummaries = listOf("demo-tools=${localTools.size}")
        
        val promptNotes = if (availableToolNames.isNotEmpty()) {
            "Available tools: ${availableToolNames.joinToString()}."
        } else {
            ""
        }
        
        return RuntimeToolAssembly(
            toolRegistry = ToolRegistry { tools(localTools) },
            availableToolNames = availableToolNames,
            toolSourceSummaries = sourceSummaries,
            promptAddendum = promptNotes,
        )
    }

    private fun buildSystemPrompt(request: Request, runtimeToolPrompt: String): String =
        buildString {
            appendLine("You are an AI Agent assistant integrated into an Android application.")
            appendLine("Current provider: ${request.provider.displayName}.")
            appendLine("Current model: ${request.modelId}.")
            appendLine("Current runtime preset: ${request.runtimePreset.title}.")
            appendLine("When users ask about the current time or math calculations, prioritize calling tools instead of calculating mentally or fabricating time.")
            runtimeToolPrompt.takeIf { it.isNotBlank() }?.let {
                appendLine("The following tools have been loaded for this run. Please call them according to their descriptions:")
                appendLine(it)
            }
            appendLine("For other questions, answer concisely and clearly state which provider you are currently using.")
            request.systemPrompt.takeIf { it.isNotBlank() }?.let {
                appendLine()
                appendLine("Additional system prompt:")
                append(it.trim())
            }
        }.trim()

    private fun buildExecutor(request: Request, model: LLModel): SingleLLMPromptExecutor {
        val apiKey = request.apiKey.trim()
        val baseUrl = request.baseUrl.ifBlank { request.provider.defaultBaseUrl }.trim()

        return when (request.provider) {
            Provider.OPENAI -> SingleLLMPromptExecutor(
                OpenAILLMClient(
                    apiKey = apiKey,
                    settings = OpenAIClientSettings(baseUrl = baseUrl),
                )
            )

            Provider.AZURE_OPENAI -> {
                val version = AzureOpenAIServiceVersion.fromString(request.extraConfig.ifBlank { "2024-10-21" })
                SingleLLMPromptExecutor(
                    OpenAILLMClient(
                        apiKey = apiKey,
                        settings = AzureOpenAIClientSettings(baseUrl = baseUrl, version = version),
                    )
                )
            }

            Provider.ANTHROPIC -> SingleLLMPromptExecutor(
                AnthropicLLMClient(
                    apiKey = apiKey,
                    settings = anthropicSettings(baseUrl = baseUrl, model = model),
                )
            )

            Provider.GOOGLE -> SingleLLMPromptExecutor(
                GoogleLLMClient(
                    apiKey = apiKey,
                    settings = GoogleClientSettings(baseUrl = baseUrl),
                )
            )

            Provider.OPENROUTER -> SingleLLMPromptExecutor(
                OpenRouterLLMClient(
                    apiKey = apiKey,
                    settings = OpenRouterClientSettings(baseUrl = baseUrl),
                )
            )

            Provider.OLLAMA -> SingleLLMPromptExecutor(OllamaClient(baseUrl = baseUrl))

            Provider.DEEPSEEK -> SingleLLMPromptExecutor(
                DeepSeekLLMClient(
                    apiKey = apiKey,
                    settings = DeepSeekClientSettings(baseUrl = baseUrl),
                )
            )

            Provider.MISTRAL -> SingleLLMPromptExecutor(
                MistralAILLMClient(
                    apiKey = apiKey,
                    settings = MistralAIClientSettings(baseUrl = baseUrl),
                )
            )

            Provider.DASHSCOPE -> SingleLLMPromptExecutor(
                DashscopeLLMClient(
                    apiKey = apiKey,
                    settings = DashscopeClientSettings(baseUrl = baseUrl),
                )
            )

            Provider.BEDROCK -> error("Unsupported path should have been blocked earlier.")
        }
    }

    private fun anthropicSettings(baseUrl: String, model: LLModel): AnthropicClientSettings {
        val knownModel = model in listOf(
            AnthropicModels.Sonnet_3_7,
            AnthropicModels.Sonnet_4,
            AnthropicModels.Opus_4,
            AnthropicModels.Opus_4_5,
            AnthropicModels.Sonnet_4_5,
            AnthropicModels.Haiku_4_5,
        )

        return if (knownModel) {
            AnthropicClientSettings(baseUrl = baseUrl)
        } else {
            AnthropicClientSettings(
                modelVersionsMap = mapOf(model to model.id),
                baseUrl = baseUrl,
            )
        }
    }

    private fun resolveModel(provider: Provider, modelId: String): LLModel {
        val normalized = modelId.trim()
        val id = normalized.lowercase()

        return when (provider) {
            Provider.OPENAI,
            Provider.AZURE_OPENAI,
            -> when (id) {
                OpenAIModels.Chat.GPT4oMini.id -> OpenAIModels.Chat.GPT4oMini
                OpenAIModels.Chat.GPT4_1.id -> OpenAIModels.Chat.GPT4_1
                OpenAIModels.Chat.GPT5Mini.id -> OpenAIModels.Chat.GPT5Mini
                OpenAIModels.Chat.GPT5.id -> OpenAIModels.Chat.GPT5
                else -> genericToolCapableModel(LLMProvider.OpenAI, normalized, openAICompatible = true)
            }

            Provider.ANTHROPIC -> when (id) {
                AnthropicModels.Sonnet_3_7.id -> AnthropicModels.Sonnet_3_7
                AnthropicModels.Sonnet_4.id -> AnthropicModels.Sonnet_4
                AnthropicModels.Opus_4.id -> AnthropicModels.Opus_4
                AnthropicModels.Opus_4_5.id -> AnthropicModels.Opus_4_5
                AnthropicModels.Sonnet_4_5.id -> AnthropicModels.Sonnet_4_5
                AnthropicModels.Haiku_4_5.id -> AnthropicModels.Haiku_4_5
                else -> genericToolCapableModel(LLMProvider.Anthropic, normalized)
            }

            Provider.GOOGLE -> when (id) {
                GoogleModels.Gemini2_5Flash.id -> GoogleModels.Gemini2_5Flash
                GoogleModels.Gemini2_5Pro.id -> GoogleModels.Gemini2_5Pro
                GoogleModels.Gemini3_Pro_Preview.id -> GoogleModels.Gemini3_Pro_Preview
                else -> genericToolCapableModel(LLMProvider.Google, normalized)
            }

            Provider.OPENROUTER -> when (id) {
                OpenRouterModels.GPT4oMini.id -> OpenRouterModels.GPT4oMini
                OpenRouterModels.DeepSeekV30324.id -> OpenRouterModels.DeepSeekV30324
                OpenRouterModels.Qwen3VL.id -> OpenRouterModels.Qwen3VL
                else -> genericToolCapableModel(LLMProvider.OpenRouter, normalized)
            }

            Provider.OLLAMA -> when (id) {
                OllamaModels.Meta.LLAMA_3_2.id -> OllamaModels.Meta.LLAMA_3_2
                OllamaModels.Meta.LLAMA_4.id -> OllamaModels.Meta.LLAMA_4
                OllamaModels.Alibaba.QWEN_3_06B.id -> OllamaModels.Alibaba.QWEN_3_06B
                else -> genericToolCapableModel(LLMProvider.Ollama, normalized)
            }

            Provider.DEEPSEEK -> when (id) {
                DeepSeekModels.DeepSeekChat.id -> DeepSeekModels.DeepSeekChat
                DeepSeekModels.DeepSeekReasoner.id -> DeepSeekModels.DeepSeekReasoner
                else -> genericToolCapableModel(LLMProvider.DeepSeek, normalized)
            }

            Provider.MISTRAL -> when (id) {
                MistralAIModels.Chat.MistralSmall2.id -> MistralAIModels.Chat.MistralSmall2
                MistralAIModels.Chat.MistralMedium31.id -> MistralAIModels.Chat.MistralMedium31
                MistralAIModels.Chat.Codestral.id -> MistralAIModels.Chat.Codestral
                else -> genericToolCapableModel(LLMProvider.MistralAI, normalized)
            }

            Provider.DASHSCOPE -> when (id) {
                DashscopeModels.QWEN_FLASH.id -> DashscopeModels.QWEN_FLASH
                DashscopeModels.QWEN_PLUS.id -> DashscopeModels.QWEN_PLUS
                DashscopeModels.QWEN_PLUS_LATEST.id -> DashscopeModels.QWEN_PLUS_LATEST
                else -> genericToolCapableModel(LLMProvider.Alibaba, normalized)
            }

            Provider.BEDROCK -> genericToolCapableModel(LLMProvider.Bedrock, normalized)
        }
    }

    private fun genericToolCapableModel(
        provider: LLMProvider,
        id: String,
        openAICompatible: Boolean = false,
    ): LLModel {
        val capabilities = buildList {
            add(LLMCapability.Completion)
            add(LLMCapability.Temperature)
            add(LLMCapability.Tools)
            add(LLMCapability.ToolChoice)
            add(LLMCapability.MultipleChoices)
            if (openAICompatible) {
                add(LLMCapability.OpenAIEndpoint.Completions)
            }
        }
        return LLModel(provider = provider, id = id, capabilities = capabilities)
    }

    private fun compactText(value: Any?, maxLength: Int = 180): String =
        value?.toString()?.replace("\n", " ")?.trim()?.let {
            if (it.length <= maxLength) it else it.take(maxLength) + "…"
        }.orEmpty()

    private fun messagePreview(message: Message): String = "${message.role.name}: ${compactText(message.content, 120)}"

    private class RuntimeSnapshotCollector(private val request: Request) {
        private var runId: String? = null
        private var agentId: String? = null
        private var strategyName: String? = null
        private val nodeNames = linkedSetOf<String>()
        private val subgraphNames = linkedSetOf<String>()
        private val toolNames = linkedSetOf<String>()
        private val llmModels = linkedSetOf<String>()
        private var historyCount: Int = 0
        private val historyPreview = mutableListOf<HistoryEntry>()
        private val storageEntries = mutableListOf<StorageEntry>()
        private val availableToolNames = linkedSetOf<String>()
        private val toolSourceSummaries = mutableListOf<String>()
        private val timeline = mutableListOf<TimelineEntry>()

        fun captureAgentStarting(runId: String, agentId: String, strategyName: String, path: String) {
            this.runId = runId
            this.agentId = agentId
            this.strategyName = strategyName
            captureTimeline(category = "agent", name = "starting", detail = "runId=$runId · agentId=$agentId", executionPath = path)
        }

        fun captureStrategyStarting(strategy: String, runId: String, path: String) {
            this.runId = runId
            this.strategyName = strategy
            captureTimeline(category = "strategy", name = strategy, detail = "starting", executionPath = path)
        }

        fun captureNode(name: String, state: String, path: String) {
            nodeNames += name
            captureTimeline(category = "node", name = name, detail = state, executionPath = path)
        }

        fun captureSubgraph(name: String?, state: String, path: String) {
            val safeName = name ?: "anonymous-subgraph"
            subgraphNames += safeName
            captureTimeline(category = "subgraph", name = safeName, detail = state, executionPath = path)
        }

        fun captureTool(name: String, state: String, path: String, detail: String) {
            toolNames += name
            captureTimeline(category = "tool", name = name, detail = "$state · ${compactText(detail)}", executionPath = path)
        }

        fun captureLlm(model: String, path: String, detail: String = "call") {
            llmModels += model
            captureTimeline(category = "llm", name = model, detail = detail, executionPath = path)
        }

        fun captureTimeline(category: String, name: String, detail: String, executionPath: String?) {
            timeline += TimelineEntry(
                category = category,
                name = name,
                detail = detail,
                executionPath = executionPath,
            )
        }

        fun captureAvailableTools(names: List<String>, sourceSummaries: List<String>) {
            availableToolNames.clear()
            availableToolNames += names
            toolSourceSummaries.clear()
            toolSourceSummaries += sourceSummaries
            if (names.isNotEmpty()) {
                captureTimeline(
                    category = "tool-registry",
                    name = "loaded",
                    detail = "${sourceSummaries.joinToString(" · ")} · tools=${names.size}",
                    executionPath = null,
                )
            }
        }

        suspend fun captureContextSnapshot(context: AIAgentContext, path: String) {
            val history = context.getHistory()
            historyCount = history.size
            historyPreview.clear()
            historyPreview += history.takeLast(8).map { message ->
                HistoryEntry(
                    role = message.role.name,
                    contentPreview = compactText(message.content, 140),
                )
            }

            val storageMap = context.storage.toMap()
            storageEntries.clear()
            storageEntries += storageMap.entries
                .sortedBy { it.key.name }
                .map { entry ->
                    StorageEntry(
                        key = entry.key.name,
                        valuePreview = compactText(entry.value, 180),
                    )
                }

            captureTimeline(
                category = "context",
                name = "snapshot",
                detail = "history=${history.size} · storage=${storageEntries.size}",
                executionPath = path,
            )
        }

        fun build(answer: String): RuntimeSnapshot = RuntimeSnapshot(
            runId = runId,
            agentId = agentId,
            strategyName = strategyName,
            providerName = request.provider.displayName,
            modelId = request.modelId,
            presetTitle = request.runtimePreset.title,
            nodeNames = nodeNames.toList(),
            subgraphNames = subgraphNames.toList(),
            availableToolNames = availableToolNames.toList(),
            toolSourceSummaries = toolSourceSummaries.toList(),
            toolNames = toolNames.toList(),
            llmModels = llmModels.toList(),
            historyCount = historyCount,
            historyPreview = historyPreview.toList(),
            storageEntries = storageEntries.toList(),
            timeline = timeline.toList(),
            finalResultPreview = compactText(answer),
        )
    }
}
