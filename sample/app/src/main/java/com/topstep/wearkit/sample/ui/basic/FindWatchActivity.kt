package com.topstep.wearkit.sample.ui.basic

import android.annotation.SuppressLint
import android.os.Bundle
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.apis.model.message.WKFinderMessage
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityFindWatchBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.utils.VibratorUtil
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import timber.log.Timber

class FindWatchActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityFindWatchBinding

    //You need to stop observe on exit
    private var observeFoundWatchDisposable: Disposable? = null

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityFindWatchBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.ds_find_watch)

        // Callback in device operation
        observeFoundWatchDisposable = wearKit.finderAbility.observeFinderMessage()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    when (it) {
                        //The watch replied that it has been found, you can stop searching.
                        WKFinderMessage.FOUND_WATCH -> {
                            VibratorUtil.stopVibrator()
                        }
                    }
                }, {
                    Timber.w(it)
                })

        viewBind.findWatch.clickTrigger {
            // start find watch
            wearKit.finderAbility.findWatch()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    toast(R.string.tip_success)
                }, {
                    Timber.w(it)
                    toast(R.string.tip_failed)
                })
        }

        viewBind.stopFindWatch.clickTrigger {
            // stop find watch
            wearKit.finderAbility.stopFindWatch()
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
        observeFoundWatchDisposable?.dispose()
    }

}