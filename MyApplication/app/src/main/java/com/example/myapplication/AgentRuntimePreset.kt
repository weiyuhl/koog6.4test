package com.example.myapplication

enum class AgentRuntimePreset(
    val id: String,
    val title: String,
    val description: String,
    val family: String,
    val usesTools: Boolean,
    val supportsStreaming: Boolean,
) {
    BasicSingleRun(
        id = "basic-single-run",
        title = "Basic single-run",
        description = "使用 AIAgent 默认单轮执行，不启用工具。",
        family = "AIAgent",
        usesTools = false,
        supportsStreaming = false,
    ),
    GraphToolsSequential(
        id = "graph-tools-sequential",
        title = "Graph tools sequential",
        description = "使用 Koog 框架的 singleRunStrategy，允许多工具顺序执行。",
        family = "GraphAIAgent",
        usesTools = true,
        supportsStreaming = false,
    ),
    GraphToolsParallel(
        id = "graph-tools-parallel",
        title = "Graph tools parallel",
        description = "使用 Koog 框架的 singleRunStrategy，允许并行工具执行。",
        family = "GraphAIAgent",
        usesTools = true,
        supportsStreaming = false,
    ),
    FunctionalToolsLoop(
        id = "functional-tools-loop",
        title = "Functional tools loop",
        description = "使用 Koog 框架的 functionalStrategy 执行 request/execute/send 循环。",
        family = "FunctionalAIAgent",
        usesTools = true,
        supportsStreaming = false,
    );

    companion object {
        fun fromId(id: String?): AgentRuntimePreset = entries.firstOrNull { it.id == id } ?: GraphToolsSequential
    }
}