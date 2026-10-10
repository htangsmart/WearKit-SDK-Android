# WearKit SDK Android Integration Guide

> GitHub: https://github.com/htangsmart/WearKit-SDK-Android/
>
> Version: public release in `CHANGED.md` **3.0.2.7**

---

<a id="1-about-wearkit"></a>

## 1. About WearKit

WearKit is a unified SDK for wearables (`com.topstep.wearkit`). **Scanning always uses BLE**; after a connection is established, the data channel may be BLE or classic Bluetooth SPP, depending on the device platform (and firmware). Developers still use the same entry points. After initialization you get a `WKWearKit`, then use `scanner`, `connector`, and the ability APIs. Choose a device platform by registering the matching `WKWearKit.Builder`. Adding only an adapter dependency without registering its builder leaves that platform unavailable:

| Device platform (`WKDeviceType`) | Adapter artifact | Builder |
|---|---|---|
| `FIT_CLOUD` | `sdk-fitcloud-adapter` | `WKFitCloudKit.Builder` |
| `FLY_WEAR` | `sdk-flywear-adapter` | `WKFlyWearKit.Builder` |
| `SHEN_JU` | `sdk-shenju-adapter` | `WKShenJuKit.Builder` |
| `PROTO_TB` | `sdk-prototb-adapter` | `WKProtoTbKit.Builder` |
| `AB_MATE` | `sdk-abmate-adapter` | `WKAbMateKit.Builder` |

Registering an adapter **does not** mean every device on that platform supports every ability. Support depends on firmware and each ability’s `Compat` API (see the feature sections below).

---

## 2. Quick Integration

> - Android application module, Kotlin, Java 8 language level.
> - App `minSdk` is **recommended to be 26**. If you do not integrate AiKit (`com.topstep.aikit`), you may lower it to **24**.
> - Bluetooth hardware and Bluetooth permissions (see [Permissions](#2-7-permission)).

<a id="2-1-maven"></a>

### 2.1 Maven setup (recommended)

Repository: `https://maven.topstepht.com/repository/maven-public/`

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.topstepht.com/repository/maven-public/")
        }
    }
}
```

Pin a single WearKit version. Declare `sdk-core` and the adapters you **actually ship**. The sample below lists required items per family (expanded); delete unused families entirely. Items marked optional are non-core: omitting them reduces dependencies, but **using the related feature may crash**.

```kotlin
dependencies {
    val wearkitVersion = "3.0.2.7"

    // ========== Core ==========
    ///////////// Required ///////////
    implementation("com.topstep.wearkit:sdk-core:$wearkitVersion")
    ///////////// Optional ///////////
    // ImageProxy.toWKImageProxy (wearKit.cameraAbility.updatePreview):
    // implementation("androidx.camera:camera-core:1.4.2")

    // ========== FitCloud (WKDeviceType.FIT_CLOUD) ==========
    ///////////// Required ///////////
    implementation("com.topstep.wearkit:sdk-fitcloud-adapter:$wearkitVersion")
    ///////////// Optional ///////////
    // AliAgent (rarely needed; also Builder.setSupportAliAgent):
    // implementation("com.topstep.wearkit:ext-aliagent:1.0.5")
    // implementation("com.topstep.wearkit:ext-aliagent-ext:1.0.5")
    // Sensor game (rarely needed; also Builder.setSensorGameApiKey):
    // implementation("com.topstep.wearkit:ext-sensorgame:1.0.5")
    // WeChat Pay certification (rarely needed; also Builder.setSupportWeChatPay):
    // implementation("com.artillery.pay:paycertification:leadingSmart_1.0.54")
    // Nordic chip DFU (very old devices, roughly 2024-era hardware):
    // implementation("no.nordicsemi.android:dfu:2.2.2")

    // ========== FlyWear (WKDeviceType.FLY_WEAR) ==========
    ///////////// Required ///////////
    implementation("com.topstep.wearkit:sdk-flywear-adapter:$wearkitVersion")

    // ========== ShenJu (WKDeviceType.SHEN_JU) ==========
    ///////////// Required ///////////
    implementation("com.topstep.wearkit:sdk-shenju-adapter:$wearkitVersion")

    // ========== ProtoTb (WKDeviceType.PROTO_TB) ==========
    ///////////// Required ///////////
    implementation("com.topstep.wearkit:sdk-prototb-adapter:$wearkitVersion")
    ///////////// Optional ///////////
    // Custom video dial (other FFmpegKit builds OK — do not mix multiple FFmpeg native libs):
    // implementation("com.antonkarpenko:ffmpeg-kit-min-gpl:2.1.0")

    // ========== AbMate (WKDeviceType.AB_MATE) ==========
    ///////////// Required ///////////
    implementation("com.topstep.wearkit:sdk-abmate-adapter:$wearkitVersion")
}
```

### 2.2 Local AAR setup

Use this only when [Maven setup](#2-1-maven) is inconvenient. AARs have no POM, so WearKit files and third-party libraries must be declared by hand. Put `sdk-*-v3.0.2.7.aar` under `libs/`, and extension packages under `libs/ext/` (filenames match `WearKit-SDK-Android/libs/`). Ext packages keep **their own** version numbers — do not force `3.0.2.7`. The sample below lists required items per family (expanded); delete unused families entirely. AARs that overlap across families can stay as-is; no need to deduplicate by hand.

```kotlin
dependencies {
    // ========== Core ==========
    ///////////// Required ///////////
    implementation(files("libs/sdk-base-v3.0.2.7.aar"))
    implementation(files("libs/sdk-apis-v3.0.2.7.aar"))
    implementation(files("libs/sdk-core-v3.0.2.7.aar"))
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.8.22"))
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.annotation:annotation:1.5.0")
    implementation("io.reactivex.rxjava3:rxjava:3.1.5")
    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")
    implementation("com.polidea.rxandroidble3:rxandroidble:1.17.2")
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("androidx.room:room-runtime:2.5.0")
    implementation("androidx.room:room-ktx:2.5.0")
    implementation("androidx.room:room-rxjava3:2.5.0")
    ///////////// Optional ///////////
    // ImageProxy.toWKImageProxy (wearKit.cameraAbility.updatePreview):
    // implementation("androidx.camera:camera-core:1.4.2")

    // ========== FitCloud (WKDeviceType.FIT_CLOUD) ==========
    ///////////// Required ///////////
    implementation(files("libs/sdk-fitcloud-v3.0.2.7.aar"))
    implementation(files("libs/sdk-fitcloud-adapter-v3.0.2.7.aar"))
    implementation(files("libs/ext/sdk-realtek-dfu-v1.0.5.aar"))
    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation("org.apache.commons:commons-compress:1.24.0")
    implementation("com.topstep.opus:lib-opustool:1.0.8")
    implementation("com.topstep.tool:lib-abpartool:1.0.1")
    implementation("pl.droidsonroids.gif:android-gif-drawable:1.2.32")
    implementation(files("libs/ext/sdk-realtek-bbpro-v1.0.4.aar"))
    implementation(files("libs/ext/sdk-realtek-file-v1.0.4.aar"))
    ///////////// Optional ///////////
    // AliAgent (rarely needed; also Builder.setSupportAliAgent):
    // implementation(files("libs/ext/sdk-aliagent-v1.0.5.aar"))
    // implementation(files("libs/ext/sdk-aliagent-ext-v1.0.5.aar"))
    // implementation("com.google.code.gson:gson:2.10")
    // implementation("com.google.firebase:firebase-crashlytics-buildtools:2.8.1")
    // implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.4")
    // implementation("com.alibaba:fastjson:1.2.83")
    // implementation("org.apache.commons:commons-text:1.9")
    // implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
    // implementation("com.aliyun.dpa:oss-android-sdk:2.9.13")
    // Sensor game (rarely needed; also Builder.setSensorGameApiKey):
    // implementation(files("libs/ext/sdk-sensorgame-v1.0.5.aar"))
    // WeChat Pay certification (rarely needed; also Builder.setSupportWeChatPay):
    // implementation("com.artillery.pay:paycertification:leadingSmart_1.0.54")
    // Nordic chip DFU (very old devices, roughly 2024-era hardware):
    // implementation("no.nordicsemi.android:dfu:2.2.2")

    // ========== FlyWear (WKDeviceType.FLY_WEAR) ==========
    ///////////// Required ///////////
    implementation(files("libs/sdk-flywear-v3.0.2.7.aar"))
    implementation(files("libs/sdk-flywear-adapter-v3.0.2.7.aar"))
    implementation(files("libs/ext/sdk-persimwear-v1.0.5.aar"))
    implementation("com.belerweb:pinyin4j:2.5.0")

    // ========== ShenJu (WKDeviceType.SHEN_JU) ==========
    ///////////// Required ///////////
    implementation(files("libs/sdk-shenju-base-v3.0.2.7.aar"))
    implementation(files("libs/sdk-shenju-core-v3.0.2.7.aar"))
    implementation(files("libs/sdk-shenju-opencv-v3.0.2.7.aar"))
    implementation(files("libs/sdk-shenju-adapter-v3.0.2.7.aar"))
    implementation("com.google.code.gson:gson:2.10.1")

    // ========== ProtoTb (WKDeviceType.PROTO_TB) ==========
    ///////////// Required ///////////
    implementation(files("libs/sdk-prototb-adapter-v3.0.2.7.aar"))
    implementation(files("libs/sdk-fitcloud-v3.0.2.7.aar"))
    implementation(files("libs/sdk-shenju-opencv-v3.0.2.7.aar"))
    implementation("com.google.protobuf:protobuf-javalite:4.33.1")
    implementation("com.belerweb:pinyin4j:2.5.0")
    implementation("org.apache.commons:commons-compress:1.24.0")
    implementation("pl.droidsonroids.gif:android-gif-drawable:1.2.32")
    implementation("com.topstep.tool:lib-abpartool:1.0.1")
    ///////////// Optional ///////////
    // Custom video dial (other FFmpegKit builds OK — do not mix multiple FFmpeg native libs):
    // implementation("com.antonkarpenko:ffmpeg-kit-min-gpl:2.1.0")

    // ========== AbMate (WKDeviceType.AB_MATE) ==========
    ///////////// Required ///////////
    implementation(files("libs/sdk-abmate-adapter-v3.0.2.7.aar"))
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("org.nanohttpd:nanohttpd:2.3.1")
    implementation("androidx.lifecycle:lifecycle-process:2.5.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation(files("libs/ext/sdk-jl-ota-v1.11.0.aar"))
    implementation("com.artillery.pay:paycertification:leadingSmart_1.0.54")
}
```

### 2.3 Initialize WKWearKit

```kotlin
fun wearKitInit(application: Application): WKWearKit {
    // 1. Logging (SDK logs via Timber)
    Timber.plant(Timber.DebugTree())

    // 2. Register platform Builders (add only the platforms you support)
    val builders = ArrayList<WKWearKit.Builder>()
    val processLifecycleObserver = MyProcessLifecycleManager().also {
        application.registerActivityLifecycleCallbacks(it)  // Foreground / background awareness
    }
    val rxBleClient = RxBleClient.create(application)
    builders.add(WKFitCloudKit.Builder(application, processLifecycleObserver, rxBleClient))
    builders.add(WKProtoTbKit.Builder(application, processLifecycleObserver, rxBleClient))
    // ...

    val wearKit = buildWKWearKit(builders)

    // 3. RxJava global error handler (ignore known undeliverable SDK exceptions to avoid crashes)
    val ignoreExceptions = HashSet<Class<out Throwable>>()
    ignoreExceptions.addAll(wearKit.rxJavaPluginsIgnoreExceptions())
    RxJavaPlugins.setErrorHandler(RxJavaPluginsErrorHandler(ignoreExceptions))
    return wearKit
}
```

`MyProcessLifecycleManager`: extend `ProcessLifecycleManager` and implement `ActivityLifecycleCallbacks`. The sample calls `setForeground(true/false)` in `onActivityResumed` (foreground when `startCount==1`) / `onActivityStopped` (background)—the SDK uses this to switch scan modes. The class name is up to you; see sample `WearKitInit`.

### 2.4 Scan for devices

```kotlin
wearKit.scanner.scan(
    type = WKDeviceType.FIT_CLOUD,   // Platform to scan
    durationSeconds = 120,           // Scan duration in seconds
    checkLocationService = true,     // Check whether location services are enabled before scanning
    acceptEmptyName = false,         // Whether to accept devices with empty names
).subscribe({ result ->
    val device = result.device  // BluetoothDevice
    val name = result.name
    val rssi = result.rssi
}, { err -> ... })
```

### 2.5 Connect a device

```kotlin
// Recommended: connect with user info
wearKit.connector.connect(
    type = WKDeviceType.FIT_CLOUD,
    address = deviceAddress,
    authMode = WKAuthMode.BIND,   // Bind mode; use LOGIN when you have a login session
    authCode = null,              // Auth code from the watch QR code (pass only when obtained reliably)
    userId = "your_user_id",
    sex = true, age = 22, height = 175f, weight = 65f,
)
// When you only have auth info and defer user info, use the other overload (type/address/authMode/authCode/userId);
// then call setUserInfo / syncUserInfo after connect.

// Observe connector state
wearKit.connector.observeConnectorState().subscribe { state ->
    // WKConnectorState: DISCONNECTED / CONNECTING / PRE_CONNECTED / CONNECTED ...
}
// Observe connector errors
wearKit.connector.observeConnectorError().subscribe { err -> ... }
```

Other connector methods:
| Method | Purpose |
|---|---|
| `setUserInfo(sex, age, height, weight)` | Update user info after connect (force-overwrite device values) |
| `syncUserInfo(WKUserInfo)` | Sync user info by timestamp comparison (returns device values if the device is newer) |
| `close()` | Disconnect and clear the currently held device |
| `clear(removeBond)` | Clear device auth (some devices factory-reset) + unbind + disconnect |
| `reconnect()` / `disconnect()` | Reconnect / disconnect (keep device reference for later auto-reconnect) |
| `setAutoCreateBond(enabled)` / `createBond()` / `removeBond()` | Auto / manual bonding control |
| `setAutoReconnectMode(mode)` / `setAutoReconnectInterval(minSeconds)` | Auto-reconnect policy |
| `isBindOrLogin()` | Whether current mode is BIND (`true` = BIND) |
| `getDisconnectedReason()` | Disconnect reason enum |
| `observeDeviceCanBond()` | Observe bondable-device changes |

### 2.6 Use abilities

Unified entry points are `wearKit.xxxAbility`. Each ability usually has `compat` (capability checks—check before calling):

```kotlin
if (!wearKit.alarmAbility.compat.isSupport()) return  // Check device support first
wearKit.alarmAbility.requestAlarms().subscribe { alarms -> ... }
```

Convention: **all methods are RxJava3 reactive**; calling when `compat.isSupport() == false` throws `WKUnsupportedException`; get methods that can return a “default value” return an invalid default when unsupported.

<a id="2-7-permission"></a>

### 2.7 Permissions

Declare the Manifest entries below for scan and connect. `BLUETOOTH_SCAN` uses `neverForLocation`, so API 31+ scanning does not request location. On API 23–30, still request `ACCESS_COARSE_LOCATION` and `ACCESS_FINE_LOCATION` at runtime.

```xml
<!-- required for API 18 - 30 -->
<uses-permission
    android:name="android.permission.BLUETOOTH"
    android:maxSdkVersion="30" />
<uses-permission
    android:name="android.permission.BLUETOOTH_ADMIN"
    android:maxSdkVersion="30" />
<!-- required for API 23 - 30 -->
<uses-permission-sdk-23 android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission-sdk-23 android:name="android.permission.ACCESS_FINE_LOCATION" />
<!-- API 31+ -->
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission
    android:name="android.permission.BLUETOOTH_SCAN"
    android:usesPermissionFlags="neverForLocation"
    tools:targetApi="s" />
```

After the Manifest matches above, request runtime permissions with `RxBleClient.getRecommendedScanRuntimePermissions`:

| API level | Bluetooth runtime permissions |
|---|---|
| 23–30 | `ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION` |
| 31+ | `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT` |

The WearKit sample’s `ACCESS_COARSE_LOCATION` / `ACCESS_FINE_LOCATION` setup differs slightly because the sample also uses location for EPO / continuous device positioning.

---

## 3. Ability Interface Overview

| Category | Ability | One-line description |
|---|---|---|
| Base | `deviceAbility` | Power on/off / reset + core data-sync entry |
| Base | `timeAbility` | Device time set / sync |
| Base | `languageAbility` | Device language set / query |
| Base | `batteryAbility` | Battery query / observe |
| Base | `alarmAbility` | Alarm query / set / observe |
| Base | `contactsAbility` | Common / emergency contacts, contact avatars, SOS |
| Base | `cameraAbility` | Remote capture, camera preview stream push |
| Base | `finderAbility` | Find watch / find phone |
| Base | `weatherAbility` | Weather push (today + next days/hours) |
| Base | `notificationAbility` | App notification / incoming-call push, hangup ack |
| Base | `mediaAbility` | Media control state sync (play/pause/track info) |
| Base | `remindAbility` | Drink water / sedentary / medicine / custom reminders |
| Config | `functionAbility` | Wear hand / time format / weather switch and other simple flags |
| Config | `unitAbility` | Metric / imperial units |
| Config | `dndAbility` | Do-not-disturb periods |
| Config | `raiseWakeupAbility` | Raise-to-wake |
| Config | `womenHealthAbility` | Women’s health (period reminders, etc.) |
| Data | `activityAbility` | Activity goals; some devices push today’s activity totals |
| Data | `heartRateAbility` | Heart-rate realtime measure / monitor / alarm / HRV config |
| Data | `bloodOxygenAbility` | SpO2 measure / monitor |
| Data | `pressureAbility` | Stress measure / monitor |
| Data | `bloodPressureAbility` | Blood-pressure measure / monitor |
| Data | `temperatureAbility` | Body-temperature measure / monitor |
| Data | `sportAbility` | Device sport state (in sport / type) |
| File | `musicAbility` | Music file push (.mp3 only) |
| File | `eBookAbility` | E-book push (text/plain) |
| File | `albumAbility` | Image push |
| File | `otaAbility` | Firmware / UI OTA |
| File | `sportUIAbility` | Sport UI asset push / replace |
| File | `logAbility` | Pull device logs |
| File | `locationMapAbility` | Location push / EPO / offline maps |
| File | `fileAbility` | Recording file list / pull / delete (device-produced files) |
| Dial | `dialAbility` | Dial query / install / uninstall / select |
| Dial | `dialStyleAbility` | Custom dial (background / style / position / color / danmu) |
| Special | `aiAbility` | AI health guidance / ChatGPT Q&A / TTS |
| Special | `businessCardAbility` | Electronic business card |
| Special | `paymentCodeAbility` | Payment code |
| Special | `lockAbility` | Screen lock / game lock |
| Special | `muslimAbility` | Muslim (prayer / Hijri / Qibla) |
| Special | `worldClockAbility` | World clock |
| Speech AI | `speechAiAbility` | Speech AI sessions (chat / record / translate / ask / dial) |
| B2B | `b2b` | HSD (kids-watch scenarios) / Titan (QR) / UGreen (danmu & config) customizations |

Data sync: historical data (steps / heart rate / sleep / sport, etc.) goes through `deviceAbility.syncData()`. Results are delivered as `WKSyncData`, distinguished by `WKSyncData.Type` (ACTIVITY/HEART_RATE/SLEEP/SPORT/...).

---

## 4. Ability Details

### 4.1 Base abilities (base)

#### WKDeviceAbility — device management & data sync
- `shutdown()` power off / `reset()` factory reset / `reboot()` reboot
- `syncItem(type, start, end)` sync a single data type (deprecated; use syncData)
- `getSyncTypes()` supported sync data types
- `syncData(timeProvider, vararg types)` sync specified types in one call (**recommended**; efficient ordered sync internally)
- `syncData(timeProvider)` sync all types
- `observeSyncState()` / `isSyncing()` sync state
- `getDeviceInfo()` / `observeDeviceInfo(replay)` basic device info (model / firmware / serial, etc.)
- <a id="4-1-third-party-data"></a>`observeThirdPartyData()` / `sendThirdPartyData(data)` exchange data with device-side third-party features (current types in `WKThirdPartyData.Type`, e.g. `STAR_BURST`); when unsupported the observe stream emits nothing and send throws `WKUnsupportedException`
- `ISyncAbility.range`: start-time range control for data sync

#### WKTimeAbility — time
- `applySystemTime()` sync phone system time to the device
- `setTime(timestampMillis, zoneOffsetMillis)` set device time from UTC timestamp + zone offset
- `setTime(timestampMillis)` set using the system default time zone

#### WKLanguageAbility — language
- `setLanguage(languageType)` set device language (use `LanguageUtil.getLanguageType` / `getSystemLanguageType`)
- `requestLanguage()` query current device language
- `requestSupportLanguageList()` query supported languages (`compat.isSupportLanguageList()`)
- Language type mapping: see `LanguageUtil` in `sdk-base`

#### WKBatteryAbility — battery
- `requestBattery()` query battery once
- `observeBatteryChange()` observe battery changes (older devices without real push use polling—**unsubscribe promptly when the page is hidden**)

#### WKAlarmAbility — alarms
- `requestAlarms()` query device alarm list
- `setAlarms(list)` set alarms in full (null/empty = clear)
- `observeAlarmsChange()` observe device-side alarm changes
- `compat`: `getAlarmMaxNumber()` max alarms / `getLabelMaxBytes()` max label bytes / `isSupportType()` whether an alarm type is supported

#### WKContactsAbility — contacts
- `requestContactsCommon()` / `setContactsCommon(common)` common contacts (empty = clear)
- `requestContactsEmergency()` / `setContactsEmergency(emergency)` emergency contacts
- `setContactsImage(number, file)` / `deleteContactsImage(number)` / `requestContactsHasImage()` contact avatars
- `observeContactsChange()` device-side contact changes (callback only when the device itself changes them)
- `observeSOS()` device SOS requests (some devices)
- `compat`: `getContactsCommonMaxNumber()` / `getContactsEmergencyMaxNumber()` / `getContactsImageMaxNumber()`

<a id="4-1-camera"></a>

#### WKCameraAbility — remote camera & preview
- `setCameraStatus(open)` notify the device of App camera open/close
- `sendCameraMessage(message)` send capture / lens-switch commands (see `WKCameraMessage`)
- `observeCameraMessage()` device requests for photo / video
- `startPreview(quality)` start preview stream (push App camera frames to the device display)
- `updatePreview(type, data)` / `updatePreview(image: WKImageProxy)` send preview frames (WKImageProxy recommended; the former is `@Deprecated`)
- `stopPreview()` stop preview and release encoder
- `setCameraInfo(zoomRatioMin, zoomRatioMax)` tell the device the camera zoom range
- `compat`: `isSupportPreview()` / `isSupportVideo()` / `getPreviewSize()`

#### WKFinderAbility — find device
- `findWatch()` ring / vibrate the watch
- `stopFindWatch()` stop
- `foundPhone()` phone-side “phone found” reply
- `observeFinderMessage()` find messages (FIND_PHONE/STOP_FIND_PHONE/FOUND_WATCH)

#### WKWeatherAbility — weather
- `setWeather(city, today, futureDays, futureHours)` push today + forecast (days/hours must be contiguous; how many are shown depends on the watch)
- `compat.isSupport()`

#### WKNotificationAbility — notifications & calls
- `sendAppNotification(packageName, title, content, tickerText)` push App notifications; use constants in `CommonAppPackage` for `packageName` (WeChat/QQ/Facebook/WhatsApp/Messenger/...); **unsupported packages fall into the "Others" category**
- `sendTelephonyNotification(type, phoneNumber, name)` push telephony events (`WKTelephonyType`: ringing / rejected / missed / hung up / ...)
- `observeTelephonyHangup()` device-initiated hangup / send-SMS actions
- `replayTelephonyHangup(endCall, sendSms)` ack hangup / SMS result
- `getTelephonyConfig()` / `setTelephonyConfig(enabled)` / `observeTelephonyConfig()` master switch for call notifications
- `compat.isSupportAppNotification(packageName)` whether the package is recognized as its own category on the device

#### WKMediaAbility — media control
- `observeMediaMessage()` media control key messages from the device (play/pause/prev/next)
- `setMusicInfo(title, artist, duration)` sync current track info to the device
- `setMusicState(state, position, speed)` playback state (0 stop / 1 play / 2 pause) + position + speed

#### WKRemindAbility — reminders
- `requestReminds()` all reminders / `requestRemind(type)` single type (drink water / sedentary / medicine / custom)
- `setReminds(list)` full set (non-custom types are not deleted even if absent from the list)
- `addOrUpdateRemind(vararg reminds)` add/update
- `deleteRemind(vararg types)` delete custom reminders
- `compat`: `isSupport()` / `isSupportType(type)` / `isSupportField(type, field)` / `createCustomRemind(list)` / `getCustomRemindMaxNumber()`
- Reminder types: `WKRemind.Type.DrinkWater/Sedentary/TakeMedicine/Custom`; check field support with `isSupportField` (switch / period / interval / repeat, etc.)

### 4.2 Config abilities (config)

Common pattern: `getConfig() → WKXxxConfig` / `setConfig(config)` / `observeConfig(replay)` + `compat`.

- **WKFunctionAbility — function flags**: `getConfig/setConfig/observeConfig`; flags include `WEAR_HAND` (wear hand) / `TIME_FORMAT` (12/24h) / `WEATHER` / `HEALTH_ENHANCED`, etc.
- **WKUnitAbility — units**: get/set/`syncConfig` (timestamp comparison; returns device values if newer) / observe; when `compat.isSupportSplitMetric()` use `isLengthMetric + isWeightMetric`, otherwise `isMetric`
- **WKDndAbility — DND**: get/set/observe; `compat.isSupport()` / `isSupportTimeAcrossDays()` (periods spanning midnight, e.g. 21:00→8:00)
- **WKRaiseWakeupAbility — raise-to-wake**: get/set/observe; `compat.isSupport()` (unsupported on devices without a screen) / `isSupportPeriod()` (whether period fields are available)
- **WKWomenHealthAbility — women’s health**: get/set/observe; `compat.isSupport()` / `isSupportRemindFlags()` (period / ovulation remind flags)

### 4.3 Health measurement data abilities (data)

Five measurement types (heart rate / SpO2 / stress / blood pressure / temperature) share the **same structure**:

```kotlin
// Realtime manual measure: durationSeconds = duration; returns intermediate values — average or take the last
heartRateAbility.measureRealtime(30).subscribe { value -> ... }

// Auto-monitor config (switch + period + interval)
heartRateAbility.getMonitorConfig()
heartRateAbility.setMonitorConfig(config)
heartRateAbility.observeMonitorConfig(true)

// Heart rate / blood pressure also have alarm config
heartRateAbility.getAlarmConfig() / setAlarmConfig(config)

// compat checks
heartRateAbility.compat.isSupport()
heartRateAbility.compat.isSupportMeasure()        // Manual measure supported?
heartRateAbility.compat.isSupportMonitorConfig()  // Auto-monitor config supported?
```

Per-ability differences:
- **WKHeartRateAbility**: `measureRealtime(duration) : Observable<Int>`; `getMaxThreshold()/setMaxThreshold()` upper limit; `getHRVConfig()/setHRVConfig()` HRV monitoring; compat includes `isSupportMinValueConfig/isSupportTimePeriod/isSupportTimeInterval/isSupportTimeAcrossDays/isSupportHRV`
- **WKBloodOxygenAbility / WKPressureAbility**: measure + monitor config
- **WKBloodPressureAbility**: `measureRealtime : Observable<WKBloodPressureItem>` (systolic / diastolic / pulse)
- **WKTemperatureAbility**: `measureRealtime : Observable<WKTemperatureItem>`
- **WKActivityAbility — activity goals / today’s activity push**:
  - Goals: `getGoalConfig()/setGoalConfig()/syncGoalConfig()/observeGoalConfig()`; `compat.getActivityAttributes()` (which metrics the device activity page shows, and thus which goals can be set); `compat.isSupportDisabledReminds()` (goal-reached remind can be turned off)
  - Today’s activity totals push (UI refresh): `observeActivityChange() : Observable<WKActivityItem>`; This is device-pushed, not a pull API; for historical or once-off totals use chapter 5 `syncData`. `WKActivityItem` units: steps, distance (meters), calories (kcal), activity/sport duration (seconds), activity count
  ```kotlin
  activityAbility.observeActivityChange()
      .subscribe { item -> /* steps / distance / calories / duration ... */ }
  ```
- **WKSportAbility — sport state**: `requestSportState()/observeSportState()` (in sport / sport type); `compat.isSupportSportState()`

### 4.4 File & push abilities (file)

Common pattern: `requestDirSpace()` (space) / `requestFiles()` (list) / `addFile(...)` (push with progress 0–100) / `deleteFile(path)` / `observeFileChange()` + `compat.isSupport()`.

- **WKMusicAbility — music**: `.mp3` only (`audio/mpeg`); other formats must be transcoded by the app (e.g. ffmpeg-kit-min); `addFile(uri, artist?)`; compat includes `isSupportRequest()/isSupportDelete()`
- **WKEBookAbility — e-book**: `text/plain` only, default UTF-8, optional `charset`; `addFile(uri, charset)`
- **WKAlbumAbility — images**: `addFile(uri)`; `compat.isSupportFolders()` (folder grouping)
- **WKOtaAbility — OTA**: `ota(file) : Observable<Int>` firmware / UI upgrade (progress 0–100)
- **WKSportUIAbility — sport UI**: `requestCloudSportUIResources()` (cloud resources) / `requestSupportSports()` / `requestSports()` / `requestSpaces()` / `install(file, spaceIndex?)` push custom sport icons/UI
- **WKLogAbility — logs**: `pull() : Observable<ProgressResult<File>>` pull device logs (missing result file = device has no logs); `compat.isSupport()`
- **WKLocationMapAbility — location / EPO / offline maps**:
  - `setLocation(location)` push one location update (for map dial pins; update as soon as a fix is available)
  - `setLocation(provider)` register a location Provider (after obtaining the kit, before connect). Besides `requestLocation()`, Provider may implement `observeLocation(options)` for continuous location sessions started by the device (subscribe = start, dispose = stop; default implementation reports `WKLocationException.ERROR_UNSUPPORTED`)
  - `updateEpo(force)` / `updateEpo(force, provider)` update device EPO (faster fixes; files from the SDK’s built-in server)
  - `requestEpoTime()` / `clearEpo()` for debugging
  - `listOfflineMap()` offline map name list; `listOfflineMapInfos()` name + size in bytes (`-1` if unknown)
  - `requestOfflineMapSpace()` map storage total / remaining bytes (`-1` if unknown); before appending a map, compare actual file size with live remaining space
  - `deleteOfflineMap(name)` delete by name
  - `downloadOfflineMap(lat, lng, radius, file)` download offline map
  - `pushOfflineMap(file, name)` push offline map
  - `setOfflineMap(lat, lng, radius, name)` download and push in one step
  - `compat.isSupportOfflineMap()/isSupportEpo()`
- **WKFileAbility — recording file management** (note: not music/album push; this is **pulling files produced by the device**):
  - `requestFilesCount()/requestFiles()` recording file list (list does not require WiFi)
  - `pullFiles(saveDir)` **pull files to the phone**: after each file is saved successfully, **automatically delete the device original** (delete failures ignored); when WiFi is required see `compat.isRequireWifi()`
  - `deleteFile(path)` delete a device file
  - `observeFileChange()` observe recording file add/remove
  - `setOpusRecordBitrate(bitrate)` set recording storage bitrate (supported by some adapters)
  - `rtsp(quality)` establish WiFi and return an RTSP stream URL (live view); `compat.isSupportRtsp()`
  - `compat.isSupport()/isRequireWifi()`

### 4.5 Dial abilities (dial)

**WKDialAbility — basic dial management**
- `requestDials()` installed dial list
- `requestSpaces()` installable spaces (check remaining space before install; multiple spaces may be available)
- `select(dialId)` set current dial
- `install(dialId, file, spaceIndex?)` install dial (throws `ERROR_DEVICE_STORAGE` when space/count is insufficient)
- `uninstall(dialId)` / `uninstall(dialId, spaceIndex)` uninstall
- `observeDialsChange()` dial change observe
- `compat`: `isSupport()` / `getDialMaxNumber()` / `isSupportUninstall(type)`

**WKDialStyleAbility — custom dials (complex; see sample `ui/dial`)**
- `requestCloudDialStyleResources()` cloud style resources
- `requestConstraint(resources)` custom constraints for a resource (background count / styles / positions, etc.)
- `createCustom(constraint, input)` build a custom dial from constraint + input → `CreateOutput(dialFile, previewFile, dialId)`
- `CreateInput` factory methods (by background type):
  - `CreateInput.base(background image uri, style index, position index, color tint...)` single-image background
  - `CreateInput.multiple(multiple background images, carousel interval...)` multi-image auto carousel
  - `CreateInput.video(video uri, crop region / start-end time...)` video background
  - `CreateInput.danMu(DanMuConfig, background color...)` danmu background (danmu strips + background color)
  - `CreateInput.customDialId` optional: only effective for FIT_CLOUD GUI dials
- Danmu coordinate helper `DanMuCoord`: MeasureSpec-like coordinate encoder (`absolute(px)` / `relative(anchor, offset)` / `resolveX/Y(spec, containerW/H, contentW/H)`)
- `installCustom(file, spaceIndex?)` install
- `compat`: `isSupport()` / `isSupportVideoBackground()` / `getVideoDuration()/getVideoMaxDurationMillis()` / `isSupportMultipleBackground()` / `isSupportDanMuBackground()` / `getQualityLevels()`
- Danmu models: `DanMuItem(imageUri, imageX, imageY, walkSpeed, animUri, animX, animY, ltr)`, `DanMuConfig(items, backgroundColor)`

> Custom dial protocol details (bin packing for background / style / position / color tint) involve internal DialView/DialWriter in `sdk-fitcloud`. Contact TopStep when integrating.

### 4.6 Special abilities (special)

- **WKAIAbility**: `initAi(initResult)` AI init; `generateAIHealthGuidance(callback)` AI health guidance (copy from today’s sleep/HRV/activity; overloads for passed-in data or auto DB lookup); `WKChatGptCallback.answer(text)` ChatGPT Q&A; `tts(path)` TTS playback
- **WKBusinessCardAbility — business card**: `request()` query; `set(map)` set (fields in `WKBusinessCard`); `compat.isSupport()`
- **WKPaymentCodeAbility — payment code**: `request()/set(map)` same pattern; `compat.isSupport()`
- **WKLockAbility — locks**: `setScreenLock(lock)/requestScreenLock()` screen lock; `setGameLock(lock)` game lock (anti-addiction periods); `compat.isSupportScreenLock()/isSupportGameLock()/getPasswordLength()`
- **WKMuslimAbility — Muslim** (formerly `prayerAbility`, renamed): `requestPrayerSwitch()/setPrayer(info)/observePrayerSwitch()` prayer; `setHijri(current, holidays)` Hijri calendar; `setQibla(degrees, distance)` Qibla; `observeMuslimMessage()`; `compat.isSupportPrayer()/isSupportQibla()/isSupportHijri()`
- **WKWorldClockAbility — world clock**: `setClocks(list)/requestClocks()`; `compat.isSupport()/getWorldClockMaxNumber()`

<a id="4-7-speech-ai"></a>

### 4.7 Speech AI abilities (speech)

**WKSpeechAiAbility** is the speech-AI core (including platforms that implement this ability such as FitCloud / ProtoTb / AbMate). Overall model:

```
Device (earbuds/watch) mic audio → WKSpeechSession.audio() → App runs ASR/LLM/translate
App results → Chat.sendTextQuestion/sendTextAnswer etc. push back to the device display
Device TTS playback → Player.start/write pushes PCM to the device speaker
```

**Prerequisites**
- `isSupport()` whether the device supports speech AI
- `getSuggestAiSDKs()` recommended AI SDKs (internal; **customer apps choosing their own AI SDK can ignore**)
- `setAiSDKInitResult(success, aiSDK?)` after connect, tell the device the App-side AI SDK init result; `aiSDK` may be null (when not using the `WKSpeechAiSDK` enum). Old parameter order `(aiSDK, success)` is `@Deprecated`

**Session model**
- Scene `WKSpeechSession.Scene`: `CHAT(0)` AI chat / `RECORD(1)` recording / `CALL_RECORD(2)` call recording / `TRANSLATE(3)` simultaneous interpretation / `TAXI(4)` / `DIAL(5)` AI dial / `ASK(6)` Q&A / `CHAT_TRANSLATE_SELF(7)` chat-translate self / `CHAT_TRANSLATE_PEER(8)` chat-translate peer
- Audio source `Source`: `PHONE_MIC` (phone mic) / `DEVICE_CMD` (device command-channel audio) / `DEVICE_SCO` (Bluetooth SCO)
- Origin `Origin`: `DEVICE` (device-initiated—**must keep observeDeviceSession subscribed**) / `APP` (App-initiated via createAppSession)
- Key methods:
  - `observeDeviceSession()` observe device-initiated sessions (**must stay subscribed**, or the device times out and releases)
  - `createAppSession(scene, source?)` App-initiated session (returns null if a session is active / scene unsupported / device not connected on DEVICE_CMD)
  - `activeSession()` current active session
  - `isSupportAppScene(scene)` / `isSupportDeviceScene(scene)` scene support checks
- `WKSpeechSession.audio()` subscribe to audio: `format` = `PCM(16k/mono/16bit)` or `OPUS(frameSize)`; onComplete = normal end; onError (`Exception`: ERROR_BATTERY low battery / ERROR_INCOMING incoming call / ERROR_DISCONNECTED disconnect, etc.)
- `release(reason)` end session (Reason: NONE/ERROR_STORAGE/ERROR_PERMISSION/UNKNOWN)

**Messages (observeMessage)**
- `SCENE_EXIT`(10001): device exits a scene (data=Scene) → App should end that scene’s logic; **SDK ends any still-active audio session before emitting this**; duplicate receipts are idempotent
- `TRANSLATE_PLAYER_STATE`(302): translate TTS playback state change (START/STOP/PAUSE/RESUME)
- `DIAL_GENERATE_IMAGE`(501)/`DIAL_GENERATE_DIAL`(502): AI dial flow confirmation
- `ASK_GENERATE_ANSWER`(601): ask scene—device confirms it received the question
- Older devices may not send SCENE_EXIT → fall back to audio stream completion

**Chat sub-ability (push back in CHAT scenes)**
- `isSupportText()` whether the device can show text (if false, send audio only)
- `sendTextQuestion(text, isComplete)` push ASR question (**full-sentence snapshot**, not incremental)
- `sendTextAnswer(text, isComplete)` push LLM answer
- `sendError(type, text)` error report (`WKSpeechAiError`)

**Record sub-ability**
- `getLang()` device-specified ASR language byte code (null = unspecified/unsupported; fall back to system language)
- `isSupportText()` / `sendTextSource(text, isComplete)` push recording ASR text to the device (full-sentence snapshot, not incremental)
- `isSupportPause()` when true, `pause()` / `resume()` are available (idempotent; local capture pauses/resumes immediately; Completable only means the device was notified)
- `isPaused()` / `getDurationMs()` may be polled on an active Record session (even without pause support; duration is accumulated recording ms excluding pause, not wall-clock)

**Translate sub-ability (translate / chat-translate push-back)**
- `getLang()` language pair set by the device (`WKTranslateLang`; fall back to system when null)
- `sendTextSource(text, isComplete)` source-text snapshot
- `sendTextTarget(text, isComplete)` target-text snapshot
- `sendTtsReady()` tell the device TTS is ready
- `startChatTranslate(mode)` / `stopChatTranslate()` enter/exit chat-translate mode (`WKChatTranslateMode`: FACE_TO_FACE / PRIVATE / PORTABLE)—while active the device may open SELF/PEER sessions

**Dial sub-ability (AI dial)**
- `sendText(text, isComplete)` final ASR text (sent only when isComplete=true)
- `sendImage(file)` push generated image for device preview (returns transfer progress)
- Flow: ASR text → wait DIAL_GENERATE_IMAGE → generate image sendImage → wait DIAL_GENERATE_DIAL → install via normal custom dial

**Ask sub-ability (Q&A)**
- `sendTextQuestion/sendTextAnswer/sendError`, same as Chat but **requires device confirmation** (ASK_GENERATE_ANSWER)

**Player sub-ability (PCM to device speaker)**
- `isSupport(scene)` whether the scene supports audio push
- `start(sampleRate, channels)` start (sample rates limited to 8k/12k/16k/24k/48k; channels 1/2)
- `write(pcm, isFinal)` blocking PCM write (isFinal flushes and waits until playback finishes)
- `stop()` end

> App-side samples for each scene (record / translate / chat-translate / AI chat) are under WearKit-SDK-Android sample `ui/ai` (RecordHandler/TranslateHandler/ChatTranslateHandler/ChatHandler, etc.). Wiring audio to a third-party AI SDK (ASR/LLM) is the customer’s responsibility.

### 4.8 B2B custom abilities (b2b)

For specific customer projects only; availability depends on device / commercial config:
- **B2b**: entry points `hsdAbility` + `titanAbility` + `ugreenAbility`
- **HsdAbility (parental control / education custom)**: `setIceLabels` (quick-contact labels) / `setParentalMode` / `requestParentalMode` (parental mode) / `setClassRoomMode` / `requestClassRoomMode` (classroom mode) / `setTasks` / `requestTasks` / `exchangeTaskReward` (tasks & rewards) / `setHabits` / `requestHabits` (habits) / `requestAppUsageInfo` / `requestGameUsageInfo` / `resetUsageInfo` (usage stats) / `setGameRankingTrends` (game ranking); compat has matching isSupport series + `getTaskMaxNumber()/getHabitMaxNumber()`
- **TitanAbility (QR codes)**: `requestQrCode()` / `setQrCode(map)`; `compat.isSupportQrCode()`
- **UGreenAbility**: `clearDanMu(type)` / `addDanMu(items)` (empty list sends no packet); `requestConfig()` / `setConfig(UGreenConfig)`; `compat.isSupportDanMu()`

---

## 5. Data Sync (Important)

Historical health / sport data is synced via `deviceAbility` using a **time provider** `WKSyncTimeProvider`:

```kotlin
val timeProvider = object : WKSyncTimeProvider {
    override fun getRange(type: Int): WKTimestampRange {
        // Return the start time for this type (e.g. last sync time) through now
    }
}
deviceAbility.syncData(timeProvider, WKSyncData.Type.ACTIVITY, WKSyncData.Type.HEART_RATE, ...)
    .subscribe { syncData ->  // WKSyncData: dispatch by type
        when (syncData.type) {
            WKSyncData.Type.ACTIVITY -> ...
            WKSyncData.Type.HEART_RATE -> ...
        }
    }
```

- `getSyncTypes()` returns types the device supports; `observeSyncState()` observes sync state
- Model classes: `WKSleepItem/WKSleepSegment/WKActivityItem/WKHRVDaily/WKSportRecord...`
- In `sdk-apis`, `SleepAlgorithm` / `SleepCalcSegment` can compute sleep stages
- Realtime today’s activity refresh uses `activityAbility.observeActivityChange()` (see 4.3), not this historical sync

---

## 6. Notification Categories (CommonAppPackage)

`WKNotificationAbility.compat.isSupportAppNotification(packageName)` checks whether an App is recognized as a **distinct category** on the device (shows the matching icon). Unsupported packages are filed under "Others" after push.

Common package constants (`CommonAppPackage` in sdk-base): SMS/EMAIL/QQ/WECHAT/FACEBOOK/TWITTER/LINKEDIN/INSTAGRAM/WHATS_APP/LINE/FACEBOOK_MESSENGER/KAKAO_TALK/MICROSOFT_TEAMS/TELEGRAM/VIBER/SNAPCHAT/HIKE/YOUTUBE/.... Categories visible on the notification-switch page are decided by **notification support capabilities from device firmware**. If a category is not enabled, it does not appear in the App’s main list but can still be selected via the “More apps” generic channel (see FAQ item 3).

---

## 7. AbMate Glasses/Earbuds Platform

> AbMate is **ZhongKe AI glasses / earbuds** (with camera, photo/video, AI voice). Integration differs from band/watch platforms: include `sdk-abmate-adapter` (device type `WKDeviceType.AB_MATE`). Protocol details follow the public APIs and sample; contact TopStep for private protocol docs when needed.

**AbMate SDK exposes two API levels**:

| Level | Entry | Use when |
|---|---|---|
| **WearKit unified API** | `WKWearKit` / `AbMateSDK` (scanner/connector and `WK*Ability`) | Scan, connect, basic device info, notifications, music, OTA, logs, file pull, dials, **speech AI (`speechAiAbility`)**, and other shared abilities |
| **Glasses business API** | `AbMateSDK.abMateManager` (`ABMateManager`) | Photo/video/on-device audio recording, display control, EQ/ANC/keys, RTSP live, WiFi file transfer, SK speaker binding, and other **glasses-specific** features |

> When integrating AbMate: complete scan/connect with `scanner`/`connector`; use shared abilities (including chat/record/translate) via `WK*Ability`; call only glasses-specific APIs through `AbMateSDK.abMateManager` (`wearKit.getRawSDK() as AbMateSDK`).
>
> **For speech AI, use [4.7 Speech AI abilities](#4-7-speech-ai)** (`wearKit.speechAiAbility`). AbMate implements this ability—**do not** use legacy `abMateManager` APIs such as `sendAIChatState` / `setAIRecordState` / `aIRecordData` / `stateAIChat`. See sample `ui/ai`.

### 7.1 Build & connect

```kotlin
// 1. Register platform Builder (same pattern as band/watch; deviceType is AB_MATE)
builders.add(
    WKAbMateKit.Builder(application, processLifecycleObserver, rxBleClient)
        .setAutoSetTime(true)      // Auto sync time after connect
        .setAutoSetLanguage(false) // Whether to auto sync language after connect
)

// 2. After connect, obtain glasses business APIs (only for glasses-specific features; speech AI uses wearKit.speechAiAbility)
val abMateSDK = wearKit.getRawSDK() as AbMateSDK
val manager = abMateSDK.abMateManager  // ABMateManager: glasses business entry
val repo = manager.getRepository()     // ABMateDeviceRepository: state/data LiveData
```

- Device-family constants (company IDs differ by product; filter on scan/connect as needed): `COMPANY_ID_ZG` (glasses) / `COMPANY_ID_ZE` (earbuds) / `COMPANY_ID_ZS` / `COMPANY_ID_ZM`
- Prefer the unified third-party data APIs: `deviceAbility.observeThirdPartyData()` / `sendThirdPartyData` (e.g. `WKThirdPartyData.Type.STAR_BURST`, see 4.1). Payment can still use `AbMateSDK` `receiveArtilleryPayData` / `sendArtilleryPayData` (names follow existing SDK naming).

### 7.2 Glasses business abilities & usage (ABMateManager, by scenario)

> The following glasses-specific abilities go through `abMateManager`. Callbacks are uniformly `onSuccess`/`onFail`. Device state and data streams are observed via LiveData on `ABMateDeviceRepository`. Speech AI is not listed here—see 4.7.

#### 7.2.1 Photo / video / audio recording (camera media)

> The camera co-processor starts from **powered-off** into photo/video/audio states. After use, App should `turnOffCamera()` to save power (idle timeout also auto-powers off, but file-transfer mode does not).

```kotlin
// Photo (mode 0=camera mode, store on device; 1=AI mode, transfer to phone)
manager.turnOnCamera(1 /*AI mode: capture 320x240 JPEG and send over Bluetooth*/, { /*photo result*/ }, { /*fail*/ })

// AI image-recognition mode: photo fragments return via repo.aiPicPackage
//   status==1 transferring (assemble picFlow) / status==0 done (assemble full JPEG) / 0xFE storage full / 0xFF Bluetooth error
//   Pass the assembled image to your AI vision capability

// Video (auto-stops at 5 minutes max, or turnOffCamera(); result stored on device)
manager.startVideoRecording(onSuccess = {}, onFail = {})
// Audio recording (auto-stops at 5 hours max)
manager.startAudioRecording(onSuccess = {}, onFail = {})

// Live camera view (RTSP) — via unified WKFileAbility:
// abMateSDK.fileAbility.rtsp(quality) → Observable<URL>; start your own player with the URL

// Stop all camera activity and power off
manager.turnOffCamera(onSuccess = {}, onFail = {})

// Duration limits (Allwinner: video 1–12 min, audio 1–240 min; 0 = unlimited)
manager.setLimitTime(0 /*video*/, 5 /*minutes*/, {}, {})
manager.setLimitTime(1 /*audio*/, 60, {}, {})

// Media management
manager.getMediaFileCount()          // File count
manager.getStorageSpaceInfo()        // Remaining space (Allwinner: camera internal storage)
manager.formatStorageSpace()         // Format
manager.deleteGlassFile(name)        // Delete a file
manager.getCoprocessorVersion()      // Co-processor version/model
manager.getGlassWorkState()          // Work state (11=recording video 12=recording audio 9=taking photo…)
```

#### 7.2.2 Glasses display & basic config

```kotlin
// Text display (teleprompter / scrolling subtitle scenarios)
manager.sendWordRequest(text) { }    // Push text (UTF-8; filter Emoji / code points > 0xFFFF)
manager.setWordSpeedRequest(speed) { } // Scroll speed; queryWordSpeedRequest for current
manager.controlWordRequest(isStart) { } // Start/stop scroll

// Screen control
manager.switchScreen(mode) { }       // Display on/off (0/1)
manager.setGlassResolution(level) { } // Resolution (levels 0–3: 240p/320p/480p/640p)
manager.setGlassVolume(mode, volume) { } // Volume (mode 0=system 1=media 2=call)

// Time / language
manager.setGlassTime()               // Sync phone time to glasses
manager.setAbMateLanguage(code)      // Glasses UI language

// Landscape/portrait video query (some firmwares; older Allwinner always landscape) — via deviceCommManager
```

#### 7.2.3 Audio / ANC / keys

```kotlin
manager.setEqualizer(mode, gains) { }  // EQ (mode: see PresetEqSetting)
manager.getEq()                        // Query current
manager.setEarphoneMode(mode)          // ANC mode
manager.setDetection(true) { }         // In-ear wear detection switch
manager.setEarOperation(keyType, keyFunction) { } // Custom key functions
manager.requireCallState()             // Query call state (device behavior on incoming calls depends on this)
manager.requireMediaType()             // Query media playback state
manager.requirePower()                 // Battery (returned in onSuccess)
```

#### 7.2.4 Connect / bind / reset / find

```kotlin
manager.setWorkMode(mode)              // 0 normal / 1 game (low-latency) mode
manager.findEarPhone(mode) / stopFindEarPhone(mode)  // Find device (or use finderAbility)
manager.earPhoneReset { /* after success you may connector.clear to unbind */ }  // Factory reset
manager.deviceShutDown()               // Power off
// Bind / reconnect / unbind: use scanner/connector authCode flow
```

#### 7.2.5 SK smart speaker (earbud projects that support SK)

```kotlin
manager.sendBindSk() / reBindSk() / unBindSk()   // Bind / reconnect / unbind SK speaker
manager.sendAppInfraredCode(code)                 // Send IR code
manager.sendInfraredCodeStudy(mode)               // IR learning on/off
manager.sendAppRecordState(state)                 // Speaker recording on/off
// Wake word / record duration / volume for the speaker: see SK speaker protocol docs
```

#### 7.2.6 Main LiveData on ABMateDeviceRepository (for UI)

- Device info: `devicePower`/`deviceName`/`deviceFirmwareVersion`/`glassSn`/`deviceProductColor`/`glassDeviceModel`/`supportLanguage`/`deviceIsTws`/`deviceTwsConnected`
- Audio state: `deviceVolume`/`devicePlayState`/`deviceAncMode`/`deviceEqSetting`/`deviceKeySettings`/`deviceWorkMode`
- Camera / media: `aiPicPackage` (photo fragments `AIPicPackage`) / `glassTakePhoto` / `videoLimitTime` / `audioLimitTime` / `isSupportAudio` / `isSupportOpus`

> Speech AI sessions, audio uplink, and text push-back use `wearKit.speechAiAbility` (see 4.7). ASR/LLM/TTS cloud services are integrated by the customer.

---

## 8. FAQ & Tips

1. **Ability does nothing**: check `compat.isSupport()` first. Judge all `isSupport*` methods before calling; unsupported calls throw `WKUnsupportedException`.
2. **RxJava crashes**: you must set `RxJavaPlugins.setErrorHandler` as in 2.3 and ignore the exception set from `wearKit.rxJavaPluginsIgnoreExceptions()`.
3. **Missing notification categories** (e.g. Messenger/WhatsApp absent from the main list): if an App’s notification switch is missing from the main list, the category is usually **not enabled in the notification support bitmap from device firmware** (device capability config, not an App bug). You can still enable that App via the “More apps” generic channel.
4. **Slow data sync**: use `syncData(timeProvider, vararg types)` to sync multiple types at once; avoid calling `syncItem` one by one.
5. **Permissions**: for scan and connect, request Bluetooth permissions as in [2.7 Permissions](#2-7-permission) (on API 31+ with `BLUETOOTH_SCAN` + `neverForLocation`, scanning itself does not require location). Camera preview needs camera permission; WiFi-mode `fileAbility` pull follows `compat.isRequireWifi()`. If the app uses EPO or continuous device location, request location permissions for that business (see the sample note in 2.7).
6. **Recording file pull**: `fileAbility.pullFiles()` deletes device originals after a successful pull; only some platforms/devices support it (`isSupport()`).
7. **Protocol questions**: follow this guide plus public APIs and the sample; contact TopStep for unpublished protocol documents.

### Duplicate classes

Realtek libraries are widely used and often collide. `sdk-fitcloud-adapter` (via `sdk-fitcloud`) pulls in required dependencies `ext-realtek-bbpro` and `ext-realtek-file`. If the host App already has equivalent Realtek classes, you get a **duplicate class** error and can exclude them on the adapter:

```kotlin
implementation("com.topstep.wearkit:sdk-fitcloud-adapter:$wearkitVersion") {
    exclude(group = "com.topstep.wearkit", module = "ext-realtek-bbpro")
    exclude(group = "com.topstep.wearkit", module = "ext-realtek-file")
}
```

Local AARs have no POM exclude: by default you must add `sdk-realtek-bbpro-v*.aar` and `sdk-realtek-file-v*.aar`. Omit them only when the host already has equivalent classes and a duplicate-class conflict occurs.

### `.so` conflicts

When multiple native libraries ship the same `.so`, keep the first one that appears:

```kotlin
android {
    packaging {
        jniLibs.pickFirsts.add("**/libc++_shared.so")
    }
}
```

### Java resource conflicts

When several Netty modules each ship `META-INF/io.netty.versions.properties`, `merge*JavaResource` fails. Keys in those files do not conflict, so merge them:

```kotlin
android {
    packaging {
        resources.merges.add("META-INF/io.netty.versions.properties")
    }
}
```

### Dependency not found

If WearKit artifacts cannot be resolved, add the repository `https://maven.topstepht.com/repository/maven-public/`:

```kotlin
maven {
    url = uri("https://maven.topstepht.com/repository/maven-public/")
}
```

---

*Generated from sdk-apis source comments; concrete behavior follows each platform adapter implementation.*
