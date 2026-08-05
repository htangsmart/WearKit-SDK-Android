package com.topstep.wearkit.sample.ui.config

import android.os.Bundle
import androidx.core.view.isVisible
import com.topstep.wearkit.apis.model.config.WKUnitConfig
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
    private val supportSplitMetric = wearKit.unitAbility.compat.isSupportSplitMetric()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityUnitConfigBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.ds_unit_config)

        viewBind.itemMetricImperial.isVisible = !supportSplitMetric
        viewBind.itemLengthUnit.isVisible = supportSplitMetric
        viewBind.itemWeightUnit.isVisible = supportSplitMetric

        observeDispose = wearKit.unitAbility.observeConfig(true)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (supportSplitMetric) {
                    viewBind.itemLengthUnit.getTextView().setText(
                        if (it.isLengthMetric) R.string.ds_metric else R.string.ds_imperial
                    )
                    viewBind.itemWeightUnit.getTextView().setText(
                        if (it.isWeightMetric) R.string.ds_metric else R.string.ds_imperial
                    )
                } else {
                    viewBind.itemMetricImperial.getTextView().setText(
                        if (it.isMetric) R.string.ds_metric else R.string.ds_imperial
                    )
                }

                viewBind.itemTemperatureUnit.getTextView().setText(
                    if (it.isCentigrade) R.string.ds_temperature_celsius else R.string.ds_temperature_fahrenheit
                )
            }, {
                Timber.w(it)
            })

        viewBind.itemMetricImperial.setOnClickListener {
            setToggle(ToggleType.METRIC)
        }
        viewBind.itemLengthUnit.setOnClickListener {
            setToggle(ToggleType.LENGTH)
        }
        viewBind.itemWeightUnit.setOnClickListener {
            setToggle(ToggleType.WEIGHT)
        }
        viewBind.itemTemperatureUnit.setOnClickListener {
            setToggle(ToggleType.TEMPERATURE)
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

    private fun setToggle(type: ToggleType) {
        val oldConfig = wearKit.unitAbility.getConfig()
        val newConfig = if (supportSplitMetric) {
            when (type) {
                ToggleType.LENGTH -> WKUnitConfig.splitMetric(
                    isLengthMetric = !oldConfig.isLengthMetric,
                    isWeightMetric = oldConfig.isWeightMetric,
                    isCentigrade = oldConfig.isCentigrade,
                )
                ToggleType.WEIGHT -> WKUnitConfig.splitMetric(
                    isLengthMetric = oldConfig.isLengthMetric,
                    isWeightMetric = !oldConfig.isWeightMetric,
                    isCentigrade = oldConfig.isCentigrade,
                )
                ToggleType.TEMPERATURE -> WKUnitConfig.splitMetric(
                    isLengthMetric = oldConfig.isLengthMetric,
                    isWeightMetric = oldConfig.isWeightMetric,
                    isCentigrade = !oldConfig.isCentigrade,
                )
                ToggleType.METRIC -> return
            }
        } else {
            when (type) {
                ToggleType.METRIC -> WKUnitConfig.base(
                    isMetric = !oldConfig.isMetric,
                    isCentigrade = oldConfig.isCentigrade,
                )
                ToggleType.TEMPERATURE -> WKUnitConfig.base(
                    isMetric = oldConfig.isMetric,
                    isCentigrade = !oldConfig.isCentigrade,
                )
                ToggleType.LENGTH, ToggleType.WEIGHT -> return
            }
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

    private enum class ToggleType {
        METRIC,
        LENGTH,
        WEIGHT,
        TEMPERATURE,
    }

}
