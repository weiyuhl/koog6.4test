package com.lhzkml.jasmine.core.assistant.tools

import com.lhzkml.jasmine.core.assistant.tools.NotificationTool
import com.lhzkml.jasmine.core.assistant.tools.AlarmTool
import com.lhzkml.jasmine.core.assistant.tools.CalendarTool

import com.lhzkml.jasmine.core.agent.tools.ToolRegistry

/**
 * 助手工具注册表助手
 * 实现从代理全量工具池中筛选出“助手专用”的工具子集（白名单机制）
 */
object ToolRegistry {

    /**
     * 助手允许使用的工具白名单
     * 重点包含：只读文件探索、网络搜索、基础计算与时间
     */
    private val WHITELIST = setOf(
        "read_file",
        "list_directory",
        "find_files",
        "search_by_regex",
        "file_info",
        "web_search",
        "open_url",
        "get_location_from_ip",
        "calculator"
    )

    /**
     * 从完整注册表中筛选出助手子集，并加入助理原生的本地工具
     */
    fun createSubset(
        context: android.content.Context, 
        fullRegistry: com.lhzkml.jasmine.core.agent.tools.ToolRegistry
    ): com.lhzkml.jasmine.core.agent.tools.ToolRegistry {
        // 1. 过滤出白名单中的 Agent 工具
        val subset = fullRegistry.subset(WHITELIST)
        
        // 2. 注入助理模块原生的移植工具
        subset.register(NotificationTool(context))
        subset.register(AlarmTool(context))
        subset.register(CalendarTool(context))
        subset.register(LocalTimeTool())
        subset.register(WebSearchTool())
        subset.register(IpLocationTool())
        subset.register(OpenUrlTool(context))
        
        return subset
    }
}
