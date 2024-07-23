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