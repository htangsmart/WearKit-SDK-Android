package com.topstep.wearkit.sample.ui.config

import android.os.Bundle
import androidx.core.view.isVisible
import com.topstep.wearkit.apis.model.config.WKFunctionConfig
import com.topstep.wearkit.apis.model.config.toBuilder
import com.topstep.wearkit.prototb.apis.PbSDK
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityFunctionConfigBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import timber.log.Timber

class FunctionConfigActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityFunctionConfigBinding
    private var observeDispose: Disposable? = null
    private var getDispose: Disposable? = null
    private var setDispose: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityFunctionConfigBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.ds_function_config)
        observeDispose = wearKit.functionAbility.observeConfig(true)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (it.isFlagEnabled(WKFunctionConfig.Flag.WEAR_HAND)) {
                    viewBind.itemWearHand.getTextView().setText(R.string.ds_wear_hand_right)
                } else {
                    viewBind.itemWearHand.getTextView().setText(R.string.ds_wear_hand_left)
                }

                if (it.isFlagEnabled(WKFunctionConfig.Flag.TIME_FORMAT)) {
                    viewBind.itemTimeFormat.getTextView().setText(R.string.ds_time_format_24_hour)
                } else {
                    viewBind.itemTimeFormat.getTextView().setText(R.string.ds_time_format_12_hour)
                }

                if (it.isFlagEnabled(WKFunctionConfig.Flag.WEATHER)) {
                    viewBind.itemWeather.getTextView().setText(R.string.ds_enabled)
                } else {
                    viewBind.itemWeather.getTextView().setText(R.string.ds_disabled)
                }

                if (it.isFlagEnabled(WKFunctionConfig.Flag.HEALTH_ENHANCED)) {
                    viewBind.itemHealthEnhanced.getTextView().setText(R.string.ds_enabled)
                } else {
                    viewBind.itemHealthEnhanced.getTextView().setText(R.string.ds_disabled)
                }

            }, {
                Timber.w(it)
            })

        viewBind.itemWearHand.setOnClickListener {
            setToggle(WKFunctionConfig.Flag.WEAR_HAND)
        }
        viewBind.itemTimeFormat.setOnClickListener {
            setToggle(WKFunctionConfig.Flag.TIME_FORMAT)
        }
        viewBind.itemWeather.setOnClickListener {
            setToggle(WKFunctionConfig.Flag.WEATHER)
        }
        viewBind.itemHealthEnhanced.setOnClickListener {
            setToggle(WKFunctionConfig.Flag.HEALTH_ENHANCED)
        }

        //for test sdk-prototb-adapter. Developer can ignore it.
        if (wearKit.getRawSDK() is PbSDK) {
            viewBind.itemPbTestGetConfig.isVisible = true
            viewBind.itemPbTestGetConfig.setOnClickListener {
                getDispose = (wearKit.getRawSDK() as PbSDK).configGetTest.getFunctionConfig()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        viewBind.tvTips.text = it.toString()
                    }, {
                        viewBind.tvTips.text = it.toString()
                    })
            }
        } else {
            viewBind.itemPbTestGetConfig.isVisible = false
        }
    }

    private fun setToggle(@WKFunctionConfig.Flag flag: Int) {
        val oldConfig = wearKit.functionAbility.getConfig()
        val newConfig = oldConfig.toBuilder()
            .setFlagEnabled(flag, !oldConfig.isFlagEnabled(flag))
            .create()
        setDispose?.dispose()
        setDispose = wearKit.functionAbility.setConfig(newConfig)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Timber.i("Set Success")
            }, { throwable ->
                viewBind.tvTips.text = throwable.toString()
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        observeDispose?.dispose()
        getDispose?.dispose()
        setDispose?.dispose()
    }

}