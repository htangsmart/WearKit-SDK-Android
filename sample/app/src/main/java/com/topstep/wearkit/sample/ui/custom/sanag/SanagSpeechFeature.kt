package com.topstep.wearkit.sample.ui.custom.sanag

import android.content.Intent
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivitySanagDemoBinding

/**
 * Entry to [SanagSpeechAiActivity] (chat + last debug audio).
 */
internal class SanagSpeechFeature(
    private val activity: SanagDemoActivity,
    private val viewBind: ActivitySanagDemoBinding,
) : SanagDemoFeature {

    private val wearKit = MyApplication.wearKit

    override fun onCreate() {
        viewBind.btnSpeech.setOnClickListener {
            if (!activity.requireDeviceConnected()) return@setOnClickListener
            if (!wearKit.speechAiAbility.isSupport()) {
                activity.toast(R.string.tip_un_support)
                return@setOnClickListener
            }
            activity.startActivity(Intent(activity, SanagSpeechAiActivity::class.java))
        }
    }
}
