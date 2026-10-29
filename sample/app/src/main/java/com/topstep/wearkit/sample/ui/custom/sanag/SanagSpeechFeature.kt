package com.topstep.wearkit.sample.ui.custom.sanag

import android.content.Intent
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivitySanagDemoBinding
import com.topstep.wearkit.sample.ui.ai.SpeechAiManager

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
            showSpeechAiDialog()
        }
    }

    private fun showSpeechAiDialog() {
        val options = arrayOf(
            activity.getString(R.string.ds_speech_ai_starburst) to SpeechAiManager.Vendor.STAR_BURST,
            activity.getString(R.string.ds_speech_ai_eyeear) to SpeechAiManager.Vendor.EYE_EAR,
            activity.getString(R.string.ds_speech_ai_mltcloud) to SpeechAiManager.Vendor.MLT_CLOUD,
        )
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.ds_speech_ai_select)
            .setItems(options.map { it.first }.toTypedArray()) { _, which ->
                SpeechAiManager.select(options[which].second)
                activity.startActivity(Intent(activity, SanagSpeechAiActivity::class.java))
            }
            .show()
    }

}
