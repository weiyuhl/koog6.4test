## 目标

将 `agents/` 目录下全部模块纳入 `MyApplication`，形成 **Koog Studio Full Surface**：

- Android 端对 `commonMain` 能力直接接入
- 对 JVM-only 能力通过 **Bridge Host** 接入
- 对测试/示例型能力通过 **验证工作台 / 测试 harness** 接入
- 最终做到：模块不漏、能力不漏、入口可见、状态可验

## 范围

本次总计划覆盖以下模块：

- `agents-core`
- `agents-tools`
- `agents-ext`
- `agents-planner`
- `agents-utils`
- `agents-features/*`
- `agents-mcp`
- `agents-mcp-server`
- `agents-test`

## 接入原则

### 1. 三条接入通道
- **Android Direct**：`commonMain` 且适合移动端直接运行的能力
- **JVM Bridge**：`jvmMain`、本地文件系统、shell、MCP、server、SQL、OTel 等能力
- **Test/Verification Only**：测试工具、mock、断言 DSL，不进入正式运行链路，但必须纳入验证链路

### 2. 不做伪接入
- 不能在 Android 端直接运行的能力，不假装“已接入”，必须明确走 bridge
- 纯测试模块不强行塞进正式 UI，而是进入 `Test Lab / Validation` 工作区

### 3. UI 统一
所有能力最终都要在 `MyApplication` 中至少具备以下之一：
- 配置入口
- 调试入口
- 运行入口
- 状态/日志/结果查看入口

## 模块接入地图

### A. agents-core
目标：完整接入 agent 类型、strategy、node DSL、session/context、environment。

落地：
- Android Direct
- 对应工作区：`Chat` / `Agent Config` / `Strategy Lab` / `Session Inspector`
- 补齐项：agent type 切换、subgraph/parallel/conditional 可视化、AIAgentService/AgentTool 入口

### B. agents-tools
目标：完整接入 tool 模型、registry、schema、validation、failure 分类。

落地：
- `commonMain` 走 Android Direct
- reflect 与 JVM 扩展走 JVM Bridge
- 对应工作区：`Tool Registry`

### C. agents-ext
目标：接入现成 built-in tools 与扩展策略能力。

子范围：
- file tools：`ReadFileTool` / `ListDirectoryTool` / `EditFileTool` / `WriteFileTool`
- search tools：`RegexSearchTool`
- shell tools：`ExecuteShellCommandTool`
- ext agent/llm DSL：subtask、choice、retry 等扩展

落地：
- 文件/搜索：优先 JVM Bridge
- shell：仅 JVM Bridge，带审批与沙箱
- 纯 common DSL：Android Direct
- 对应工作区：`Code Tools` / `Strategy Lab`

### D. agents-planner
目标：接入 `PlannerAIAgent`、Simple planner、GOAP planner。

落地：
- Android Direct 为主
- 对应工作区：`Planner Lab`
- 能看到：state、plan、action、goal、replan、最终状态

### E. agents-utils
目标：把模型信息、隐藏字段、通用辅助结构接入到状态展示和日志脱敏链路。

落地：
- Android Direct
- 对应工作区：`Session Inspector` / `Events`

### F. agents-features/*
目标：逐模块完整接入 feature 安装、配置、状态与观测。

子模块分组：
- Direct 优先：
  - `agents-features-event-handler`
  - `agents-features-memory`
  - `agents-features-trace`
  - `agents-features-tokenizer`
  - `agents-features-snapshot`
- Bridge 优先：
  - `agents-features-opentelemetry`
  - `agents-features-sql`
  - `agents-features-acp`
  - `agents-features-a2a-core`
  - `agents-features-a2a-client`
  - `agents-features-a2a-server`

对应工作区：
- `Events / Debug / Remote`
- `Memory & Snapshot`
- `Trace / Tokenizer`
- `A2A / ACP`

### G. agents-mcp
目标：接入 MCP tool registry provider 与 MCP tool bridge。

落地：
- JVM Bridge Only
- 对应工作区：`MCP Studio`
- 能配置 stdio/process/transport，列出远端 MCP tools，执行并查看结果

### H. agents-mcp-server
目标：把 Koog agent 暴露成 MCP server 或接入 server 运行态。

落地：
- JVM Bridge Only
- 对应工作区：`MCP Server`
- 能启动/停止 server、查看暴露能力、查看连接状态与日志

### I. agents-test
目标：把测试工具纳入验证体系，而不是产品运行态。

落地：
- Test/Verification Only
- 对应工作区：`Test Lab`
- 能运行 deterministic agent tests、mock llm、tool assertions、snapshot regressions

## 应用目标工作区

`MyApplication` 最终扩展为：
- Chat
- Agent Config
- Strategy Lab
- Tool Registry
- Code Tools
- Planner Lab
- Events / Debug / Remote
- Memory & Snapshot
- Trace / Tokenizer
- MCP Studio
- MCP Server
- A2A / ACP
- Session Inspector
- Test Lab

## 桥接架构

新增并拆分 JVM hosts：
- `reflect-bridge-host`：继续承载 tools reflect
- `code-tools-bridge-host`：file/search/shell
- `feature-bridge-host`：OTel / SQL / ACP / A2A
- `mcp-bridge-host`：MCP client/server
- `test-bridge-host`：测试 harness 与 deterministic runs

Android 端统一通过协议层接入，UI 中保留 `source = direct / bridge / test-harness`。

## 分阶段实施

### Phase 1：范围盘点与模块地图
- 为每个 `agents/*` 模块出 capability matrix
- 标注 direct / bridge / test-only 归属
- 产出模块到工作区映射表

### Phase 2：扩展 Bridge 架构
- 把当前单一 reflect bridge 扩展为多 host 架构
- 定义统一协议：capabilities、schema、invoke、logs、status、diagnostics

### Phase 3：agents-ext 全接
- file/search/shell/tools 接入
- Code Tools 工作区落地
- 路径白名单、审批、沙箱策略落地

### Phase 4：planner + utils 全接
- Planner Lab
- GOAP/state/plan 可视化
- utils 脱敏与元信息接入

### Phase 5：features 全接
- memory/snapshot/trace/tokenizer/event-handler
- OTel/SQL/A2A/ACP bridge 化接入
- 每个 feature 都有配置、运行态和观测面板

### Phase 6：mcp / mcp-server 全接
- MCP Studio / MCP Server 工作区
- 支持列工具、调用、连接、日志、生命周期控制

### Phase 7：agents-test 验证体系
- Test Lab
- deterministic tests、mock llm、tool regression、snapshot regression

### Phase 8：全量验收
- 按模块逐项勾验
- direct/bridge/test-harness 三条链路全部构建通过
- 形成操作清单与回归脚本

## 验收标准

- `agents/` 下每个模块都能在 `MyApplication` 中找到对应入口
- 每个模块都明确标注为 direct / bridge / test-only
- JVM-only 能力均可从 Android 端远程查看和调用
- 纯测试模块具备独立验证入口而非缺席
- 至少有一份 capability matrix、一份操作清单、一套回归命令

## 风险

- 这是比当前范围大很多的工程，必须拆阶段，不能一次性硬接
- `shell / file edit / MCP / server / SQL / OTel` 都涉及安全与宿主环境，不宜直接进 Android 本地运行
- `agents-test` 的目标是“验证接入”，不是“用户态功能”

## 当前建议下一步

1. 先做 **Phase 1 capability matrix**
2. 再做 **Bridge 多 host 架构设计**
3. 第一批实施建议从 `agents-ext + agents-planner` 开始

