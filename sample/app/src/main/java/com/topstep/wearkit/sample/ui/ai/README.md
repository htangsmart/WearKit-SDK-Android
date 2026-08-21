# SpeechAi 接入文档（对话 / 录音 / 翻译 / 对话翻译）

本文基于 Sample：`SpeechAiActivity`、`SpeechAiManager` 及对应 Handler，说明如何用 `WKSpeechAiAbility` 接入 **对话（CHAT）**、**录音（RECORD / CALL_RECORD）**、**翻译（TRANSLATE）**、**对话翻译（CHAT_TRANSLATE）**。

> WearKit 只负责设备侧会话、音频与文本/播放控制通道。  
> ASR / LLM / 翻译引擎由 App 自行接入（Sample 使用 AiKit 仅作演示）。

---

## 1. 能力总览

| 功能 | Scene | 入口 | Sample |
|------|-------|------|--------|
| 对话 | `CHAT(0)` | 设备或 App 开 Session | `ChatActivity` / `ChatHandler` |
| 录音 | `RECORD(1)` / `CALL_RECORD(2)` | 设备或 App 开 Session | `RecordActivity` / `RecordHandler` |
| 翻译 | `TRANSLATE(3)` | 设备或 App 开 Session | `TranslateActivity` / `TranslateHandler` |
| 对话翻译 | `CHAT_TRANSLATE_SELF(7)` / `CHAT_TRANSLATE_PEER(8)` | 先 `startChatTranslate(mode)`，再开 Session | `ChatTranslateActivity` / `ChatTranslateHandler` |

公共入口：

```kotlin
val speechAi = wearKit.speechAiAbility
```

---

## 2. 接入准备（必做）

### 2.1 能力判断

```kotlin
if (!speechAi.isSupport()) {
    // 设备不支持 SpeechAi，无需初始化 AI
    return
}
```

再按场景细分：

```kotlin
// 对话
speechAi.session.isSupportDeviceScene(WKSpeechSession.Scene.CHAT)
speechAi.session.isSupportAppScene(WKSpeechSession.Scene.CHAT)

// 录音（普通录音 / 通话录音）
speechAi.session.isSupportDeviceScene(WKSpeechSession.Scene.RECORD)
speechAi.session.isSupportAppScene(WKSpeechSession.Scene.RECORD)
speechAi.session.isSupportDeviceScene(WKSpeechSession.Scene.CALL_RECORD)

// 翻译
speechAi.session.isSupportDeviceScene(WKSpeechSession.Scene.TRANSLATE)
speechAi.session.isSupportAppScene(WKSpeechSession.Scene.TRANSLATE)

// 对话翻译（自身 / 对方任一即可）
speechAi.session.isSupportDeviceScene(WKSpeechSession.Scene.CHAT_TRANSLATE_SELF)
    || speechAi.session.isSupportAppScene(WKSpeechSession.Scene.CHAT_TRANSLATE_SELF)
    || speechAi.session.isSupportDeviceScene(WKSpeechSession.Scene.CHAT_TRANSLATE_PEER)
    || speechAi.session.isSupportAppScene(WKSpeechSession.Scene.CHAT_TRANSLATE_PEER)
```

### 2.2 全程订阅设备 Session（最重要）

设备主动开流只会在有观察者时投递。Sample 在进程级 `SpeechAiManager.init()` 里订阅：

```kotlin
speechAi.session.observeDeviceSession().subscribe { session ->
    // 立刻订阅 session.audio()，并按 scene 挂业务 Handler
}
```

约束：

1. **必须长期订阅** `observeDeviceSession()`（仅订 `observeMessage()` 不够）。
2. 同一时刻最多 **1 个**活跃 Session。
3. 收到 Session 后尽快订 `audio()`（App 约 3s / 设备约 5s 未订会自动 release）。
4. `audio()` **只能订阅一次**；dispose ≈ `release(NONE)`。

### 2.3 音频来源

| Source | 含义 | 格式 |
|--------|------|------|
| `PHONE_MIC` | 手机麦克风 | PCM 16k / mono / 16bit |
| `DEVICE_SCO` | 蓝牙 SCO | PCM |
| `DEVICE_CMD` | 设备 BLE Opus | Opus |

App 创建时可传 `source`；传 `null` 时由 SDK 按设备能力选择（通常优先 SCO）。

### 2.4 权限

使用 `PHONE_MIC` / SCO 时申请录音权限（Sample：`PermissionHelper.requestRecordAudio`）。

---

## 3. 通用 Session 生命周期

```text
设备发起：observeDeviceSession → audio() → 业务 ASR/LLM → 回传文本/TTS
App 发起：createAppSession → audio() → 同上

结束音频：audio onComplete / onError / release()
离开场景：优先 observeMessage → SCENE_EXIT
          （老设备可能没有 EXIT，需用 audio 结束 + UI 生命周期兜底）
```

```kotlin
// App 发起
val session = speechAi.session.createAppSession(scene, source)
// null：不支持 / 已有活跃 Session / DEVICE_CMD 但未连接

session?.audio()?.subscribe(
    { bytes -> /* 送 ASR */ },
    { err -> /* Exception：低电、来电、断连等 */ },
    { /* 正常结束 */ },
)
```

文本一律发 **累计全文快照**，不是增量：

```text
"今天" → "今天是" → "今天是星期几？" (isComplete=true)
```

---

## 4. 对话（CHAT）

### 4.1 流程

```text
Session(CHAT) + audio()
  → App ASR / LLM
  →（可选）chat.sendTextQuestion / sendTextAnswer
  →（可选）player 把 TTS PCM 播到设备
  → SCENE_EXIT 或 audio 结束 → 离场
```

Sample：

* UI：`ChatActivity`
* 业务：`ChatHandler`
* 入口：`SpeechAiActivity` → 对话

### 4.2 设备发起

保持 `observeDeviceSession()` 订阅即可。`scene == CHAT` 时启动对话业务。

### 4.3 App 发起

```kotlin
if (!speechAi.session.isSupportAppScene(WKSpeechSession.Scene.CHAT)) return

val session = speechAi.session.createAppSession(
    WKSpeechSession.Scene.CHAT,
    WKSpeechSession.Source.PHONE_MIC, // 或 DEVICE_SCO / null
)
```

### 4.4 回传文本

```kotlin
if (speechAi.chat.isSupportText()) {
    speechAi.chat.sendTextQuestion(asrFullText, isComplete).subscribe()
    speechAi.chat.sendTextAnswer(llmFullText, isComplete).subscribe()
}
```

注意：

* Chat **不需要**等 `ASK_GENERATE_ANSWER`（那是 Ask 场景）。
* `isSupportText() == false` 时只做音频对话，不要发文本。

### 4.5 TTS 到设备（可选）

```kotlin
if (speechAi.player.isSupport(WKSpeechSession.Scene.CHAT)) {
    speechAi.player.start(sampleRate = 16000, channels = 1)
    speechAi.player.write(pcm, isFinal = false) // 阻塞写
    speechAi.player.write(lastPcm, isFinal = true)
    speechAi.player.stop()
}
```

Sample 通过 `MyAudioPlayer` 按 Source 路由到设备 Opus / SCO / 手机外放。

### 4.6 离场

Chat 多为持续流。Sample 在 `audio` 结束时 `release()`；若之后仍收到 `SCENE_EXIT`，幂等处理即可。

---

## 5. 录音（RECORD / CALL_RECORD）

### 5.1 流程

```text
（设备可能先设置录音语言）
Session(RECORD 或 CALL_RECORD) + audio()
  → record.getLang() 取 ASR 语言（可 null，再兜底）
  → App 侧 ASR（可本地展示 / 落盘；WearKit 无录音文本回传 API）
  → audio 结束或 SCENE_EXIT → 离场
```

| Scene | 含义 |
|-------|------|
| `RECORD` | 普通录音 |
| `CALL_RECORD` | 通话录音（通常仅设备发起） |

两者共用同一套业务逻辑。Sample 用同一个 `RecordHandler` 处理。

Sample：

* UI：`RecordActivity`
* 业务：`RecordHandler`
* 入口：`SpeechAiActivity` → 录音

### 5.2 语言

```kotlin
// 设备发起：优先用设备侧语言
val lang = speechAi.record.getLang()
// null：设备未指定或不支持选语言
// 非 null：设备语言字节码（如 LANG_ZH=0x01，LANG_EN=0x03，与 LanguageUtil 一致）

// App 发起：Sample 用 UI 选择的 locale（如 "zh-CN" / "en-US"）
```

### 5.3 设备发起

保持 `observeDeviceSession()` 订阅。`scene` 为 `RECORD` 或 `CALL_RECORD` 时启动录音业务。

### 5.4 App 发起

```kotlin
if (!speechAi.session.isSupportAppScene(WKSpeechSession.Scene.RECORD)) return

val session = speechAi.session.createAppSession(
    WKSpeechSession.Scene.RECORD,
    source, // PHONE_MIC / DEVICE_SCO / DEVICE_CMD
)
```

> App 侧一般只开 `RECORD`。`CALL_RECORD` 多为设备在通话场景下主动发起。

### 5.5 业务侧处理（App 自有）

WearKit `speechAi.record` **仅提供** `getLang()`，不负责把 ASR 文本回传设备。

典型做法（见 `RecordHandler`）：

1. 订阅 `session.audio()`，把音频送入自有 ASR。
2. 用累计全文更新 UI / 本地存储。
3. 按需保存原始音频（Sample 另有 Debug WAV 落盘，非协议必需）。

### 5.6 离场

录音多为一段有限音频流。Sample 在 `audio` 结束时 `release()`（`releaseOnAudioEnd = true`）；若仍收到 `SCENE_EXIT`，幂等即可。App 也可主动 `session.release()` / `SpeechAiManager.stopActiveSession()` 结束。

---

## 6. 翻译（TRANSLATE）

### 6.1 流程

```text
（设备可能先 TRANSLATE_LANG_SET）
Session(TRANSLATE) + audio()
  → getLang() 取语言对（可 null，再兜底）
  → ASR + 翻译
  → sendTextSource / sendTextTarget（累计快照）
  → TTS 就绪后 sendTtsReady()
  → 监听 TRANSLATE_PLAYER_STATE 控制播放
  → SCENE_EXIT → 离场
```

Sample：

* UI：`TranslateActivity`
* 业务：`TranslateHandler`

### 6.2 语言对

```kotlin
val lang = speechAi.translate.getLang()
    ?: WKTranslateLang.defaultFromSystemLocale()
// lang.source / lang.target：设备语言字节码（如 LANG_ZH=0x01，LANG_EN=0x03）
```

* 设备发起：优先用 `getLang()`。
* App 发起：Sample 用 UI 选择的源/目标语言。

### 6.3 App 发起

```kotlin
val session = speechAi.session.createAppSession(
    WKSpeechSession.Scene.TRANSLATE,
    source, // PHONE_MIC / DEVICE_SCO / DEVICE_CMD
)
```

### 6.4 回传与 TTS

```kotlin
speechAi.translate.sendTextSource(sourceFullText, isComplete).subscribe()
speechAi.translate.sendTextTarget(targetFullText, isComplete).subscribe()

// TTS 音频准备好后通知设备
speechAi.translate.sendTtsReady().subscribe()

// 设备播放控制
speechAi.observeMessage().subscribe { msg ->
    if (msg.type == WKSpeechAiMessage.Type.TRANSLATE_PLAYER_STATE) {
        val state = msg.data as WKTranslatePlayerState
        // START / STOP / PAUSE / RESUME
    }
}
```

### 6.5 离场

优先等 `SCENE_EXIT(TRANSLATE)`；老设备无 EXIT 时用 `audio` 结束 + 页面销毁兜底。

---

## 7. 对话翻译（CHAT_TRANSLATE）

对话翻译面向「手机 / 耳机 / 充电仓」三方场景，分两层：

1. **产品模式**：`startChatTranslate` / `stopChatTranslate`（协议 `0x08-0xB3`）
2. **音频 Session**：`CHAT_TRANSLATE_SELF`（己方）/ `CHAT_TRANSLATE_PEER`（对方）

### 7.1 模式

| Mode | 含义（Sample 约定） |
|------|---------------------|
| `FACE_TO_FACE` | 面对面：己方手机麦，对方充电仓 |
| `PRIVATE` | 私密：己方耳机，对方充电仓 |
| `PORTABLE` | 便携：己方手机麦，对方手机/耳机 |

### 7.2 进入模式（先于 Session）

Sample：`SpeechAiActivity` 选模式后：

```kotlin
speechAi.translate.startChatTranslate(WKChatTranslateMode.FACE_TO_FACE)
    .subscribe({
        // 再进入对话翻译页；保持 observeDeviceSession 订阅
        // 设备可能随后打开 SELF / PEER Session
    }, { /* fail */ })
```

退出模式（离开页面时）：

```kotlin
SpeechAiManager.stopActiveSession() // 若还有活跃音频 Session
speechAi.translate.stopChatTranslate().subscribe()
```

### 7.3 音频 Session

设备在模式内可主动开 `CHAT_TRANSLATE_SELF` / `CHAT_TRANSLATE_PEER`。  
App 也可在支持时：

```kotlin
speechAi.session.createAppSession(
    WKSpeechSession.Scene.CHAT_TRANSLATE_SELF, // 或 PEER
    source,
)
```

文本仍走 `translate` 通道：

```kotlin
speechAi.translate.sendTextSource(...)
speechAi.translate.sendTextTarget(...)
// 对话翻译不要调用 sendTtsReady（与普通翻译不同）
```

Sample 的 `ChatTranslateHandler` 会按 **模式 × 己方/对方** 决定：

* 是否下发原文 / 译文
* TTS 播到设备（`DEVICE_CMD` / `DEVICE_SCO`）还是手机（`PHONE_MIC`）

> 对话翻译 **不** 调用 `sendTtsReady`，也 **不** 依赖 `TRANSLATE_PLAYER_STATE`（与普通翻译不同）。

### 7.4 离场

```kotlin
speechAi.observeMessage().subscribe { msg ->
    if (msg.type == WKSpeechAiMessage.Type.SCENE_EXIT) {
        val scene = msg.data as? WKSpeechSession.Scene
        if (scene == CHAT_TRANSLATE_SELF || scene == CHAT_TRANSLATE_PEER) {
            // 结束当前 utterance / 或退出整页并 stopChatTranslate()
        }
    }
}
```

注意：单次 utterance 的 `SCENE_EXIT` 与「退出整个对话翻译模式」不同；离开产品模式务必再调 `stopChatTranslate()`。

---

## 8. 推荐 App 结构（对齐 Sample）

```text
Application / 进程级
  └─ SpeechAiManager
       ├─ init：observeDeviceSession + 初始化自有 AI SDK
       ├─ createAppSession(scene, source)
       └─ 按 scene 创建 Handler（Chat / Record / Translate / ChatTranslate）

UI
  ├─ SpeechAiActivity      能力入口、对话翻译选模式
  ├─ ChatActivity          对话页
  ├─ RecordActivity        录音页
  ├─ TranslateActivity     翻译页
  └─ ChatTranslateActivity 对话翻译页
```

要点：

1. **进程级**订阅 `observeDeviceSession`，不要只在某个 Activity `onResume` 才订。
2. AI SDK 就绪后再处理 Session（Sample：`State.READY` 前会 drop 并 `session.release()`）。
3. Handler 订阅 `observeMessage`，匹配本场景的 `SCENE_EXIT` 后释放。
4. 切换设备 / 断开前停止 Player 与活跃 Session。

---

## 9. 常见问题

| 现象 | 原因 / 处理 |
|------|-------------|
| 设备开了 AI，App 无响应 | 未订阅 `observeDeviceSession`，开流被静默丢弃 |
| `createAppSession` 返回 null | 场景不支持、已有 Session、或 DEVICE_CMD 未连接 |
| 设备文本乱 / 只显示最后几个字 | 发了增量文本；必须发累计全文 |
| Chat 发了文本设备不显示 | 未检查 `chat.isSupportText()` |
| 录音语言不对 | 设备发起要用 `record.getLang()` |
| 翻译语言不对 | 设备发起要用 `translate.getLang()` |
| 对话翻译进了页面但无音频 | 未 `startChatTranslate`，或能力位不含 SELF/PEER |
| 退出对话翻译后设备仍占模式 | 未调用 `stopChatTranslate()` |
| 老设备不发 SCENE_EXIT | 用 `audio` 结束 + 页面生命周期兜底 |

---

## 10. Sample 文件索引

| 文件 | 作用 |
|------|------|
| `ui/ai/SpeechAiActivity.kt` | 入口、能力检查、对话翻译选模式 |
| `ui/ai/SpeechAiManager.kt` | 进程级 Session / Handler |
| `ui/ai/handler/SceneHandler.kt` | Handler 基类、SCENE_EXIT |
| `ui/ai/handler/SessionAudioSource.kt` | `audio()` → AiKit |
| `ui/ai/chat/*` | 对话 |
| `ui/ai/record/*` | 录音 |
| `ui/ai/translate/*` | 翻译 |
| `ui/ai/chattranslate/*` | 对话翻译 |
| `sdk-apis/.../WKSpeechAiAbility.kt` | 公共 API |
| `sdk-apis/.../WKSpeechSession.kt` | Session / Scene / Source |
| `sdk-apis/.../WKChatTranslateMode.kt` | 对话翻译模式 |

API 细节以 `WKSpeechAiAbility` 源码注释为准。
