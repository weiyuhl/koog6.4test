## Koog agents 能力矩阵

本表用于把 `agents/` 目录下全部模块映射到 `MyApplication` 的接入方案。

状态说明：
- `已接`：当前应用已具备明确入口与可运行链路
- `部分`：已有部分能力或占位，但未全量闭环
- `未接`：尚未进入应用
- `验证`：仅纳入测试/验证链路，不进正式运行态

| 模块 | 子模块/范围 | 核心能力 | 接入方式 | 目标工作区 | Phase | 当前状态 |
|---|---|---|---|---|---|---|
| agents-core | agent types | `AIAgent` / `GraphAIAgent` / `FunctionalAIAgent` / `StatefulSingleUseAIAgent` / `AIAgentService` / `AIAgentTool` | Android Direct | Chat / Agent Config / Strategy Lab | 2 | 部分 |
| agents-core | strategy DSL | `strategy` / `node` / `edge` / `parallel` / `subgraph` / `routing` | Android Direct | Strategy Lab | 2 | 部分 |
| agents-core | built-in execution nodes | LLM request / tool execute / send tool result / streaming / moderation | Android Direct | Strategy Lab / Events | 2 | 部分 |
| agents-core | context/session/environment | runId / nodePath / session / environment / tool result kinds | Android Direct | Session Inspector / Events | 2 | 部分 |
| agents-tools | tool model | `Tool` / `SimpleTool` / descriptor / parameter type | Android Direct | Tool Registry | 3 | 已接 |
| agents-tools | registry/schema | `ToolRegistry` / merge / descriptor JSON / schema | Android Direct | Tool Registry | 3 | 已接 |
| agents-tools | validation/failure | `ToolException` / `validate` / `validateNotNull` / `fail` | Android Direct | Tool Registry | 3 / 6 | 已接 |
| agents-tools | reflect jvmMain | `ToolFromCallable` / `asTool` / `asTools` / `asToolsByInterface` / builder reflect | JVM Bridge | Tool Registry | 5 / 6 | 已接 |
| agents-ext | file tools | `ReadFileTool` / `ListDirectoryTool` / `EditFileTool` / `WriteFileTool` | JVM Bridge | Code Tools | 3 | 未接 |
| agents-ext | search tools | `RegexSearchTool` | JVM Bridge | Code Tools | 3 | 未接 |
| agents-ext | shell tools | `ExecuteShellCommandTool` / confirmation / executor | JVM Bridge | Code Tools | 3 | 未接 |
| agents-ext | graph/agent ext DSL | subtask / retry / choice / extended graph helpers | Android Direct + JVM Bridge | Strategy Lab | 3 / 4 | 未接 |
| agents-planner | planner core | `PlannerAIAgent` / planner strategy / replanning | Android Direct | Planner Lab | 4 | 未接 |
| agents-planner | simple llm planner | LLM-based plan build/assess/execute loop | Android Direct | Planner Lab | 4 | 未接 |
| agents-planner | GOAP | goals / actions / state search / plan graph | Android Direct | Planner Lab | 4 | 未接 |
| agents-utils | model/meta utils | model info / hidden string / shared helper types | Android Direct | Session Inspector / Events | 4 | 未接 |
| agents-features-event-handler | event stream | standardized lifecycle / llm / tool / node events | Android Direct | Events / Debug / Remote | 5 | 部分 |
| agents-features-memory | memory | memory provider / retrieval / persistence hooks | Android Direct | Memory & Snapshot | 5 | 未接 |
| agents-features-trace | trace | trace spans / agent run tracing / correlation | Android Direct | Trace / Tokenizer | 5 | 未接 |
| agents-features-tokenizer | tokenizer | token counting / cache / prompt token metrics | Android Direct | Trace / Tokenizer | 5 | 未接 |
| agents-features-snapshot | snapshot | checkpoint / resume / reproducibility | Android Direct + JVM Bridge | Memory & Snapshot | 5 | 未接 |
| agents-features-opentelemetry | telemetry export | OTel wiring / exporters / external observability | JVM Bridge | Events / Debug / Remote | 5 | 未接 |
| agents-features-sql | sql persistence | SQL-backed storage / runtime persistence | JVM Bridge | Memory & Snapshot | 5 | 未接 |
| agents-features-acp | ACP integration | ACP runtime / protocol bridging / remote actions | JVM Bridge | A2A / ACP | 5 | 未接 |
| agents-features-a2a-core | agent-to-agent core | A2A protocol core / shared model | JVM Bridge | A2A / ACP | 5 | 未接 |
| agents-features-a2a-client | remote agent client | send task / subscribe / receive remote events | JVM Bridge | A2A / ACP | 5 | 未接 |
| agents-features-a2a-server | remote agent server | expose agent as A2A endpoint / task server | JVM Bridge | A2A / ACP | 5 | 未接 |
| agents-mcp | MCP client/tool bridge | MCP transport / tool registry provider / remote tools | JVM Bridge | MCP Studio | 6 | 未接 |
| agents-mcp-server | MCP server runtime | expose Koog-side capability as MCP server | JVM Bridge | MCP Server | 6 | 未接 |
| agents-test | deterministic tests | mock llm / mock tools / graph assertions / test DSL | Test-only | Test Lab | 7 | 未接 |

## 能力接入总览

### 已接/部分已接
- `agents-core`：Chat、Agent Config、Strategy、Session、Events 基础面已具备，但不是全量
- `agents-tools commonMain`：已完成 Tool Registry + schema + 调试执行
- `agents-tools reflect(jvmMain)`：已通过 `reflect-bridge-host` 接入
- `agents-features-event-handler`：已有事件面板与部分链路

### 第一批高优先级未接
1. `agents-ext` 文件/搜索/shell
2. `agents-planner`
3. `agents-features-memory / snapshot / tokenizer / trace`

### 第二批高成本未接
1. `agents-features-opentelemetry`
2. `agents-features-sql`
3. `agents-features-a2a-*`
4. `agents-features-acp`
5. `agents-mcp` / `agents-mcp-server`

### 验证专用
- `agents-test` 不进正式用户态，但必须进入 `Test Lab` 与回归脚本

## 建议实施顺序

### Wave 1
- `agents-ext`
- `agents-planner`
- `agents-utils`

### Wave 2
- `agents-features-memory`
- `agents-features-snapshot`
- `agents-features-tokenizer`
- `agents-features-trace`

### Wave 3
- `agents-features-opentelemetry`
- `agents-features-sql`
- `agents-features-acp`
- `agents-features-a2a-*`

### Wave 4
- `agents-mcp`
- `agents-mcp-server`
- `agents-test`

## 每波实施的统一交付物
- 依赖接入
- direct/bridge 运行链路
- 对应工作区 UI
- 配置存储
- diagnostics / 日志 / 状态面板
- 最小单测 + 构建验证 + 手工验收项

