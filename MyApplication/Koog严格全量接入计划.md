## 目标

将 `MyApplication` 从当前聊天 Demo 升级为 **Koog Studio / Agent Workbench**，对以下范围做**严格全量接入**：

- `agents-core`
- `agents-tools`
- `agents-tools` 中 `jvmMain` 的 reflect 能力，通过 **JVM bridge** 纳入 Android 端可用能力面

本计划的目标不是“演示几个示例”，而是做到：

- 能力不漏项
- 关键能力有 UI、配置与运行入口
- Android 端可实际使用
- 可验证、可持续迭代

## 已确认路线

采用 **严格全量路线**：

1. Android 端直接接入 `agents-core` 与 `agents-tools` 的 `commonMain` 能力
2. 通过仓库内新增 **JVM bridge / host** 承载 `agents-tools` 的 `jvmMain reflect` 能力
3. Android UI 统一接入两侧能力，避免“common 接了、reflect 漏掉”的情况

## 范围界定

### 本次必须覆盖

- `agents-core` 本体能力
- `agents-tools` 本体能力
- `agents-tools/src/jvmMain/.../tools/reflect`

### 不自动计入本次范围

- `agents-ext` 中额外 built-in tools
- `agents-features-*` 其它扩展模块（snapshot / telemetry / mcp 等）
- `a2a`、`agents-mcp-*` 等独立模块

如后续需要，可在本计划完成后追加二期接入。

## 已盘点的能力面

### agents-core

- Agent 类型：`AIAgent` / `GraphAIAgent` / `FunctionalAIAgent` / `StatefulSingleUseAIAgent` / `AIAgentService` / `AIAgentTool`
- Strategy / DSL：`strategy`、`node`、`edge`、`forwardTo`、`onCondition`、`transformed`、`subgraph`、`parallel`
- 预置节点：LLM request / multiple / force-one-tool / streaming / execute-tool / execute-multiple-tools / moderate / send-tool-result 等
- Functional API：`requestLLM*`、`executeMultipleTools`、`sendMultipleToolResults`、`compressHistory`、`latestTokenUsage`、`llm.readSession/writeSession`
- Context / Session / Execution：`AIAgentContext`、graph/functional/llm context、`runId`、`parentContext`、execution info、node path
- Environment：`AIAgentEnvironment`、`ReceivedToolResult`、`SafeTool`、`TerminationTool`、`ToolResultKind`
- Feature 基础设施：handler、debugger、remote client/server、writer、pipeline、message processor

### agents-tools

- Tool 模型：`Tool`、`SimpleTool`、`ToolDescriptor`、`ToolParameterDescriptor`、`ToolParameterType`
- Registry：`ToolRegistry` 与 builder/merge/retrieve 能力
- 序列化：tool args/result 编解码、descriptor JSON 序列化、schema generator
- 注解：`@Tool`、`@LLMDescription`
- 错误与校验：`ToolException`、`validate`、`validateNotNull`、`fail`
- 兼容层：`ToolArgs` / `ToolResult`（虽废弃但仍需识别）

### agents-tools reflect（JVM bridge 必须覆盖）

- `ToolFromCallable`
- `ToolSet`
- `KFunction.asTool(...)`
- `instance.asTools()`
- `ToolRegistry.Builder.tool(KFunction, ...)`
- callable -> tool 自动描述提取、默认参数、复杂类型、suspend/sync 兼容、非可序列化参数失败路径

## 目标应用形态

`MyApplication` 将从“双页聊天 UI”升级为多工作区应用，建议名称可沿用 `Koog Studio` 内部概念。

### 工作区

1. **Chat**
   - 聊天运行入口
   - streaming 输出
   - tool call / tool result / error 可视化

2. **Agent Config**
   - provider / model / system prompt / temperature / max iterations
   - agent 类型选择
   - strategy 模板选择
   - tool call policy / moderation / history compression / streaming 设置

3. **Strategy Lab**
   - graph / functional / single-run / streaming 预设
   - 节点链路预览
   - 子图 / 并行 / 条件路由配置与执行轨迹

4. **Tool Registry**
   - registry 列表
   - descriptor/schema 浏览
   - 手动调用工具
   - JSON 参数编辑
   - result / validation / failure 可视化

5. **Events / Debug / Remote**
   - lifecycle、llm、tool、node、strategy、streaming 事件流
   - debugger 输出
   - remote client/server 状态与日志

6. **Session / State Inspector**
   - runId、agent state、node path、recent context、message history、tool history

## 严格全量架构方案

### Android 侧

- Compose 多工作区 UI
- 本地状态/配置持久化
- Agent Runtime 控制器
- Tool Registry 控制器
- Event Timeline 存储
- Session Inspector 状态模型

### JVM bridge 侧

- 在仓库内新增 JVM host / bridge 模块
- 封装 reflect API：callable 注册、ToolSet 扫描、descriptor 导出、参数 schema、工具执行
- 通过明确协议暴露给 Android：
  - 列出 reflect tools
  - 获取 descriptor/schema
  - 生成 registry 片段
  - 调试调用工具
  - 返回 success/failure/result payload

### Android 与 JVM bridge 的集成目标

- Android 端把 common tools 与 reflect tools 视为统一能力面
- UI 上不区分“这个工具来自 common 还是 reflect”，但调试信息中保留来源标识
- 所有工具最终都能进入统一 registry / 调试 / 展示链路

## 分阶段实施

### Phase 1：应用壳重构

- 重做导航为多工作区
- 抽离统一 runtime / config / store
- 定义持久化模型
- 保留现有聊天能力作为 Chat 工作区子集

### Phase 2：agents-core 主干全接

- agent 类型切换
- single-run / graph / functional / streaming 预设
- force-one-tool / multi-tool / parallel-tools / subgraph 预设
- context / session / event 面板基础接入

### Phase 3：agents-tools common 全接

- Tool Registry 工作台
- descriptor/schema 浏览
- JSON 参数编辑与调试执行
- success/failure/result 类型展示

### Phase 4：core feature 面板

- handler timeline
- debugger 输出
- remote client/server 配置与状态展示
- writer 输出入口

### Phase 5：JVM bridge 接入 reflect

- 新增 JVM bridge 模块
- 接入 `ToolFromCallable` / `ToolSet` / `asTool` / `asTools`
- Android 端整合 bridge 提供的 reflect tool 能力
- 完成 UI 与运行链路闭环

### Phase 6：验收与补漏

- 按能力清单逐项验收
- 补齐遗漏 UI / 配置 / 调试入口
- 完成回归构建与测试

## 验收标准

### Core 验收

- 每种核心 agent/strategy 模式都可从 UI 启动
- 可看到运行结果、事件、错误、状态
- streaming、tool calls、subgraph、parallel 至少各有一个可运行预设

### Tools 验收

- 可浏览 registry / descriptor / schema
- 可手动调试工具
- 可清晰区分参数解析失败、校验失败、执行失败、结果反序列化失败

### Reflect 验收

- JVM bridge 能发现并暴露 reflect tools
- Android 可调用 reflect tools 并查看 descriptor/schema/result
- `ToolSet` / callable 工具不会在 UI 或运行链路中缺席

## Wave 1 扩展进度

### Phase 1：code-tools bridge 协议与 host

- 已完成 `code-tools-bridge-protocol` 与 `code-tools-bridge-host`
- 已覆盖目录浏览、读写/编辑文件、regex 搜索、受控 shell、policy diagnostics
- 已具备 `/snapshot`、`/diagnostics`、`/shell/approval`、`/invoke` 闭环

### Phase 2：Android Code Tools 工作区

- 已完成 Android 侧 bridge client、配置持久化与 Code Tools 工作区
- Tool Registry 已统一聚合 common / reflect / code-tools 三源工具
- 已支持目录、文件、搜索、shell approval 与执行历史调试

### Phase 3：Planner Lab 与 agents-utils

- 已新增 Planner Lab 工作区，覆盖 `SimpleLLMPlanner` 可重规划演示与 GOAP demo
- 已将 `ModelInfo` / `HiddenString` 脱敏预览接入 Agent Config、Session Inspector、Events
- planner runtime summary、plan snapshots、timeline 已可视化

### Phase 4：测试、回归与验收

- 已补 app 与 host 的 targeted tests，覆盖 Planner metadata、code-tools failure classification、shell approval policy
- 已完成 `:app:testDebugUnitTest`、`:app:assembleDebug`、host 定向测试回归
- 当前 Wave 1 已达到“可运行、可调试、可验证”的收口状态

## 风险与注意事项

- reflect 能力是 JVM-only，不能在 Android 端直接伪装成本地 common API
- “严格全量”会带来明显架构扩张，不能继续以 Demo 代码方式堆补丁
- `agents-features-*` / `agents-ext` 不应与本次范围混淆，避免范围失控

## 当前下一步

1. 如需继续扩展，可为 Planner Lab 增加更多真实 planner 策略与事件接线
2. 可继续补 Compose UI 层交互测试与 bridge host HTTP 级集成测试
3. Wave 1 已完成，下一步转入新的扩展范围时再建立新阶段计划

本文件是严格全量接入的主计划文档，后续每完成一个阶段都要更新。