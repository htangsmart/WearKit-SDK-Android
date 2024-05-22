package com.topstep.wearkit.sample.ui.basic

import android.annotation.SuppressLint
import android.os.Bundle
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.apis.model.message.WKFinderMessage
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityFindPhoneBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.utils.VibratorUtil
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import timber.log.Timber

class FindPhoneActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityFindPhoneBinding

    //You need to stop observe on exit
    private var observeFindPhoneDisposable: Disposable? = null

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityFindPhoneBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.ds_find_phone)

        // Callback in device operation
        observeFindPhoneDisposable = wearKit.finderAbility.observeFinderMessage()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    when (it) {
                        //watch initiates phone search and stops search
                        WKFinderMessage.FIND_PHONE -> {
                            VibratorUtil.vibrateAndPlayTone()
                        }

                        WKFinderMessage.STOP_FIND_PHONE -> {
                            VibratorUtil.stopVibrator()
                        }
                    }
                }, {
                    Timber.w(it)
                })

        viewBind.btnReplay.clickTrigger {
            wearKit.finderAbility.foundPhone()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    toast(R.string.tip_success)

                }, {
                    Timber.w(it)
                    toast(R.string.tip_failed)
                })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        observeFindPhoneDisposable?.dispose()
    }

}