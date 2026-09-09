package com.topstep.wearkit.sample.ui.custom.sanag

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivitySanagDemoBinding
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import timber.log.Timber

/**
 * Others setting: factory reset and unbind.
 */
internal class SanagOthersSettingFeature(
    private val activity: SanagDemoActivity,
    private val viewBind: ActivitySanagDemoBinding,
) : SanagDemoFeature {

    private val wearKit = MyApplication.wearKit
    private var resetDisposable: Disposable? = null
    private var unbindDisposable: Disposable? = null

    override fun onCreate() {
        viewBind.btnOthersReset.setOnClickListener {
            if (!activity.requireDeviceConnected()) return@setOnClickListener
            MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.tip_prompt)
                .setMessage(R.string.ds_device_reset_confirm_msg)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    resetDevice()
                }
                .show()
        }
        viewBind.btnOthersUnbind.setOnClickListener {
            if (!activity.requireDeviceConnected()) return@setOnClickListener
            MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.tip_prompt)
                .setMessage(R.string.device_unbind_confirm_msg)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    unbindDevice()
                }
                .show()
        }
    }

    override fun onDestroy() {
        resetDisposable?.dispose()
        resetDisposable = null
        unbindDisposable?.dispose()
        unbindDisposable = null
    }

    private fun resetDevice() {
        resetDisposable?.dispose()
        resetDisposable = wearKit.deviceAbility.reset()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                wearKit.connector.close()
                activity.toast(R.string.tip_success)
            }, {
                Timber.w(it)
                activity.toast(R.string.tip_failed)
            })
    }

    private fun unbindDevice() {
        unbindDisposable?.dispose()
        unbindDisposable = wearKit.connector.clear(true)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                SanagPreferencesStorage.clearLastDevice()
                viewBind.tvDeviceInfo.text = activity.getString(R.string.device_state_no_device)
                activity.toast(R.string.tip_success)
            }, {
                Timber.w(it)
                activity.toast(R.string.tip_failed)
            })
    }
}
