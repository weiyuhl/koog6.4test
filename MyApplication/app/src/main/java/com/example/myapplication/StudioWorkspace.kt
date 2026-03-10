package com.example.myapplication

enum class StudioWorkspace(
    val route: String,
    val title: String,
    val subtitle: String,
) {
    Chat(
        route = "workspace/chat",
        title = "Chat",
        subtitle = "聊天运行、流式输出、日志和对话历史",
    ),
    AgentConfig(
        route = "workspace/agent-config",
        title = "Agent Config",
        subtitle = "供应商、模型、运行参数与 Agent 入口配置",
    ),
    StrategyLab(
        route = "workspace/strategy-lab",
        title = "Strategy Lab",
        subtitle = "graph / functional / streaming / subgraph / parallel 预设入口",
    ),
    ToolRegistry(
        route = "workspace/tool-registry",
        title = "Tool Registry",
        subtitle = "查看 registry、descriptor、schema 和调试入口",
    ),
    EventsDebug(
        route = "workspace/events-debug",
        title = "Events / Debug / Remote",
        subtitle = "事件时间线、调试输出与远程能力占位面板",
    ),
    SessionInspector(
        route = "workspace/session-inspector",
        title = "Session Inspector",
        subtitle = "查看 run 状态、消息历史、上下文和工作区快照",
    );

    companion object {
        fun fromStoredRoute(route: String?): StudioWorkspace =
            entries.firstOrNull { it.route == route } ?: Chat
    }
}