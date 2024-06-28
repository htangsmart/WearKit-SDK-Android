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