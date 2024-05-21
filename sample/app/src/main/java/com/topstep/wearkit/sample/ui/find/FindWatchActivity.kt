package com.topstep.wearkit.sample.ui.find

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.apis.model.message.WKFinderMessage
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityWatchBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.utils.VibratorUtil
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers

class FindWatchActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityWatchBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityWatchBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.title = "find watch phone"
        initView()
        initData()
        initEvent()
    }

    @SuppressLint("CheckResult")
    private fun initView() {
        // Callback in device operation
        wearKit.finderAbility.observeFinderMessage()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    when (it) {
                        // watch find phone
                        WKFinderMessage.FIND_PHONE -> {
                            VibratorUtil.vibrateAndPlayTone()
                        }

                        // watch stop find phone
                        WKFinderMessage.STOP_FIND_PHONE -> {
                            VibratorUtil.stopVibrator()
                        }

                    }
                }, {

                })

    }

    private fun initData() {


    }


    @SuppressLint("CheckResult", "Range")
    private fun initEvent() {
        viewBind.findWatch.clickTrigger {
            // phone find watch
            wearKit.finderAbility.findWatch()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    toast(getString(R.string.tip_success))
                }, {
                    toast(getString(R.string.tip_failed))
                })
        }

        viewBind.stopFindWatch.clickTrigger {
            // phone stop find watch
            wearKit.finderAbility.stopFindWatch()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    toast(getString(R.string.tip_success))
                }, {
                    toast(getString(R.string.tip_failed))
                })
        }
    }


    private fun toast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }
}