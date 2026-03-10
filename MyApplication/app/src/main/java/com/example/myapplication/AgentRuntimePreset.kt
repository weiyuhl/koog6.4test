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
        description = "使用 graph single-run strategy，允许多工具顺序执行。",
        family = "GraphAIAgent",
        usesTools = true,
        supportsStreaming = false,
    ),
    GraphToolsParallel(
        id = "graph-tools-parallel",
        title = "Graph tools parallel",
        description = "使用 graph single-run strategy，允许并行工具执行。",
        family = "GraphAIAgent",
        usesTools = true,
        supportsStreaming = false,
    ),
    GraphSubgraphTools(
        id = "graph-subgraph-tools",
        title = "Subgraph tools loop",
        description = "使用 graph + subgraph 组合执行工具循环，便于观察 subgraph 生命周期。",
        family = "GraphAIAgent",
        usesTools = true,
        supportsStreaming = false,
    ),
    GraphParallelSignalMerge(
        id = "graph-parallel-signal-merge",
        title = "Parallel signal merge",
        description = "使用 parallel node 汇总多路输入信号，再生成最终回答。",
        family = "GraphAIAgent",
        usesTools = false,
        supportsStreaming = false,
    ),
    GraphConditionalRouting(
        id = "graph-conditional-routing",
        title = "Conditional routing graph",
        description = "使用 condition edges + transformed routes + storage 在图中分流到不同执行路径。",
        family = "GraphAIAgent",
        usesTools = true,
        supportsStreaming = false,
    ),
    StreamingWithTools(
        id = "streaming-with-tools",
        title = "Streaming with tools",
        description = "使用 graph streaming strategy，文本增量输出并支持工具。",
        family = "GraphAIAgent",
        usesTools = true,
        supportsStreaming = true,
    ),
    FunctionalToolsLoop(
        id = "functional-tools-loop",
        title = "Functional tools loop",
        description = "使用 functionalStrategy 执行 request/execute/send 循环。",
        family = "FunctionalAIAgent",
        usesTools = true,
        supportsStreaming = false,
    );

    companion object {
        fun fromId(id: String?): AgentRuntimePreset = entries.firstOrNull { it.id == id } ?: StreamingWithTools
    }
}