package com.lhzkml.codestudio

import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.planner.AIAgentPlannerStrategy
import ai.koog.agents.planner.PlannerAIAgent
import ai.koog.agents.planner.goap.goap
import ai.koog.agents.planner.llm.PlanStep
import ai.koog.agents.planner.llm.SimpleLLMPlanner
import ai.koog.agents.planner.llm.SimplePlan
import ai.koog.agents.planner.llm.SimplePlanAssessment
import ai.koog.agents.utils.HiddenString
import ai.koog.agents.utils.ModelInfo
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.dsl.ModerationResult
import ai.koog.prompt.executor.ollama.client.OllamaModels
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.ResponseMetaInfo
import ai.koog.prompt.streaming.StreamFrame
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.reflect.typeOf
import kotlin.time.TimeSource

enum class PlannerLabMode(val title: String, val subtitle: String) {
    SimpleReplanning("Simple planner", "使用 SimpleLLMPlanner + 可重规划演示，展示 plan / step / replan 流程"),
    GoapTorch("GOAP demo", "使用 GOAP planner 做一个纯本地、无网络的洞穴探索规划示例"),
}

data class MetadataPreview(
    val modelInfo: ModelInfo,
    val maskedApiKey: String,
    val maskedSystemPrompt: String,
    val runtimePresetTitle: String,
)

data class PlannerLabRunResult(
    val mode: PlannerLabMode,
    val goal: String,
    val status: String,
    val initialState: String,
    val finalState: String,
    val planSnapshots: List<String>,
    val executedSteps: List<String>,
    val eventLog: List<String>,
    val notes: List<String>,
    val replanCount: Int,
    val iterationCount: Int,
    val elapsedMillis: Long,
    val modelInfo: ModelInfo,
    val maskedApiKey: String,
    val maskedSystemPrompt: String,
)

object PlannerLabRunner {
    private val json = Json { encodeDefaults = true }

    suspend fun run(mode: PlannerLabMode, metadata: MetadataPreview, task: String): PlannerLabRunResult = when (mode) {
        PlannerLabMode.SimpleReplanning -> runSimplePlanner(metadata, task)
        PlannerLabMode.GoapTorch -> runGoapPlanner(metadata)
    }

    fun metadataPreview(
        provider: Provider,
        modelId: String,
        apiKey: String,
        systemPrompt: String,
        runtimePreset: Preset,
    ): MetadataPreview = MetadataPreview(
        modelInfo = ModelInfo(
            provider = provider.name.lowercase(),
            model = modelId.trim().ifBlank { provider.defaultModelId },
            displayName = provider.displayName,
        ),
        maskedApiKey = HiddenString(apiKey.trim()).toString(),
        maskedSystemPrompt = HiddenString(systemPrompt.trim(), placeholder = "HIDDEN:system-prompt").toString(),
        runtimePresetTitle = runtimePreset.title,
    )

    private suspend fun runSimplePlanner(metadata: MetadataPreview, task: String): PlannerLabRunResult {
        val planner = RecordingSimplePlanner()
        val executor = PlannerDemoPromptExecutor()
        val mark = TimeSource.Monotonic.markNow()
        val result = PlannerAIAgent(
            promptExecutor = executor,
            strategy = AIAgentPlannerStrategy(name = "planner-simple", planner = planner),
            agentConfig = AIAgentConfig(
                prompt = prompt("planner-lab-simple") { system("You are a helpful planning assistant.") },
                model = OllamaModels.Meta.LLAMA_3_2,
                maxAgentIterations = 12,
            ),
        ).run(task.ifBlank { DEFAULT_SIMPLE_TASK })

        return PlannerLabRunResult(
            mode = PlannerLabMode.SimpleReplanning,
            goal = task.ifBlank { DEFAULT_SIMPLE_TASK },
            status = "success",
            initialState = task.ifBlank { DEFAULT_SIMPLE_TASK },
            finalState = result,
            planSnapshots = planner.planSnapshots.mapIndexed { index, plan -> "v${index + 1} · ${plan.goal}: ${plan.steps.joinToString(" -> ") { it.description }}" },
            executedSteps = planner.executedSteps.toList(),
            eventLog = planner.eventLog.toList(),
            notes = listOf(
                "使用本地 deterministic PromptExecutor，不依赖真实网络和 API key",
                "ModelInfo 和 HiddenString 仍基于当前 Agent Config 生成，方便 UI 统一展示",
            ),
            replanCount = planner.replanCount,
            iterationCount = planner.executedSteps.size,
            elapsedMillis = mark.elapsedNow().inWholeMilliseconds,
            modelInfo = metadata.modelInfo,
            maskedApiKey = metadata.maskedApiKey,
            maskedSystemPrompt = metadata.maskedSystemPrompt,
        )
    }

    private suspend fun runGoapPlanner(metadata: MetadataPreview): PlannerLabRunResult {
        val eventLog = mutableListOf<String>()
        val planSteps = listOf("Gather wood", "Craft torch", "Explore cave")
        val planner = goap<GoapTorchState>(stateType = typeOf<GoapTorchState>()) {
            action("Gather wood", precondition = { !it.hasWood }, belief = { it.copy(hasWood = true) }) { _, state ->
                eventLog += "action · Gather wood"
                state.copy(hasWood = true, notes = state.notes + "Collected dry wood for crafting")
            }
            action("Craft torch", precondition = { it.hasWood && !it.hasTorch }, belief = { it.copy(hasTorch = true) }) { _, state ->
                eventLog += "action · Craft torch"
                state.copy(hasTorch = true, notes = state.notes + "Crafted a torch to enter the cave safely")
            }
            action("Explore cave", precondition = { it.hasTorch && !it.caveExplored }, belief = { it.copy(caveExplored = true) }) { _, state ->
                eventLog += "action · Explore cave"
                state.copy(caveExplored = true, notes = state.notes + "Explored the cave and mapped the safe route")
            }
            goal("Explore cave safely", condition = { it.caveExplored })
        }
        val mark = TimeSource.Monotonic.markNow()
        val result = PlannerAIAgent(
            promptExecutor = SilentPromptExecutor(),
            strategy = AIAgentPlannerStrategy(name = "planner-goap", planner = planner),
            agentConfig = AIAgentConfig(
                prompt = prompt("planner-lab-goap") { system("You are a GOAP demo planner.") },
                model = OllamaModels.Meta.LLAMA_3_2,
                maxAgentIterations = 10,
            ),
        ).run(GoapTorchState())

        return PlannerLabRunResult(
            mode = PlannerLabMode.GoapTorch,
            goal = "Explore the cave safely",
            status = if (result.caveExplored) "success" else "incomplete",
            initialState = GoapTorchState().toString(),
            finalState = result.toString(),
            planSnapshots = listOf("v1 · Explore the cave safely: ${planSteps.joinToString(" -> ")}"),
            executedSteps = planSteps,
            eventLog = eventLog + result.notes.map { "state · $it" },
            notes = listOf("GOAP 演示不走 LLM，完全使用预条件 / belief / goal 进行本地搜索"),
            replanCount = 0,
            iterationCount = eventLog.size,
            elapsedMillis = mark.elapsedNow().inWholeMilliseconds,
            modelInfo = metadata.modelInfo,
            maskedApiKey = metadata.maskedApiKey,
            maskedSystemPrompt = metadata.maskedSystemPrompt,
        )
    }

    private const val DEFAULT_SIMPLE_TASK = "Ship the Android Planner Lab and verify it with focused tests"

    private data class GoapTorchState(
        val hasWood: Boolean = false,
        val hasTorch: Boolean = false,
        val caveExplored: Boolean = false,
        val notes: List<String> = emptyList(),
    )

    private class SilentPromptExecutor : PromptExecutor {
        override suspend fun execute(prompt: Prompt, model: ai.koog.prompt.llm.LLModel, tools: List<ToolDescriptor>): List<Message.Response> =
            listOf(Message.Assistant("GOAP demo executor was not expected to call the model.", ResponseMetaInfo.Empty))

        override fun executeStreaming(prompt: Prompt, model: ai.koog.prompt.llm.LLModel, tools: List<ToolDescriptor>): kotlinx.coroutines.flow.Flow<StreamFrame> =
            emptyFlow<StreamFrame>()

        override suspend fun moderate(prompt: Prompt, model: ai.koog.prompt.llm.LLModel): ModerationResult = ModerationResult(
            isHarmful = false,
            categories = emptyMap(),
        )

        override fun close() = Unit
    }

    private class PlannerDemoPromptExecutor : PromptExecutor {
        private val progress = mutableListOf<String>()
        private val initialPlan = SimplePlan(
            goal = DEFAULT_SIMPLE_TASK,
            steps = mutableListOf(
                PlanStep("Inspect current planner surfaces"),
                PlanStep("Implement the first Planner Lab run loop"),
            ),
        )
        private val replanned = SimplePlan(
            goal = DEFAULT_SIMPLE_TASK,
            steps = mutableListOf(
                PlanStep("Add Planner Lab workspace"),
                PlanStep("Surface planner summary and agents-utils metadata"),
                PlanStep("Verify with focused unit tests"),
            ),
        )

        override suspend fun execute(prompt: Prompt, model: ai.koog.prompt.llm.LLModel, tools: List<ToolDescriptor>): List<Message.Response> {
            val request = prompt.messages.joinToString("\n") { it.content }
            val content = when {
                "Main Goal -- Create a Plan" in request && "Previous Plan (failed)" in request -> json.encodeToString(replanned)
                "Main Goal -- Create a Plan" in request -> json.encodeToString(initialPlan)
                "Inspect current planner surfaces" in request -> appendProgress("Inspected current planner surfaces. blocked: Planner Lab workspace is still missing.")
                "Implement the first Planner Lab run loop" in request -> appendProgress("Built the first Planner Lab run loop skeleton.")
                "Add Planner Lab workspace" in request -> appendProgress("Added Planner Lab workspace and navigation wiring.")
                "Surface planner summary and agents-utils metadata" in request -> appendProgress("Surfaced planner summary, ModelInfo, and HiddenString masking in the supporting panels.")
                "Verify with focused unit tests" in request -> appendProgress("Added focused planner tests and verified deterministic execution.")
                else -> appendProgress("Planner demo executor handled an unrecognized prompt.")
            }
            return listOf(Message.Assistant(content, ResponseMetaInfo.Empty))
        }

        override fun executeStreaming(prompt: Prompt, model: ai.koog.prompt.llm.LLModel, tools: List<ToolDescriptor>): kotlinx.coroutines.flow.Flow<StreamFrame> =
            emptyFlow<StreamFrame>()

        override suspend fun moderate(prompt: Prompt, model: ai.koog.prompt.llm.LLModel): ModerationResult = ModerationResult(
            isHarmful = false,
            categories = emptyMap(),
        )

        override fun close() = Unit

        private fun appendProgress(line: String): String {
            progress += line
            return progress.joinToString("\n")
        }
    }

    private class RecordingSimplePlanner : SimpleLLMPlanner() {
        val planSnapshots = mutableListOf<SimplePlan>()
        val executedSteps = mutableListOf<String>()
        val eventLog = mutableListOf<String>()
        var replanCount: Int = 0
            private set
        private var didReplan = false

        override suspend fun assessPlan(context: ai.koog.agents.core.agent.context.AIAgentFunctionalContext, state: String, plan: SimplePlan?): SimplePlanAssessment<SimplePlan> {
            if (plan == null) return SimplePlanAssessment.NoPlan()
            return if (!didReplan && "blocked:" in state.lowercase()) {
                didReplan = true
                replanCount += 1
                eventLog += "planner · replan requested because state reported a blocker"
                SimplePlanAssessment.Replan(plan, "The current state reported a blocker, so the planner should revise the plan.")
            } else {
                SimplePlanAssessment.Continue(plan)
            }
        }

        override suspend fun buildPlan(context: ai.koog.agents.core.agent.context.AIAgentFunctionalContext, state: String, plan: SimplePlan?): SimplePlan {
            val built = super.buildPlan(context, state, plan)
            planSnapshots += built.copy(steps = built.steps.toMutableList())
            eventLog += "planner · built plan with ${built.steps.size} step(s)"
            return built
        }

        override suspend fun executeStep(context: ai.koog.agents.core.agent.context.AIAgentFunctionalContext, state: String, plan: SimplePlan): String {
            plan.steps.firstOrNull { !it.isCompleted }?.let { step ->
                executedSteps += step.description
                eventLog += "step · ${step.description}"
            }
            return super.executeStep(context, state, plan)
        }
    }
}
