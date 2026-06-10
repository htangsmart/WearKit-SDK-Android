package com.topstep.wearkit.sample.ui.special

import com.topstep.wearkit.base.utils.FixedHashMap
import com.topstep.wearkit.sample.R
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

class BusinessCardActivity : QrCodeAbilityDemoActivity() {

    override fun titleRes(): Int = R.string.business_card

    override fun requestSupport(): Single<FixedHashMap<String, String>> {
        return wearKit.businessCardAbility.request()
    }

    override fun setQrCode(map: FixedHashMap<String, String>): Completable {
        return wearKit.businessCardAbility.set(map)
    }
}
