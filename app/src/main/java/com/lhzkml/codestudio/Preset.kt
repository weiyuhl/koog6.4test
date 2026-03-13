package com.lhzkml.codestudio

enum class Preset(
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
        description = "使用 AIAgent 默认单轮执行，不启用工具词",
        family = "AIAgent",
        usesTools = false,
        supportsStreaming = false,
    ),
    GraphToolsSequential(
        id = "graph-tools-sequential",
        title = "Graph tools sequential",
        description = "Use the framework's singleRunStrategy, allowing sequential execution of multiple tools.",
        family = "GraphAIAgent",
        usesTools = true,
        supportsStreaming = false,
    ),
    GraphToolsParallel(
        id = "graph-tools-parallel",
        title = "Graph tools parallel",
        description = "Use the framework's singleRunStrategy, allowing parallel tool execution.",
        family = "GraphAIAgent",
        usesTools = true,
        supportsStreaming = false,
    ),
    FunctionalToolsLoop(
        id = "functional-tools-loop",
        title = "Functional tools loop",
        description = "Use the framework's functionalStrategy to execute request/execute/send loop.",
        family = "FunctionalAIAgent",
        usesTools = true,
        supportsStreaming = false,
    );

    companion object {
        fun fromId(id: String?): Preset = entries.firstOrNull { it.id == id } ?: GraphToolsSequential
    }
}
