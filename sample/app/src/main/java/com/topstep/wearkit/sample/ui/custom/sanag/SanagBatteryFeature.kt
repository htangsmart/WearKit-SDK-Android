package com.topstep.wearkit.sample.ui.custom.sanag

import com.topstep.wearkit.apis.model.WKBattery
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivitySanagDemoBinding
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.asFlow
import timber.log.Timber

/**
 * Request and observe battery.
 */
internal class SanagBatteryFeature(
    private val activity: SanagDemoActivity,
    private val viewBind: ActivitySanagDemoBinding,
) : SanagDemoFeature {

    private val wearKit = MyApplication.wearKit
    private var batteryDisposable: Disposable? = null

    override fun onCreate() {
        viewBind.btnBattery.setOnClickListener {
            if (!activity.requireDeviceConnected()) return@setOnClickListener
            batteryDisposable?.dispose()
            batteryDisposable = wearKit.batteryAbility.requestBattery()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    updateBatteryText(it)
                }, {
                    Timber.w(it)
                    activity.toast(R.string.tip_failed)
                })
        }
    }

    override fun observe(scope: CoroutineScope) {
        scope.launch {
            wearKit.batteryAbility.observeBatteryChange()
                .asFlow()
                .catch { Timber.w(it) }
                .collect { updateBatteryText(it) }
        }
    }

    override fun onDestroy() {
        batteryDisposable?.dispose()
        batteryDisposable = null
    }

    private fun updateBatteryText(battery: WKBattery) {
        viewBind.tvBattery.text = "Battery level:${battery.percentage} isCharging:${battery.isCharging}"
    }
}
