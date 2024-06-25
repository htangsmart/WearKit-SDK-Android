package com.topstep.wearkit.sample.ui.basic

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.TextView
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.apis.model.config.WKFunctionConfig
import com.topstep.wearkit.apis.model.config.toBuilder
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityTimeFormatBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import timber.log.Timber

@SuppressLint("CheckResult")
class TimeFormatActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityTimeFormatBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityTimeFormatBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.time_format)
        val is24HourFormat = wearKit.functionAbility.getConfig().isFlagEnabled(WKFunctionConfig.Flag.TIME_FORMAT)
        if (is24HourFormat) {
            viewBind.time24Tv.setTextColor(getColor(R.color.colorPrimary))
        } else {
            viewBind.time12Tv.setTextColor(getColor(R.color.colorPrimary))
        }


        viewBind.time12Tv.clickTrigger {
            setTimeFormat(false, viewBind.time12Tv)
        }

        viewBind.time24Tv.clickTrigger {
            setTimeFormat(true, viewBind.time24Tv)
        }
    }

    private fun setTimeFormat(is24HourFormat: Boolean, tv: TextView) {
        viewBind.time12Tv.setTextColor(getColor(R.color.black))
        viewBind.time24Tv.setTextColor(getColor(R.color.black))
        val time = wearKit.functionAbility.getConfig().toBuilder()
            .setFlagEnabled(
                WKFunctionConfig.Flag.TIME_FORMAT,
                is24HourFormat
            ).create()
        wearKit.functionAbility.setConfig(time).observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                tv.setTextColor(getColor(R.color.colorPrimary))
                toast(R.string.tip_success)
            }, {
                Timber.i(it)
            })
    }

}