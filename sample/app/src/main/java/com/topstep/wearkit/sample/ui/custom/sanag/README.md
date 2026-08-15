# 塞那对接文档

Sample 入口：主界面菜单 → **塞那定制**（`SanagDemoActivity`）。
当前 Demo 已覆盖：
* 扫描 / 连接 / 断开
* 电量

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