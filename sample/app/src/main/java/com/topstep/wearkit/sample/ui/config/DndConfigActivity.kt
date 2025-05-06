package com.topstep.wearkit.sample.ui.config

import android.os.Bundle
import androidx.core.view.isVisible
import com.topstep.wearkit.apis.model.config.WKDndConfig
import com.topstep.wearkit.prototb.apis.PbSDK
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityDndConfigBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.ui.dialog.TimePickerDialogFragment
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import timber.log.Timber
import java.util.Locale

class DndConfigActivity : BaseActivity(), TimePickerDialogFragment.Listener {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityDndConfigBinding
    private var observeDispose: Disposable? = null
    private var getDispose: Disposable? = null
    private var setDispose: Disposable? = null

    private fun formatMinutes(minutes: Int): String {
        return String.format(Locale.US, "%02d:%02d", minutes / 60, minutes % 60)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityDndConfigBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.ds_dnd_config)
        observeDispose = wearKit.dndAbility.observeConfig(true)
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
                setDnd(
                    wearKit.dndAbility.getConfig().copy(isEnabled = isChecked)
                )
            }
        }
        viewBind.itemStart.setOnClickListener {
            TimePickerDialogFragment.newInstance(
                timeMinute = wearKit.dndAbility.getConfig().start,
                title = getString(R.string.ds_config_start_time)
            ).show(supportFragmentManager, DIALOG_START)
        }
        viewBind.itemEnd.setOnClickListener {
            TimePickerDialogFragment.newInstance(
                timeMinute = wearKit.dndAbility.getConfig().end,
                title = getString(R.string.ds_config_end_time)
            ).show(supportFragmentManager, DIALOG_END)
        }

        //for test sdk-prototb-adapter. Developer can ignore it.
        if (wearKit.getRawSDK() is PbSDK) {
            viewBind.itemPbTestGetConfig.isVisible = true
            viewBind.itemPbTestGetConfig.setOnClickListener {
                getDispose = (wearKit.getRawSDK() as PbSDK).configGetTest.getDndConfig()
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

    private fun setDnd(config: WKDndConfig) {
        setDispose?.dispose()
        setDispose = wearKit.dndAbility.setConfig(config)
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
                setDnd(
                    wearKit.dndAbility.getConfig().copy(
                        start = timeMinute
                    )
                )
            }
            DIALOG_END -> {
                setDnd(
                    wearKit.dndAbility.getConfig().copy(
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