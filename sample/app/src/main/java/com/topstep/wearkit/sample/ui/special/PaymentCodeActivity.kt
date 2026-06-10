package com.topstep.wearkit.sample.ui.special

import com.topstep.wearkit.base.utils.FixedHashMap
import com.topstep.wearkit.sample.R
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

class PaymentCodeActivity : QrCodeAbilityDemoActivity() {

    override fun titleRes(): Int = R.string.payment_code

    override fun requestSupport(): Single<FixedHashMap<String, String>> {
        return wearKit.paymentCodeAbility.request()
    }

    override fun setQrCode(map: FixedHashMap<String, String>): Completable {
        return wearKit.paymentCodeAbility.set(map)
    }
}
