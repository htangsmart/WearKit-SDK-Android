# AiKit SDK Android Integration Guide

> Version: **1.0.3**
>
> AiKit (`com.topstep.aikit`) is a cloud AI library independent of WearKit. Device-side speech sessions, audio uplink, and text push-back still go through WearKit `speechAiAbility` — see [WearKit guide · 4.7 Speech AI](wearkit-en.md#4-7-speech-ai).

---

<a id="1-about-aikit"></a>

## 1. About AiKit

`aikit-base` defines the vendor-neutral contract `AiKit`. Each channel module wraps one vendor SDK and implements the same `audio` / `chat` / `text` abilities.

This guide covers:

| Module | Entry class | Vendor |
|---|---|---|
| `aikit-starburst` | `StarBurstKit` | ByteDance StarBurst |
| `aikit-eyeear` | `EyeEarKit` | Artillery EyeEar (Aliyun) |
| `aikit-mltcloud` | `MltCloudKit` | Artillery MltCloud ChatCore |

> This guide does not cover `AiHealthAbility` or `AiDietPlanAbility` (Topstep channel only; rarely used). `aikit-topstep` / `aikit-watchfun` / `aikit-xksmart` are also out of scope here.

Declaring a channel module does **not** mean every watch supports speech AI. Session support depends on firmware and `wearKit.speechAiAbility.isSupport()`.

---

<a id="2-quick-integration"></a>

## 2. Quick integration

> - Android app module, Kotlin, Java 8 language level.
> - Host `minSdk`: **21** if StarBurst only; **26** for EyeEar / MltCloud. When pairing with WearKit speech AI, prefer host `minSdk` **26** (same as [WearKit guide · chapter 2](wearkit-en.md#2-quick-integration)).
> - Plant a Timber `Tree` before creating a kit. AiKit logs via Timber.
> - RxJava 3. Subscribe on the consuming thread; `observeOn` the main thread before UI updates.
> - Cloud channels need network. Use only **one** channel per process; `release()` the old kit before `init` on a new channel.

<a id="2-1-maven"></a>

### 2.1 Maven repositories

AiKit shares WearKit’s repository: `https://maven.topstepht.com/repository/maven-public/`. Channels transitively pull vendor SDKs; the host must also configure those private repos:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.topstepht.com/repository/maven-public/")
        }
        // Artillery AI SDK (EyeEar / MltCloud)
        maven {
            credentials {
                username = "5ff28ca9ed01613630f9d551"
                password = "cxN-HwJ]yzST"
            }
            url = uri("https://packages.aliyun.com/6718aa5c2c78927f26d82a35/maven/mltcloudai")
        }
        // Volcano / StarBurst AI SDK
        maven {
            url = uri("https://artifact.bytedance.com/repository/encop_and_sol_ai_product/")
        }
        // QQ Music SDK pulled by Artillery AI SDK
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

### 2.2 Dependencies

Declare `aikit-base` plus **one** channel, same version:

```kotlin
dependencies {
    val aikitVersion = "1.0.3"
    implementation("com.topstep.aikit:aikit-base:$aikitVersion")

    // Pick exactly one channel
    implementation("com.topstep.aikit:aikit-starburst:$aikitVersion")
    // implementation("com.topstep.aikit:aikit-eyeear:$aikitVersion")
    // implementation("com.topstep.aikit:aikit-mltcloud:$aikitVersion")
}
```

<a id="2-3-init"></a>

### 2.3 Init and release

Entry classes differ (`StarBurstKit` / `EyeEarKit` / `MltCloudKit`), but all implement `AiKit`. Init flow is the same: construct → (optional) subscribe auth → `init` → use after success → `release` on disconnect.

```kotlin
// Pick one channel; StarBurstKit must be constructed on the main thread (@MainThread)
val kit: AiKit = EyeEarKit(context.applicationContext)
// val kit: AiKit = StarBurstKit(context.applicationContext)
// val kit: AiKit = MltCloudKit(context.applicationContext)

// These three channels do not replay auth Subject: subscribe before init or you may miss states
val auth = kit.observeAuthState()
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe { state ->
        // 0 disconnected, 1 failed, 2 connecting, 3 authenticated
    }

// StarBurst handshake: device → kit; EyeEar / MltCloud never emit this type
val inbound = wearKit.deviceAbility.observeThirdPartyData()
    .filter { it.type == WKThirdPartyData.Type.STAR_BURST }
    .subscribe { kit.sendInitData(it.data) }

kit.init(
    params = AiKit.InitParams(
        channel = channel,       // required project id for StarBurst; ignored by EyeEar / MltCloud
        macAddress = deviceMac,  // used for EyeEar / MltCloud auth; StarBurst may pass device MAC
        customPrompt = null,     // StarBurst only; ignored by other channels
    ),
    handler = object : AiKit.InitHandler {
        override fun receiveInitData(bytes: ByteArray) {
            // StarBurst: kit → device; other channels do not deliver meaningful callbacks
            wearKit.deviceAbility.sendThirdPartyData(
                WKThirdPartyData(
                    type = WKThirdPartyData.Type.STAR_BURST,
                    data = bytes,
                )
            ).subscribe({}, { /* log */ })
        }

        override fun onInitSuccess() {
            // Pick WKSpeechAiSDK.STAR_BURST / EYE_EAR / MLT_CLOUD for the channel; null if unused
            wearKit.speechAiAbility
                .setAiSDKInitResult(true, WKSpeechAiSDK.EYE_EAR)
                .subscribe({}, { /* log */ })
            // kit.audio / kit.chat / kit.text are ready
        }

        override fun onInitFail() {
            wearKit.speechAiAbility
                .setAiSDKInitResult(false, WKSpeechAiSDK.EYE_EAR)
                .subscribe({}, { /* log */ })
            kit.release()
        }
    },
)

// Dispose auth / inbound with the owner; on disconnect or channel switch: kit.release()
```

`init` / `release` may be called repeatedly without throwing. Treat `onInitSuccess()` as success; only then call `audio` / `chat` / `text`. `isInitParamsEquivalent(a, b)` tells whether a second `init` can be skipped (default compares all fields; EyeEar / MltCloud compare only the MAC they consume). Third-party data: [WearKit · 4.1](wearkit-en.md#4-1-third-party-data). `setAiSDKInitResult`: [WearKit · 4.7](wearkit-en.md#4-7-speech-ai).

`InitParams`:

| Field | Role |
|---|---|
| `channel` | Vendor project / channel id. StarBurst passes it to vendor init; EyeEar / MltCloud ignore it and use a fixed kit-internal channel. |
| `macAddress` | Device MAC. Used for EyeEar / MltCloud auth. |
| `customPrompt` | Optional persona. Applied only by StarBurst (or refresh via `StarBurstKit.updateCustomPrompt`); ignored by EyeEar / MltCloud. |
| `appKey` | Customer id; unused for these three channels. |

Audio contract (all channels): input 16 kHz mono 16-bit PCM or Ogg-Opus; output PCM with the same format. `AiKit.AUDIO_FILE_FORMAT_WAV` (default `true`) chooses `.wav` vs raw `.pcm` when writing files; path suffix must match.

---

<a id="3-channels"></a>

## 3. Channels and channel-specific APIs

Shared abilities use `AiKit` (`audio` / `chat` / `text`). The table shows coverage per channel; subsections list APIs on that kit class that are **beyond** the `AiKit` contract.

| Module | Entry | minSdk | Shared ability coverage |
|---|---|---|---|
| `aikit-starburst` | `StarBurstKit(context)` `@MainThread` | 21 | ASR, chat, translate, summary, TTS, `generateImage` (http/https URL) |
| `aikit-eyeear` | `EyeEarKit(context)` | 26 | ASR, chat, translate, summary, TTS (no `answer` / `generateImage`) |
| `aikit-mltcloud` | `MltCloudKit(context)` | 26 | ASR, chat, translate, summary, TTS, `answer`, `generateImage` (local file path) |

Unimplemented abilities (e.g. `health` / `dietPlan` on these three) fail with `AiOperationException("... not supported by this channel")`. Unimplemented `answer` / `generateImage` throw `UnsupportedOperationException`.

<a id="3-1-starburst"></a>

### 3.1 StarBurst (`StarBurstKit`)

- **Bluetooth handshake**: `InitHandler.receiveInitData` / `sendInitData` via WearKit `WKThirdPartyData.Type.STAR_BURST` (see §2.3 sample).
- `updateCustomPrompt(customPrompt)`: refresh persona without re-auth (main thread).
- `isBtInitReady()`: whether the current generation has received BT init success.

<a id="3-2-eyeear"></a>

### 3.2 EyeEar (`EyeEarKit`)

- `setTestEnvironment(isTestEnvironment)`: call **before** `init`.
- `setDenoiseEnable(enabled)`: chat denoise switch.
- `getAIChatLanguageCodes()`: language codes supported for chat.
- `fileAsr(filePath, locale, maxSpeakers)`: long-file transcription (not on `AiAudioAbility`).
- `supportsIntentReply(intent)` / `playIntentReply(requestId, text, locale, voiceId)`: final intent prompt playback. Find-phone, dialog exit, and shutdown do not use this API; expired requests return false; blank text restores input without playing a prompt.

<a id="3-3-mltcloud"></a>

### 3.3 MltCloud (`MltCloudKit`)

- `setChatModelType(type)` / `chatModelType`: set/read LLM model name (e.g. `"deepseek"`, `"doubao"`, `"zhipu"`); blank values ignored. Consumed only on the domestic LLM branch.
- `getSupportLanguages(type)`: overseas language table (`MltSupportLanguageType.RECORDING` / `TTS`). Subscribe after successful `init`; each subscription is a new request with a 15s timeout; not the domestic capability table.

```kotlin
(kit as MltCloudKit).setChatModelType("deepseek")

(kit as MltCloudKit).getSupportLanguages(MltSupportLanguageType.TTS)
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe({ list -> /* MltSupportLanguage */ }, { /* timeout or failure */ })
```

---

<a id="4-abilities"></a>

## 4. Ability details

<a id="4-1-asr"></a>

### 4.1 Speech recognition (`kit.audio.asr`)

`kit.audio.asr(source, params)` returns `Observable<AiAsrResult>`. Results are cumulative snapshots keyed by sentence index; `OriginalText` and `TranslateText` may interleave. Treat `onComplete` as the end. Do not use `dispose` to mean “user stopped speaking”: call `AiAudioSource.stop()` and keep the subscription until `onComplete` or `onError`. Recognition may continue for a few seconds after `stop()`.

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
            is AiAsrResult.TranslateText -> { /* same fields, target language */ }
            is AiAsrResult.OriginalFile -> { /* result.filePath */ }
            is AiAsrResult.TranslateTts -> { /* PCM in result.bytes; usually empty when isComplete */ }
            is AiAsrResult.SpeakerResult -> { /* vendor JSON; comments say Volcano path */ }
        }
    }, { error ->
        // AiOperationException, or Throwable passed to AiAudioSource.stop
    }, {
        // this utterance finished
    })
```

When `AiAsrParams.autoStop = false`, the Observable never completes or errors by itself; the host must `stop()` the source.

Watch audio: subclass `AiAudioSource`; the base calls `getFormat()` on the first non-empty frame, returning `AiAudioFormat.PCM` or `AiAudioFormat.OPUS(frameSize)` (same moment `WKSpeechSession.format` becomes non-null). `sendData` only after the source has started. `stop(throwable)` ends the stream; a non-null throwable becomes `asr`’s `onError`. WearKit sample `SessionAudioSource` (`ui/ai/handler`) is sample only.

Phone mic: `AiAudioSourceRecorder(context, sco)`; `sco = true` uses Bluetooth SCO. Runtime `RECORD_AUDIO` required.

EyeEar long-file ASR: see `fileAsr` in [3.2](#3-2-eyeear).

<a id="4-2-chat"></a>

### 4.2 Realtime chat (`kit.chat.chat`)

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
    }, { /* AiOperationException or source error */ }, { /* session ended */ })
```

`ttsPlayer = null` uses built-in `TtsAudioPlayer` (`AudioTrack`). To play elsewhere (e.g. watch), pass an `AiChatTtsPlayer` subclass whose `onWrite` blocks until that PCM packet is sent. Barge-in and play-complete depend on this player’s state. WearKit sample `MyAudioPlayer` is a reference.

Common `AiChatResult.OnIntent.intent` constants:

| Constant | Value |
|---|---|
| `INTENT_PHOTO_START` | `photo.start` |
| `INTENT_PHOTO_UNDERSTAND` | `photo.understand` (`query` is the question text) |
| `INTENT_VIDEO_START` / `INTENT_VIDEO_STOP` | `video.start` / `video.stop` |
| `INTENT_VOLUME_UP` / `INTENT_VOLUME_DOWN` / `INTENT_VOLUME_SET` | `volume.up` / `volume.down` / `volume.set` |
| `INTENT_DIALOG_EXIT` | `dialog.exit` (handled inside the SDK) |
| `INTENT_ROLE_SET` | `role.set` |
| `INTENT_DEVICE_SHUTDOWN` | `device.shutdown` |
| `INTENT_VOICE_CLONE` | `voice.clone` |
| `INTENT_MUSIC_PLAY` / `INTENT_MUSIC_STOP` / `INTENT_MUSIC_PRE` / `INTENT_MUSIC_NEXT` | `music.play` / `music.stop` / `music.prev` / `music.next` |
| `INTENT_START_RECORD` / `INTENT_STOP_RECORD` | `recording.start` / `recording.stop` |
| `INTENT_QUERY_BATTERY` | `device.battery` |
| `INTENT_FIND_PHONE` / `INTENT_STOP_FIND_PHONE` | `find.phone` / `stop.phone` |
| `INTENT_LOCATION_QUERY` | `location.query` |

`INTENT_VOLUME_SET` `value` is a JSON object or JSON array. Call `setConsumed` so the channel knows whether the host handled the intent. EyeEar intent reply playback: [3.2](#3-2-eyeear).

`AiKit.CHAT_ENERGY_GATE_ENABLED` (default `false`) and `CHAT_ENERGY_GATE_OPEN_DBFS` (default `-50f`) apply to the next chat source created with `energyGateCapable = true`. Frames below the threshold are zeroed so vendor VAD does not treat noise floor as barge-in.

Default `chat.updateLocation`, `updateLocationSendPing`, and `sendText` are no-ops / return false. Only channels that override them consume location or text turns.

<a id="4-3-text"></a>

### 4.3 Text abilities (`kit.text`)

Streaming text is a cumulative full snapshot. The last item has `isComplete = true`.

```kotlin
kit.text.translate("今天是星期几？", originalLocale = "zh", translateLocale = "en")
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe({ result ->
        // result.text, result.isComplete
    }, { /* AiOperationException */ }, { })

kit.text.summary(longText, locale = "zh")

kit.text.tts("你好", locale = "zh", voiceType = null)
    .subscribe { pcm ->
        // 16 kHz mono 16-bit PCM in pcm.bytes (or pcm.base64)
    }
```

| Method | These three channels |
|---|---|
| `translate` / `summary` / `tts` | StarBurst, EyeEar, MltCloud |
| `answer` | MltCloud only; `AiTextResult.text` is the cumulative answer |
| `generateImage(prompt, width, height, vendorStyle)` | MltCloud (local file path; `vendorStyle` e.g. `"308"`) and StarBurst (http/https URL; ignores `vendorStyle`) |

MltCloud model switch and overseas language table: [3.3](#3-3-mltcloud).

---

<a id="5-wearkit-speech"></a>

## 5. Pairing with WearKit speech AI

Responsibilities:

```
Device mic → WKSpeechSession.audio() → AiAudioSource (e.g. SessionAudioSource)
          → AiKit.audio.asr / AiKit.chat.chat (cloud ASR/LLM/TTS)
App results → Chat.sendTextQuestion / sendTextAnswer (etc.) back to device
Device speaker ← Player.start/write (or custom AiChatTtsPlayer)
```

Checklist:

1. Follow [WearKit · 4.7](wearkit-en.md#4-7-speech-ai) for `speechAiAbility`: `isSupport()`, keep `observeDeviceSession()` subscribed, create/hold `WKSpeechSession`.
2. Report init success/failure to the device: see `setAiSDKInitResult` in [§2.3](#2-3-init) (includes StarBurst third-party handshake).
3. Scene push-back (record ASR, translate, chat text, etc.) still uses `WKSpeechSession` sub-abilities — AiKit does not write the device directly.

Full scene orchestration: WearKit sample `ui/ai` (`SpeechAiManager`, `ChatHandler`, `RecordHandler`, `TranslateHandler`, etc.) — sample only.

---

<a id="6-errors-lifecycle"></a>

## 6. Permissions, lifecycle, threads, and errors

- Cloud calls need `INTERNET`. Phone-mic paths (`AiAudioSourceRecorder` or similar) need runtime `RECORD_AUDIO`. Watch command-channel audio does not use the phone mic.
- `AiAudioSource.stop()` posts to the main thread and does not wait, avoiding deadlocks between vendor callbacks and the main thread.
- Do not leak a failed init kit: call `release()` on the failed instance. StarBurst `init` resets auth state to `0` before a new handshake.
- Dispose Rx subscriptions with the page/process owner. `release()` the kit when the device disconnects.
- EyeEar realtime chat `error` events are reported as `AiOperationException` and cancel the chat subscription; the kit does not auto-retry that session.

`AiOperationException.errCode`:

| Constant | Value | Meaning |
|---|---|---|
| `ERR_UNKNOWN` | `Int.MIN_VALUE` | Unknown |
| `ERR_UNAUTHORIZED` | `-1002` | Unauthorized |
| `ERR_NETWORK_SOCKET_TIMEOUT` | `-1001` | Network error |
| `ERR_EMPTY_REQUEST` | `1000` | Empty request (e.g. empty audio) |
| `ERR_DUPLICATE_REQUEST` | `1001` | Duplicate request (e.g. chat already open) |
| `ERR_QUOTA_INSUFFICIENT` | `4001` | Quota exhausted |

---

<a id="7-verify-faq"></a>

## 7. Verification and FAQ

**Verification**

1. Resolve `aikit-base` plus the chosen channel at the delivered release version (e.g. `1.0.2`).
2. `init` reaches `onInitSuccess` and auth state `3`. StarBurst should also show bidirectional third-party data bytes beforehand.
3. After `AiAudioSource.stop()`, `asr` emits at least one `OriginalText` with `isComplete = true`, then `onComplete`.
4. `chat` emits `OnText` with `isAsr = true` for the question and `isAsr = false` for the answer.

**FAQ**

1. **Dependency resolution fails**: besides `maven-public`, add vendor repos and credentials from §2.1.
2. **Auth never reaches 3**: for StarBurst check bidirectional third-party data; for EyeEar / MltCloud check MAC and network; confirm `observeAuthState()` was subscribed before `init`.
3. **ASR lost after early dispose**: `stop()` the source first, then wait for `onComplete`.
4. **Device speech mismatch**: use `speechAiAbility` sessions and push-back APIs, not AiKit alone.
5. **Channel does not support an ability**: see chapter 3; unimplemented abilities fail immediately by design.

---

*Authoritative against android-aikit public APIs and WearKit sample usage; vendor SDK behavior follows each channel implementation.*
