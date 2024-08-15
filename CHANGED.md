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