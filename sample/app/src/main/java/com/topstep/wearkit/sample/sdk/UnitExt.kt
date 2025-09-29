package com.topstep.wearkit.sample.sdk

import androidx.annotation.CheckResult
import com.topstep.fitcloud.sdk.v2.FcSDK
import com.topstep.fitcloud.sdk.v2.model.config.FcFunctionConfig
import com.topstep.fitcloud.sdk.v2.model.config.toBuilder
import com.topstep.wearkit.apis.WKWearKit
import com.topstep.wearkit.apis.model.config.WKUnitConfig
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable

/**
 * The WKUnitConfig only has `isMetric` and  `isCentigrade` field.
 *
 * But the [FcSDK] has extension unit settings. The file show you how to use [WKWearKit.getRawSDK] to use raw apis which [WKWearKit] don't provide
 */

// set only apparent temperature unit
fun WKWearKit.setApparentTemperatureUnit(isCentigrade: Boolean): Completable {
    val fcSDK = this.getRawSDK() as? FcSDK
    if (fcSDK == null) return Completable.complete()
    val c = fcSDK.functionAbility.getConfig()
        .toBuilder().setFlagEnabled(FcFunctionConfig.Flag.APPARENT_TEMPERATURE_UNIT, !isCentigrade)
        .create()
    return fcSDK.functionAbility.setConfig(c)
}

// get only apparent temperature unit. True for centigrade, false for fahrenheit.
fun WKWearKit.getApparentTemperatureUnit(): Boolean {
    val fcSDK = this.getRawSDK() as? FcSDK
    if (fcSDK == null) return false
    return !fcSDK.functionAbility.getConfig().isFlagEnabled(FcFunctionConfig.Flag.APPARENT_TEMPERATURE_UNIT)
}

// observe apparent temperature unit. True for centigrade, false for fahrenheit.
fun WKWearKit.observeApparentTemperatureUnit(replay: Boolean): Observable<Boolean> {
    return observeRawSDK().flatMap { rawSDK ->
        val fcSDK = rawSDK as? FcSDK
        if (fcSDK == null) {
            Observable.just(false)
        } else {
            fcSDK.functionAbility.observeConfig(replay).map { !it.isFlagEnabled(FcFunctionConfig.Flag.APPARENT_TEMPERATURE_UNIT) }
        }
    }
}

// Combine like previous SDK version
data class MyUnitConfig(
    val isMetric: Boolean = true,
    val isCentigrade: Boolean = true,
    val isApparentTemperatureCentigrade: Boolean = true,
)

fun WKWearKit.getUnitConfig(): MyUnitConfig {
    val config = this.unitAbility.getConfig()
    return MyUnitConfig(
        isMetric = config.isMetric,
        isCentigrade = config.isCentigrade,
        isApparentTemperatureCentigrade = this.getApparentTemperatureUnit()
    )
}

@CheckResult
fun WKWearKit.setUnitConfig(config: MyUnitConfig): Completable {
    return this.unitAbility.setConfig(
        WKUnitConfig(
            isMetric = config.isMetric,
            isCentigrade = config.isCentigrade,
        )
    ).andThen(Completable.defer { this.setApparentTemperatureUnit(config.isApparentTemperatureCentigrade) })
}

@CheckResult
fun WKWearKit.observeUnitConfig(replay: Boolean): Observable<MyUnitConfig> {
    return this.unitAbility.observeConfig(replay).zipWith(observeApparentTemperatureUnit(replay)) { config, others ->
        MyUnitConfig(
            isMetric = config.isMetric,
            isCentigrade = config.isCentigrade,
            isApparentTemperatureCentigrade = others
        )
    }
}