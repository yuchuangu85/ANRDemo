# ANRDemo 项目代码架构设计

日期：2026-05-24  
版本：v1.0  
输入文档：`docs/anr-demo-app-plan.md`

## 1. 架构目标

基于已整理的 ANR 类型，项目代码按“资料目录 + 触发执行 + 安全控制 + 诊断展示”分层：

1. **统一管理 ANR 场景元数据**：类型、超时、风险、触发参数、adb 命令、预期日志。
2. **统一触发入口**：UI、deep link、adb、Broadcast、Service、Job、Provider 都落到同一个场景分发模型。
3. **隔离危险代码**：所有会卡死、死锁、内存压力、OOM 的代码集中在 `anr/triggers`，避免污染 UI 和业务层。
4. **先教学再触发**：任何危险场景必须经过说明页 / 确认弹窗 / 倒计时。
5. **可验证**：每个场景都有可观察日志、预期 ANR reason、恢复命令和单元测试元数据校验。

## 2. 推荐包结构

```text
app/src/main/java/com/codemx/anrdemo/
├── MainActivity.kt
├── anr/
│   ├── catalog/
│   │   ├── AnrCatalog.kt
│   │   ├── AnrScenario.kt
│   │   ├── AnrCategory.kt
│   │   ├── AnrRiskLevel.kt
│   │   ├── AnrTriggerKind.kt
│   │   └── AnrDefaults.kt
│   ├── dispatch/
│   │   ├── AnrScenarioDispatcher.kt
│   │   ├── AnrTriggerRequest.kt
│   │   ├── TriggerResult.kt
│   │   └── DeepLinkParser.kt
│   ├── safety/
│   │   ├── SafetyGate.kt
│   │   ├── RuntimeGuards.kt
│   │   ├── MemoryBudget.kt
│   │   └── DeviceCapabilities.kt
│   ├── diagnostics/
│   │   ├── AnrLogTags.kt
│   │   ├── ExitInfoReader.kt
│   │   ├── DiagnosticCommands.kt
│   │   └── ThreadStateFormatter.kt
│   └── triggers/
│       ├── MainThreadTriggers.kt
│       ├── DeadlockTriggers.kt
│       ├── MemoryPressureTriggers.kt
│       ├── DemoBroadcastReceiver.kt
│       ├── BlockingService.kt
│       ├── ForegroundStartTimeoutService.kt
│       ├── DemoJobService.kt
│       ├── BlockingContentProvider.kt
│       ├── ShortForegroundService.kt
│       └── NotificationHelper.kt
├── ui/
│   ├── AnrDemoScreen.kt
│   ├── ScenarioListScreen.kt
│   ├── ScenarioDetailScreen.kt
│   ├── ScenarioCard.kt
│   ├── ConfirmAnrDialog.kt
│   ├── AdbCommandSheet.kt
│   ├── DiagnosticsPanel.kt
│   └── theme/...
└── util/
    ├── TimeFormat.kt
    └── ClipboardHelper.kt
```

测试目录：

```text
app/src/test/java/com/codemx/anrdemo/
├── anr/catalog/AnrCatalogTest.kt
├── anr/dispatch/DeepLinkParserTest.kt
└── anr/safety/MemoryBudgetTest.kt
```

## 3. 核心模块职责

### 3.1 `anr/catalog`：场景元数据层

只保存“这个 ANR 是什么”，不执行危险逻辑。

#### `AnrScenario`

```kotlin
data class AnrScenario(
    val id: String,
    val title: String,
    val category: AnrCategory,
    val riskLevel: AnrRiskLevel,
    val triggerKind: AnrTriggerKind,
    val timeoutDescription: String,
    val recommendedBlockMs: Long?,
    val minApi: Int? = null,
    val maxApi: Int? = null,
    val expectedReason: String,
    val explanation: String,
    val defaultRequest: AnrTriggerRequest,
    val adbCommand: String?,
    val recoveryCommand: String = "adb shell am force-stop com.codemx.anrdemo",
    val enabledByDefault: Boolean = true,
)
```

#### `AnrCategory`

```kotlin
enum class AnrCategory {
    UserPerceived,
    ComponentLifecycle,
    ResourcePressure,
    LongRunningAdvanced,
    DiagnosticOnly,
}
```

#### `AnrTriggerKind`

```kotlin
enum class AnrTriggerKind {
    MainThreadBlock,
    Deadlock,
    MemoryPressure,
    Broadcast,
    Service,
    ForegroundServiceStart,
    JobService,
    ContentProvider,
    ShortForegroundService,
    AdbOnly,
}
```

### 3.2 `anr/dispatch`：触发分发层

统一处理 UI、deep link、adb 进入的请求。

```kotlin
data class AnrTriggerRequest(
    val scenarioId: String,
    val blockMs: Long? = null,
    val mode: String? = null,
    val maxMb: Int? = null,
    val chunkMb: Int? = null,
    val foreground: Boolean? = null,
    val allowDangerousOom: Boolean = false,
)
```

`AnrScenarioDispatcher` 负责：

1. 根据 `scenarioId` 找到 `AnrScenario`。
2. 调用 `SafetyGate` 判断是否允许触发。
3. 按 `triggerKind` 分发到具体 trigger 或 Android 组件。
4. 记录 `Log.d("ANRDemo", ...)`，便于 logcat 观察。

分发示意：

```kotlin
class AnrScenarioDispatcher(
    private val context: Context,
    private val safetyGate: SafetyGate,
) {
    fun dispatch(request: AnrTriggerRequest): TriggerResult {
        val scenario = AnrCatalog.requireScenario(request.scenarioId)
        safetyGate.requireAllowed(scenario, request)

        return when (scenario.triggerKind) {
            AnrTriggerKind.MainThreadBlock -> MainThreadTriggers.blockMainThread(request.blockMs ?: 8_000)
            AnrTriggerKind.Deadlock -> DeadlockTriggers.trigger(request)
            AnrTriggerKind.MemoryPressure -> MemoryPressureTriggers.trigger(request)
            AnrTriggerKind.Broadcast -> BroadcastLauncher.send(context, request)
            AnrTriggerKind.Service -> ServiceLauncher.startBlockingService(context, request)
            AnrTriggerKind.ForegroundServiceStart -> ServiceLauncher.startForegroundTimeoutService(context, request)
            AnrTriggerKind.JobService -> JobLauncher.schedule(context, request)
            AnrTriggerKind.ContentProvider -> ProviderCaller.queryBlockingProvider(context, request)
            AnrTriggerKind.ShortForegroundService -> ServiceLauncher.startShortService(context, request)
            AnrTriggerKind.AdbOnly -> TriggerResult.NotRunnableFromUi
        }
    }
}
```

## 4. 各 ANR 类型的代码映射

| ANR / 根因场景 | `scenarioId` | 触发类型 | 主要文件 | 默认参数 |
|---|---|---|---|---|
| Input dispatch timeout | `input-dispatch` | `MainThreadBlock` | `MainThreadTriggers.kt` | `blockMs=8000` |
| No focused window | `no-focused-window` | `MainThreadBlock` / `AdbOnly` | `MainThreadTriggers.kt` | 可选高级 |
| Deadlock / 锁竞争 | `deadlock` | `Deadlock` | `DeadlockTriggers.kt` | `mode=contention&blockMs=8000` |
| Memory pressure / GC thrash | `memory-pressure` | `MemoryPressure` | `MemoryPressureTriggers.kt` | `maxMb=128&chunkMb=8&blockMs=8000` |
| Broadcast foreground | `broadcast-foreground` | `Broadcast` | `DemoBroadcastReceiver.kt` | `foreground=true&blockMs=12000` |
| Broadcast background | `broadcast-background` | `Broadcast` | `DemoBroadcastReceiver.kt` | `foreground=false&blockMs=70000` |
| Service executing foreground | `service-foreground` | `Service` | `BlockingService.kt` | `blockMs=25000` |
| Service executing background | `service-background` | `Service` | `BlockingService.kt` | `blockMs=210000` |
| FGS 未及时 `startForeground()` | `fgs-start-timeout` | `ForegroundServiceStart` | `ForegroundStartTimeoutService.kt` | `mode=skipStartForeground` |
| JobScheduler 回调超时 | `job-service` | `JobService` | `DemoJobService.kt` | `blockMs=10000` |
| ContentProvider not responding | `content-provider` | `ContentProvider` | `BlockingContentProvider.kt` | `blockMs=8000` |
| FGS `shortService` | `short-service` | `ShortForegroundService` | `ShortForegroundService.kt` | `blockMs=190000` |

## 5. 触发器设计

### 5.1 `MainThreadTriggers`

职责：制造最基础、最稳定的主线程不可响应。

```kotlin
object MainThreadTriggers {
    fun blockMainThread(durationMs: Long): TriggerResult {
        val started = SystemClock.elapsedRealtime()
        Log.d(AnrLogTags.TRIGGER, "Blocking main thread for ${durationMs}ms")
        Thread.sleep(durationMs)
        return TriggerResult.Completed(SystemClock.elapsedRealtime() - started)
    }
}
```

设计约束：

- 只在用户确认后调用。
- 默认 8 秒，超过 input dispatch 5 秒。
- UI 文案提示用户在阻塞期间再次点击或滑动以触发 input dispatch ANR。

### 5.2 `DeadlockTriggers`

职责：演示死锁 / 锁竞争如何导致已有 ANR 类型。

推荐两个模式：

1. **可控锁竞争**：默认模式，后台线程持锁睡眠 8 秒，主线程等待锁，稳定触发 input dispatch。
2. **经典死锁**：高级模式，两个锁互相等待，不自动恢复，需要 force-stop。

```kotlin
object DeadlockTriggers {
    private val lockA = Any()
    private val lockB = Any()

    fun trigger(request: AnrTriggerRequest): TriggerResult {
        return when (request.mode) {
            "classic" -> triggerClassicDeadlock()
            else -> triggerLockContention(request.blockMs ?: 8_000)
        }
    }
}
```

安全策略：

- `classic` 模式默认隐藏或需长按启用。
- 说明页必须标注：**死锁是 ANR 根因，不是独立 Android timeout 类型**。

### 5.3 `MemoryPressureTriggers`

职责：演示“内存泄漏 / GC thrash / 系统高负载”如何诱发 ANR。

设计要点：

- 默认采用有限额分配：例如 64-256MB，按设备可用内存动态收敛。
- 默认不无限分配，避免直接 OOM crash。
- 先构造压力，再触发主线程 6-8 秒阻塞，让 ANR 仍然落到可解释的 Input / Service / Broadcast 类型。

```kotlin
object MemoryPressureTriggers {
    private val retainedChunks = mutableListOf<ByteArray>()

    fun trigger(request: AnrTriggerRequest): TriggerResult {
        val budget = MemoryBudget.resolve(
            requestedMaxMb = request.maxMb ?: 128,
            chunkMb = request.chunkMb ?: 8,
            allowDangerousOom = request.allowDangerousOom,
        )
        leakChunksUntilPressure(budget.maxMb, budget.chunkMb)
        return triggerGcThrashOnMainThread(request.blockMs ?: 8_000)
    }
}
```

UI 必须提供：

- 当前已保留内存大小。
- “释放内存”按钮。
- “危险 OOM 模式”默认关闭。
- 文案说明：**OOM 是 crash，内存压力诱发 ANR 才是本场景目标**。

### 5.4 `DemoBroadcastReceiver`

职责：演示同步 / 异步 BroadcastReceiver 超时。

模式：

- `foreground=true`：发送带 `Intent.FLAG_RECEIVER_FOREGROUND` 的广播，默认阻塞 12 秒。
- `foreground=false`：后台优先级广播，默认阻塞 70 秒。
- `mode=goAsyncNoFinish`：调用 `goAsync()` 后不调用 `finish()`，演示 async receiver timeout。

### 5.5 `BlockingService`

职责：演示 `Service.onCreate()` / `onStartCommand()` / `onBind()` 超时。

模式：

- `mode=onCreate`：在 `onCreate()` 阻塞。
- `mode=onStartCommand`：在 `onStartCommand()` 阻塞。
- 默认 foreground service executing timeout：25 秒。
- background service 200 秒场景仅高级入口触发。

### 5.6 `ForegroundStartTimeoutService`

职责：演示 `startForegroundService()` 后没有及时 `startForeground()`。

模式：

- `skipStartForeground`：完全不调用。
- `delayStartForeground`：延迟 6-8 秒调用。

依赖：

- `NotificationHelper` 创建 channel。
- Manifest 声明 `FOREGROUND_SERVICE`。
- Android 13+ 可提示通知权限，但本场景核心是服务未及时进入 foreground。

### 5.7 `DemoJobService`

职责：演示 JobScheduler 回调未及时返回。

模式：

- `onStartJob`：阻塞 10 秒。
- `onStopJob`：阻塞 10 秒。
- Android 14+ 明确提示显式 ANR 行为；较低版本可能静默或表现不同。

### 5.8 `BlockingContentProvider`

职责：演示远程 ContentProvider 查询阻塞。

设计：

- Provider 放到独立进程 `:provider`。
- `query()` 阻塞 6-10 秒。
- 调用方记录 query 开始/结束时间。
- 如果设备不弹系统 ANR，UI 仍作为“ContentProvider not responding 诊断型场景”展示。

### 5.9 `ShortForegroundService`

职责：Android 14+ `shortService` 超时。

策略：

- 高级分组。
- 默认不自动触发，需二次确认。
- 运行超过约 3 分钟且不 `stopSelf()`。

## 6. UI 架构

### 6.1 页面结构

```text
MainActivity
└── ANRDemoTheme
    └── AnrDemoScreen
        ├── ScenarioListScreen
        │   └── ScenarioCard
        ├── ScenarioDetailScreen
        ├── ConfirmAnrDialog
        ├── AdbCommandSheet
        └── DiagnosticsPanel
```

### 6.2 UI 状态

无需引入 ViewModel 依赖；首版可用 Compose state：

```kotlin
data class AnrDemoUiState(
    val selectedCategory: AnrCategory? = null,
    val selectedScenario: AnrScenario? = null,
    val pendingRequest: AnrTriggerRequest? = null,
    val showConfirmDialog: Boolean = false,
    val showAdbCommand: Boolean = false,
    val lastTriggerResult: TriggerResult? = null,
)
```

### 6.3 安全交互

每个危险按钮流程：

```text
点击触发 -> 场景详情 -> 风险说明 -> 二次确认 -> 3 秒倒计时 -> dispatch(request)
```

高风险场景额外要求：

- background service 200s、classic deadlock、shortService 3min、dangerous OOM 必须长按或输入确认文案。
- 所有场景展示恢复命令：`adb shell am force-stop com.codemx.anrdemo`。

## 7. Deep Link / adb 触发架构

Manifest 为 `MainActivity` 增加 deep link：

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="anrdemo" android:host="scenario" />
</intent-filter>
```

Deep link 格式：

```text
anrdemo://scenario/{scenarioId}?key=value
```

示例：

```bash
adb shell am start \
  -a android.intent.action.VIEW \
  -d 'anrdemo://scenario/deadlock?mode=contention&blockMs=8000' \
  com.codemx.anrdemo
```

`DeepLinkParser` 输出 `AnrTriggerRequest`，再交给 `AnrScenarioDispatcher`。

## 8. AndroidManifest 组件规划

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<application ...>
    <activity android:name=".MainActivity" android:exported="true">...</activity>

    <receiver
        android:name=".anr.triggers.DemoBroadcastReceiver"
        android:exported="true" />

    <service
        android:name=".anr.triggers.BlockingService"
        android:exported="false" />

    <service
        android:name=".anr.triggers.ForegroundStartTimeoutService"
        android:exported="false" />

    <service
        android:name=".anr.triggers.DemoJobService"
        android:permission="android.permission.BIND_JOB_SERVICE"
        android:exported="true" />

    <provider
        android:name=".anr.triggers.BlockingContentProvider"
        android:authorities="com.codemx.anrdemo.blocking-provider"
        android:process=":provider"
        android:exported="false" />

    <service
        android:name=".anr.triggers.ShortForegroundService"
        android:foregroundServiceType="shortService"
        android:exported="false" />
</application>
```

注意：Android 14+ 前台服务类型权限按具体类型追加；`shortService` 不需要类型专属权限，但仍需要 `FOREGROUND_SERVICE`。

## 9. 安全控制设计

### 9.1 `SafetyGate`

职责：触发前统一校验。

```kotlin
class SafetyGate(
    private val deviceCapabilities: DeviceCapabilities,
    private val memoryBudget: MemoryBudget,
) {
    fun requireAllowed(scenario: AnrScenario, request: AnrTriggerRequest) {
        checkApiRange(scenario)
        checkDangerousMode(scenario, request)
        checkMemoryBudget(request)
        checkLongRunningConfirmation(scenario)
    }
}
```

### 9.2 `MemoryBudget`

内存场景必须限制最大分配：

- 默认最大 128MB。
- 低内存设备自动降到 32-64MB。
- `allowDangerousOom=false` 时禁止无限循环分配。
- 提供 `clearRetainedChunks()` 释放演示内存。

## 10. 诊断与日志设计

统一 tag：

```kotlin
object AnrLogTags {
    const val APP = "ANRDemo"
    const val TRIGGER = "ANRDemo.Trigger"
    const val SAFETY = "ANRDemo.Safety"
    const val DIAGNOSTICS = "ANRDemo.Diagnostics"
}
```

每个触发器至少打印：

- scenarioId
- triggerKind
- blockMs
- thread name
- start time / expected timeout
- recovery command

App 内展示：

- 当前场景预期 reason。
- 推荐 logcat 命令。
- Android 11+ 最近 `ApplicationExitInfo`。
- 死锁场景展示 main thread 与 lock owner 的诊断提示。
- 内存压力场景展示 retained MB、Runtime max/free/total memory。

## 11. 测试策略

### 11.1 单元测试

`AnrCatalogTest`：

- 每个 `scenarioId` 唯一。
- 每个场景有 `timeoutDescription`、`expectedReason`、`riskLevel`。
- 高风险场景 `enabledByDefault=false` 或需要额外确认。
- Deadlock 场景说明必须包含“根因”。
- Memory pressure 场景必须默认 `allowDangerousOom=false`。

`DeepLinkParserTest`：

- 能解析 `deadlock?mode=contention&blockMs=8000`。
- 能解析 `memory-pressure?maxMb=128&chunkMb=8&blockMs=8000`。
- 非法数值回退到默认值。

`MemoryBudgetTest`：

- 低内存设备预算收敛。
- 不允许 dangerous OOM 时拒绝无限分配。

### 11.2 手动 / 设备验证

首版最小验证矩阵：

| 场景 | 期望验证 |
|---|---|
| Input dispatch | logcat 出现 `Input dispatching timed out` |
| Deadlock contention | traces 中 main thread blocked/waiting；reason 为 input timeout |
| Memory pressure | logcat 可见 GC / memory pressure，随后 input timeout 或组件 timeout |
| Broadcast foreground | logcat 出现 `Broadcast of Intent` |
| Service foreground | logcat 出现 executing service timeout |
| FGS start timeout | 出现 did not call `startForeground()` 相关异常/ANR |
| JobService | Android 14+ 出现 JobScheduler ANR；低版本记录差异 |
| ContentProvider | 记录 provider query 阻塞耗时；视设备观察 ANR |

## 12. 实施顺序建议

1. 建 `anr/catalog`，先让 App 展示完整类型表。
2. 建 `dispatch` + `safety`，统一触发入口。
3. 实现 `MainThreadTriggers`、`DeadlockTriggers`、`MemoryPressureTriggers`，覆盖用户要求新增场景。
4. 实现 Broadcast / Service / FGS / Job / Provider 组件。
5. 加 deep link 和 adb 命令展示。
6. 补 README 与 diagnostics panel。
7. 补单元测试与 `assembleDebug` 验证。

## 13. 首版落地边界

首版建议优先实现：

- `input-dispatch`
- `deadlock`
- `memory-pressure`
- `broadcast-foreground`
- `service-foreground`
- `fgs-start-timeout`
- `job-service`
- `content-provider`

暂缓或默认折叠：

- `broadcast-background` 70 秒
- `service-background` 210 秒
- `short-service` 约 3 分钟
- dangerous OOM 无限分配模式
