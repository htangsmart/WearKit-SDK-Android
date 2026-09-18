# v3.0.2.7

2026-09-17

1. ADD: Add ASR text send for recording in `WKSpeechAiAbility.Record.sendTextSource`
2. ADD: Add pause / resume / duration APIs in `WKSpeechAiAbility.Record`
3. ADD: Add `UGreenAbility.requestConfig` / `setConfig` with `UGreenConfig`
4. ADD: Add continuous location in `WKLocationProvider.observeLocation` (sdk-prototb-adapter)
5. ADD: Add third-party data exchange in `WKDeviceAbility` (`observeThirdPartyData` / `sendThirdPartyData`), currently `WKThirdPartyData.Type.STAR_BURST`
6. ADD: sdk-abmate-adapter now implements `WKSpeechAiAbility`
7. CHANGE: sdk-prototb-adapter `WKDialStyleAbility.createCustom` now supports concurrent packing
8. CHANGE: Optimize W30 video dial frame extraction
9. CHANGE: Improve `WKSpeechAiMessage.Type.ASK_SWITCH_MODEL` AI model naming
10. CHANGE: Optimize sdk-abmate-adapter packet parsing to reduce sticky-packet issues
11. FIX: Fix ProtoTB remote camera preview backlog and packet pacing
12. FIX: Prevent ProtoTB stale preview start callback from stopping a new session
13. FIX: Drop duplicate device frames by sequence to fix sync failure during calls (ONES#200945)
14. FIX: Fix weather hourly encoding for firmware that parses a fixed 14-byte item
15. FIX: Fix AbMate missing default `WKLocationMapAbility` causing crash
16. FIX: Fix wrong `DIAL_COMPONENT` feature flag on 8AA9 project
17. FIX: Update android-gif-drawable for 16k page-size support
18. FIX: Fix Station mode missing BSSID causing repeated system popups
19. FIX: Sport detail v2.0 unknown type no longer fails the entire parse

# v3.0.2.6

2026-09-09

1. ADD: Add `UGreenAbility` for customized ability (b2b)
2. ADD: Add new vendor id in `WKProductType`
3. ADD: Add offline map feature in FitCloud, including map size and device free space in `WKLocationMapAbility`
4. ADD: Add camera zoom command in sdk-prototb-adapter
5. CHANGE: sdk-prototb-adapter danmu dial now supports multiple danmu and trigger animation
6. ADD: Add more notification type support in sdk-prototb-adapter
7. FIX: Fix wrong package name for Drive/Gpay in `CommonAppPackage`
8. ADD: Add more `FcShape` values (47, 48) in sdk-fitcloud
9. ADD: Add custom dial id support
10. ADD: Add audio scene translation (dialog translation)
11. ADD: Add opus bitrate setting for recording (FitCloud only currently)
12. FIX: Fix abnormal TTS stop during translation
13. FIX: Fix lost `.mp3` suffix for long music file names
14. FIX: Preserve weight one-decimal-place protocol data (ONES#199825)
15. FIX: Hide Snapchat/Messenger business card types on FitCloud platform
16. FIX: AI dial preview image now masked to device shape
17. CHANGE: `WKDialQuality` HD level renamed to SD (due to video compression quality gap)

# v3.0.2.5

2026-08-12

1. ADD: Add more constant definitions in `WKSportType`
2. ADD: Add `TitanAbility` for customized ability
3. ADD: Add EPO features in `WKLocationMapAbility`
4. ADD: Add compat of `getPasswordLength` in `WKLockAbility`
5. ADD: Add compat of `isSupport` in `WKRaiseWakeupAbility`
6. CHANGE: Modify preview api in `WKCameraAbility`
7. ADD: Add video control in `WKCameraAbility`.
8. ADD: Add `WKSpeechAiAbility` for speech AI features (chat / translate / dial / ask / record)
9. ADD: Add connect without user info in `WKConnector`
10. CHANGE: Modify constructor of `WKUnitConfig`, and add compat of split unit of length/weight.
11. Add: Add quality params of `WKDialStyleAbility.CreateInput`. See `WKDialStyleAbility.Compat.getQualityLevels`
12. Add: `WKBusinessCard` add type `ZALO`

# v3.0.2.4

2026-07-06

1. ADD: Add `WKLockAbility`
2. ADD: Add `HsdAbility` for customized ability
3. ADD: Add `WKLocationMapAbility`
4. ADD: Add `WKMuslimAbility` instead of `WKPrayerAbility`
5. ADD: Add HRV features. `WKHeartRateAbility.setHRVConfig`,`WKSyncData.HRV`
6. CHANGE: `WKAlarm` add `type` field
7. CHANGE: The Weather add more field. Such as `quality`, `humidity`...
8. CHANGE：`WKAlarmAbility.getAlarmMaxNumber` now may return 0.
9. Fix: Fix some bugs

# v3.0.2

2026-01-15

1. CHANGE: sdk-prototb-adapter first release version and supports 16k
2. Fix: Fix some bugs

# v3.0.1

2025-10-29

1. CHANGE: Optional SDK supports 16k
2. Add: a new SDK compatible with the new watch platform
   implementation("com.topstep.wearkit:sdk-prototb-adapter:$latest_version")
3. CHANGE: 'WKSportRecord' add 'speed'
4. Fix: Fix some bugs

# v3.0.1-beta14

2025-09-22

1. Adapt to Android 15 and 16k policies
2. CHANGED: updata Realtek sdk version from 1.0.3 to 1.0.4
3. fix some bugs

# v3.0.1-beta13

2025-05-19

1. CHANGED: updata Realtek sdk version
2. fix some bugs

# v3.0.1-beta09

2024-08-14

1. CHANGED: Add Realtek file transfer dependencies in “sdk-fitcloud”. If there is a compilation error for duplicate classes, exclude them like this
   ```groovy
   //"sdk-fitcloud" included in "sdk-fitcloud-adapter", so just exclude them in "sdk-fitcloud-adapter" is OK.
   implementation("com.topstep.wearkit:sdk-fitcloud-adapter:$latest_version") {
       exclude group: "com.topstep.wearkit", module: "ext-realtek-bbpro"
       exclude group: "com.topstep.wearkit", module: "ext-realtek-file"
   }
   ```
2. CHANGE: `WKMusicAbility` remove useless parameters
3. CHANGE: The definition of `WKWeatherCode` has changed.
    ```kotlin
   CLEAR_DAY change to CLEAR
   PARTLY_CLOUDY_DAY change to CLOUDY
   CLOUDY change to OVERCAST
   
   LIGHT_RAIN,MODERATE_RAIN change to RAIN
   STORM_RAIN removed, use HEAVY_RAIN instead
   
   LIGHT_HAZE,MODERATE_HAZE,HEAVY_HAZE change to HAZE
   
   LIGHT_SNOW,MODERATE_SNOW change to RAIN
   STORM_SNOW removed, use HEAVY_SNOW instead
   
   FOG change to SMOKE_FOG
   SAND,DUST change to SAND_DUST
   
   SLEET change to FREEZING_RAIN
   
   HAIL_SLEET added
    ```
4. CHANGED: Update "sdk-aliagent-v1.0.5.aar", fix error on Android 14. (Very few watches have this feature)
5. ADD: Add `WKEBookAbility`,`WKAlbumAbility`

# v3.0.1-beta08

2024-08-01

1. ADD: Add `WKBloodPressureAbility`
2. FIX: Fix bug of `WKDndAbility`

# v3.0.1-beta07

2024-07-23

1. ADD: Add auto download when use `WKDialStyleAbility.createCustom`
2. OPTIMIZE: Optimize FlyWear-SDK connection speed
3. FIX: Fix bug of ShenJu-SDK init on work thread.
4. FIX: Fix bug of ShenJu-SDK has none default shape.
5. FIX: Fix bug of FitCloud-SDK pace of sport data

# v3.0.1-beta06

2024-07-16

1. ADD: `WKWeatherHour` add `windScale`,`ultraviolet` and `visibility` fields
2. ADD: `WKSportType` add `SWIM_IN_POOL` and `SWIM_OPEN_WEATHER` constants
3. ADD: `WKWearKit` add `release` method for help release sdk
4. ADD: `WKConnector` add `isBindOrLogin` method to determine whether this connection is in BIND mode or LOGIN mode
5. ADD: `WKSportRecord` add more sport fields
6. ADD: `WKSportUIAbility` for sport ui push
7. CHANGE: `WKSportType` constant value changed
8. CHANGE: `WKDialStyleAbility` apis changed. `WKDialStyleProvider` remove. How to migrate from beta05 ? Please refer to the sample.
9. CHANGE: `WKConnector` modify `close(clearAuth,removeBond)` to `clear(clearAuth)`
10. FIX: Fix some bugs

# v3.0.1-beta05

2024-06-28

1. ADD: `WKDialStyleAbility` for custom watchface

# v3.0.1-beta04

2024-06-24

1. CHANGE: Move `WKLanguageAbility` static method to `LanguageUtil`
2. CHANGE: `WKUnSupportException` rename to `WKUnsupportedException`
3. CHANGE: `WKOtaException` rename to `WKFileTransferException`
4. CHANGE:`WKGoalConfig` rename to `WKActivityGoalConfig`, and a slight change in the corresponding name of the `WKActivityAbility` method
5. CHANGE:`WKDeviceAbility.syncItem` and `syncData` need start and end time now.
6. CHANGE:`WKSyncData.toSleep` return `List<WKSleepDaily>` now.
7. ADD: `WKMusicAbility.Compat` add `isSupportRequest` ,`isSupportDelete` methods
8. ADD: `WKBloodOxygenAbility.Compat` add `isSupportMeasure`, `isSupportMonitorConfig` methods
9. ADD: `WKPressureAbility.Compat` add `isSupportMeasure`, `isSupportMonitorConfig` methods
10. ADD: `WKHeartRateAbility.Compat` add `isSupportMeasure`, `isSupportMonitorConfig`, `isSupportAlarmConfig` methods
11. ADD: `WKSleepAlgorithm.SHEN_JU` type added.
12. ADD: `WKActivityHelper` use for help to calculate `WKDailyActivity`
13. ADD: `WKSportAbility`