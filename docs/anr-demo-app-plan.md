# ANR 类型、超时时间与 ANR Demo App 开发计划

日期：2026-05-24  
版本：v1.0  

## 1. 需求摘要

目标是在当前 `ANRDemo` Android 工程中开发一个可教学、可复现、可对比的 ANR Demo App：

1. 在 App 首页列出 Android 常见 ANR 类型、默认超时时间、触发组件、典型原因、验证方式。
2. 每种 ANR 类型提供一个“安全说明 + 倒计时 + 手动触发”的独立场景。
3. 支持从 UI 触发，也给出 `adb` 命令触发路径，便于采集 logcat、traces、ApplicationExitInfo。
4. 保持工程简单：当前项目已经是 Kotlin + Compose App，不新增三方依赖，优先使用 Android Framework API。

## 2. 当前工程事实

- App 模块已启用 Compose：`app/build.gradle.kts:37-39`。
- 包名 / namespace：`com.codemx.anrdemo`，见 `app/build.gradle.kts:7`、`app/build.gradle.kts:15`。
- `minSdk=24`、`targetSdk=36`，见 `app/build.gradle.kts:16-17`。
- 当前只有一个入口 Activity：`app/src/main/AndroidManifest.xml:14-24`。
- 当前 UI 还是默认 `Greeting`：`app/src/main/java/com/codemx/anrdemo/MainActivity.kt:20-29`、`app/src/main/java/com/codemx/anrdemo/MainActivity.kt:33-39`。

## 3. ANR 类型与默认超时时间整理

> 说明：下表以 Android 官方文档面向 App 开发者的口径为主；官方也说明 AOSP / Pixel 默认时间可能被 OEM 调整。为确保 Demo 可复现，触发耗时应略高于阈值，例如 5 秒场景用 6-8 秒，10 秒场景用 12-15 秒。

| ANR 类型 | 默认超时 / 时间窗口 | 触发对象 | 典型 ANR reason / 现象 | Demo 可行性 |
|---|---:|---|---|---|
| Input dispatch timeout | 5 秒 | 前台窗口主线程未响应输入事件 | `Input dispatching timed out` | 必做；最稳定 |
| Input dispatch - no focused window | 5 秒 | 没有可聚焦窗口或首帧过慢 | `Input dispatching timed out ... no focused window` | 可选；设备/窗口状态相关，复现不如主线程阻塞稳定 |
| Deadlock / 锁竞争导致主线程阻塞 | 不是独立系统阈值；取决于被阻塞组件对应的 ANR 类型。作为前台交互 Demo 时按 Input dispatch 5 秒触发 | 主线程等待后台线程持有的锁，或主线程与后台线程互相等待 | `Input dispatching timed out`；traces 中 main thread `Blocked/Waiting`，可看到锁持有线程 | 必做；教学价值高且复现稳定 |
| Memory pressure / 内存泄漏、频繁 GC、系统高负载诱发 ANR | 不是独立系统阈值；当内存压力导致主线程、Binder、Receiver、Service 或 Job 回调超过对应阈值时触发。OOM 本身通常是 crash，不应等同于 ANR | 持续分配并保留对象造成泄漏/GC thrash，同时在主线程或组件回调执行被拖慢 | 可能表现为 `Input dispatching timed out`、`executing service`、`Broadcast of Intent`；logcat 可见频繁 GC、内存压力或 OOM 前兆 | 高级必做；需限额保护，避免直接把 App 打到 OOM crash |
| Broadcast receiver timeout：foreground priority intent | Android 13 及以下：10 秒；Android 14+：10-20 秒，取决于进程是否 CPU-starved | `BroadcastReceiver.onReceive()` 或 `goAsync()` 后 `PendingResult.finish()` | `Broadcast of Intent ...`，flag 含 `0x10000000` | 必做；显式广播稳定 |
| Broadcast receiver timeout：background priority intent | Android 13 及以下：60 秒；Android 14+：60-120 秒，取决于进程是否 CPU-starved | 同上，但 Intent 未设置 `FLAG_RECEIVER_FOREGROUND` | `Broadcast of Intent ...`，flag 不含 `0x10000000` | 必做但耗时长；默认用 70 秒 |
| Executing service timeout：foreground service / 前台进程中的 service 执行 | 20 秒 | `Service.onCreate()` / `onStartCommand()` / `onBind()` 未及时返回 | `executing service ...` | 必做；用 25 秒阻塞 lifecycle |
| Executing service timeout：background service | 200 秒 | 后台 service lifecycle 未及时返回 | `executing service ...` | 可选；耗时过长，放入“高级/长耗时”分组 |
| `startForegroundService()` 后未调用 `startForeground()` | 官方开发者文档口径：5 秒 | 前台服务启动后未及时展示 foreground notification | `Context.startForegroundService() did not then call Service.startForeground()` / `ForegroundServiceDidNotStartInTimeException` | 必做；用“跳过 startForeground”复现 |
| JobScheduler interaction timeout | `onStartJob()` / `onStopJob()`：几秒；AOSP 常见操作响应超时为 8 秒；user-initiated job 通知要求常见为 10 秒 | `JobService` 主线程回调未返回，或用户发起 job 未及时 `setNotification()` | JobScheduler 相关 ANR；target Android 14+ 时显式上报 | 必做基础版；user-initiated job 做可选高级版 |
| Content provider not responding | 官方不统一给固定公开阈值；远程 provider 查询、provider 冷启动和 Binder 线程/主线程阻塞都计入；可用客户端检测阈值做 Demo（例如 5 秒） | 远程 `ContentProvider` 查询或发布过程过慢 | `Content provider not responding` | 必做教学版；provider 放独立进程并用客户端检测阈值 |
| Foreground service type `shortService` timeout | 约 3 分钟；超时后仍不停止会 ANR | Android 14+ `FOREGROUND_SERVICE_TYPE_SHORT_SERVICE` | ANR message 提到 `FOREGROUND_SERVICE_TYPE_SHORT_SERVICE` | 可选高级版；耗时较长但有教学价值 |
| Android 15+ FGS `dataSync` / `mediaProcessing` 总时长限制 | 24 小时窗口内累计 6 小时；超时后需几秒内 stopSelf，否则 RemoteServiceException | 特定类型前台服务 | 更偏“前台服务超时失败”，不是本 Demo 的核心 ANR 类型 | 不纳入首版；文档说明即可 |

## 4. Demo App 信息架构

### 4.1 首页

`MainActivity` 改为 Compose 场景列表：

- 顶部：ANRDemo 标题 + 警告文案“会故意卡死 App，仅在测试机运行”。
- 分组：
  - 用户可感知 ANR：Input dispatch / no focused window / Deadlock。
  - 组件生命周期 ANR：Broadcast / Service / FGS start / JobService / ContentProvider。
  - 资源压力诱发 ANR：内存泄漏、GC thrash、CPU/系统负载升高。
  - 高级长耗时：background broadcast 60s、background service 200s、shortService 3min。
- 每个卡片显示：类型、超时阈值、最低 Android 版本/限制、触发按钮、查看 adb 命令按钮。

### 4.2 场景详情页 / 弹窗

每个场景统一流程：

1. 显示“将阻塞多少秒、会看到什么、如何恢复”。
2. 点击触发前二次确认；倒计时 3 秒。
3. 执行触发动作。
4. 页面显示对应 logcat 过滤命令和预期 ANR subject。

## 5. 代码设计

### 5.1 数据模型

新增：`app/src/main/java/com/codemx/anrdemo/anr/AnrScenario.kt`

- `id: String`
- `title: String`
- `category: AnrCategory`
- `timeoutDescription: String`
- `recommendedBlockMs: Long`
- `minApi: Int?`
- `riskLevel: Low/Medium/High`
- `triggerKind: UiAction/Broadcast/Service/Job/Provider/AdbOnly`
- `adbCommand: String?`
- `expectedReason: String`

新增：`app/src/main/java/com/codemx/anrdemo/anr/AnrCatalog.kt`

- 集中维护上表所有类型。
- 单元测试校验每种类型都有 timeout、风险提示、验证命令。

### 5.2 UI 层

修改：`app/src/main/java/com/codemx/anrdemo/MainActivity.kt`

- 删除默认 Greeting。
- 渲染 `AnrCatalog.scenarios`。
- 提供 `ScenarioCard`、`ConfirmAnrDialog`、`AdbCommandSheet`。

新增：`app/src/main/java/com/codemx/anrdemo/ui/AnrDemoScreen.kt`

- Compose 列表、分类 Tab、确认弹窗。

### 5.3 触发实现

新增包：`app/src/main/java/com/codemx/anrdemo/anr/triggers/`

1. `MainThreadTriggers.kt`
   - `blockMainThread(durationMs)`：`Thread.sleep(durationMs)` 或 CPU busy loop。
   - Input dispatch demo：按钮点击后主线程阻塞 6-8 秒，用户再次触摸触发 input dispatch ANR。
   - Deadlock demo：主线程请求 `lockA`，后台线程持有 `lockA` 后等待主线程持有的 `lockB`；也提供更可控的单锁版本（后台线程持锁睡眠 8 秒，主线程等待锁）用于稳定触发 input dispatch ANR。

2. `DeadlockTriggers.kt`
   - `triggerClassicDeadlock()`：构造两个锁、两个线程的互相等待，主线程进入 `BLOCKED/WAITING`。
   - `triggerLockContention(durationMs)`：后台线程持锁执行长任务，主线程尝试进入同步块，便于稳定复现 5 秒 input timeout。
   - UI 文案明确：死锁不是新的系统超时类型，而是导致已有 ANR 类型（最常见 Input dispatch）的根因。

3. `MemoryPressureTriggers.kt`
   - `leakChunksUntilPressure(maxMb, chunkMb)`：逐步分配并保留 ByteArray / Bitmap-like 对象，模拟内存泄漏，但设置上限，避免默认直接 OOM。
   - `triggerGcThrashOnMainThread(durationMs)`：在主线程短周期分配临时对象并主动制造 GC 压力，用 6-8 秒窗口触发 input dispatch ANR。
   - `triggerSystemLoadThenBlock(component)`：先制造内存压力和后台 CPU 压力，再触发 Input / Broadcast / Service 场景，用来展示“系统负载高使原本临界耗时超过阈值”。
   - 单独提供“危险 OOM 演示”开关：默认关闭；若开启，说明预期更可能是 `OutOfMemoryError` crash，而不是 ANR。

4. `DemoBroadcastReceiver.kt`
   - 同步 receiver：`onReceive()` 内阻塞 12 秒 / 70 秒。
   - 异步 receiver：`goAsync()` 后后台线程延迟 `finish()` 或故意不调用 `finish()`。
   - Intent action：`com.codemx.anrdemo.ACTION_BLOCKING_BROADCAST`。

5. `BlockingService.kt`
   - `onCreate()` 或 `onStartCommand()` 内阻塞 25 秒。
   - 用于 executing service foreground timeout。
   - 背景 200 秒版本放高级分组，默认按钮可隐藏或需长按。

6. `ForegroundStartTimeoutService.kt`
   - 通过 `ContextCompat.startForegroundService()` / `startForegroundService()` 启动。
   - 模式 A：完全不调用 `startForeground()`。
   - 模式 B：延迟 6-8 秒再调用。
   - 需要 notification channel 和 `FOREGROUND_SERVICE` 权限；Android 13+ 说明通知权限对可见性的影响。

7. `DemoJobService.kt`
   - `onStartJob()` 主线程阻塞 10 秒，覆盖 JobScheduler 回调 ANR。
   - `onStopJob()` 也提供阻塞模式。
   - user-initiated job / `setNotification()` 场景作为 Android 14+ 高级项。

8. `BlockingContentProvider.kt`
   - 在 manifest 中配置 `android:process=":provider"`，模拟远程 provider。
   - `query()` 阻塞 6-10 秒。
   - 调用侧使用 `ContentResolver` 或 `ContentProviderClient`，并记录开始/结束时间；如果平台不直接弹 ANR，仍作为 provider not responding 教学场景展示 traces/logcat。

9. `ShortForegroundService.kt`（高级）
   - Android 14+，声明 `foregroundServiceType="shortService"`。
   - 运行超过约 3 分钟并不 `stopSelf()`，观察 ANR。

### 5.4 Manifest 规划

修改：`app/src/main/AndroidManifest.xml`

- 增加权限：
  - `android.permission.FOREGROUND_SERVICE`
  - Android 13+ 可选：`android.permission.POST_NOTIFICATIONS`
  - Android 14+ 按需：`android.permission.FOREGROUND_SERVICE_SPECIAL_USE` 不建议首版使用；`shortService` 通常不需要独立权限但需要类型声明。
- 增加组件：
  - `.anr.triggers.DemoBroadcastReceiver`
  - `.anr.triggers.BlockingService`
  - `.anr.triggers.ForegroundStartTimeoutService`
  - `.anr.triggers.DemoJobService`，权限 `android.permission.BIND_JOB_SERVICE`
  - `.anr.triggers.BlockingContentProvider`，独立进程 `:provider`
  - `.anr.triggers.ShortForegroundService`，Android 14+ 高级场景
  - Activity deep link intent-filter：`anrdemo://scenario/{id}`，用于 adb 直接触发 Deadlock / Memory pressure 等 UI-only 场景

## 6. `adb` 验证命令规划

每个卡片显示对应命令，示例：

```bash
# 观察 ANR / ActivityManager / system_server 日志
adb logcat -v time ActivityManager:E AndroidRuntime:E ANRDemo:D *:S

# 触发 foreground priority broadcast，预期 10-20s 阈值
adb shell am broadcast \
  -a com.codemx.anrdemo.ACTION_BLOCKING_BROADCAST \
  -n com.codemx.anrdemo/.anr.triggers.DemoBroadcastReceiver \
  --ez foreground true \
  --ei blockMs 12000

# 触发死锁 / 锁竞争场景（由 Activity 接收 deep link 后执行）
adb shell am start \
  -a android.intent.action.VIEW \
  -d 'anrdemo://scenario/deadlock?mode=contention&blockMs=8000' \
  com.codemx.anrdemo

# 触发内存压力 + GC thrash 场景（默认有限额，避免直接 OOM crash）
adb shell am start \
  -a android.intent.action.VIEW \
  -d 'anrdemo://scenario/memory-pressure?maxMb=128&chunkMb=8&blockMs=8000' \
  com.codemx.anrdemo

# 恢复卡死/死锁后的 App
adb shell am force-stop com.codemx.anrdemo

# 查看最近退出原因，Android 11+
adb shell dumpsys activity exit-info com.codemx.anrdemo

# 拉取 traces（路径因版本/权限而异）
adb shell ls /data/anr
```

## 7. 分阶段实施步骤

### Phase 1：资料表 + UI 骨架

- 新增 `AnrScenario` / `AnrCatalog`。
- 替换默认 `Greeting` 为场景列表。
- 所有卡片先只展示说明和 adb 命令，不触发 ANR。
- 验证：`./gradlew :app:assembleDebug`。

### Phase 2：稳定可复现的核心场景

实现并验证：

1. Input dispatch 5s：主线程阻塞 8s。
2. Deadlock / lock contention：主线程等待锁 8s 或进入经典死锁，触发 input dispatch ANR。
3. Broadcast foreground 10-20s：阻塞 12-15s。
4. Executing service foreground 20s：阻塞 25s。
5. FGS start 5s：不调用或延迟 `startForeground()`。
6. JobService：`onStartJob()` 阻塞 10s。

验证：每个场景至少采集一次 logcat reason，并记录到 README。

### Phase 3：长耗时 / 高级场景

实现并默认折叠：

1. Broadcast background 60-120s：阻塞 70s。
2. Background service 200s：阻塞 210s；仅 adb 或长按触发。
3. ContentProvider 独立进程查询阻塞。
4. Memory pressure / leak-induced ANR：逐步泄漏 64-256MB（按设备内存动态限制）并制造 GC thrash，再触发 6-8s input timeout；默认不直接 OOM。
5. Android 14+ shortService 约 3 分钟 timeout。

### Phase 4：教学与诊断能力

- README 增加“类型-时间-触发-日志”矩阵。
- App 内增加“如何分析 ANR”：main thread、receiver thread、binder thread、JobService main thread、锁持有线程、GC / memory pressure 线索。
- Android 11+ 使用 `ApplicationExitInfo` 展示最近 ANR 摘要。
- 可选：一键复制 logcat 命令。

## 8. 可测试验收标准

1. App 首页展示至少 10 个 ANR / timeout / ANR 根因场景，并包含超时时间、触发方式、预期 reason。
2. 核心 6 个场景可通过 UI 或 adb 触发，且 logcat 能看到对应 ANR / timeout 关键字。
3. 所有危险场景触发前都有确认弹窗和建议使用测试机提示。
4. `AnrCatalog` 单元测试覆盖：
   - 每个场景有非空 timeout 文案。
   - 每个场景有 riskLevel。
   - 每个 adb 可触发场景有 command。
5. 构建通过：`./gradlew :app:assembleDebug`。
6. 不新增三方依赖。
7. Deadlock 场景的说明页必须展示“ANR 类型 = Input dispatch / 对应组件 timeout；根因 = deadlock/lock contention”。
8. Memory pressure 场景默认有内存分配上限和停止按钮；危险 OOM 模式必须默认关闭，并提示 OOM 更可能产生 crash 而不是 ANR。

## 9. 风险与缓解

| 风险 | 影响 | 缓解 |
|---|---|---|
| OEM 修改 ANR 阈值 | 某些设备复现时间不同 | UI 文案说明“默认 AOSP/Pixel，OEM 可变”；触发耗时高于阈值 |
| Android 版本行为差异 | Android 14+ broadcast / JobScheduler 表现不同 | Catalog 标明版本差异；高级场景按 API gate 控制 |
| 触发 ANR 会影响开发机体验 | App 卡死、需要强停 | 二次确认；提供 `adb shell am force-stop com.codemx.anrdemo` |
| 背景 service 受 Android 8+ 限制 | 200s 场景不稳定 | 放高级 adb 场景；从前台 Activity 显式触发并说明限制 |
| ContentProvider ANR 不一定弹系统 ANR 对话框 | 教学效果不稳定 | 独立进程 + 客户端检测阈值 + traces/logcat 说明，作为诊断型场景 |
| 死锁场景无法自动恢复 | App 长时间不可用 | 提供可控锁竞争版本作为默认；经典死锁需二次确认，并展示 force-stop 命令 |
| 内存泄漏演示直接 OOM crash | 偏离 ANR 教学目标、影响设备 | 默认使用上限分配 + GC thrash，不默认无限分配；危险 OOM 单独开关 |
| 系统高负载导致复现不稳定 | 不同设备表现差异大 | 把它标为“诱发型根因”而非独立 ANR 类型；通过先造压力再触发 Input/Broadcast/Service 阈值验证 |

## 10. 推荐首版范围

首版只做“高成功率 + 教学价值高”的场景：

- Input dispatch 5s
- Deadlock / lock contention 导致 Input dispatch 5s
- Memory pressure / GC thrash 诱发 Input dispatch 5s（带上限保护）
- Broadcast foreground 10-20s
- Broadcast background 60-120s（高级折叠）
- Executing service foreground 20s
- FGS startForeground 5s
- JobService callback 约 8-10s
- ContentProvider 独立进程阻塞教学版

暂缓：background service 200s、shortService 3min、Android 15 FGS 6h，因为耗时长或更偏政策/服务超时失败。

## 11. 参考来源

- Android Developers：Diagnose and fix ANRs — https://developer.android.com/topic/performance/anrs/diagnose-and-fix-anrs
- Android Developers：Find the unresponsive thread — https://developer.android.com/topic/performance/anrs/find-unresponsive-thread
- Android Developers：Android vitals ANRs — https://developer.android.com/topic/performance/vitals/anr
- Android Developers：Foreground service timeouts — https://developer.android.com/develop/background-work/services/fgs/timeout
- Android Developers：Foreground service types / shortService — https://developer.android.com/develop/background-work/services/fgs/service-types
- AOSP：JobServiceContext timeout constants — https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/apex/jobscheduler/service/java/com/android/server/job/JobServiceContext.java
