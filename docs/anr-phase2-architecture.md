# ANRDemo 二期功能代码架构设计与开发准备

日期：2026-05-24  
版本：v1.0  
输入文档：`docs/anr-code-architecture.md`、当前首版代码

## 1. 二期目标

针对首版未完成的代码功能，二期目标是把“危险触发演示”从可用提升到可控、可诊断、可验证：

1. 触发前支持 3 秒倒计时，可取消。
2. 高危场景支持强确认策略：长按、输入确认文案、二次确认。
3. 高级场景可在 UI 中选择模式和参数，而不是只能使用默认 request。
4. 接入 `ApplicationExitInfo`、ContentProvider 耗时、内存压力实时状态等诊断面板。
5. 把 Broadcast async、JobService onStopJob、dangerous OOM 等高级场景纳入可控架构。
6. 准备设备级 adb smoke test 脚本和测试边界。

## 2. 当前代码缺口映射

| 未完成功能 | 当前状态 | 需要新增/改造模块 |
|---|---|---|
| 触发前 3 秒倒计时 | `ConfirmAnrDialog` 确认后立即 dispatch | `ui/trigger/CountdownTriggerController.kt`、`CountdownConfirmDialog` |
| 高危场景强确认 | 只通过 `enabledByDefault=false` 禁用部分按钮 | `SafetyPolicy`、`ConfirmationRequirement`、`DangerousConfirmDialog` |
| 高级模式 UI | `AnrScenario.defaultRequest` 固定参数 | `ScenarioParameterSpec`、`ScenarioRequestBuilder`、`ScenarioOptionsSheet` |
| `ApplicationExitInfo` UI | 有 `ExitInfoReader`，未接入 | `DiagnosticsPanel`、`DiagnosticsUiState` |
| 内存压力实时刷新 | UI 静态读取 `retainedMb()` | `MemoryPressureState`、轮询/刷新事件 |
| ContentProvider 诊断 | dispatcher 同步 query 后只返回耗时 | `ProviderDiagnosticResult`、`ProviderDiagnosticsPanel` |
| JobService 高级模式 | dispatcher 支持 mode，但 catalog/UI 未拆场景 | catalog 增加 `job-service-stop` 或参数化选项 |
| Broadcast async 模式 | receiver 支持 `goAsyncNoFinish`，catalog/UI 未暴露 | catalog 增加 `broadcast-async-no-finish` |
| README / adb smoke | 未实现脚本和设备验证记录 | `scripts/anr-smoke.sh`、README 章节 |

## 3. 新增架构分层

在现有 `anr/catalog`、`anr/dispatch`、`anr/safety`、`anr/triggers`、`ui` 基础上新增：

```text
app/src/main/java/com/codemx/anrdemo/
├── anr/
│   ├── catalog/
│   │   ├── ConfirmationRequirement.kt      # 场景需要何种确认
│   │   ├── ScenarioParameterSpec.kt        # 参数元数据
│   │   └── ScenarioRequestBuilder.kt       # 参数 -> AnrTriggerRequest
│   ├── diagnostics/
│   │   ├── DiagnosticsSnapshot.kt          # 诊断快照模型
│   │   ├── DiagnosticsRepository.kt        # ExitInfo/内存/最近结果聚合
│   │   ├── ProviderDiagnosticResult.kt     # provider 查询耗时模型
│   │   └── MemoryPressureState.kt          # 内存压力状态模型
│   └── safety/
│       ├── SafetyPolicy.kt                 # 场景风险策略
│       └── ConfirmationToken.kt            # 强确认结果
├── ui/
│   ├── trigger/
│   │   ├── CountdownTriggerController.kt   # 倒计时状态机
│   │   ├── TriggerFlowState.kt             # Idle/Confirming/CountingDown/Dispatching
│   │   ├── ConfirmationDialogs.kt          # 普通/高危确认
│   │   └── ScenarioOptionsSheet.kt         # 模式与参数输入
│   └── diagnostics/
│       ├── DiagnosticsPanel.kt
│       ├── ExitInfoCard.kt
│       ├── MemoryPressureCard.kt
│       └── ProviderDiagnosticCard.kt
└── scripts/
    └── anr-smoke.sh                        # 设备级 smoke 触发脚本
```

## 4. Catalog 扩展设计

### 4.1 `ConfirmationRequirement`

用于替代简单的 `enabledByDefault`，把“能否触发”和“如何确认”分开。

```kotlin
enum class ConfirmationRequirement {
    NormalConfirm,          // 普通弹窗确认
    CountdownConfirm,       // 普通确认 + 3 秒倒计时
    LongPressConfirm,       // 长按按钮
    TypePhraseConfirm,      // 输入确认短语
    DisabledDocumentation,  // 仅说明，不可触发
}
```

建议映射：

| 场景 | 确认策略 |
|---|---|
| input-dispatch | `CountdownConfirm` |
| deadlock contention | `CountdownConfirm` |
| deadlock classic | `TypePhraseConfirm`，短语 `DEADLOCK` |
| memory-pressure bounded | `CountdownConfirm` |
| dangerous OOM | `TypePhraseConfirm`，短语 `OOM` |
| broadcast foreground | `CountdownConfirm` |
| broadcast background | `LongPressConfirm` |
| service background 200s | `TypePhraseConfirm`，短语 `SERVICE200` |
| shortService 3min | `TypePhraseConfirm`，短语 `SHORT_SERVICE` |
| no-focused-window | `DisabledDocumentation` |

### 4.2 `ScenarioParameterSpec`

为 UI 参数面板提供元数据。

```kotlin
sealed class ScenarioParameterSpec {
    data class DurationMs(
        val key: String = "blockMs",
        val label: String,
        val defaultValue: Long,
        val min: Long,
        val max: Long,
    ) : ScenarioParameterSpec()

    data class Choice(
        val key: String,
        val label: String,
        val defaultValue: String,
        val options: List<Option>,
    ) : ScenarioParameterSpec()

    data class IntRange(
        val key: String,
        val label: String,
        val defaultValue: Int,
        val min: Int,
        val max: Int,
        val step: Int,
    ) : ScenarioParameterSpec()

    data class BooleanToggle(
        val key: String,
        val label: String,
        val defaultValue: Boolean,
    ) : ScenarioParameterSpec()
}
```

`AnrScenario` 二期新增字段：

```kotlin
val confirmationRequirement: ConfirmationRequirement
val parameterSpecs: List<ScenarioParameterSpec> = emptyList()
val confirmationPhrase: String? = null
val documentationOnlyReason: String? = null
```

### 4.3 新增/拆分场景

二期 catalog 建议新增独立卡片：

1. `deadlock-classic`：经典死锁，强确认。
2. `memory-dangerous-oom`：危险 OOM，默认隐藏或高级分组。
3. `broadcast-async-no-finish`：`goAsync()` 后不 `finish()`。
4. `job-service-stop`：`onStopJob()` 阻塞。
5. `content-provider-diagnostic`：Provider 查询耗时诊断，独立展示结果。

## 5. 触发流程状态机

### 5.1 `TriggerFlowState`

```kotlin
sealed interface TriggerFlowState {
    data object Idle : TriggerFlowState
    data class SelectingOptions(val scenario: AnrScenario) : TriggerFlowState
    data class Confirming(val scenario: AnrScenario, val request: AnrTriggerRequest) : TriggerFlowState
    data class AwaitingPhrase(val scenario: AnrScenario, val request: AnrTriggerRequest) : TriggerFlowState
    data class CountingDown(val scenario: AnrScenario, val request: AnrTriggerRequest, val secondsLeft: Int) : TriggerFlowState
    data class Dispatching(val scenario: AnrScenario, val request: AnrTriggerRequest) : TriggerFlowState
    data class Finished(val result: TriggerResult) : TriggerFlowState
}
```

### 5.2 流程

```text
ScenarioCard 点击触发
  -> ScenarioOptionsSheet 选择参数
  -> ConfirmationDialogs 根据 ConfirmationRequirement 展示普通/长按/输入确认
  -> CountdownTriggerController 3 秒倒计时
  -> AnrScenarioDispatcher.dispatch(request)
  -> DiagnosticsRepository 记录 last result / provider result / memory state
```

### 5.3 倒计时实现约束

- Compose 使用 `LaunchedEffect(state)` + `delay(1000)`。
- 倒计时期间显示“3 / 2 / 1 秒后触发”。
- 倒计时可取消。
- 倒计时结束后在主线程 dispatch；这对 Input / Deadlock / MemoryPressure 是预期行为。

## 6. 高危确认架构

### 6.1 `SafetyPolicy`

```kotlin
object SafetyPolicy {
    fun requirementFor(scenario: AnrScenario, request: AnrTriggerRequest): ConfirmationRequirement
    fun isDangerous(request: AnrTriggerRequest): Boolean
    fun phraseFor(scenario: AnrScenario): String?
}
```

### 6.2 强确认规则

- `TypePhraseConfirm`：用户必须输入完全匹配短语。
- `LongPressConfirm`：按钮按住 1.5 秒后才进入倒计时。
- `DisabledDocumentation`：按钮显示“仅说明”，不调用 dispatch。
- dangerous OOM 即使通过 deep link 也必须被 `SafetyGate` 拒绝，除非 request 明确 `allowDangerousOom=true` 且 UI/adb 走强确认 token。

### 6.3 `ConfirmationToken`

防止 deep link 直接绕过 UI 强确认：

```kotlin
data class ConfirmationToken(
    val scenarioId: String,
    val issuedAtElapsedMs: Long,
    val requirement: ConfirmationRequirement,
)
```

`AnrTriggerRequest` 二期可加：

```kotlin
val confirmationToken: ConfirmationToken? = null
```

`SafetyGate` 检查危险模式：

- UI dispatch 带 token。
- adb deep link 对危险模式默认返回 `Rejected`，提示使用 UI 强确认。
- 普通场景不需要 token。

## 7. 诊断面板架构

### 7.1 `DiagnosticsRepository`

聚合三类状态：

1. 最近触发结果：`TriggerResult`。
2. 最近 ANR exit info：`ExitInfoReader.latestAnrSummary(context)`。
3. 实时资源状态：内存 retained MB、Runtime heap。
4. Provider 查询结果：耗时、是否超过阈值、URI。

```kotlin
data class DiagnosticsSnapshot(
    val lastTriggerText: String? = null,
    val latestExitInfo: String? = null,
    val retainedMemoryMb: Int = 0,
    val heapUsedMb: Int = 0,
    val heapMaxMb: Int = 0,
    val lastProviderResult: ProviderDiagnosticResult? = null,
)
```

首版不引入 ViewModel 依赖；二期仍可保持 Compose state：

```kotlin
class DiagnosticsRepository(private val context: Context) {
    fun snapshot(): DiagnosticsSnapshot
    fun recordTriggerResult(result: TriggerResult)
    fun recordProviderResult(result: ProviderDiagnosticResult)
}
```

### 7.2 UI 组件

- `DiagnosticsPanel`：首页顶部可折叠。
- `ExitInfoCard`：显示最近 ANR exit info，带刷新按钮。
- `MemoryPressureCard`：显示 retained MB / heap used / max，带释放内存按钮。
- `ProviderDiagnosticCard`：显示最近 provider query 耗时和解释。

### 7.3 Provider 诊断结果

`AnrScenarioDispatcher.queryProvider()` 当前返回 `Completed(elapsedMs)`，二期改成：

```kotlin
data class ProviderDiagnosticResult(
    val uri: String,
    val elapsedMs: Long,
    val requestedBlockMs: Long,
    val likelyAnrThresholdExceeded: Boolean,
)
```

`TriggerResult` 二期扩展：

```kotlin
sealed class TriggerResult {
    data class Started(...)
    data class Completed(...)
    data class ProviderCompleted(val result: ProviderDiagnosticResult)
    data class Rejected(...)
    data object NotRunnableFromUi
}
```

## 8. 高级场景参数 UI

### 8.1 `ScenarioOptionsSheet`

根据 `scenario.parameterSpecs` 动态渲染：

- Duration：Slider 或 +/- 按钮。
- Choice：单选 chips。
- IntRange：数字步进器。
- BooleanToggle：switch。

输出：`Map<String, String>` 或直接 `AnrTriggerRequest`。

### 8.2 `ScenarioRequestBuilder`

```kotlin
object ScenarioRequestBuilder {
    fun build(scenario: AnrScenario, values: Map<String, String>): AnrTriggerRequest
}
```

职责：

- 合并 defaultRequest。
- 校验 min/max。
- 对 dangerous 参数自动设置更高 confirmation requirement，而不是直接允许。

### 8.3 具体高级模式

| 场景 | 参数 |
|---|---|
| deadlock | mode: `contention` / `classic`; blockMs |
| memory-pressure | maxMb; chunkMb; blockMs; allowDangerousOom |
| broadcast | foreground; blockMs; mode: `sync` / `goAsyncNoFinish` |
| service | mode: `foreground` / `background`; blockMs |
| fgs-start-timeout | mode: `skipStartForeground` / `delayStartForeground`; blockMs |
| job-service | mode: `onStartJob` / `onStopJob`; blockMs |
| content-provider | blockMs; diagnosticThresholdMs |

## 9. README 与 adb smoke 设计

### 9.1 README 结构

新增章节：

1. 构建与运行。
2. 安全警告与恢复命令。
3. 场景矩阵：id、类型、超时、UI/adb 触发方式。
4. 如何采集 logcat / traces / ApplicationExitInfo。
5. Android 版本差异。
6. 已验证设备记录表。

### 9.2 `scripts/anr-smoke.sh`

目标：只跑短耗时、可恢复场景，不默认跑 70s/200s/3min/经典死锁/OOM。

```bash
#!/usr/bin/env bash
set -euo pipefail
PKG="com.codemx.anrdemo"

adb shell am force-stop "$PKG"
adb shell am start -n "$PKG/.MainActivity"
sleep 2

adb shell am start -a android.intent.action.VIEW -d 'anrdemo://scenario/input-dispatch?blockMs=8000' "$PKG" || true
sleep 10
adb shell am force-stop "$PKG"

adb shell am broadcast --receiver-foreground \
  -a com.codemx.anrdemo.ACTION_BLOCKING_BROADCAST \
  -n "$PKG/.anr.triggers.DemoBroadcastReceiver" \
  --ez foreground true --el blockMs 12000 || true
sleep 15
adb shell am force-stop "$PKG"
```

脚本输出：

- 每个场景开始/结束时间。
- 建议另开 logcat 命令。
- 不判定所有场景一定 ANR，因为设备/OEM 阈值可能不同。

## 10. 二期测试计划

### 10.1 单元测试

新增：

1. `ScenarioRequestBuilderTest`
   - 默认参数合并。
   - duration min/max clamp。
   - dangerous OOM 触发强确认策略。
2. `SafetyPolicyTest`
   - 每个 Dangerous 场景需要 phrase。
   - documentation-only 场景不能 dispatch。
3. `TriggerFlowStateTest`
   - 确认策略到状态机转换。
4. `DiagnosticsRepositoryTest`
   - snapshot 包含 memory retained / heap 信息。

### 10.2 Compose UI 测试

可选但建议：

- 场景列表显示。
- 点击普通场景显示确认弹窗。
- 高危场景显示输入确认。
- 命令弹窗包含 force-stop。

### 10.3 设备手动验证

首批只验证：

- Input dispatch。
- Deadlock contention。
- Memory pressure bounded。
- Broadcast foreground。
- Service foreground。
- FGS start timeout。
- ContentProvider diagnostic。

长耗时场景单独记录，不放 smoke 默认流程。

## 11. 开发任务拆分

### Task 1：Catalog 与安全策略扩展

文件：

- `anr/catalog/ConfirmationRequirement.kt`
- `anr/catalog/ScenarioParameterSpec.kt`
- `anr/catalog/ScenarioRequestBuilder.kt`
- `anr/safety/SafetyPolicy.kt`
- `anr/safety/ConfirmationToken.kt`
- 修改 `AnrScenario.kt`、`AnrCatalog.kt`、`AnrTriggerRequest.kt`、`SafetyGate.kt`

验收：

- 所有场景有 confirmation requirement。
- Dangerous 场景有 phrase 或 disabled reason。
- `ScenarioRequestBuilderTest`、`SafetyPolicyTest` 通过。

### Task 2：触发状态机 + 倒计时

文件：

- `ui/trigger/TriggerFlowState.kt`
- `ui/trigger/CountdownTriggerController.kt`
- `ui/trigger/ConfirmationDialogs.kt`
- 修改 `AnrDemoScreen.kt`

验收：

- 普通场景确认后显示 3/2/1 倒计时。
- 倒计时可取消。
- 倒计时结束才 dispatch。

### Task 3：高级参数 UI

文件：

- `ui/trigger/ScenarioOptionsSheet.kt`
- 修改 `ScenarioCard` 点击流程。

验收：

- deadlock 可选 `contention/classic`。
- memory-pressure 可选 maxMb/chunkMb/blockMs。
- broadcast 可选 sync/goAsyncNoFinish。
- job-service 可选 onStartJob/onStopJob。

### Task 4：诊断面板

文件：

- `anr/diagnostics/DiagnosticsSnapshot.kt`
- `anr/diagnostics/DiagnosticsRepository.kt`
- `anr/diagnostics/ProviderDiagnosticResult.kt`
- `anr/diagnostics/MemoryPressureState.kt`
- `ui/diagnostics/DiagnosticsPanel.kt`
- `ui/diagnostics/ExitInfoCard.kt`
- `ui/diagnostics/MemoryPressureCard.kt`
- `ui/diagnostics/ProviderDiagnosticCard.kt`

验收：

- 首页展示最近 trigger result。
- 可刷新 ApplicationExitInfo。
- 内存 retained/heap 信息可刷新。
- ContentProvider 查询显示耗时结果。

### Task 5：新增高级场景卡片

文件：

- 修改 `AnrCatalog.kt`
- 修改 `DemoBroadcastReceiver.kt` / `DemoJobService.kt` 如有必要

验收：

- `broadcast-async-no-finish` 独立显示。
- `job-service-stop` 独立显示或通过参数选择。
- `deadlock-classic`、`memory-dangerous-oom` 在高级分组中受强确认保护。

### Task 6：README 与 smoke 脚本

文件：

- `README.md`
- `scripts/anr-smoke.sh`

验收：

- README 有运行、恢复、场景矩阵、诊断命令。
- smoke 脚本默认只执行短耗时可恢复场景。

## 12. 推荐执行顺序

1. Task 1：先补 catalog/safety，避免 UI 先行导致危险场景绕过保护。
2. Task 2：实现倒计时和确认状态机。
3. Task 3：实现参数 UI。
4. Task 4：接入诊断面板。
5. Task 5：开放高级场景。
6. Task 6：补 README 和 smoke。
7. 最后运行：

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug --no-daemon
```

## 13. 不在二期默认实现的内容

- App 内直接读取 `/data/anr` traces：普通 app 权限通常不足，继续用 adb 文档化。
- 自动判定所有 ANR 是否成功：设备/OEM/Android 版本差异较大，二期只做 smoke 和人工验证记录。
- 引入 ViewModel / Navigation / 第三方库：当前项目保持无新增三方依赖；如后续 UI 复杂度继续上升再评估。
