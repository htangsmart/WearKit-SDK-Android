# WearKit SDK Android 接入指南(绿联)

> GitHub：[https://github.com/htangsmart/WearKit-SDK-Android/](https://github.com/htangsmart/WearKit-SDK-Android/)
>
> 版本：`CHANGED.md` 公开版 **3.0.2.8**

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
    val wearkitVersion = "3.0.2.8"
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

### 3.6 OTA 固件升级

绿联走 FitCloud 统一入口，无单独 b2b OTA API。通用说明见 [wearkit-cn · 4.4 `WKOtaAbility`](../wearkit-cn.md#4-4-ota)。

- `wearKit.otaAbility.ota(file) : Observable<Int>`：固件升级，进度 0–100；传输失败抛 `WKFileTransferException`
- 须已连接（`WKConnectorState.CONNECTED`）
- 升级过程中设备可能断开，属正常现象；完成后重新连接，用 `deviceAbility.getDeviceInfo()` / `observeDeviceInfo` 核对 `version`
- 示例见 WearKit sample `ui/ota/LocalOtaActivity`（sample only）

```kotlin
wearKit.otaAbility.ota(file)
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe({ progress -> /* 0-100 */ }, { /* WKFileTransferException 等 */ })
```

## 4. 版本变更

### 2026/10/15
wearkit:3.0.2.8
* 绿联功能无修改。

aikit:1.0.3
* 火山修复vad可能导致asr失败的问题

sample_2026_10_15
* 删除TranslateTtsController，简化翻译功能tts播放逻辑
* 优化翻译功能tts播放录音，避免sco和手机扬声器的冲突场景
* 优化对话翻译响应速度

绿联反馈问题修复：
翻译：
1. 开启（耳机）： 开启后，大概录制了两三句（包括 tts 播放），会话主动结束了；
已解决：问题原因是耳机录音和手机扬声器播放是冲突的，修改了tts播放通道，播放到耳机，而不是手机扬声器。

2. 开启（手机/设备）： 大部分时间正常，偶现主动结束；
未解决：不容易复现，继续测试排查。

对话翻译：
1. 面对面（手机+充电仓）： 
	自身（手机）： 长按后，任务结束了，并且回到了 Speech+Ai 页面；
已解决：问题原因是设备超时5秒自动结束对话翻译。更新固件到102版本，此版本改为25超时。并且demo优化了响应速度。

2. 私密听译（耳机+充电仓）
	自身（耳机）： 长按说话，有对应原文和译文输出，但是输出后，立马回到了 Speech+Ai 页面；
已解决：同上。

3. 便携交流（手机+耳机）： 整体流程没问题（长按说话松手后，asr 文本和翻译文本需要等一段时间才回来，大概六七秒）；
已解决：同上。

其他已知问题：
1. 使用耳机拾音时，sco状态不稳定可能会中断流程，sdk待优化
2. 使用耳机或设备拾音时，可能会丢失前一两个字，sdk待优化
3. 问答功能说完问题要多按一伙，否则请求容易取消，demo待优化

### 2026/10/27
wearkit:3.0.2.8-SNAPSHOT
* 修复耳机录音状态不稳定时重试，避免直接中断体验不好

aikit:1.0.3-SNAPSHOT
* 解决对话/问答说完话立即结束音频，最后一个问题不会识别的bug。
* 解决对话/问答偶尔网络超时太长的问题
* 增加AiChatResult.OnWarning和AiAsrResult.OnWarning信息

sample_2026_10_27
* AI处理添加错误/警告信息显示
* 解决对话/问答偶尔网络超时太长的问题

其他已知问题：
1. 使用耳机或设备拾音时，可能会丢失前一两个字，sdk待优化

---