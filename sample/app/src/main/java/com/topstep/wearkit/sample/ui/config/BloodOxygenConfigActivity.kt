package com.topstep.wearkit.sample.ui.config

import android.os.Bundle
import androidx.core.view.isVisible
import com.topstep.wearkit.apis.model.config.WKBloodOxygenMonitorConfig
import com.topstep.wearkit.prototb.apis.PbSDK
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityBloodOxygenConfigBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.ui.dialog.SelectIntDialogFragment
import com.topstep.wearkit.sample.ui.dialog.TimePickerDialogFragment
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import timber.log.Timber
import java.util.Locale

class BloodOxygenConfigActivity : BaseActivity(), TimePickerDialogFragment.Listener,
    SelectIntDialogFragment.Listener {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityBloodOxygenConfigBinding
    private var observeDispose1: Disposable? = null
    private var getDispose: Disposable? = null
    private var setDispose: Disposable? = null

    private fun formatMinutes(minutes: Int): String {
        return String.format(Locale.US, "%02d:%02d", minutes / 60, minutes % 60)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityBloodOxygenConfigBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.ds_blood_oxygen_config)
        observeDispose1 = wearKit.bloodOxygenAbility.observeMonitorConfig(true)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                viewBind.itemMonitorConfig.getSwitchView().isChecked = it.isEnabled
                viewBind.itemStart.getTextView().text = formatMinutes(it.start)
                viewBind.itemEnd.getTextView().text = formatMinutes(it.end)
                viewBind.itemInterval.getTextView().text = getString(R.string.unit_minute_param, it.interval)
            }, {
                Timber.w(it)
            })

        viewBind.itemMonitorConfig.getSwitchView().setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed) {
                setMonitor(
                    wearKit.bloodOxygenAbility.getMonitorConfig().copy(isEnabled = isChecked)
                )
            }
        }
        viewBind.itemStart.setOnClickListener {
            TimePickerDialogFragment.newInstance(
                timeMinute = wearKit.bloodOxygenAbility.getMonitorConfig().start,
                title = getString(R.string.ds_config_start_time)
            ).show(supportFragmentManager, DIALOG_START)
        }
        viewBind.itemEnd.setOnClickListener {
            TimePickerDialogFragment.newInstance(
                timeMinute = wearKit.bloodOxygenAbility.getMonitorConfig().end,
                title = getString(R.string.ds_config_end_time)
            ).show(supportFragmentManager, DIALOG_END)
        }
        viewBind.itemInterval.setOnClickListener {
            val value = wearKit.bloodOxygenAbility.getMonitorConfig().interval
            SelectIntDialogFragment.newInstance(
                min = 1,
                max = 12,
                multiples = 5,
                value = value,
                title = getString(R.string.ds_config_interval_time),
                des = getString(R.string.unit_minute)
            ).show(supportFragmentManager, DIALOG_INTERVAL)
        }

        //for test sdk-prototb-adapter. Developer can ignore it.
        if (wearKit.getRawSDK() is PbSDK) {
            viewBind.itemPbTestGetConfig.isVisible = true
            viewBind.itemPbTestGetConfig.setOnClickListener {
                getDispose = (wearKit.getRawSDK() as PbSDK).configGetTest.getBloodOxygenConfig()
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

    private fun setMonitor(config: WKBloodOxygenMonitorConfig) {
        setDispose?.dispose()
        setDispose = wearKit.bloodOxygenAbility.setMonitorConfig(config)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Timber.i("Set Success")
            }, { throwable ->
                viewBind.tvTips.text = throwable.toString()
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        observeDispose1?.dispose()
        getDispose?.dispose()
        setDispose?.dispose()
    }

    override fun onDialogTimePicker(tag: String?, timeMinute: Int) {
        when (tag) {
            DIALOG_START -> {
                setMonitor(
                    wearKit.bloodOxygenAbility.getMonitorConfig().copy(
                        start = timeMinute
                    )
                )
            }
            DIALOG_END -> {
                setMonitor(
                    wearKit.bloodOxygenAbility.getMonitorConfig().copy(
                        end = timeMinute
                    )
                )
            }
        }
    }

    override fun onDialogSelectInt(tag: String?, selectValue: Int) {
        when (tag) {
            DIALOG_INTERVAL -> {
                setMonitor(
                    wearKit.bloodOxygenAbility.getMonitorConfig().copy(
                        interval = selectValue
                    )
                )
            }
        }
    }

    companion object {
        private const val DIALOG_START = "start"
        private const val DIALOG_END = "end"
        private const val DIALOG_INTERVAL = "interval"
    }

}