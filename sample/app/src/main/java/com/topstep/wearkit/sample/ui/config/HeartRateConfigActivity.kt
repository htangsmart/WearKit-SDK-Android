package com.topstep.wearkit.sample.ui.config

import android.os.Bundle
import androidx.core.view.isVisible
import com.topstep.wearkit.apis.model.config.WKHeartRateAlarmConfig
import com.topstep.wearkit.apis.model.config.WKHeartRateMonitorConfig
import com.topstep.wearkit.prototb.apis.PbSDK
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityHeartRateConfigBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.ui.dialog.SelectIntDialogFragment
import com.topstep.wearkit.sample.ui.dialog.TimePickerDialogFragment
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import timber.log.Timber
import java.util.Locale

class HeartRateConfigActivity : BaseActivity(), TimePickerDialogFragment.Listener,
    SelectIntDialogFragment.Listener {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityHeartRateConfigBinding
    private var observeDispose1: Disposable? = null
    private var observeDispose2: Disposable? = null
    private var getDispose: Disposable? = null
    private var setDispose: Disposable? = null

    private fun formatMinutes(minutes: Int): String {
        return String.format(Locale.US, "%02d:%02d", minutes / 60, minutes % 60)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityHeartRateConfigBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.ds_heart_rate_config)
        observeDispose1 = wearKit.heartRateAbility.observeMonitorConfig(true)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                viewBind.itemMonitorConfig.getSwitchView().isChecked = it.isEnabled
                viewBind.itemStart.getTextView().text = formatMinutes(it.start)
                viewBind.itemEnd.getTextView().text = formatMinutes(it.end)
                viewBind.itemInterval.getTextView().text = getString(R.string.unit_minute_param, it.interval)
            }, {
                Timber.w(it)
            })
        observeDispose2 = wearKit.heartRateAbility.observeAlarmConfig(true)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                viewBind.itemAlarmExercise.getSwitchView().isChecked = it.exercise.isEnabled
                viewBind.itemExerciseMin.getTextView().text = it.exercise.min.toString()
                viewBind.itemExerciseMax.getTextView().text = it.exercise.max.toString()

                viewBind.itemAlarmResting.getSwitchView().isChecked = it.resting.isEnabled
                viewBind.itemRestingMin.getTextView().text = it.resting.min.toString()
                viewBind.itemRestingMax.getTextView().text = it.resting.max.toString()
            }, {
                Timber.w(it)
            })
        viewBind.itemThreshold.getTextView().text = wearKit.heartRateAbility.getMaxThreshold().toString()

        viewBind.itemMonitorConfig.getSwitchView().setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed) {
                setMonitor(
                    wearKit.heartRateAbility.getMonitorConfig().copy(isEnabled = isChecked)
                )
            }
        }
        viewBind.itemStart.setOnClickListener {
            TimePickerDialogFragment.newInstance(
                timeMinute = wearKit.heartRateAbility.getMonitorConfig().start,
                title = getString(R.string.ds_config_start_time)
            ).show(supportFragmentManager, DIALOG_START)
        }
        viewBind.itemEnd.setOnClickListener {
            TimePickerDialogFragment.newInstance(
                timeMinute = wearKit.heartRateAbility.getMonitorConfig().end,
                title = getString(R.string.ds_config_end_time)
            ).show(supportFragmentManager, DIALOG_END)
        }
        viewBind.itemInterval.setOnClickListener {
            val value = wearKit.heartRateAbility.getMonitorConfig().interval
            SelectIntDialogFragment.newInstance(
                min = 1,
                max = 12,
                multiples = 5,
                value = value,
                title = getString(R.string.ds_config_interval_time),
                des = getString(R.string.unit_minute)
            ).show(supportFragmentManager, DIALOG_INTERVAL)
        }
        viewBind.itemAlarmExercise.getSwitchView().setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed) {
                val config = wearKit.heartRateAbility.getAlarmConfig()
                setAlarm(
                    config.copy(
                        exercise = config.exercise.copy(isEnabled = isChecked)
                    )
                )
            }
        }
        viewBind.itemExerciseMin.setOnClickListener {
            val config = wearKit.heartRateAbility.getAlarmConfig()
            SelectIntDialogFragment.newInstance(
                min = 100,
                max = 200,
                value = config.exercise.min,
                title = getString(R.string.ds_config_min_value),
                des = getString(R.string.unit_bmp)
            ).show(supportFragmentManager, DIALOG_EXERCISE_MIN)
        }
        viewBind.itemExerciseMax.setOnClickListener {
            val config = wearKit.heartRateAbility.getAlarmConfig()
            SelectIntDialogFragment.newInstance(
                min = 100,
                max = 200,
                value = config.exercise.max,
                title = getString(R.string.ds_config_max_value),
                des = getString(R.string.unit_bmp)
            ).show(supportFragmentManager, DIALOG_EXERCISE_MAX)
        }
        viewBind.itemAlarmResting.getSwitchView().setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed) {
                val config = wearKit.heartRateAbility.getAlarmConfig()
                setAlarm(
                    config.copy(
                        resting = config.resting.copy(isEnabled = isChecked)
                    )
                )
            }
        }
        viewBind.itemRestingMin.setOnClickListener {
            val config = wearKit.heartRateAbility.getAlarmConfig()
            SelectIntDialogFragment.newInstance(
                min = 100,
                max = 150,
                value = config.resting.min,
                title = getString(R.string.ds_config_min_value),
                des = getString(R.string.unit_bmp)
            ).show(supportFragmentManager, DIALOG_RESTING_MIN)
        }
        viewBind.itemRestingMax.setOnClickListener {
            val config = wearKit.heartRateAbility.getAlarmConfig()
            SelectIntDialogFragment.newInstance(
                min = 100,
                max = 150,
                value = config.resting.max,
                title = getString(R.string.ds_config_max_value),
                des = getString(R.string.unit_bmp)
            ).show(supportFragmentManager, DIALOG_RESTING_MAX)
        }
        viewBind.itemThreshold.setOnClickListener {
            SelectIntDialogFragment.newInstance(
                min = 150,
                max = 220,
                value = wearKit.heartRateAbility.getMaxThreshold(),
                title = getString(R.string.ds_heart_rate_threshold),
                des = getString(R.string.unit_bmp)
            ).show(supportFragmentManager, DIALOG_THRESHOLD)
        }

        //for test sdk-prototb-adapter. Developer can ignore it.
        if (wearKit.getRawSDK() is PbSDK) {
            viewBind.itemPbTestGetConfig.isVisible = true
            viewBind.itemPbTestGetConfig.setOnClickListener {
                getDispose = (wearKit.getRawSDK() as PbSDK).configGetTest.getHeartRateConfig()
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

    private fun setMonitor(config: WKHeartRateMonitorConfig) {
        setDispose?.dispose()
        setDispose = wearKit.heartRateAbility.setMonitorConfig(config)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Timber.i("Set Success")
            }, { throwable ->
                viewBind.tvTips.text = throwable.toString()
            })
    }

    private fun setAlarm(config: WKHeartRateAlarmConfig) {
        setDispose?.dispose()
        setDispose = wearKit.heartRateAbility.setAlarmConfig(config)
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
        observeDispose2?.dispose()
        getDispose?.dispose()
        setDispose?.dispose()
    }

    override fun onDialogTimePicker(tag: String?, timeMinute: Int) {
        when (tag) {
            DIALOG_START -> {
                setMonitor(
                    wearKit.heartRateAbility.getMonitorConfig().copy(
                        start = timeMinute
                    )
                )
            }
            DIALOG_END -> {
                setMonitor(
                    wearKit.heartRateAbility.getMonitorConfig().copy(
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
                    wearKit.heartRateAbility.getMonitorConfig().copy(
                        interval = selectValue
                    )
                )
            }
            DIALOG_EXERCISE_MIN -> {
                val config = wearKit.heartRateAbility.getAlarmConfig()
                setAlarm(
                    config.copy(
                        exercise = config.exercise.copy(min = selectValue)
                    )
                )
            }
            DIALOG_EXERCISE_MAX -> {
                val config = wearKit.heartRateAbility.getAlarmConfig()
                setAlarm(
                    config.copy(
                        exercise = config.exercise.copy(max = selectValue)
                    )
                )
            }
            DIALOG_RESTING_MIN -> {
                val config = wearKit.heartRateAbility.getAlarmConfig()
                setAlarm(
                    config.copy(
                        resting = config.resting.copy(min = selectValue)
                    )
                )
            }
            DIALOG_RESTING_MAX -> {
                val config = wearKit.heartRateAbility.getAlarmConfig()
                setAlarm(
                    config.copy(
                        resting = config.resting.copy(max = selectValue)
                    )
                )
            }
            DIALOG_THRESHOLD -> {
                setDispose?.dispose()
                setDispose = wearKit.heartRateAbility.setMaxThreshold(selectValue)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        Timber.i("Set Success")
                        viewBind.itemThreshold.getTextView().text = selectValue.toString()
                    }, { throwable ->
                        viewBind.tvTips.text = throwable.toString()
                    })
            }
        }
    }

    companion object {
        private const val DIALOG_START = "start"
        private const val DIALOG_END = "end"
        private const val DIALOG_INTERVAL = "interval"
        private const val DIALOG_EXERCISE_MIN = "exercise_min"
        private const val DIALOG_EXERCISE_MAX = "exercise_max"
        private const val DIALOG_RESTING_MIN = "resting_min"
        private const val DIALOG_RESTING_MAX = "resting_max"
        private const val DIALOG_THRESHOLD = "threshold"
    }

}