package com.topstep.wearkit.sample.ui.others

import com.topstep.wearkit.base.utils.FixedHashMap
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.ui.special.QrCodeAbilityDemoActivity
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

class TitanQrCodeActivity : QrCodeAbilityDemoActivity() {

    override fun titleRes(): Int = R.string.qr_code_base

    override fun requestSupport(): Single<FixedHashMap<String, String>> {
        return wearKit.b2b.titanAbility.requestQrCode()
    }

    override fun setQrCode(map: FixedHashMap<String, String>): Completable {
        return wearKit.b2b.titanAbility.setQrCode(map)
    }
}
