package com.topstep.wearkit.sample.ui.config

import android.os.Bundle
import androidx.core.view.isVisible
import com.topstep.wearkit.prototb.apis.PbSDK
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityUnitConfigBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import timber.log.Timber

class UnitConfigActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityUnitConfigBinding
    private var observeDispose: Disposable? = null
    private var getDispose: Disposable? = null
    private var setDispose: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityUnitConfigBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.ds_unit_config)
        observeDispose = wearKit.unitAbility.observeConfig(true)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (it.isMetric) {
                    viewBind.itemMetricImperial.getTextView().setText(R.string.ds_metric)
                } else {
                    viewBind.itemMetricImperial.getTextView().setText(R.string.ds_imperial)
                }

                if (it.isCentigrade) {
                    viewBind.itemTemperatureUnit.getTextView().setText(R.string.ds_temperature_celsius)
                } else {
                    viewBind.itemTemperatureUnit.getTextView().setText(R.string.ds_temperature_fahrenheit)
                }

            }, {
                Timber.w(it)
            })

        viewBind.itemMetricImperial.setOnClickListener {
            setToggle(true)
        }
        viewBind.itemTemperatureUnit.setOnClickListener {
            setToggle(false)
        }

        //for test sdk-prototb-adapter. Developer can ignore it.
        if (wearKit.getRawSDK() is PbSDK) {
            viewBind.itemPbTestGetConfig.isVisible = true
            viewBind.itemPbTestGetConfig.setOnClickListener {
                getDispose = (wearKit.getRawSDK() as PbSDK).configGetTest.getUnitConfig()
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

    private fun setToggle(isSetMetric: Boolean) {
        val oldConfig = wearKit.unitAbility.getConfig()
        val newConfig = if (isSetMetric) {
            oldConfig.copy(isMetric = !oldConfig.isMetric)
        } else {
            oldConfig.copy(isCentigrade = !oldConfig.isCentigrade)
        }
        setDispose?.dispose()
        setDispose = wearKit.unitAbility.setConfig(newConfig)
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