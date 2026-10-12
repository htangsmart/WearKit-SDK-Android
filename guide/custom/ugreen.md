# WearKit SDK Android 接入指南(绿联)

> GitHub：[https://github.com/htangsmart/WearKit-SDK-Android/](https://github.com/htangsmart/WearKit-SDK-Android/)
>
> 版本：`CHANGED.md` 公开版 **3.0.2.7**

---


## 1. WearKit 说明

见 [wearkit-cn.md §1](../wearkit-cn.md#1-about-wearkit)。

绿联接入只使用 `FIT_CLOUD` 设备平台即可。

---

## 2. 快速集成

见 [wearkit-cn.md §2 快速集成](../wearkit-cn.md#2-quick-integration)。

绿联接入只使用 `FIT_CLOUD` 设备平台，Maven 接入只放 `FIT_CLOUD` 相关依赖即可。

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

```kotlin
dependencies {
    val wearkitVersion = "3.0.2.7"
    implementation("com.topstep.wearkit:sdk-core:$wearkitVersion")
    implementation("com.topstep.wearkit:sdk-fitcloud-adapter:$wearkitVersion")
}
```

---


## 3. 功能说明

### 3.1 WKDeviceAbility 设备管理

- `shutdown()` 关机 / `reset()` 复位 / `reboot()` 重启设备
- `getDeviceInfo()` / `observeDeviceInfo(replay)` 设备基础信息（型号/固件版本/序列号等）
- `observeThirdPartyData()` / `sendThirdPartyData(data)` 与设备侧第三方能力交换数据（当前类型见 `WKThirdPartyData.Type`，如 `STAR_BURST`）；不支持时观察流不发射、发送抛
`WKUnsupportedException`

### 3.2 WKBatteryAbility 电量

- `requestBattery()` 查询一次电量
- `observeBatteryChange()` 监听电量变化（旧设备无真实监听时为轮询，**页面隐藏时应尽快退订**）

### 3.3 情绪窗、壁纸

情绪窗、壁纸都是通过表盘功能实现。
绿联项目的表盘有两个推送位，第0个用于壁纸功能，第1个用于情绪窗功能

见 [wearkit-cn · 4.5 表盘能力](../wearkit-cn.md#4-5-dial)。

### 3.4 语音 AI 能力（speech）

- 设备侧会话：见 [wearkit-cn · 4.7 语音 AI](../wearkit-cn.md#4-7-speech-ai)
- 云端 AiKit：见 [aikit-cn](../aikit-cn.md)

### 3.5 b2b.ugreenAbility 弹幕和配置

使用 `wearkit.b2b.ugreenAbility`：

- 弹幕：`clearDanMu(type)` / `addDanMu(items)`（空列表不发包）/ `compat.isSupportDanMu()`；
- 配置：`requestConfig()` / `setConfig(UGreenConfig)`；

---