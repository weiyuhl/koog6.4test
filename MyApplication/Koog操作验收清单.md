## Koog 全量接入操作/验收清单

### 1. 目标
- 用这份清单手工确认 `MyApplication` 中的 core / tools / reflect bridge / feature panels 已可用。
- 建议按顺序执行，避免遗漏依赖步骤。

### 2. 启动前准备
- 确认 Android Studio 可正常打开 `MyApplication`。
- 确认本机 JDK 17 可用。
- 如需真实模型调用，先准备可用的 `API Key / Base URL / Model ID`。
- 如只做工具与界面验收，可不先验证真实 LLM 输出质量。

### 3. 启动 reflect bridge host
- 在仓库根目录或 `MyApplication` 目录运行：`./gradlew :reflect-bridge-host:run`
- Windows PowerShell 可用：`.\gradlew.bat :reflect-bridge-host:run`
- 默认端口为 `8095`。
- Android 模拟器里建议使用：`http://10.0.2.2:8095`
- 先访问或观察日志确认：
  - `/health` 返回 `{"status":"ok"}`
  - `/tools` 能返回 reflect tools 列表

### 4. 启动 Android App
- 在 Android Studio 运行 `app` 模块。
- 进入应用后，先打开 `Agent Config` 工作区。

### 5. Agent Config 工作区检查
- 检查以下设置项是否存在并可编辑：
  - Provider
  - Model ID
  - API Key
  - Base URL
  - Extra Config
  - Runtime Preset
  - System prompt
  - Temperature
  - Max iterations
  - Local writer
  - Debugger
  - Remote client
  - Reflect bridge 开关与 URL
- 修改几项配置后，重启 App，确认配置能恢复。
- 打开 reflect bridge：
  - 启用 `reflect bridge`
  - Base URL 填 `http://10.0.2.2:8095`

### 6. Chat 工作区检查
- 输入普通问题，确认消息可发送、可看到运行状态。
- 如已配置真实模型：
  - 确认能得到正常回答
  - 确认流式文本会逐步出现，而不是一次性整段返回
- 可用提示词示例：
  - `请先告诉我你当前使用的是哪个 provider。`
  - `现在几点？请优先调用工具。`
  - `请计算 12*13，并说明是否使用了工具。`

### 7. Events 工作区检查
- 先执行 1~2 次聊天请求。
- 打开 `Events` 工作区，确认可看到：
  - 事件时间线
  - feature message 输出
  - writer/debugger/remote client 相关状态
- 开启 `Local writer` 后，再执行一次请求，确认事件面板内容有增长。

### 8. Session Inspector 工作区检查
- 执行多次请求后，打开 `Session Inspector`。
- 确认可看到：
  - 历史消息/会话摘要
  - 运行快照
  - 结构化状态信息
- 重启 App 后，确认本地持久化内容仍可恢复。

### 9. Strategy Lab 工作区检查
- 切换不同 `Runtime Preset`，至少覆盖：
  - `StreamingWithTools`
  - `SingleRun`
  - `Graph / Functional / Routing` 中任意 2 个
- 每切换一个预设，执行一次请求，确认：
  - 可以正常运行
  - 运行摘要与当前预设一致

### 10. Tool Registry 工作区：common tools
- 打开 `Tool Registry`，确认能看到 `common-local` 工具。
- 检查每个工具都有：
  - 描述
  - 参数列表
  - schema/descriptor JSON
- 成功执行样例：
  - `current_time`：直接执行
  - 数学工具：输入合法数字并执行
- 检查执行结果面板是否展示：
  - tool name
  - source
  - registration
  - args json
  - result
  - history

### 11. Tool Registry 工作区：失败路径
- `validation_probe`
  - `ticketId` 留空，`attempts` 填 `2`
  - 预期：`VALIDATION_FAILURE`
- `execution_failure_probe`
  - `reason` 填任意文本
  - 预期：`EXECUTION_FAILURE`
- `result_serialization_probe`
  - `text` 填任意文本
  - 预期：`RESULT_SERIALIZATION_FAILURE`
- 对参数填错类型，例如整数工具填文本：
  - 预期：`ARGUMENT_PARSE_FAILURE`

### 12. Tool Registry 工作区：reflect tools
- 确认已连接 reflect bridge 后，打开 `Tool Registry`。
- 预期可看到 `reflect-bridge` 来源工具，例如：
  - `reflect_echo`
  - `delayed_uppercase`
  - `concat`
  - `createPerson`
  - `formatPerson`
  - `interfaceAdd`
  - `raisePower`
- 逐项检查：
  - source 显示为 `reflect-bridge`
  - registration 显示 `asTool / asTools / asToolsByInterface / Builder.tool(...)` 等来源
- 成功样例：
  - `reflect_echo`：`text=hello`
  - `delayed_uppercase`：`text=koog`, `repeat=2`
  - `interfaceAdd`：`a=2`, `b=3`
- 预期能看到 `Reflect bridge diagnostics` 区域，且至少有一条 `REGISTRATION_FAILURE` 诊断。

### 13. Debugger / Remote Client 配置面检查
- 在 `Agent Config` 中切换 `Debugger` 与 `Remote client` 开关。
- 检查相关 host/port 输入框可编辑、会持久化。
- 无需强制连通远端，只需确认配置入口、摘要展示、持久化链路正常。

### 14. 最小回归命令
- 提交前建议至少执行：
  - `.\gradlew.bat :app:testDebugUnitTest`
  - `.\gradlew.bat :reflect-bridge-host:test`
  - `.\gradlew.bat :app:assembleDebug`

### 15. 提交前结果判定
- 若以下全部满足，可认为本轮接入通过：
  - App 可启动
  - 配置可保存并恢复
  - Chat 可运行
  - Events / Session / Strategy 页面可查看内容
  - common tools 可执行
  - reflect tools 可加载并执行
  - 失败分类清晰可见
  - 单测与 `assembleDebug` 通过

### 16. 建议随提交一起保留的文件
- `Koog严格全量接入计划.md`
- `Koog操作验收清单.md`
- reflect bridge 相关模块与测试
- Tool Registry / Agent Config / Runner 相关改动

