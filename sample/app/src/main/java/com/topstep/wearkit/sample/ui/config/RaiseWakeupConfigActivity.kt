package com.topstep.wearkit.sample.ui.config

import android.os.Bundle
import androidx.core.view.isVisible
import com.topstep.wearkit.apis.model.config.WKRaiseWakeupConfig
import com.topstep.wearkit.prototb.apis.PbSDK
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityRaisewakeupConfigBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.ui.dialog.TimePickerDialogFragment
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import timber.log.Timber
import java.util.Locale

class RaiseWakeupConfigActivity : BaseActivity(), TimePickerDialogFragment.Listener {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityRaisewakeupConfigBinding
    private var observeDispose: Disposable? = null
    private var getDispose: Disposable? = null
    private var setDispose: Disposable? = null

    private fun formatMinutes(minutes: Int): String {
        return String.format(Locale.US, "%02d:%02d", minutes / 60, minutes % 60)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityRaisewakeupConfigBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.ds_raisewakeup_config)
        observeDispose = wearKit.raiseWakeupAbility.observeConfig(true)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                viewBind.itemConfig.getSwitchView().isChecked = it.isEnabled
                viewBind.itemStart.getTextView().text = formatMinutes(it.start)
                viewBind.itemEnd.getTextView().text = formatMinutes(it.end)
            }, {
                Timber.w(it)
            })

        viewBind.itemConfig.getSwitchView().setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed) {
                setRaiseWakeup(
                    wearKit.raiseWakeupAbility.getConfig().copy(isEnabled = isChecked)
                )
            }
        }
        viewBind.itemStart.setOnClickListener {
            TimePickerDialogFragment.newInstance(
                timeMinute = wearKit.raiseWakeupAbility.getConfig().start,
                title = getString(R.string.ds_config_start_time)
            ).show(supportFragmentManager, DIALOG_START)
        }
        viewBind.itemEnd.setOnClickListener {
            TimePickerDialogFragment.newInstance(
                timeMinute = wearKit.raiseWakeupAbility.getConfig().end,
                title = getString(R.string.ds_config_end_time)
            ).show(supportFragmentManager, DIALOG_END)
        }

        //for test sdk-prototb-adapter. Developer can ignore it.
        if (wearKit.getRawSDK() is PbSDK) {
            viewBind.itemPbTestGetConfig.isVisible = true
            viewBind.itemPbTestGetConfig.setOnClickListener {
                getDispose = (wearKit.getRawSDK() as PbSDK).configGetTest.getRaiseWakeupConfig()
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

    private fun setRaiseWakeup(config: WKRaiseWakeupConfig) {
        setDispose?.dispose()
        setDispose = wearKit.raiseWakeupAbility.setConfig(config)
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

    override fun onDialogTimePicker(tag: String?, timeMinute: Int) {
        when (tag) {
            DIALOG_START -> {
                setRaiseWakeup(
                    wearKit.raiseWakeupAbility.getConfig().copy(
                        start = timeMinute
                    )
                )
            }
            DIALOG_END -> {
                setRaiseWakeup(
                    wearKit.raiseWakeupAbility.getConfig().copy(
                        end = timeMinute
                    )
                )
            }
        }
    }

    companion object {
        private const val DIALOG_START = "start"
        private const val DIALOG_END = "end"
    }

}