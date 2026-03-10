package com.example.myapplication

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlannerLabRunnerTest {
    private val metadata = PlannerLabRunner.metadataPreview(
        provider = KoogProvider.OPENAI,
        modelId = "gpt-4.1-mini",
        apiKey = "super-secret",
        systemPrompt = "You are the planner",
        runtimePreset = AgentRuntimePreset.BasicSingleRun,
    )

    @Test
    fun simple_planner_demo_replans_once_and_masks_sensitive_values() = runBlocking {
        val result = PlannerLabRunner.run(PlannerLabMode.SimpleReplanning, metadata, "Ship the Android Planner Lab")

        assertEquals(PlannerLabMode.SimpleReplanning, result.mode)
        assertEquals(1, result.replanCount)
        assertTrue(result.planSnapshots.size >= 2)
        assertTrue(result.executedSteps.contains("Inspect current planner surfaces"))
        assertTrue(result.finalState.isNotBlank())
        assertTrue(result.eventLog.any { it.contains("replan", ignoreCase = true) })
        assertEquals("HIDDEN:non-empty", result.maskedApiKey)
        assertEquals("HIDDEN:system-prompt", result.maskedSystemPrompt)
    }

    @Test
    fun goap_demo_reaches_goal_without_replanning() = runBlocking {
        val result = PlannerLabRunner.run(PlannerLabMode.GoapTorch, metadata, "")

        assertEquals(PlannerLabMode.GoapTorch, result.mode)
        assertEquals(0, result.replanCount)
        assertTrue(result.finalState.contains("caveExplored=true"))
        assertEquals(listOf("Gather wood", "Craft torch", "Explore cave"), result.executedSteps)
    }

    @Test
    fun metadata_preview_uses_provider_default_model_and_keeps_empty_secrets_blank() {
        val preview = PlannerLabRunner.metadataPreview(
            provider = KoogProvider.OPENAI,
            modelId = "   ",
            apiKey = "",
            systemPrompt = "",
            runtimePreset = AgentRuntimePreset.BasicSingleRun,
        )

        assertEquals(KoogProvider.OPENAI.defaultModelId, preview.modelInfo.model)
        assertEquals("", preview.maskedApiKey)
        assertEquals("", preview.maskedSystemPrompt)
        assertEquals(AgentRuntimePreset.BasicSingleRun.title, preview.runtimePresetTitle)
    }
}