## Wave 1 实施清单

### 范围
Wave 1 聚焦三块：
- `agents-ext`
- `agents-planner`
- `agents-utils`

目标不是一次把所有能力都做完，而是先把 **代码工具能力 + 规划能力 + 通用元信息能力** 形成下一轮可运行闭环。

## Wave 1 总目标

### 1. Code Tools 工作区落地
让 `MyApplication` 具备最小可用的“代码代理工具台”：
- 目录浏览
- 文件读取
- 文件编辑
- 文件写入
- 正则搜索
- shell 执行（受控）

### 2. Planner Lab 工作区落地
让 `MyApplication` 能真正运行规划型 agent：
- PlannerAIAgent
- simple LLM planner
- GOAP planner
- 计划、步骤、目标、执行结果可视化

### 3. utils 接入到状态展示链路
把：
- `ModelInfo`
- `HiddenString`
- 相关元信息类型
接入到：
- Session Inspector
- Events
- 配置摘要与脱敏展示

## 模块拆分与接入方式

### A. agents-ext

#### A1. file/search/shell tools
能力：
- `ReadFileTool`
- `ListDirectoryTool`
- `EditFileTool`
- `WriteFileTool`
- `RegexSearchTool`
- `ExecuteShellCommandTool`

接入方式：
- **JVM Bridge 优先**
- 不在 Android 本地直接碰宿主文件系统

目标工作区：
- `Code Tools`
- `Tool Registry`（桥接工具也要能在这里看到）

#### A2. ext graph/agent helpers
能力：
- subtask
- retry
- choice
- 扩展 graph DSL

接入方式：
- `commonMain` 走 Android Direct
- JVM 专属辅助能力走 Bridge

目标工作区：
- `Strategy Lab`

### B. agents-planner

能力：
- `PlannerAIAgent`
- simple planner
- GOAP planner
- plan/replan/goal/action 模型

接入方式：
- **Android Direct**

目标工作区：
- `Planner Lab`
- `Session Inspector`

### C. agents-utils

能力：
- `ModelInfo`
- `HiddenString`
- 其它元信息辅助结构

接入方式：
- **Android Direct**

目标工作区：
- `Session Inspector`
- `Events`
- `Agent Config`

## 具体实施任务

### Task 1：扩展 Bridge 协议到 Code Tools
- 新增 `code-tools-bridge-protocol`
- 定义目录、文件、搜索、shell 的请求/响应 DTO
- 增加统一 diagnostics / approval / error 分类

### Task 2：新增 code-tools-bridge-host
- 基于 JVM 文件系统 provider 暴露：
  - list directory
  - read file
  - edit file
  - write file
  - regex search
- 暴露 shell 执行能力，但必须支持：
  - 开关控制
  - 路径白名单
  - 命令审批/确认
  - 超时与退出码回传

### Task 3：Android 端 Code Tools 工作区
- 新增 `Code Tools` 页面
- 支持：
  - 目录树浏览
  - 文件内容查看
  - 搜索表单
  - 编辑/写入表单
  - shell 命令执行面板
- 所有操作都要展示：
  - source
  - args
  - result
  - error kind
  - history

### Task 4：把 code tools 统一接入 Tool Registry
- bridge 提供的 file/search/shell 工具要出现在统一 Tool Registry
- 可以从 Tool Registry 手工调试，也可以从 Code Tools 专页调试

### Task 5：Strategy Lab 接入 ext helpers
- 补一组 ext 预设：
  - retry strategy demo
  - choice/routing demo
  - subtask/subgraph demo
- 展示节点链路与执行轨迹

### Task 6：新增 Planner Lab 工作区
- 可选择 planner 类型：
  - simple planner
  - GOAP planner
- 展示：
  - current state
  - plan steps
  - chosen action
  - replan count
  - final state/result

### Task 7：Planner runtime 与状态模型
- 抽象 planner request / runtime summary / execution log
- 接入 Session Inspector 与 Events
- 支持保留最近一次 plan 与执行记录

### Task 8：agents-utils 接入
- `ModelInfo` 接入运行摘要
- `HiddenString` 接入 API Key / secret 文本展示
- 统一配置摘要脱敏逻辑

### Task 9：测试与验收
- `code-tools-bridge-host` 单测
- Android 侧 Code Tools 参数/失败分类测试
- Planner Lab 最小运行测试
- `assembleDebug` + host test + app test

## UI 交付物
- 新工作区：`Code Tools`
- 新工作区：`Planner Lab`
- `Strategy Lab` 扩展 ext presets
- `Session Inspector` 增加 planner/model/meta 展示
- `Agent Config` 增加 code-tools bridge 配置

## 配置项

### Code Tools Bridge
- enable code tools bridge
- bridge base URL
- workspace root
- allow shell execution
- shell approval mode
- allowed path prefixes

### Planner
- planner type
- max planning iterations
- replanning policy
- goal/state demo presets

## 失败分类要求
Code Tools 至少区分：
- path validation failure
- file not found
- non-text file failure
- patch apply failure
- regex parse/search failure
- shell denied
- shell timeout
- shell execution failure
- transport failure

Planner 至少区分：
- plan build failure
- action execution failure
- replan exhaustion
- state serialization failure

## Wave 1 验收标准
- 能从 Android 端通过 bridge 浏览/读取/编辑/写入文件
- 能执行 regex search
- shell 命令可受控执行并返回日志
- Tool Registry 中能看到 code tools
- Planner Lab 能运行至少一个 simple planner 和一个 GOAP demo
- Session Inspector 能看到 planner 与 model meta 信息
- 单测、host test、assembleDebug 全通过

## 建议实施顺序
1. 协议 + host
2. Code Tools UI
3. Tool Registry 统一接入
4. Planner Lab
5. utils 脱敏/元信息
6. 回归测试与验收

