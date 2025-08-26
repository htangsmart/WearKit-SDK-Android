package com.topstep.wearkit.sample.sdk

import androidx.annotation.CheckResult
import com.topstep.fitcloud.sdk.v2.FcSDK
import com.topstep.fitcloud.sdk.v2.model.message.FcMessageType
import com.topstep.wearkit.apis.WKWearKit
import io.reactivex.rxjava3.core.Observable

@CheckResult
fun WKWearKit.observeSOS(): Observable<Any> {
    return observeRawSDK().flatMap { rawSDK ->
        val fcSDK = rawSDK as? FcSDK
        if (fcSDK == null) {
            Observable.never()
        } else {
            fcSDK.connector.messageFeature().observerMessage().filter {
                it.type == FcMessageType.SOS
            }
        }
    }
}