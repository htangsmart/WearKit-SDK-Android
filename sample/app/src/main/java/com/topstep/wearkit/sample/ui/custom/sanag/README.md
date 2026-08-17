# 塞那对接文档

Sample 入口：主界面菜单 → **塞那定制**（`SanagDemoActivity`）。
当前 Demo 已覆盖：

* 扫描 / 连接 / 断开
* 电量
* 版本和OTA
* 文件
* AI音频

---

## 接入准备

### 1. 依赖

至少引入 `sdk-core` 与 `sdk-fitcloud-adapter`：

```kotlin
val wearkitVersion = "3.0.2.5"
implementation("com.topstep.wearkit:sdk-core:$wearkitVersion")
implementation("com.topstep.wearkit:sdk-fitcloud-adapter:$wearkitVersion")
```

### 2. 初始化

塞那走 FitCloud 协议，初始化时必须加入 `WKFitCloudKit.Builder`。完整流程见 `WearKitInit.kt`，要点：

1. 先 `Timber.plant(...)`（SDK 用 Timber 打日志）。
2. `buildWKWearKit(builders)` 创建 `WKWearKit`。
3. 将 `wearKit.rxJavaPluginsIgnoreExceptions()` 注册到 `RxJavaPlugins.setErrorHandler`，避免 BLE 不可投递异常把 App 打崩。

```kotlin
val builders = ArrayList<WKWearKit.Builder>()
builders.add(WKFitCloudKit.Builder(application, processLifecycleObserver, rxBleClient))
val wearKit = buildWKWearKit(builders)

val ignoreExceptions = HashSet<Class<out Throwable>>()
ignoreExceptions.addAll(wearKit.rxJavaPluginsIgnoreExceptions())
RxJavaPlugins.setErrorHandler(/* 忽略 ignoreExceptions 中的类型 */)
```

### 3. 权限

扫描和连接前申请 BLE 权限：

| 系统版本                       | 权限                                                         |
|----------------------------|------------------------------------------------------------|
| Android 12 以下（API &lt; 31） | `ACCESS_COARSE_LOCATION`、`ACCESS_FINE_LOCATION`；扫描时需打开定位服务 |
| Android 12 及以上             | `BLUETOOTH_SCAN`、`BLUETOOTH_CONNECT`                       |

Demo 通过 `PermissionHelper.requestBle()` 申请。连接前必须再次确认权限已授予。

---

## 连接

### 1.扫描

使用 `RxBleClient` 进行蓝牙扫描，也可以自行使用Android原生扫描接口。

```kotlin
// 扫描前断开
wearKit.connector.close()
val scanSettings = ScanSettings.Builder()
    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
    .setShouldCheckLocationServicesState(false)
    .build()
rxBleClient.scanBleDevices(scanSettings)
```

### 2.发起连接

```kotlin
wearKit.connector.connect(
    type = WKDeviceType.FIT_CLOUD,   // 塞那固定走 FitCloud
    address = device.address,        // 扫描得到的 MAC
    authMode = WKAuthMode.AUTO,      // 首次绑定 / 回连登录由 SDK 判断
    authCode = null,                 // 无二维码绑定时传 null
    userId = user.id.toString(),     // 业务用户 ID，回连必须与绑定时一致
)
```

参数说明：

| 参数         | 取值          | 说明                                  |
|------------|-------------|-------------------------------------|
| `type`     | `FIT_CLOUD` | 塞那走 FitCloud 适配器                    |
| `address`  | 扫描 MAC      | 必填                                  |
| `authMode` | 建议 `AUTO`   | `BIND` 首次绑定，`LOGIN` 回连；不确定时用 `AUTO` |
| `authCode` | 一般 `null`   | 仅在从设备二维码精确拿到授权码时传入                  |
| `userId`   | 业务用户 ID     | 回连校验用，不要每次随机生成                      |

### 3.监听连接状态

```kotlin
wearKit.connector.observeConnectorState()
    .startWithItem(wearKit.connector.getConnectorState())
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe { state ->
        when (state) {
            WKConnectorState.DISCONNECTED -> { /* 未连接 */
            }
            WKConnectorState.PRE_CONNECTING,
            WKConnectorState.CONNECTING,
            WKConnectorState.PRE_CONNECTED
                -> { /* 连接中，PRE_CONNECTED 表示 GATT 已通但尚未准备好 */
            }
            WKConnectorState.CONNECTED -> { /* 可交互 */
            }
        }
    }
```

| 状态               | 含义                             |
|------------------|--------------------------------|
| `DISCONNECTED`   | 未连接（含 `close()`、蓝牙关闭）          |
| `PRE_CONNECTING` | 等待下次自动重连                       |
| `CONNECTING`     | 正在扫描/连接                        |
| `PRE_CONNECTED`  | GATT 已连，正在绑定/登录/基础配置，此时不要发业务指令 |
| `CONNECTED`      | 已就绪                            |

可用 `wearKit.connector.observeConnectorError()` 观察连接失败原因。

### 4.断开

```kotlin
wearKit.connector.close()
```

`close()` 会断开并清除当前设备，不会自动重连。需要保留设备、稍后重连时才考虑 `disconnect()` / `reconnect()`（一般用 `close()` 即可）。

换绑或恢复出厂用 `wearKit.connector.clear(removeBond)`。

---

## 电量

`WKBattery`：

- `percentage`：0–100
- `isCharging`：是否充电中

### 1.主动请求

```kotlin
wearKit.batteryAbility.requestBattery()
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe({ battery ->
        // battery.percentage / battery.isCharging
    }, { error ->
        Timber.w(error)
    })
```

### 2.监听变化

设备主动上报时走 `observeBatteryChange()`。

```kotlin
wearKit.batteryAbility.observeBatteryChange()
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe({ battery ->
        // 刷新 UI
    }, { Timber.w(it) })
```

---

## 版本和OTA

连接成功后从 `WKDeviceInfo` 读取：

- `model`：项目号
- `version`：版本号

```kotlin
// 连接就绪后立刻读一次
val info = wearKit.deviceAbility.getDeviceInfo()
// info.model / info.version

// 设备信息变化时刷新（如重连后）
wearKit.deviceAbility.observeDeviceInfo(false)
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe({ info ->
        // info.model / info.version
    }, { Timber.w(it) })
```

---

使用 `wearKit.otaAbility.ota(file)` 进行 OTA ，进度为 0–100。

```kotlin
val dialog = ProgressDialog(context).apply {
    setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
    setCancelable(false)
    setCanceledOnTouchOutside(false)
    max = 100
    show()
}
copyUriToFile(uri)
    .flatMapObservable { wearKit.otaAbility.ota(it) }
    .observeOn(AndroidSchedulers.mainThread())
    .doFinally { dialog.dismiss() }
    .subscribe({ progress ->
        dialog.progress = progress
    }, { error ->
        Timber.w(error)
    })
```

注意：

- 必须已连接（`WKConnectorState.CONNECTED`）。
- 升级过程中设备可能断开，属正常现象；完成后重新连接再读版本号确认。
- 建议电量高于 20%，手机与设备距离保持在 0.5 米内。

---

## 文件

使用 `wearKit.fileAbility` 管理设备上的媒体文件（列表 / 拉取 / 删除）。

调用前先检查 `compat.isSupport()`。`requestFilesCount()`、`deleteFile()` 不需要 WiFi；`requestFiles()` / `pullFiles()` 在 `compat.isRequireWifi()` 为 `true` 时需要 WiFi 权限。

### 1. 获取数量

```kotlin
wearKit.fileAbility.requestFilesCount()
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe({ count ->
        // 设备上的文件数量
    }, { Timber.w(it) })
```

### 2. 拉取文件

`pullFiles(saveDir)`：`saveDir` 为 `null` 时保存到 `Context.getExternalCacheDir()`。每拉完一个文件会删除设备上的原文件。

```kotlin
fun ensureFileWifiReady(fileAbility: WKFileAbility, onReady: () -> Unit) {
    if (!fileAbility.compat.isRequireWifi()) {
        onReady()
        return
    }
    PermissionHelper.requestFileWifi(context) { granted ->
        if (granted) onReady()
    }
}

ensureFileWifiReady(wearKit.fileAbility) {
    wearKit.fileAbility.pullFiles(null)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({ event ->
            when (event) {
                is WKFileTransferEvent.OnFileProgress -> {
                    // event.index / event.count / event.progress
                }
                is WKFileTransferEvent.OnFileCompleted -> {
                    // event.devicePath / event.savePath
                }
                is WKFileTransferEvent.OnAllCompleted -> {
                    // event.savePaths
                }
            }
        }, { Timber.w(it) })
}
```

注意：

- 必须已连接（`WKConnectorState.CONNECTED`）。
- `isRequireWifi() == true` 时需声明 `ACCESS_WIFI_STATE`、`CHANGE_WIFI_STATE`，并申请 `NEARBY_WIFI_DEVICES`（API 33+）或 `ACCESS_FINE_LOCATION`。

---

## AI音频

入口：塞那 Demo → **对话**（`SanagSpeechAiActivity`）。塞那目前只对接设备发起的 **CHAT**。

WearKit 只负责设备侧会话和音频传输。ASR / LLM 由 App 自行接入。

### 1. 能力检查

```kotlin
val speechAi = wearKit.speechAiAbility
if (!speechAi.isSupport()) {
    // 设备不支持语音 AI，无需初始化后续逻辑
    return
}
// 可选：是否展示对话入口
speechAi.session.isSupportDeviceScene(WKSpeechSession.Scene.CHAT)
```

### 2. 订阅设备会话（必须）

`observeDeviceSession()` **必须在整个可用期间保持订阅**。没有观察者时，设备开麦会被 SDK 忽略，设备侧会自己超时。

同一时刻最多一个会话。发出的 `WKSpeechSession` 尚未开始，必须尽快调用 `audio()`，否则会被自动释放。

```kotlin
speechAi.session.observeDeviceSession()
    .subscribe({ session ->
        if (session.scene != WKSpeechSession.Scene.CHAT) return@subscribe
        startChat(session)
    }, { Timber.w(it) })
```

| 属性 / 方法           | 说明                                            |
|-------------------|-----------------------------------------------|
| `origin`          | `DEVICE` 设备发起 / `APP` App 发起                  |
| `scene`           | 场景，塞那只用 `CHAT`                                |
| `format`          | 音频格式，**仅在 `audio()` 发出第一帧后才有值**               |
| `audio()`         | 订阅一次，开始收音频；再次订阅会 `ERROR_STATE`                |
| `release(reason)` | 结束会话，幂等；dispose `audio()` 等价于 `release(NONE)` |
| `isActive()`      | 是否为当前活跃会话                                     |

`format`：

| 类型                            | 参数                            |
|-------------------------------|-------------------------------|
| `WKSpeechSession.Format.PCM`  | 单声道、16 kHz、16-bit             |
| `WKSpeechSession.Format.OPUS` | 单声道、16 kHz，`frameSize` 为每帧字节数 |

### 3. 接收音频

```kotlin
fun startChat(session: WKSpeechSession) {
    session.audio().subscribe({ bytes ->
        // bytes 按 session.format 解码后交给自己的 ASR / LLM
        val format = session.format // 首帧之后才非 null
    }, { error ->
        // 异常结束：低电、来电、断连、超时等，见 WKSpeechSession.Exception
        Timber.w(error)
    }, {
        // 正常结束（设备关麦，或 App 调用了 release / dispose）
    })
}
```

`audio()` 的 `onError` 为 `WKSpeechSession.Exception`：

| errorCode            | 含义             |
|----------------------|----------------|
| `ERROR_BATTERY`      | 设备低电关闭         |
| `ERROR_INCOMING`     | 来电关闭           |
| `ERROR_CONFLICT`     | 设备状态冲突，无法开始    |
| `ERROR_DISCONNECTED` | 连接断开           |
| `ERROR_STATE`        | 重复订阅 `audio()` |
| `ERROR_UNKNOWN`      | 未知错误           |

正常关麦走 `onComplete`，不会走 `onError`。

### 4. 离场

音频流结束只表示这一路音频停了。页面 / AI 逻辑应在 `observeMessage()` 收到对应场景的 `SCENE_EXIT` 后再退出。

收到 `SCENE_EXIT` 时，SDK 也会结束仍活跃的音频会话（已关闭则幂等）。

```kotlin
speechAi.observeMessage().subscribe({ msg ->
    if (msg.type == WKSpeechAiMessage.Type.SCENE_EXIT &&
        msg.data == WKSpeechSession.Scene.CHAT
    ) {
        // 结束本次对话
    }
}, { Timber.w(it) })
```

也可主动 `session.release(WKSpeechSession.Reason.NONE)`。