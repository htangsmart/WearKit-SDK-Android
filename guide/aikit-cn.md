# AiKit SDK Android 接入指南

> 版本：**1.0.2**
>
> AiKit（`com.topstep.aikit`）是独立于 WearKit 的云端 AI 库。设备侧语音会话、音频上行与文本回推仍走 WearKit 的 `speechAiAbility`，见 [WearKit 接入指南 · 4.7 语音 AI](wearkit-cn.md#4-7-speech-ai)。

---



<a id="1-about-aikit"></a>

## 1. AiKit 说明

`aikit-base` 定义与供应商无关的契约 `AiKit`。各通道模块包装一家供应商 SDK，实现同一套 `audio` / `chat` / `text` 能力。

本文覆盖：


| 模块                | 入口类            | 供应商            |
| ----------------- | -------------- | -------------- |
| `aikit-starburst` | `StarBurstKit` | 字节星芒           |
| `aikit-eyeear`    | `EyeEarKit`    | 骆方案（阿里 EyeEar） |
| `aikit-mltcloud`  | `MltCloudKit`  | 骆云 ChatCore    |


> 本文暂不介绍 `AiHealthAbility`、`AiDietPlanAbility`（仅 Topstep 通道实现，使用较少）。`aikit-topstep` / `aikit-watchfun` / `aikit-xksmart` 亦不在本文范围。

注册/引入某个通道模块，**不代表**每只手表都支持语音 AI。设备是否支持会话取决于固件与 `wearKit.speechAiAbility.isSupport()`。

---



<a id="2-quick-integration"></a>

## 2. 快速集成

> - Android 应用模块、Kotlin、Java 8 语言级别。
> - 宿主 `minSdk`：仅 StarBurst 可为 **21**；EyeEar / MltCloud 为 **26**。与 WearKit 语音 AI 联用时，建议宿主 `minSdk` **26**（与 [WearKit 接入指南 · 第 2 章](wearkit-cn.md#2-quick-integration) 一致）。
> - 创建 kit 初始化 Timber。AiKit 通过 Timber 打日志。
> - RxJava 3。在消费线程订阅；更新 UI 前 `observeOn` 到主线程。
> - 云端通道需要网络。同一进程同时只用**一条**通道；切换前对旧 kit 调用 `release()`，再 `init` 新通道。



<a id="2-1-maven"></a>

### 2.1 Maven 仓库

AiKit 与 WearKit 共用同一仓库：`https://maven.topstepht.com/repository/maven-public/`。通道会传递依赖供应商 SDK，宿主还须配置对应私有源：

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.topstepht.com/repository/maven-public/")
        }
        // 骆方案 AI SDK（EyeEar / MltCloud）
        maven {
            credentials {
                username = "5ff28ca9ed01613630f9d551"
                password = "cxN-HwJ]yzST"
            }
            url = uri("https://packages.aliyun.com/6718aa5c2c78927f26d82a35/maven/mltcloudai")
        }
        // 火山 AI SDK（StarBurst）
        maven {
            url = uri("https://artifact.bytedance.com/repository/encop_and_sol_ai_product/")
        }
        // 骆方案 AI SDK 引用的 QQ 音乐 SDK
        maven {
            credentials {
                username = "tvsdk_client"
                password = "tvsdk-client"
            }
            url = uri("https://ct.y.qq.com/TVapp/nexus/repository/maven-public/")
        }
    }
}
```



<a id="2-2-dependencies"></a>

### 2.2 依赖

声明 `aikit-base` 加**一条**通道，版本锁定一致：

```kotlin
dependencies {
    val aikitVersion = "1.0.2"
    implementation("com.topstep.aikit:aikit-base:$aikitVersion")

    // 只选一条通道
    implementation("com.topstep.aikit:aikit-starburst:$aikitVersion")
    // implementation("com.topstep.aikit:aikit-eyeear:$aikitVersion")
    // implementation("com.topstep.aikit:aikit-mltcloud:$aikitVersion")
}
```



<a id="2-3-init"></a>

### 2.3 初始化与释放

各通道入口类不同（`StarBurstKit` / `EyeEarKit` / `MltCloudKit`），但都实现 `AiKit`，初始化流程相同：构造 →（可选）订阅鉴权 → `init` → 成功后使用 → 断开时 `release`。

```kotlin
// 任选一条通道；StarBurstKit 须在主线程构造（@MainThread）
val kit: AiKit = EyeEarKit(context.applicationContext)
// val kit: AiKit = StarBurstKit(context.applicationContext)
// val kit: AiKit = MltCloudKit(context.applicationContext)

// 本三通道的 Subject 不重放：在 init 之前订阅，否则可能错过状态
val auth = kit.observeAuthState()
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe { state ->
        // 0 断开，1 失败，2 连接中，3 已认证
    }

// StarBurst 握手：设备回包 → kit；EyeEar / MltCloud 不会走到这条类型
val inbound = wearKit.deviceAbility.observeThirdPartyData()
    .filter { it.type == WKThirdPartyData.Type.STAR_BURST }
    .subscribe { kit.sendInitData(it.data) }

kit.init(
    params = AiKit.InitParams(
        channel = channel,       // StarBurst 必填项目号；EyeEar / MltCloud 忽略
        macAddress = deviceMac,  // EyeEar / MltCloud 鉴权用；StarBurst 可传设备 MAC
        customPrompt = null,     // 仅 StarBurst 生效；其余通道忽略
    ),
    handler = object : AiKit.InitHandler {
        override fun receiveInitData(bytes: ByteArray) {
            // StarBurst：kit → 设备；其他通道不会回调有意义的数据
            wearKit.deviceAbility.sendThirdPartyData(
                WKThirdPartyData(
                    type = WKThirdPartyData.Type.STAR_BURST,
                    data = bytes,
                )
            ).subscribe({}, { /* log */ })
        }

        override fun onInitSuccess() {
            // 按实际通道选择 WKSpeechAiSDK.STAR_BURST / EYE_EAR / MLT_CLOUD；未用枚举可传 null
            wearKit.speechAiAbility
                .setAiSDKInitResult(true, WKSpeechAiSDK.EYE_EAR)
                .subscribe({}, { /* log */ })
            // 可使用 kit.audio / kit.chat / kit.text
        }

        override fun onInitFail() {
            wearKit.speechAiAbility
                .setAiSDKInitResult(false, WKSpeechAiSDK.EYE_EAR)
                .subscribe({}, { /* log */ })
            kit.release()
        }
    },
)

// auth / inbound 与所有者一起 dispose；设备断开或切换通道时 kit.release()
```

`init` / `release` 均可重复调用，不应因此抛异常。以 `onInitSuccess()` 为成功；成功后再调 `audio` / `chat` / `text`。`isInitParamsEquivalent(a, b)` 判断第二次 `init` 能否跳过（默认全字段比较；EyeEar / MltCloud 只比较自己消费的 MAC）。第三方数据透传见 [WearKit · 4.1](wearkit-cn.md#4-1-third-party-data)；`setAiSDKInitResult` 见 [WearKit · 4.7](wearkit-cn.md#4-7-speech-ai)。

`InitParams`：


| 字段             | 作用                                                                                  |
| -------------- | ----------------------------------------------------------------------------------- |
| `channel`      | 供应商项目 / 渠道号。StarBurst 传给供应商 init；EyeEar / MltCloud 忽略，使用 kit 内部固定渠道。                |
| `macAddress`   | 设备 MAC。EyeEar / MltCloud 用它做鉴权。                                                     |
| `customPrompt` | 可选人设。仅 StarBurst 应用（也可用 `StarBurstKit.updateCustomPrompt` 刷新）；EyeEar / MltCloud 忽略。 |
| `appKey`       | 客户标识；本三通道可忽略。                                                                       |


音频约定（所有通道）：输入 16 kHz、单声道、16 bit，PCM 或 Ogg-Opus；输出 PCM 同样规格。`AiKit.AUDIO_FILE_FORMAT_WAV`（默认 `true`）决定写文件用 `.wav` 还是裸 `.pcm`，路径后缀须一致。

---



<a id="3-channels"></a>

## 3. 通道一览与差异化 API

公共能力走 `AiKit`（`audio` / `chat` / `text`）。下表为各通道对公共能力的覆盖；其后各小节只列**该通道入口类上、超出** `AiKit` **契约**的 API。


| 模块                | 入口                                    | minSdk | 公共能力覆盖                                              |
| ----------------- | ------------------------------------- | ------ | --------------------------------------------------- |
| `aikit-starburst` | `StarBurstKit(context)` `@MainThread` | 21     | ASR、对话、翻译、摘要、TTS、`generateImage`（返回 http/https URL） |
| `aikit-eyeear`    | `EyeEarKit(context)`                  | 26     | ASR、对话、翻译、摘要、TTS（无 `answer` / `generateImage`）      |
| `aikit-mltcloud`  | `MltCloudKit(context)`                | 26     | ASR、对话、翻译、摘要、TTS、`answer`、`generateImage`（本地文件路径）   |


未覆写的能力（如本三通道的 `health` / `dietPlan`）会以 `AiOperationException("... not supported by this channel")` 失败；`answer` / `generateImage` 在未实现通道上抛 `UnsupportedOperationException`。



<a id="3-1-starburst"></a>

### 3.1 StarBurst（`StarBurstKit`）

- **蓝牙握手**：`InitHandler.receiveInitData` / `sendInitData` 经 WearKit `WKThirdPartyData.Type.STAR_BURST` 透传（见 §2.3 示例）。
- `updateCustomPrompt(customPrompt)`：不重新鉴权，仅刷新人设（须主线程）。
- `isBtInitReady()`：当前 generation 是否已收到 BT init success。



<a id="3-2-eyeear"></a>

### 3.2 EyeEar（`EyeEarKit`）

- `setTestEnvironment(isTestEnvironment)`：须在 `init` **之前**调用。
- `setDenoiseEnable(enabled)`：对话降噪开关。
- `getAIChatLanguageCodes()`：对话支持的语言编码列表。
- `fileAsr(filePath, locale, maxSpeakers)`：长文件转写（不在 `AiAudioAbility` 上）。
- `supportsIntentReply(intent)` / `playIntentReply(requestId, text, locale, voiceId)`：意图最终提示播报。查找手机、退出对话、关机不走此 API；请求过期返回 false；空文案恢复输入且不播提示。



<a id="3-3-mltcloud"></a>

### 3.3 MltCloud（`MltCloudKit`）

- `setChatModelType(type)` / `chatModelType`：设置/读取 LLM 模型名（如 `"deepseek"`、`"doubao"`、`"zhipu"`）；空白值忽略。仅国内分支 LLM 请求消费该值。
- `getSupportLanguages(type)`：海外语言能力表（`MltSupportLanguageType.RECORDING` / `TTS`）。须在 `init` 成功后订阅；每次订阅独立请求，15 秒超时；不代表国内能力。

```kotlin
(kit as MltCloudKit).setChatModelType("deepseek")

(kit as MltCloudKit).getSupportLanguages(MltSupportLanguageType.TTS)
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe({ list -> /* MltSupportLanguage */ }, { /* 超时或失败 */ })
```

---



<a id="4-abilities"></a>

## 4. 能力详细说明

<a id="4-1-asr"></a>

### 4.1 语音识别（`kit.audio.asr`）

`kit.audio.asr(source, params)` 返回 `Observable<AiAsrResult>`。结果按句子下标给出累计快照，`OriginalText` 与 `TranslateText` 可能交错到达。以 `onComplete` 为结束。不要用 `dispose` 表示「用户停止说话」：调用 `AiAudioSource.stop()`，并保持订阅直到 `onComplete` 或 `onError`。`stop()` 之后识别还会再持续几秒。

```kotlin
val params = AiAsrParams(
    originalLocale = "zh",
    translateLocale = "en",
    originalFileRequired = false,
    translateTtsRequired = false,
    autoStop = true,
)
kit.audio.asr(source, params)
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe({ result ->
        when (result) {
            is AiAsrResult.OriginalText -> { /* result.index, result.text, result.isComplete */ }
            is AiAsrResult.TranslateText -> { /* 同样的字段，目标语言 */ }
            is AiAsrResult.OriginalFile -> { /* result.filePath */ }
            is AiAsrResult.TranslateTts -> { /* PCM 在 result.bytes；isComplete 时通常为空 */ }
            is AiAsrResult.SpeakerResult -> { /* 供应商 JSON；注释写明用于火山路径 */ }
        }
    }, { error ->
        // AiOperationException，或传给 AiAudioSource.stop 的 Throwable
    }, {
        // 这一句结束
    })
```

`AiAsrParams.autoStop = false` 时，Observable 不会自己 `onComplete` / `onError`；宿主必须 `stop()` 音源。

手表音频：继承 `AiAudioSource`，在首帧非空数据时由基类调用 `getFormat()`，返回 `AiAudioFormat.PCM` 或 `AiAudioFormat.OPUS(frameSize)`（与 `WKSpeechSession.format` 变为非 null 的时刻一致）。`sendData` 只在音源已 start 后入队。`stop(throwable)` 结束流；非 null 的 throwable 会成为 `asr` 的 `onError`。WearKit sample 的 `SessionAudioSource`（`ui/ai/handler`）可参考，属 sample only。

手机麦克风：`AiAudioSourceRecorder(context, sco)`；`sco = true` 走蓝牙 SCO。运行时需要 `RECORD_AUDIO`。

EyeEar 长文件转写见 [3.2](#3-2-eyeear) 的 `fileAsr`。



<a id="4-2-chat"></a>

### 4.2 实时对话（`kit.chat.chat`）

```kotlin
kit.chat.chat(
    audioSource = source,
    photoSource = null,
    locale = "zh",
    vadEnabled = false,
    multiModeEnabled = false,
    isSupportEcho = true,
    location = null,
    ttsPlayer = null,
    voiceId = null,
).observeOn(AndroidSchedulers.mainThread())
    .subscribe({ event ->
        when (event) {
            is AiChatResult.OnChat -> Unit
            is AiChatResult.OnText -> {
                // event.dialogId, event.isAsr, event.text, event.isComplete
            }
            is AiChatResult.OnIntent -> {
                // event.intent, event.requestId, event.query, event.value
                event.setConsumed(true)
            }
            is AiChatResult.OnPhoto -> Unit
            is AiChatResult.OnTtsPlayComplete -> Unit
        }
    }, { /* AiOperationException 或音源错误 */ }, { /* 会话结束 */ })
```

`ttsPlayer = null` 时使用内置 `TtsAudioPlayer`（`AudioTrack`）。要在别处播放（例如手表），传入 `AiChatTtsPlayer` 子类，其 `onWrite` 必须阻塞到该包 PCM 发送完成。对话打断和播放完成依赖这个播放器的状态。WearKit sample 的 `MyAudioPlayer` 可参考。

`AiChatResult.OnIntent.intent` 常用常量：


| 常量                                                                                   | 值                                                         |
| ------------------------------------------------------------------------------------ | --------------------------------------------------------- |
| `INTENT_PHOTO_START`                                                                 | `photo.start`                                             |
| `INTENT_PHOTO_UNDERSTAND`                                                            | `photo.understand`（`query` 是问题文本）                         |
| `INTENT_VIDEO_START` / `INTENT_VIDEO_STOP`                                           | `video.start` / `video.stop`                              |
| `INTENT_VOLUME_UP` / `INTENT_VOLUME_DOWN` / `INTENT_VOLUME_SET`                      | `volume.up` / `volume.down` / `volume.set`                |
| `INTENT_DIALOG_EXIT`                                                                 | `dialog.exit`（SDK 内部已处理）                                  |
| `INTENT_ROLE_SET`                                                                    | `role.set`                                                |
| `INTENT_DEVICE_SHUTDOWN`                                                             | `device.shutdown`                                         |
| `INTENT_VOICE_CLONE`                                                                 | `voice.clone`                                             |
| `INTENT_MUSIC_PLAY` / `INTENT_MUSIC_STOP` / `INTENT_MUSIC_PRE` / `INTENT_MUSIC_NEXT` | `music.play` / `music.stop` / `music.prev` / `music.next` |
| `INTENT_START_RECORD` / `INTENT_STOP_RECORD`                                         | `recording.start` / `recording.stop`                      |
| `INTENT_QUERY_BATTERY`                                                               | `device.battery`                                          |
| `INTENT_FIND_PHONE` / `INTENT_STOP_FIND_PHONE`                                       | `find.phone` / `stop.phone`                               |
| `INTENT_LOCATION_QUERY`                                                              | `location.query`                                          |


`INTENT_VOLUME_SET` 的 `value` 是 JSON 对象或 JSON 数组。调用 `setConsumed`，让通道知道宿主是否处理了该意图。EyeEar 意图提示播报见 [3.2](#3-2-eyeear)。

`AiKit.CHAT_ENERGY_GATE_ENABLED`（默认 `false`）和 `CHAT_ENERGY_GATE_OPEN_DBFS`（默认 `-50f`）作用于下一次创建、且 `energyGateCapable = true` 的对话音源。低于阈值的帧会被置零，避免供应商 VAD 把底噪当成打断。

`chat.updateLocation`、`updateLocationSendPing`、`sendText` 的默认实现是空操作 / 返回 false。只有覆写了它们的通道才会消费位置或接受文本轮次。



<a id="4-3-text"></a>

### 4.3 文本能力（`kit.text`）

流式文本是累计全量快照。最后一条的 `isComplete = true`。

```kotlin
kit.text.translate("今天是星期几？", originalLocale = "zh", translateLocale = "en")
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe({ result ->
        // result.text, result.isComplete
    }, { /* AiOperationException */ }, { })

kit.text.summary(longText, locale = "zh")

kit.text.tts("你好", locale = "zh", voiceType = null)
    .subscribe { pcm ->
        // 16 kHz 单声道 16 bit PCM，在 pcm.bytes（或 pcm.base64）
    }
```


| 方法                                                  | 本三通道                                                                                  |
| --------------------------------------------------- | ------------------------------------------------------------------------------------- |
| `translate` / `summary` / `tts`                     | StarBurst、EyeEar、MltCloud 均实现                                                         |
| `answer`                                            | 仅 MltCloud。`AiTextResult.text` 是累计答案                                                  |
| `generateImage(prompt, width, height, vendorStyle)` | MltCloud（本地文件路径；`vendorStyle` 例如 `"308"`）和 StarBurst（http/https URL；忽略 `vendorStyle`） |


MltCloud 模型切换与海外语言表见 [3.3](#3-3-mltcloud)。

---



<a id="5-wearkit-speech"></a>

## 5. 与 WearKit 语音 AI 联用

职责划分：

```
设备麦克风 → WKSpeechSession.audio() → AiAudioSource（如 SessionAudioSource）
         → AiKit.audio.asr / AiKit.chat.chat（云端 ASR/LLM/TTS）
App 结果 → Chat.sendTextQuestion / sendTextAnswer 等回推设备
设备扬声器 ← Player.start/write（或自定义 AiChatTtsPlayer）
```

要点：

1. 先按 [WearKit · 4.7](wearkit-cn.md#4-7-speech-ai) 完成 `speechAiAbility`：`isSupport()`、订阅 `observeDeviceSession()`、创建/持有 `WKSpeechSession`。
2. `init` 成功/失败时回告设备：见 [§2.3](#2-3-init) 中的 `setAiSDKInitResult`（含 StarBurst 第三方数据握手）。
3. 场景回推（录音 ASR、翻译、对话文本等）仍走 `WKSpeechSession` 各子能力，不由 AiKit 直接写设备。

完整场景编排见 WearKit sample `ui/ai`（`SpeechAiManager`、`ChatHandler`、`RecordHandler`、`TranslateHandler` 等），属 sample only。

---



<a id="6-errors-lifecycle"></a>

## 6. 权限、生命周期、线程与错误

- 云端调用需要 `INTERNET`。使用 `AiAudioSourceRecorder` 或任何手机麦克风路径时，运行时需要 `RECORD_AUDIO`。手表指令通道上的音频不用手机麦克风。
- `AiAudioSource.stop()` 投递到主线程且不等待，避免供应商回调线程和主线程互相等待。
- 失败的 init 不要把旧 kit 漏掉：对失败实例调用 `release()`。StarBurst 的 `init` 会在新握手之前把鉴权状态重置为 `0`。
- Rx 订阅随页面或进程所有者 dispose。设备断开时 `release()` kit。
- EyeEar 实时对话的 `error` 事件以 `AiOperationException` 上报并取消对话订阅；kit 不会自动重试该会话。

`AiOperationException.errCode`：


| 常量                           | 值               | 含义               |
| ---------------------------- | --------------- | ---------------- |
| `ERR_UNKNOWN`                | `Int.MIN_VALUE` | 未知               |
| `ERR_UNAUTHORIZED`           | `-1002`         | 未授权              |
| `ERR_NETWORK_SOCKET_TIMEOUT` | `-1001`         | 网络错误             |
| `ERR_EMPTY_REQUEST`          | `1000`          | 请求内容为空（如输入音频为空）  |
| `ERR_DUPLICATE_REQUEST`      | `1001`          | 请求重复（如对话已开启再开一次） |
| `ERR_QUOTA_INSUFFICIENT`     | `4001`          | 额度不足             |


---



<a id="7-verify-faq"></a>

## 7. 验证与常见问题

**验证建议**

1. 能解析 `aikit-base` 加上所选通道，版本为交付的正式版（如 `1.0.2`）。
2. `init` 到达 `onInitSuccess`，鉴权状态为 `3`。StarBurst 在此之前还能看到双向的第三方数据字节。
3. `AiAudioSource.stop()` 之后，`asr` 至少发出一条 `isComplete = true` 的 `OriginalText`，然后 `onComplete`。
4. `chat` 对问题发出 `isAsr = true` 的 `OnText`，对回答发出 `isAsr = false` 的 `OnText`。

**常见问题**

1. **依赖解析失败**：除 `maven-public` 外，按第 2.1 节补齐供应商仓库与凭证。
2. **鉴权状态一直不是 3**：StarBurst 检查第三方数据透传是否双向畅通；EyeEar / MltCloud 检查 MAC、网络；观察是否在 `init` 前订阅了 `observeAuthState()`。
3. **ASR 提前 dispose 丢结果**：先 `stop()` 音源，等 `onComplete`。
4. **与设备语音对不上**：确认走了 `speechAiAbility` 会话与回推 API，而不是只调了 AiKit。
5. **通道不支持某能力**：查第 3 章能力表；未实现能力会立刻失败，属预期。

---

*以 android-aikit 公开 API 与 WearKit sample 用法为准；供应商 SDK 行为以对应通道实现为准。*