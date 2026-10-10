# WearKit SDK Android 接入指南

> GitHub：https://github.com/htangsmart/WearKit-SDK-Android/
>
> 版本：`CHANGED.md` 公开版 **3.0.2.7**

---

<a id="1-about-wearkit"></a>

## 1. WearKit 说明

WearKit 是面向可穿戴设备的统一 SDK（`com.topstep.wearkit`）。**扫描一律走 BLE**；连接建立后的数据通道可能是 BLE，也可能是经典蓝牙 SPP，取决于设备平台（及固件），开发者仍用同一套入口。初始化完成后得到 `WKWearKit`，后续通过 `scanner`、`connector` 和各 ability API 使用。设备平台通过注册对应的 `WKWearKit.Builder` 实现来选择。只加 adapter 依赖、不注册 builder，该设备平台不可用：

| 设备平台（`WKDeviceType`） | Adapter 产物 | Builder |
|---|---|---|
| `FIT_CLOUD` | `sdk-fitcloud-adapter` | `WKFitCloudKit.Builder` |
| `FLY_WEAR` | `sdk-flywear-adapter` | `WKFlyWearKit.Builder` |
| `SHEN_JU` | `sdk-shenju-adapter` | `WKShenJuKit.Builder` |
| `PROTO_TB` | `sdk-prototb-adapter` | `WKProtoTbKit.Builder` |
| `AB_MATE` | `sdk-abmate-adapter` | `WKAbMateKit.Builder` |

注册某个 adapter **不代表**该设备平台的所有设备都支持所有能力。功能支持取决于固件版本和各 ability 的 `Compat` API（见后续各功能指南）。

---

## 2. 快速集成

> - Android 应用模块、Kotlin、Java 8 语言级别。
> - App 的 `minSdk` **建议使用 26**。若不接入 AiKit（`com.topstep.aikit`），可降到 **24**。
> - 蓝牙硬件与蓝牙权限（见[权限](#2-7-permission)一节）。

<a id="2-1-maven"></a>

### 2.1 Maven 接入（推荐）

仓库：`https://maven.topstepht.com/repository/maven-public/`

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

锁定同一 WearKit 版本。声明 `sdk-core` 以及**实际发布**的 adapter。下面示例列出各家族必须项（已展开）；不用的家族整段删除。标为可选的是非核心功能：不添加可减少依赖，但**使用对应功能时可能崩溃**。

```kotlin
dependencies {
    val wearkitVersion = "3.0.2.7"

    // ========== 核心 ==========
    ///////////// 必须 ///////////
    implementation("com.topstep.wearkit:sdk-core:$wearkitVersion")
    ///////////// 可选 ///////////
    // ImageProxy.toWKImageProxy（wearKit.cameraAbility.updatePreview）：
    // implementation("androidx.camera:camera-core:1.4.2")

    // ========== FitCloud（WKDeviceType.FIT_CLOUD）==========
    ///////////// 必须 ///////////
    implementation("com.topstep.wearkit:sdk-fitcloud-adapter:$wearkitVersion")
    ///////////// 可选 ///////////
    // AliAgent（极少项目需要；同时 Builder.setSupportAliAgent）：
    // implementation("com.topstep.wearkit:ext-aliagent:1.0.5")
    // implementation("com.topstep.wearkit:ext-aliagent-ext:1.0.5")
    // 体感游戏（极少项目需要；同时 Builder.setSensorGameApiKey）：
    // implementation("com.topstep.wearkit:ext-sensorgame:1.0.5")
    // 微信支付认证（极少项目需要；同时 Builder.setSupportWeChatPay）：
    // implementation("com.artillery.pay:paycertification:leadingSmart_1.0.54")
    // Nordic 芯片 DFU（非常旧的设备，大概2024年的设备）：
    // implementation("no.nordicsemi.android:dfu:2.2.2")

    // ========== FlyWear（WKDeviceType.FLY_WEAR）==========
    ///////////// 必须 ///////////
    implementation("com.topstep.wearkit:sdk-flywear-adapter:$wearkitVersion")

    // ========== ShenJu（WKDeviceType.SHEN_JU）==========
    ///////////// 必须 ///////////
    implementation("com.topstep.wearkit:sdk-shenju-adapter:$wearkitVersion")

    // ========== ProtoTb（WKDeviceType.PROTO_TB）==========
    ///////////// 必须 ///////////
    implementation("com.topstep.wearkit:sdk-prototb-adapter:$wearkitVersion")
    ///////////// 可选 ///////////
    // 自定义视频表盘（可以是其他 FFmpegKit，但不要混用多个 FFmpeg native 库）：
    // implementation("com.antonkarpenko:ffmpeg-kit-min-gpl:2.1.0")

    // ========== AbMate（WKDeviceType.AB_MATE）==========
    ///////////// 必须 ///////////
    implementation("com.topstep.wearkit:sdk-abmate-adapter:$wearkitVersion")
}
```

### 2.2 本地 AAR 接入

仅在不方便使用 [Maven 接入](#2-1-maven) 时使用。AAR 无 POM，WearKit 文件与第三方库都要手写。把 `sdk-*-v3.0.2.7.aar` 放到 `libs/`，扩展包放到 `libs/ext/`（文件名与 `WearKit-SDK-Android/libs/` 一致）。ext 包用**自己的**版本号，不要套用 `3.0.2.7`。下面示例列出各家族必须项（已展开）；不用的家族整段删除。与其他家族重复的 AAR 可原样保留，无需手工去重。

```kotlin
dependencies {
    // ========== 核心 ==========
    ///////////// 必须 ///////////
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
    ///////////// 可选 ///////////
    // ImageProxy.toWKImageProxy（wearKit.cameraAbility.updatePreview）：
    // implementation("androidx.camera:camera-core:1.4.2")

    // ========== FitCloud（WKDeviceType.FIT_CLOUD）==========
    ///////////// 必须 ///////////
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
    ///////////// 可选 ///////////
    // AliAgent（极少项目需要；同时 Builder.setSupportAliAgent）：
    // implementation(files("libs/ext/sdk-aliagent-v1.0.5.aar"))
    // implementation(files("libs/ext/sdk-aliagent-ext-v1.0.5.aar"))
    // implementation("com.google.code.gson:gson:2.10")
    // implementation("com.google.firebase:firebase-crashlytics-buildtools:2.8.1")
    // implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.4")
    // implementation("com.alibaba:fastjson:1.2.83")
    // implementation("org.apache.commons:commons-text:1.9")
    // implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
    // implementation("com.aliyun.dpa:oss-android-sdk:2.9.13")
    // 体感游戏（极少项目需要；同时 Builder.setSensorGameApiKey）：
    // implementation(files("libs/ext/sdk-sensorgame-v1.0.5.aar"))
    // 微信支付认证（极少项目需要；同时 Builder.setSupportWeChatPay）：
    // implementation("com.artillery.pay:paycertification:leadingSmart_1.0.54")
    // Nordic 芯片 DFU（非常旧的设备，大概2024年的设备）：
    // implementation("no.nordicsemi.android:dfu:2.2.2")

    // ========== FlyWear（WKDeviceType.FLY_WEAR）==========
    ///////////// 必须 ///////////
    implementation(files("libs/sdk-flywear-v3.0.2.7.aar"))
    implementation(files("libs/sdk-flywear-adapter-v3.0.2.7.aar"))
    implementation(files("libs/ext/sdk-persimwear-v1.0.5.aar"))
    implementation("com.belerweb:pinyin4j:2.5.0")

    // ========== ShenJu（WKDeviceType.SHEN_JU）==========
    ///////////// 必须 ///////////
    implementation(files("libs/sdk-shenju-base-v3.0.2.7.aar"))
    implementation(files("libs/sdk-shenju-core-v3.0.2.7.aar"))
    implementation(files("libs/sdk-shenju-opencv-v3.0.2.7.aar"))
    implementation(files("libs/sdk-shenju-adapter-v3.0.2.7.aar"))
    implementation("com.google.code.gson:gson:2.10.1")

    // ========== ProtoTb（WKDeviceType.PROTO_TB）==========
    ///////////// 必须 ///////////
    implementation(files("libs/sdk-prototb-adapter-v3.0.2.7.aar"))
    implementation(files("libs/sdk-fitcloud-v3.0.2.7.aar"))
    implementation(files("libs/sdk-shenju-opencv-v3.0.2.7.aar"))
    implementation("com.google.protobuf:protobuf-javalite:4.33.1")
    implementation("com.belerweb:pinyin4j:2.5.0")
    implementation("org.apache.commons:commons-compress:1.24.0")
    implementation("pl.droidsonroids.gif:android-gif-drawable:1.2.32")
    implementation("com.topstep.tool:lib-abpartool:1.0.1")
    ///////////// 可选 ///////////
    // 自定义视频表盘（可以是其他 FFmpegKit，但不要混用多个 FFmpeg native 库）：
    // implementation("com.antonkarpenko:ffmpeg-kit-min-gpl:2.1.0")

    // ========== AbMate（WKDeviceType.AB_MATE）==========
    ///////////// 必须 ///////////
    implementation(files("libs/sdk-abmate-adapter-v3.0.2.7.aar"))
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("org.nanohttpd:nanohttpd:2.3.1")
    implementation("androidx.lifecycle:lifecycle-process:2.5.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation(files("libs/ext/sdk-jl-ota-v1.11.0.aar"))
    implementation("com.artillery.pay:paycertification:leadingSmart_1.0.54")
}
```

### 2.3 初始化 WKWearKit

```kotlin
fun wearKitInit(application: Application): WKWearKit {
    // 1. 日志（SDK 使用 Timber 输出日志）
    Timber.plant(Timber.DebugTree())

    // 2. 注册各平台 Builder（想支持哪些平台就 add 哪些）
    val builders = ArrayList<WKWearKit.Builder>()
    val processLifecycleObserver = MyProcessLifecycleManager().also {
        application.registerActivityLifecycleCallbacks(it)  // 前/后台状态感知
    }
    val rxBleClient = RxBleClient.create(application)
    builders.add(WKFitCloudKit.Builder(application, processLifecycleObserver, rxBleClient))
    builders.add(WKProtoTbKit.Builder(application, processLifecycleObserver, rxBleClient))
    // ...

    val wearKit = buildWKWearKit(builders)

    // 3. RxJava 全局异常处理（忽略 SDK 已知不可分发异常，避免崩溃）
    val ignoreExceptions = HashSet<Class<out Throwable>>()
    ignoreExceptions.addAll(wearKit.rxJavaPluginsIgnoreExceptions())
    RxJavaPlugins.setErrorHandler(RxJavaPluginsErrorHandler(ignoreExceptions))
    return wearKit
}
```

`MyProcessLifecycleManager`：继承 `ProcessLifecycleManager` 并实现 `ActivityLifecycleCallbacks`；sample 在 `onActivityResumed`（进入前台，`startCount==1`）/`onActivityStopped`（进入后台）调用 `setForeground(true/false)`——SDK 依赖它切换前后台扫描模式。类名可自定，实现可参考 sample `WearKitInit`。

### 2.4 扫描设备

```kotlin
wearKit.scanner.scan(
    type = WKDeviceType.FIT_CLOUD,   // 要扫描的平台
    durationSeconds = 120,           // 扫描时长（秒）
    checkLocationService = true,     // 扫描前检查定位服务是否开启
    acceptEmptyName = false,         // 是否接受空名字设备
).subscribe({ result ->
    val device = result.device  // BluetoothDevice
    val name = result.name
    val rssi = result.rssi
}, { err -> ... })
```

### 2.5 连接设备

```kotlin
// 推荐：带用户信息连接
wearKit.connector.connect(
    type = WKDeviceType.FIT_CLOUD,
    address = deviceAddress,
    authMode = WKAuthMode.BIND,   // 绑定模式；有登录态可用 LOGIN
    authCode = null,              // 从手表二维码扫到的 auth code（仅能准确获取时传）
    userId = "your_user_id",
    sex = true, age = 22, height = 175f, weight = 65f,
)
// 仅认证信息、暂不传用户信息时可用另一重载（type/address/authMode/authCode/userId）；
// 连接后再 setUserInfo / syncUserInfo。

// 观察连接状态
wearKit.connector.observeConnectorState().subscribe { state ->
    // WKConnectorState: DISCONNECTED / CONNECTING / PRE_CONNECTED / CONNECTED ...
}
// 观察连接错误
wearKit.connector.observeConnectorError().subscribe { err -> ... }
```

连接相关其他方法：
| 方法 | 用途 |
|---|---|
| `setUserInfo(sex, age, height, weight)` | 连接后更新用户信息（强制覆盖设备值） |
| `syncUserInfo(WKUserInfo)` | 按时间戳比较后同步用户信息（设备更新则返回设备值） |
| `close()` | 断开并清除当前持有设备 |
| `clear(removeBond)` | 清除设备认证信息（部分设备会恢复出厂）+ 解绑 + 断开 |
| `reconnect()` / `disconnect()` | 重连 / 断开（保留设备引用，稍后自动回连） |
| `setAutoCreateBond(enabled)` / `createBond()` / `removeBond()` | 自动/手动配对控制 |
| `setAutoReconnectMode(mode)` / `setAutoReconnectInterval(minSeconds)` | 自动重连策略 |
| `isBindOrLogin()` | 当前是否 BIND 模式（true=BIND） |
| `getDisconnectedReason()` | 断开原因枚举 |
| `observeDeviceCanBond()` | 观察可配对设备变化 |

### 2.6 使用能力

统一入口 `wearKit.xxxAbility`，每个能力通常带 `compat`（能力支持判断，先查再调）：

```kotlin
if (!wearKit.alarmAbility.compat.isSupport()) return  // 先判断设备是否支持
wearKit.alarmAbility.requestAlarms().subscribe { alarms -> ... }
```

约定：**所有方法均为 RxJava3 响应式**；`compat.isSupport() == false` 时调用对应方法会抛 `WKUnsupportedException`；能返回 "默认值" 的 get 方法在设备不支持时返回无效默认值。

<a id="2-7-permission"></a>

### 2.7 权限

扫描和连接按下面的 Manifest 声明。`BLUETOOTH_SCAN` 使用 `neverForLocation`，因此 API 31+ 扫描不申请定位。API 23–30 运行时仍申请 `ACCESS_COARSE_LOCATION` 与 `ACCESS_FINE_LOCATION`。

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

Manifest 按上面声明后，运行时权限用 `RxBleClient.getRecommendedScanRuntimePermissions`：

| API 级别 | 蓝牙运行时权限 |
|---|---|
| 23–30 | `ACCESS_COARSE_LOCATION`、`ACCESS_FINE_LOCATION` |
| 31+ | `BLUETOOTH_SCAN`、`BLUETOOTH_CONNECT` |

WearKit sample 的 `ACCESS_COARSE_LOCATION`、`ACCESS_FINE_LOCATION` 与上面有些差别，因为 sample 还要把定位用于 EPO / 设备持续定位。

---

## 3. 能力接口总览

| 分类 | 能力 | 一句话说明 |
|---|---|---|
| 基础 | `deviceAbility` | 设备开关机/复位 + 数据同步核心入口 |
| 基础 | `timeAbility` | 设备时间设置/同步 |
| 基础 | `languageAbility` | 设备语言设置/查询 |
| 基础 | `batteryAbility` | 电量查询/监听 |
| 基础 | `alarmAbility` | 闹钟查询/设置/监听 |
| 基础 | `contactsAbility` | 常用/紧急联系人、联系人头像、SOS |
| 基础 | `cameraAbility` | 遥控拍照、相机预览流推送 |
| 基础 | `finderAbility` | 找手表/找手机 |
| 基础 | `weatherAbility` | 天气推送（今天+未来数天/数小时） |
| 基础 | `notificationAbility` | App 通知/来电推送、来电拦截回执 |
| 基础 | `mediaAbility` | 媒体控制状态同步（播放/暂停/歌曲信息） |
| 基础 | `remindAbility` | 喝水/久坐/吃药/自定义提醒 |
| 配置 | `functionAbility` | 佩戴手/时间制式/天气开关等简单功能位 |
| 配置 | `unitAbility` | 公英制单位设置 |
| 配置 | `dndAbility` | 勿扰时段 |
| 配置 | `raiseWakeupAbility` | 抬腕亮屏 |
| 配置 | `womenHealthAbility` | 女性健康（经期提醒等） |
| 数据 | `activityAbility` | 活动目标（步数/卡路里目标设置） |
| 数据 | `heartRateAbility` | 心率实时测量/监测/报警/HRV 配置 |
| 数据 | `bloodOxygenAbility` | 血氧测量/监测 |
| 数据 | `pressureAbility` | 压力测量/监测 |
| 数据 | `bloodPressureAbility` | 血压测量/监测 |
| 数据 | `temperatureAbility` | 体温测量/监测 |
| 数据 | `sportAbility` | 设备运动状态（运动中/类型） |
| 文件 | `musicAbility` | 音乐文件推送（仅 .mp3） |
| 文件 | `eBookAbility` | 电子书推送（text/plain） |
| 文件 | `albumAbility` | 图片推送 |
| 文件 | `otaAbility` | 固件/UI OTA |
| 文件 | `sportUIAbility` | 运动 UI 资源推送/替换 |
| 文件 | `logAbility` | 拉取设备日志 |
| 文件 | `locationMapAbility` | 定位推送/EPO/离线地图 |
| 文件 | `fileAbility` | 录音文件列表/拉取/删除（记录类文件） |
| 表盘 | `dialAbility` | 表盘查询/安装/卸载/选中 |
| 表盘 | `dialStyleAbility` | 自定义表盘（背景/样式/位置/颜色/弹幕） |
| 特殊 | `aiAbility` | AI 健康指导/ChatGPT 问答/TTS |
| 特殊 | `businessCardAbility` | 电子名片 |
| 特殊 | `paymentCodeAbility` | 支付码 |
| 特殊 | `lockAbility` | 屏幕锁/游戏锁 |
| 特殊 | `muslimAbility` | 穆斯林（祷告/回历/朝向） |
| 特殊 | `worldClockAbility` | 世界时钟 |
| 语音AI | `speechAiAbility` | 语音 AI 会话（对话/录音/翻译/问答/表盘） |
| B2B | `b2b` | HSD（儿童手表场景）/ Titan（二维码）/ UGreen（弹幕与配置）定制 |

数据同步：历史数据（步数/心率/睡眠/运动等）统一走 `deviceAbility.syncData()`，同步结果经 `WKSyncData` 下发，按 `WKSyncData.Type` 区分（ACTIVITY/HEART_RATE/SLEEP/SPORT/...）。

---

## 4. 能力详细说明

### 4.1 基础能力（base）

#### WKDeviceAbility 设备管理 & 数据同步
- `shutdown()` 关机 / `reset()` 复位 / `reboot()` 重启设备
- `syncItem(type, start, end)` 同步单类型数据（已废弃，用 syncData）
- `getSyncTypes()` 获取设备支持的所有同步数据类型
- `syncData(timeProvider, vararg types)` 一次同步指定类型（**推荐**，内部按序高效同步）
- `syncData(timeProvider)` 同步全部类型
- `observeSyncState()` / `isSyncing()` 同步状态
- `getDeviceInfo()` / `observeDeviceInfo(replay)` 设备基础信息（型号/固件版本/序列号等）
- <a id="4-1-third-party-data"></a>`observeThirdPartyData()` / `sendThirdPartyData(data)` 与设备侧第三方能力交换数据（当前类型见 `WKThirdPartyData.Type`，如 `STAR_BURST`）；不支持时观察流不发射、发送抛 `WKUnsupportedException`
- `ISyncAbility.range`：数据同步的起始时间范围控制

#### WKTimeAbility 时间
- `applySystemTime()` 把手机系统时间同步到设备
- `setTime(timestampMillis, zoneOffsetMillis)` 按 UTC 时间戳+时区偏移设置设备时间
- `setTime(timestampMillis)` 使用系统默认时区设置

#### WKLanguageAbility 语言
- `setLanguage(languageType)` 设置设备语言（用 `LanguageUtil.getLanguageType/getSystemLanguageType` 取值）
- `requestLanguage()` 查询设备当前语言
- `requestSupportLanguageList()` 查询设备支持的语言列表（`compat.isSupportLanguageList()`）
- 语言类型对照见 `sdk-base` 的 `LanguageUtil`

#### WKBatteryAbility 电量
- `requestBattery()` 查询一次电量
- `observeBatteryChange()` 监听电量变化（旧设备无真实监听时为轮询，**页面隐藏时应尽快退订**）

#### WKAlarmAbility 闹钟
- `requestAlarms()` 查询设备闹钟列表
- `setAlarms(list)` 全量设置闹钟（null/空=清空）
- `observeAlarmsChange()` 设备端闹钟变化监听
- `compat`：`getAlarmMaxNumber()` 闹钟数量上限 / `getLabelMaxBytes()` 标签字节上限 / `isSupportType()` 是否支持某种闹钟类型

#### WKContactsAbility 联系人
- `requestContactsCommon()` / `setContactsCommon(common)` 常用联系人（空=清空）
- `requestContactsEmergency()` / `setContactsEmergency(emergency)` 紧急联系人
- `setContactsImage(number, file)` / `deleteContactsImage(number)` / `requestContactsHasImage()` 联系人头像
- `observeContactsChange()` 设备端联系人变更（仅设备自身改动时回调）
- `observeSOS()` 设备 SOS 请求（部分设备支持）
- `compat`：`getContactsCommonMaxNumber()` / `getContactsEmergencyMaxNumber()` / `getContactsImageMaxNumber()`

<a id="4-1-camera"></a>

#### WKCameraAbility 遥控相机 & 预览
- `setCameraStatus(open)` App 相机开/关状态通知设备
- `sendCameraMessage(message)` 发送拍照/切换镜头等控制指令（见 `WKCameraMessage`）
- `observeCameraMessage()` 设备请求拍照/录像消息
- `startPreview(quality)` 启动预览流推送（把 App 相机画面推送到设备端显示）
- `updatePreview(type, data)` / `updatePreview(image: WKImageProxy)` 发送预览帧（推荐 WKImageProxy 方式；前者已 `@Deprecated`）
- `stopPreview()` 停止预览并释放编码器
- `setCameraInfo(zoomRatioMin, zoomRatioMax)` 告知设备相机变焦范围
- `compat`：`isSupportPreview()` / `isSupportVideo()` / `getPreviewSize()`

#### WKFinderAbility 查找设备
- `findWatch()` 让手表响铃/震动
- `stopFindWatch()` 停止
- `foundPhone()` 手机端"找到手机"回复
- `observeFinderMessage()` 设备查找消息（FIND_PHONE/STOP_FIND_PHONE/FOUND_WATCH）

#### WKWeatherAbility 天气
- `setWeather(city, today, futureDays, futureHours)` 推送今天+未来天气（天/小时必须连续，显示数量取决于手表）
- `compat.isSupport()`

#### WKNotificationAbility 通知 & 来电
- `sendAppNotification(packageName, title, content, tickerText)` 推送 App 通知；`packageName` 用 `CommonAppPackage` 中常量（微信/QQ/Facebook/WhatsApp/Messenger/...），**不支持的包名会归入 "Others" 类别**
- `sendTelephonyNotification(type, phoneNumber, name)` 推送来电（`WKTelephonyType`：来电/拒接/未接/挂断...）
- `observeTelephonyHangup()` 设备发起挂断/发短信动作
- `replayTelephonyHangup(endCall, sendSms)` 回执挂断/发短信结果
- `getTelephonyConfig()` / `setTelephonyConfig(enabled)` / `observeTelephonyConfig()` 来电通知总开关
- `compat.isSupportAppNotification(packageName)` 该包名是否被设备识别为独立类别

#### WKMediaAbility 媒体控制
- `observeMediaMessage()` 设备上的媒体控制按键消息（播放/暂停/上下曲）
- `setMusicInfo(title, artist, duration)` 当前播放歌曲信息同步到设备
- `setMusicState(state, position, speed)` 播放状态（0停/1播/2暂停）+进度 + 倍速

#### WKRemindAbility 提醒
- `requestReminds()` 全部提醒 / `requestRemind(type)` 单类型（喝水/久坐/吃药/自定义）
- `setReminds(list)` 全量设置（非自定义类型即使不在列表也不会被删）
- `addOrUpdateRemind(vararg reminds)` 增改
- `deleteRemind(vararg types)` 删除自定义提醒
- `compat`：`isSupport()` / `isSupportType(type)` / `isSupportField(type, field)` / `createCustomRemind(list)` / `getCustomRemindMaxNumber()`
- 提醒类型：`WKRemind.Type.DrinkWater/Sedentary/TakeMedicine/Custom`，字段支持度用 `isSupportField` 逐项判断（开关/时段/间隔/重复等）

### 4.2 配置能力（config）

统一模式：`getConfig() → WKXxxConfig` / `setConfig(config)` / `observeConfig(replay)` + `compat`。

- **WKFunctionAbility 功能位**：`getConfig/setConfig/observeConfig`；Flag 含 `WEAR_HAND`（佩戴手）/`TIME_FORMAT`（12/24h）/`WEATHER`/`HEALTH_ENHANCED` 等
- **WKUnitAbility 单位**：get/set/`syncConfig`（按时间戳比较，设备更新则回传设备值）/observe；`compat.isSupportSplitMetric()` 支持公英制分别设置时用 `isLengthMetric + isWeightMetric`，否则用 `isMetric`
- **WKDndAbility 勿扰**：get/set/observe；`compat.isSupport()` / `isSupportTimeAcrossDays()`（跨天时段，如 21:00→8:00）
- **WKRaiseWakeupAbility 抬腕亮屏**：get/set/observe；`compat.isSupport()`（无屏幕设备不支持）/ `isSupportPeriod()`（时段字段是否可用）
- **WKWomenHealthAbility 女性健康**：get/set/observe；`compat.isSupport()` / `isSupportRemindFlags()`（经期/排卵提醒标志位）

### 4.3 健康测量数据能力（data）

五类测量（心率/血氧/压力/血压/体温）**结构完全一致**：

```kotlin
// 实时手动测量：durationSeconds 测量时长；返回多个中间值，取平均或最后一个
heartRateAbility.measureRealtime(30).subscribe { value -> ... }

// 自动监测配置（开关 + 时段 + 间隔）
heartRateAbility.getMonitorConfig()
heartRateAbility.setMonitorConfig(config)
heartRateAbility.observeMonitorConfig(true)

// 心率/血压等还有报警配置
heartRateAbility.getAlarmConfig() / setAlarmConfig(config)

// compat 判断
heartRateAbility.compat.isSupport()
heartRateAbility.compat.isSupportMeasure()        // 是否支持手动测量
heartRateAbility.compat.isSupportMonitorConfig()  // 是否支持自动监测配置
```

各自差异：
- **WKHeartRateAbility**：`measureRealtime(duration) : Observable<Int>`；`getMaxThreshold()/setMaxThreshold()` 上限；`getHRVConfig()/setHRVConfig()` HRV 监测；compat 含 `isSupportMinValueConfig/isSupportTimePeriod/isSupportTimeInterval/isSupportTimeAcrossDays/isSupportHRV`
- **WKBloodOxygenAbility / WKPressureAbility**：measure + 监测配置
- **WKBloodPressureAbility**：`measureRealtime : Observable<WKBloodPressureItem>`（含高压/低压/脉搏）
- **WKTemperatureAbility**：`measureRealtime : Observable<WKTemperatureItem>`
- **WKActivityAbility 活动目标**：`getGoalConfig()/setGoalConfig()/syncGoalConfig()/observeGoalConfig()`；`compat.getActivityAttributes()`（设备活动页展示哪些指标，也决定目标设置项）；`compat.isSupportDisabledReminds()`（目标达成提醒可关闭）
- **WKSportAbility 运动状态**：`requestSportState()/observeSportState()`（是否运动中/运动类型）；`compat.isSupportSportState()`

### 4.4 文件与推送能力（file）

统一模式：`requestDirSpace()`（空间）/`requestFiles()`（列表）/`addFile(...)`（推送，带进度 0-100）/`deleteFile(path)`/`observeFileChange()` + `compat.isSupport()`。

- **WKMusicAbility 音乐**：仅支持 `.mp3`（`audio/mpeg`）；其他格式需自行转码（如 ffmpeg-kit-min）；`addFile(uri, artist?)`；compat 含 `isSupportRequest()/isSupportDelete()`
- **WKEBookAbility 电子书**：仅 `text/plain`，默认 UTF-8，可传 `charset`；`addFile(uri, charset)`
- **WKAlbumAbility 图片**：`addFile(uri)`；`compat.isSupportFolders()`（分文件夹）
- **WKOtaAbility OTA**：`ota(file) : Observable<Int>` 固件/UI 升级（进度 0-100）
- **WKSportUIAbility 运动 UI**：`requestCloudSportUIResources()`（云端资源）/`requestSupportSports()`/`requestSports()`/`requestSpaces()`/`install(file, spaceIndex?)` 推送自定义运动图标/UI
- **WKLogAbility 日志**：`pull() : Observable<ProgressResult<File>>` 拉取设备日志（result 文件不存在=设备无日志）；`compat.isSupport()`
- **WKLocationMapAbility 定位/EPO/离线地图**：
  - `setLocation(location)` 主动推送一次定位（地图表盘点位用，定位成功即更新）
  - `setLocation(provider)` 注册定位 Provider（获取 kit 后、连接前注册）。Provider 除 `requestLocation()` 外，可实现 `observeLocation(options)` 供设备发起的连续定位会话（订阅=开定位，dispose=停定位；默认实现报 `WKLocationException.ERROR_UNSUPPORTED`）
  - `updateEpo(force)` / `updateEpo(force, provider)` 更新设备 EPO（加速定位，文件从 SDK 内置服务器获取）
  - `requestEpoTime()` / `clearEpo()` 调试用
  - `listOfflineMap()` 设备离线地图名称列表；`listOfflineMapInfos()` 名称+字节大小（未知为 `-1`）
  - `requestOfflineMapSpace()` 地图存储区总量/剩余字节（未知为 `-1`）；追加地图前以实际文件大小与实时剩余空间比较
  - `deleteOfflineMap(name)` 按名称删除
  - `downloadOfflineMap(lat, lng, radius, file)` 下载离线地图
  - `pushOfflineMap(file, name)` 推送离线地图
  - `setOfflineMap(lat, lng, radius, name)` 下载并推送一步完成
  - `compat.isSupportOfflineMap()/isSupportEpo()`
- **WKFileAbility 录音文件管理**（注意：不是音乐/相册等推送，是"设备产生文件的拉取"）：
  - `requestFilesCount()/requestFiles()` 录音文件列表（列表不需要 WiFi）
  - `pullFiles(saveDir)` **拉取文件到手机**：每文件保存成功后**自动删除设备端原件**（删除失败忽略）；需要 WiFi 时见 `compat.isRequireWifi()`
  - `deleteFile(path)` 删除设备文件
  - `observeFileChange()` 设备录音文件增删监听
  - `setOpusRecordBitrate(bitrate)` 设置录音存储码率（部分适配层支持）
  - `rtsp(quality)` 建立 WiFi 连接并返回 RTSP 串流地址（实时画面），`compat.isSupportRtsp()`
  - `compat.isSupport()/isRequireWifi()`

### 4.5 表盘能力（dial）

**WKDialAbility 基础表盘管理**
- `requestDials()` 已装表盘列表
- `requestSpaces()` 可安装空间（安装前预查剩余空间，多空间可选）
- `select(dialId)` 设为当前表盘
- `install(dialId, file, spaceIndex?)` 安装表盘（空间/数量不足抛 `ERROR_DEVICE_STORAGE`）
- `uninstall(dialId)` / `uninstall(dialId, spaceIndex)` 卸载
- `observeDialsChange()` 表盘变化监听
- `compat`：`isSupport()` / `getDialMaxNumber()` / `isSupportUninstall(type)`

**WKDialStyleAbility 自定义表盘（复杂，推荐看 sample `ui/dial`）**
- `requestCloudDialStyleResources()` 云端风格资源
- `requestConstraint(resources)` 获取该资源的自定义约束（可用背景数/样式/位置等）
- `createCustom(constraint, input)` 按约束+输入生成自定义表盘，产出 `CreateOutput(dialFile, previewFile, dialId)`
- `CreateInput` 工厂方法（按背景类型）：
  - `CreateInput.base(背景图 uri, 样式索引, 位置索引, 颜色着色...)` 单图背景
  - `CreateInput.multiple(多张背景图, 轮播间隔...)` 多图背景自动轮播
  - `CreateInput.video(视频 uri, 裁剪区域/起止时间...)` 视频背景
  - `CreateInput.danMu(DanMuConfig, 背景色...)` 弹幕背景（弹幕条 + 背景色）
  - `CreateInput.customDialId` 可选：仅 FIT_CLOUD GUI 表盘生效
- 弹幕坐标工具 `DanMuCoord`：类似 Android MeasureSpec 的坐标编码器（`absolute(px)` / `relative(anchor, offset)` / `resolveX/Y(spec, 容器宽高, 内容宽高)`）
- `installCustom(file, spaceIndex?)` 安装
- `compat`：`isSupport()` / `isSupportVideoBackground()`（视频背景）/ `getVideoDuration()/getVideoMaxDurationMillis()` / `isSupportMultipleBackground()` / `isSupportDanMuBackground()`（弹幕）/ `getQualityLevels()`
- 弹幕相关模型：`DanMuItem(imageUri, imageX, imageY, walkSpeed, animUri, animX, animY, ltr)`、`DanMuConfig(items, backgroundColor)`

> 自定义表盘协议细节（背景/样式/位置/颜色染色的 bin 打包）涉及 `sdk-fitcloud` 的 DialView/DialWriter 内部实现，需对接时可询问 TopStep。

### 4.6 特殊能力（special）

- **WKAIAbility**：`initAi(initResult)` AI 初始化；`generateAIHealthGuidance(callback)` AI 健康指导（基于当天睡眠/HRV/活动数据生成文案，支持传入数据或自动查库两个重载）；`WKChatGptCallback.answer(text)` ChatGPT 问答；`tts(path)` 语音合成播放
- **WKBusinessCardAbility 名片**：`request()` 查询设备名片；`set(map)` 设置（字段见 `WKBusinessCard`）；`compat.isSupport()`
- **WKPaymentCodeAbility 支付码**：`request()/set(map)` 同上模式；`compat.isSupport()`
- **WKLockAbility 锁**：`setScreenLock(lock)/requestScreenLock()` 屏幕锁；`setGameLock(lock)` 游戏锁（防沉迷时段）；`compat.isSupportScreenLock()/isSupportGameLock()/getPasswordLength()`
- **WKMuslimAbility 穆斯林**（原 `prayerAbility`，已废弃改此名）：`requestPrayerSwitch()/setPrayer(info)/observePrayerSwitch()` 祷告；`setHijri(current, holidays)` 回历；`setQibla(degrees, distance)` 朝向；`observeMuslimMessage()`；`compat.isSupportPrayer()/isSupportQibla()/isSupportHijri()`
- **WKWorldClockAbility 世界时钟**：`setClocks(list)/requestClocks()`；`compat.isSupport()/getWorldClockMaxNumber()`

<a id="4-7-speech-ai"></a>

### 4.7 语音 AI 能力（speech）

**WKSpeechAiAbility** 是语音 AI 核心（含 FitCloud / ProtoTb / AbMate 等已实现该 ability 的平台）。整体模型：

```
设备(耳机/手表) 麦克风音频 → WKSpeechSession.audio() → App 做 ASR/LLM/翻译
App 结果 → Chat.sendTextQuestion/sendTextAnswer 等回推设备显示
设备播放 TTS → Player.start/write 下发 PCM 到设备扬声器
```

**使用前提**
- `isSupport()` 设备是否支持语音 AI
- `getSuggestAiSDKs()` 推荐 AI SDK（内部用；**客户 App 自选 AI SDK 可忽略**）
- `setAiSDKInitResult(success, aiSDK?)` 连接后告知设备 App 端 AI SDK 初始化结果；`aiSDK` 可空（未使用 `WKSpeechAiSDK` 枚举时）。旧参数序 `(aiSDK, success)` 已 `@Deprecated`

**会话模型（Session）**
- 场景 `WKSpeechSession.Scene`：`CHAT(0)` AI 畅聊 / `RECORD(1)` 录音 / `CALL_RECORD(2)` 通话录音 / `TRANSLATE(3)` 同声传译 / `TAXI(4)` / `DIAL(5)` AI 表盘 / `ASK(6)` 问答 / `CHAT_TRANSLATE_SELF(7)` 对话翻译-自身 / `CHAT_TRANSLATE_PEER(8)` 对话翻译-对方
- 音频源 `Source`：`PHONE_MIC`（手机麦克风）/ `DEVICE_CMD`（设备指令通道音频）/ `DEVICE_SCO`（蓝牙 SCO）
- 发起方 `Origin`：`DEVICE`（设备主动发起，**需全程订阅 observeDeviceSession**）/ `APP`（App 发起 createAppSession）
- 关键方法：
  - `observeDeviceSession()` 观察设备发起的会话（**必须保持订阅**，否则设备超时自释放）
  - `createAppSession(scene, source?)` App 发起会话（有活跃会话/场景不支持/设备未连 DEVICE_CMD 时返回 null）
  - `activeSession()` 当前活跃会话
  - `isSupportAppScene(scene)` / `isSupportDeviceScene(scene)` 场景支持判断
- `WKSpeechSession.audio()` 订阅音频流：`format` = `PCM(16k/mono/16bit)` 或 `OPUS(frameSize)`；onComplete=正常结束；onError（`Exception`：ERROR_BATTERY 低电 / ERROR_INCOMING 来电 / ERROR_DISCONNECTED 断连等）
- `release(reason)` 结束会话（Reason：NONE/ERROR_STORAGE/ERROR_PERMISSION/UNKNOWN）

**消息（observeMessage）**
- `SCENE_EXIT`(10001)：设备退出某场景（data=Scene）→ App 应结束该场景逻辑；**SDK 会先结束仍活跃的 audio 会话再发此消息**，重复收到幂等
- `TRANSLATE_PLAYER_STATE`(302)：翻译 TTS 播放状态变更（START/STOP/PAUSE/RESUME）
- `DIAL_GENERATE_IMAGE`(501)/`DIAL_GENERATE_DIAL`(502)：AI 表盘流程确认
- `ASK_GENERATE_ANSWER`(601)：问答场景设备确认收到问题
- 旧设备可能不发 SCENE_EXIT → 以 audio 流结束兜底

**Chat 子能力（CHAT 场景回推）**
- `isSupportText()` 设备能否显示文本（false 时只传音频）
- `sendTextQuestion(text, isComplete)` 推送 ASR 问题（**整句快照**，非增量）
- `sendTextAnswer(text, isComplete)` 推送 LLM 回答
- `sendError(type, text)` 错误上报（`WKSpeechAiError`）

**Record 子能力**
- `getLang()` 设备指定的 ASR 语言字节码（null=未指定/不支持，回退系统语言）
- `isSupportText()` / `sendTextSource(text, isComplete)` 向设备推送录音 ASR 文本（整句快照，非增量）
- `isSupportPause()` 为 true 时可用 `pause()` / `resume()`（幂等；本地采集立即暂停/恢复，Completable 只表示设备是否收到通知）
- `isPaused()` / `getDurationMs()` 可在活跃 Record 会话上轮询（即使不支持 pause；时长为累计录音毫秒、不含暂停，非墙钟）

**Translate 子能力（翻译/对话翻译回推）**
- `getLang()` 设备设置的语言对（`WKTranslateLang`，null 时回退系统）
- `sendTextSource(text, isComplete)` 原文快照
- `sendTextTarget(text, isComplete)` 译文快照
- `sendTtsReady()` 告知设备 TTS 已就绪
- `startChatTranslate(mode)` / `stopChatTranslate()` 进入/退出对话翻译模式（`WKChatTranslateMode`：FACE_TO_FACE 面对面 / PRIVATE 私密听译 / PORTABLE 便携交流）——模式激活期间设备可能开 SELF/PEER 会话

**Dial 子能力（AI 表盘）**
- `sendText(text, isComplete)` 最终 ASR 文本（仅 isComplete=true 下发）
- `sendImage(file)` 生成图片推给设备预览（返回传输进度）
- 流程：ASR 文本 → 等 DIAL_GENERATE_IMAGE → 生成图 sendImage → 等 DIAL_GENERATE_DIAL → 走普通自定义表盘安装

**Ask 子能力（问答）**
- `sendTextQuestion/sendTextAnswer/sendError`，同 Chat 但**需设备确认**（ASK_GENERATE_ANSWER）

**Player 子能力（设备扬声器播 PCM）**
- `isSupport(scene)` 该场景是否支持下发音频
- `start(sampleRate, channels)` 开启（采样率限 8k/12k/16k/24k/48k，声道 1/2）
- `write(pcm, isFinal)` 阻塞式写入 PCM（isFinal 会 flush 并等待播完）
- `stop()` 结束

> 各场景（录音/翻译/对话翻译/AI 对话）App 侧实现示例见 WearKit-SDK-Android sample `ui/ai` 目录（RecordHandler/TranslateHandler/ChatTranslateHandler/ChatHandler 等）。音频数据接第三方 AI SDK（ASR/LLM）由客户自行集成。

### 4.8 B2B 定制能力（b2b）

仅特定客户项目使用，能力以设备/商务配置为准：
- **B2b**：`hsdAbility` + `titanAbility` + `ugreenAbility` 子能力入口
- **HsdAbility（家长管控/教育定制场景）**：`setIceLabels`（快捷联系人标签）/`setParentalMode`/`requestParentalMode`（家长模式）/`setClassRoomMode`/`requestClassRoomMode`（课堂模式）/`setTasks`/`requestTasks`/`exchangeTaskReward`（任务与奖励）/`setHabits`/`requestHabits`（习惯）/`requestAppUsageInfo`/`requestGameUsageInfo`/`resetUsageInfo`（使用统计）/`setGameRankingTrends`（游戏排行）；compat 对应 isSupport 系列 + `getTaskMaxNumber()/getHabitMaxNumber()`
- **TitanAbility（二维码）**：`requestQrCode()`/`setQrCode(map)`；`compat.isSupportQrCode()`
- **UGreenAbility**：`clearDanMu(type)` / `addDanMu(items)`（空列表不发包）；`requestConfig()` / `setConfig(UGreenConfig)`；`compat.isSupportDanMu()`

---

## 5. 数据同步（重要）

历史健康/运动数据通过 `deviceAbility` 同步，使用**时间提供者** `WKSyncTimeProvider`：

```kotlin
val timeProvider = object : WKSyncTimeProvider {
    override fun getRange(type: Int): WKTimestampRange {
        // 返回该类型数据的起始时间（如上次同步时间）到当前时间
    }
}
deviceAbility.syncData(timeProvider, WKSyncData.Type.ACTIVITY, WKSyncData.Type.HEART_RATE, ...)
    .subscribe { syncData ->  // WKSyncData：按 type 分发各类数据
        when (syncData.type) {
            WKSyncData.Type.ACTIVITY -> ...
            WKSyncData.Type.HEART_RATE -> ...
        }
    }
```

- `getSyncTypes()` 返回设备支持的类型；`observeSyncState()` 观察同步状态
- 模型类：`WKSleepItem/WKSleepSegment/WKActivityItem/WKHRVDaily/WKSportRecord...`
- `sdk-apis` 内 `SleepAlgorithm` / `SleepCalcSegment` 可算睡眠分期

---

## 6. 通知类别（CommonAppPackage）

`WKNotificationAbility.compat.isSupportAppNotification(packageName)` 判断某 App 是否被设备识别为**独立类别**（显示对应图标）；不支持的包名推送后设备归入 "Others"。

常用包名常量（`CommonAppPackage`，位于 sdk-base）：SMS/EMAIL/QQ/WECHAT/FACEBOOK/TWITTER/LINKEDIN/INSTAGRAM/WHATS_APP/LINE/FACEBOOK_MESSENGER/KAKAO_TALK/MICROSOFT_TEAMS/TELEGRAM/VIBER/SNAPCHAT/HIKE/YOUTUBE/...。通知开关页面可见的类别由**设备固件下发的通知支持能力**决定，某类别未开启时 App 主列表不显示、仍可通过"更多 App"通用通道勾选（见常见问题 3）。

---

## 7. AbMate 眼镜/耳机平台专项

> AbMate 是**中科 AI 眼镜 / 耳机**设备（带摄像头、支持拍照录像、AI 语音对话）。接入方式与手环/手表平台不同，本平台需引入 `sdk-abmate-adapter`（设备类型 `WKDeviceType.AB_MATE`）。协议细节以公开 API / sample 为准；需对接私有协议说明时联系 TopStep。

**AbMate SDK 提供两级 API**：

| 级别 | 入口 | 适用场景 |
|---|---|---|
| **WearKit 统一 API** | `WKWearKit` / `AbMateSDK`（scanner/connector 及各 `WK*Ability`） | 扫描、连接、基础设备信息、通知、音乐、OTA、日志、文件拉取、表盘、**语音 AI（`speechAiAbility`）** 等通用能力 |
| **眼镜业务 API** | `AbMateSDK.abMateManager`（`ABMateManager`） | 拍照/录像/机内录音、屏显控制、均衡器/降噪/按键、RTSP 直播、WiFi 文件传输、SK 音箱绑定等**眼镜专属能力** |

> 客户集成 AbMate：先用 `scanner`/`connector` 完成扫描连接；通用能力（含语音对话/录音/翻译）直接走 `WK*Ability`；仅眼镜专属能力经 `AbMateSDK.abMateManager`（`wearKit.getRawSDK() as AbMateSDK`）调用。
>
> **语音 AI 请使用 [4.7 语音 AI 能力](#4-7-speech-ai)**（`wearKit.speechAiAbility`）。AbMate 已实现该能力，**不要**再走 `abMateManager` 的 `sendAIChatState` / `setAIRecordState` / `aIRecordData` / `stateAIChat` 等旧接口。示例见 sample `ui/ai`。

### 7.1 构建与连接

```kotlin
// 1. 注册平台 Builder（与手环/手表相同方式，deviceType 用 AB_MATE）
builders.add(
    WKAbMateKit.Builder(application, processLifecycleObserver, rxBleClient)
        .setAutoSetTime(true)      // 连接后自动对时
        .setAutoSetLanguage(false) // 连接后是否自动同步语言
)

// 2. 连接后获取眼镜业务 API（仅专属能力需要；语音 AI 用 wearKit.speechAiAbility）
val abMateSDK = wearKit.getRawSDK() as AbMateSDK
val manager = abMateSDK.abMateManager  // ABMateManager：眼镜业务能力入口
val repo = manager.getRepository()     // ABMateDeviceRepository：状态/数据 LiveData
```

- 设备族常量（不同产品公司 ID 不同，扫描/连接时按需过滤）：`COMPANY_ID_ZG`（眼镜）/ `COMPANY_ID_ZE`（耳机）/ `COMPANY_ID_ZS` / `COMPANY_ID_ZM`
- 第三方数据透传优先走统一 API：`deviceAbility.observeThirdPartyData()` / `sendThirdPartyData`（如 `WKThirdPartyData.Type.STAR_BURST`，见 4.1）。支付等仍可通过 `AbMateSDK` 的 `receiveArtilleryPayData` / `sendArtilleryPayData`（方法名沿用 SDK 既有命名）。

### 7.2 眼镜业务能力与使用方式（ABMateManager，按业务场景分类）

> 下列专属能力经 `abMateManager` 调用；回调风格统一为 `onSuccess`/`onFail`，设备状态与数据流通过 `ABMateDeviceRepository` 的 LiveData 观察。语音 AI 不在此列，见 4.7。

#### 7.2.1 拍照 / 录像 / 录音（摄像头媒体）

> 摄像头协处理器从**关机状态**进入拍照/录像/录音状态。App 使用完应 `turnOffCamera()` 关子系统省电（无操作超时也会自动关，但文件传输模式不会）。

```kotlin
// 拍照（mode 0=摄像模式存机内，1=AI 模式直接传手机）
manager.turnOnCamera(1 /*AI 模式：拍 320x240 JPEG 并经蓝牙传手机*/, { /*拍照结果*/ }, { /*失败*/ })

// AI 识图模式：照片分片经 repo.aiPicPackage 回传
//   status==1 传输中(拼 picFlow) / status==0 完成(拼完收尾得到完整 JPEG) / 0xFE 存储满 / 0xFF 蓝牙异常
//   拼好的图交给接入的 AI 识图能力处理

// 录像（最长 5 分钟自动停，或 turnOffCamera() 停止；结果存机内存储）
manager.startVideoRecording(onSuccess = {}, onFail = {})
// 录音（最长 5 小时自动停）
manager.startAudioRecording(onSuccess = {}, onFail = {})

// 摄像头实时画面（RTSP 直播）—— 走统一 WKFileAbility：
// abMateSDK.fileAbility.rtsp(quality) → Observable<URL>，拿到 URL 后自行起播放器

// 停止一切摄像头活动并关机
manager.turnOffCamera(onSuccess = {}, onFail = {})

// 时长限制（全志平台录像 1~12 分钟、录音 1~240 分钟，0=不限）
manager.setLimitTime(0 /*录像*/, 5 /*分钟*/, {}, {})
manager.setLimitTime(1 /*录音*/, 60, {}, {})

// 媒体管理
manager.getMediaFileCount()          // 文件数
manager.getStorageSpaceInfo()        // 剩余空间（全志：摄像头内部存储）
manager.formatStorageSpace()         // 格式化
manager.deleteGlassFile(name)        // 删除指定文件
manager.getCoprocessorVersion()      // 协处理器版本/型号
manager.getGlassWorkState()          // 工作状态（11=录像中 12=录音中 9=拍照中…）
```

#### 7.2.2 眼镜屏显与基础配置

```kotlin
// 文字显示（提词器/滚动字幕场景）
manager.sendWordRequest(text) { }    // 推送文本（UTF-8；过滤 Emoji/超 0xFFFF 码位）
manager.setWordSpeedRequest(speed) { } // 滚动速度；queryWordSpeedRequest 查当前
manager.controlWordRequest(isStart) { } // 开始/停止滚动

// 屏幕控制
manager.switchScreen(mode) { }       // 屏显开关（0/1）
manager.setGlassResolution(level) { } // 分辨率（0~3 档：240p/320p/480p/640p）
manager.setGlassVolume(mode, volume) { } // 音量（mode 0=系统 1=媒体 2=通话）

// 对时 / 语言
manager.setGlassTime()               // 手机时间同步到眼镜
manager.setAbMateLanguage(code)      // 眼镜 UI 语言

// 录像横竖屏查询（部分固件支持；全志旧固件恒为横屏）—— 经 deviceCommManager 查询
```

#### 7.2.3 音频 / 降噪 / 按键

```kotlin
manager.setEqualizer(mode, gains) { }  // 均衡器（mode 见 PresetEqSetting）
manager.getEq()                        // 查询当前
manager.setEarphoneMode(mode)          // 降噪模式（ANC）
manager.setDetection(true) { }         // 入耳佩戴检测开关
manager.setEarOperation(keyType, keyFunction) { } // 按键自定义功能
manager.requireCallState()             // 查询通话状态（来电时设备侧行为依赖它）
manager.requireMediaType()             // 查询媒体播放状态
manager.requirePower()                 // 电量（onSuccess 回传）
```

#### 7.2.4 连接 / 绑定 / 恢复 / 查找

```kotlin
manager.setWorkMode(mode)              // 0 普通 / 1 游戏（低延迟）模式
manager.findEarPhone(mode) / stopFindEarPhone(mode)  // 查找设备（或统一用 finderAbility）
manager.earPhoneReset { /* 成功后可执行 connector.clear 解绑 */ }  // 恢复出厂
manager.deviceShutDown()               // 关机
// 绑定/回连/解绑：走 scanner/connector 的 authCode 机制
```

#### 7.2.5 SK 智能音箱（支持 SK 的耳机项目）

```kotlin
manager.sendBindSk() / reBindSk() / unBindSk()   // 绑定/回连/解绑 SK 音箱
manager.sendAppInfraredCode(code)                 // 下发红外码
manager.sendInfraredCodeStudy(mode)               // 红外学习开关
manager.sendAppRecordState(state)                 // 音响录音开关
// 音响的唤醒词/录音时长/音量等：见 SK 音箱协议说明
```

#### 7.2.6 ABMateDeviceRepository 主要 LiveData（UI 观察用）

- 设备信息：`devicePower`/`deviceName`/`deviceFirmwareVersion`/`glassSn`/`deviceProductColor`/`glassDeviceModel`/`supportLanguage`/`deviceIsTws`/`deviceTwsConnected`
- 音频状态：`deviceVolume`/`devicePlayState`/`deviceAncMode`/`deviceEqSetting`/`deviceKeySettings`/`deviceWorkMode`
- 摄像头/媒体：`aiPicPackage`（拍照分片 `AIPicPackage`）/`glassTakePhoto`/`videoLimitTime`/`audioLimitTime`/`isSupportAudio`/`isSupportOpus`

> 语音 AI 会话、音频上行与文本回推走 `wearKit.speechAiAbility`（见 4.7）；ASR/LLM/TTS 云端服务由客户自行接入。

---

## 8. 常见问题与提示

1. **能力不生效**：先查 `compat.isSupport()`；所有 `isSupport*` 类方法都要在调用前判断，不支持时会抛 `WKUnsupportedException`。
2. **RxJava 崩溃**：务必按 2.3 设置 `RxJavaPlugins.setErrorHandler`，忽略 `wearKit.rxJavaPluginsIgnoreExceptions()` 返回的异常集。
3. **通知类别缺失**（如主列表没有 Messenger/WhatsApp）：某个 App 的通知开关在主列表不显示，通常是**设备固件下发的通知支持能力位图**中该类别未开启（属设备能力配置，不是 App 问题）；此时仍可通过"更多 App"通用通道勾选该 App 的通知。
4. **数据同步慢**：用 `syncData(timeProvider, vararg types)` 一次性同步多类型，避免逐个 `syncItem`。
5. **权限**：扫描与连接按 [2.7 权限](#2-7-permission) 申请蓝牙权限（API 31+ 且 `BLUETOOTH_SCAN` 带 `neverForLocation` 时，扫描本身不申请定位）。相机预览需相机权限；`fileAbility` WiFi 拉取见 `compat.isRequireWifi()`；EPO / 设备持续定位等业务若使用定位，另按业务申请定位权限（见 2.7 对 sample 的说明）。
6. **录音文件拉取**：`fileAbility.pullFiles()` 拉完自动删设备原件；仅部分平台/设备支持（`isSupport()`）。
7. **协议疑问**：以本指南与公开 API / sample 为准；需对接未公开的协议说明时联系 TopStep。

### 冲突类处理

Realtek 库使用比较广泛，开发者反馈里冲突较多。`sdk-fitcloud-adapter`（经 `sdk-fitcloud`）会带入必须依赖 `ext-realtek-bbpro` 与 `ext-realtek-file`。宿主 App 如果已有同类 Realtek 类，则会报 **duplicate class**，可以在 adapter 上排除：

```kotlin
implementation("com.topstep.wearkit:sdk-fitcloud-adapter:$wearkitVersion") {
    exclude(group = "com.topstep.wearkit", module = "ext-realtek-bbpro")
    exclude(group = "com.topstep.wearkit", module = "ext-realtek-file")
}
```

本地 AAR 没有 POM exclude：默认必须添加 `sdk-realtek-bbpro-v*.aar` 与 `sdk-realtek-file-v*.aar`。仅当宿主已有等价类并发生重复类冲突时才省略。

### so 冲突

多个 native 库提供了同一个 `.so` 时，保留先出现的那一份：

```kotlin
android {
    packaging {
        jniLibs.pickFirsts.add("**/libc++_shared.so")
    }
}
```

### Java 资源冲突

多个 Netty 模块各自带一份 `META-INF/io.netty.versions.properties` 时，`merge*JavaResource` 会失败。这些文件的键互不冲突，合并即可：

```kotlin
android {
    packaging {
        resources.merges.add("META-INF/io.netty.versions.properties")
    }
}
```

### 无法找到依赖

解析不到 WearKit 依赖时，添加仓库 `https://maven.topstepht.com/repository/maven-public/`：

```kotlin
maven {
    url = uri("https://maven.topstepht.com/repository/maven-public/")
}
```

---

*文档生成自 sdk-apis 源码注释；具体行为以各平台 adapter 实现为准。*
