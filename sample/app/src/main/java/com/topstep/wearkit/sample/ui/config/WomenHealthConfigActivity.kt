package com.topstep.wearkit.sample.ui.config

import android.os.Bundle
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topstep.wearkit.apis.model.config.WKWomenHealthConfig
import com.topstep.wearkit.prototb.apis.PbSDK
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityWomenHealthConfigBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.ui.dialog.DatePickerDialogFragment
import com.topstep.wearkit.sample.ui.dialog.SelectIntDialogFragment
import com.topstep.wearkit.sample.ui.dialog.TimePickerDialogFragment
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WomenHealthConfigActivity : BaseActivity(), TimePickerDialogFragment.Listener,
    SelectIntDialogFragment.Listener, DatePickerDialogFragment.Listener {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityWomenHealthConfigBinding
    private var observeDispose: Disposable? = null
    private var getDispose: Disposable? = null
    private var setDispose: Disposable? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun formatMinutes(minutes: Int): String {
        return String.format(Locale.US, "%02d:%02d", minutes / 60, minutes % 60)
    }

    private fun modeText(mode: Int): String {
        return when (mode) {
            WKWomenHealthConfig.Mode.MENSTRUATION -> getString(R.string.ds_women_health_mode_menstruation)
            WKWomenHealthConfig.Mode.PREGNANCY_PREPARE -> getString(R.string.ds_women_health_mode_pregnancy_prepare)
            WKWomenHealthConfig.Mode.PREGNANCY -> getString(R.string.ds_women_health_mode_pregnancy)
            else -> getString(R.string.ds_women_health_mode_none)
        }
    }

    private fun remindTypeText(remindType: Int): String {
        return if (remindType == WKWomenHealthConfig.RemindType.DUE_DAYS) {
            getString(R.string.ds_women_health_remind_type_due)
        } else {
            getString(R.string.ds_women_health_remind_type_pregnancy)
        }
    }

    private fun hasRemindFlag(flags: Int, flag: Int): Boolean {
        return flags and flag != 0
    }

    private fun updateRemindFlag(flag: Int, enabled: Boolean): Int {
        val flags = wearKit.womenHealthAbility.getConfig().remindFlags
        return if (enabled) flags or flag else flags and flag.inv()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityWomenHealthConfigBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.ds_women_health_config)

        val supportRemindFlags = wearKit.womenHealthAbility.compat.isSupportRemindFlags()
        viewBind.itemFlagPeriodStart.isVisible = supportRemindFlags
        viewBind.itemFlagPeriodEnd.isVisible = supportRemindFlags
        viewBind.itemFlagFertileStart.isVisible = supportRemindFlags
        viewBind.itemFlagFertileEnd.isVisible = supportRemindFlags

        observeDispose = wearKit.womenHealthAbility.observeConfig(true)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                viewBind.itemMode.getTextView().text = modeText(it.mode)
                viewBind.itemRemindTime.getTextView().text = formatMinutes(it.remindTime)
                viewBind.itemRemindAdvance.getTextView().text = getString(R.string.unit_day_count_param, it.remindAdvance)
                viewBind.itemRemindType.getTextView().text = remindTypeText(it.remindType)
                viewBind.itemCycle.getTextView().text = getString(R.string.unit_day_count_param, it.cycle)
                viewBind.itemDuration.getTextView().text = getString(R.string.unit_day_count_param, it.duration)
                viewBind.itemLatest.getTextView().text = dateFormat.format(it.latest)
                viewBind.itemMenstruationEnd.getTextView().text = it.menstruationEnd.toString()
                if (supportRemindFlags) {
                    viewBind.itemFlagPeriodStart.getSwitchView().isChecked =
                        hasRemindFlag(it.remindFlags, WKWomenHealthConfig.RemindFlags.MENSTRUATION_PERIOD_START)
                    viewBind.itemFlagPeriodEnd.getSwitchView().isChecked =
                        hasRemindFlag(it.remindFlags, WKWomenHealthConfig.RemindFlags.MENSTRUATION_PERIOD_END)
                    viewBind.itemFlagFertileStart.getSwitchView().isChecked =
                        hasRemindFlag(it.remindFlags, WKWomenHealthConfig.RemindFlags.FERTILE_PERIOD_START)
                    viewBind.itemFlagFertileEnd.getSwitchView().isChecked =
                        hasRemindFlag(it.remindFlags, WKWomenHealthConfig.RemindFlags.FERTILE_PERIOD_END)
                }
            }, {
                Timber.w(it)
                viewBind.tvTips.text = it.toString()
            })

        viewBind.itemMode.setOnClickListener {
            val modes = intArrayOf(
                WKWomenHealthConfig.Mode.NONE,
                WKWomenHealthConfig.Mode.MENSTRUATION,
                WKWomenHealthConfig.Mode.PREGNANCY_PREPARE,
                WKWomenHealthConfig.Mode.PREGNANCY,
            )
            val labels = arrayOf(
                getString(R.string.ds_women_health_mode_none),
                getString(R.string.ds_women_health_mode_menstruation),
                getString(R.string.ds_women_health_mode_pregnancy_prepare),
                getString(R.string.ds_women_health_mode_pregnancy),
            )
            val checked = modes.indexOf(wearKit.womenHealthAbility.getConfig().mode).coerceAtLeast(0)
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.ds_women_health_mode)
                .setSingleChoiceItems(labels, checked) { dialog, which ->
                    setWomenHealth(wearKit.womenHealthAbility.getConfig().copy(mode = modes[which]))
                    dialog.dismiss()
                }
                .show()
        }
        viewBind.itemRemindTime.setOnClickListener {
            TimePickerDialogFragment.newInstance(
                timeMinute = wearKit.womenHealthAbility.getConfig().remindTime,
                title = getString(R.string.ds_women_health_remind_time)
            ).show(supportFragmentManager, DIALOG_REMIND_TIME)
        }
        viewBind.itemRemindAdvance.setOnClickListener {
            val value = wearKit.womenHealthAbility.getConfig().remindAdvance
            SelectIntDialogFragment.newInstance(
                min = 1,
                max = 3,
                value = value.coerceIn(1, 3),
                title = getString(R.string.ds_women_health_remind_advance),
                des = getString(R.string.unit_day_count)
            ).show(supportFragmentManager, DIALOG_REMIND_ADVANCE)
        }
        viewBind.itemRemindType.setOnClickListener {
            val types = intArrayOf(
                WKWomenHealthConfig.RemindType.PREGNANCY_DAYS,
                WKWomenHealthConfig.RemindType.DUE_DAYS,
            )
            val labels = arrayOf(
                getString(R.string.ds_women_health_remind_type_pregnancy),
                getString(R.string.ds_women_health_remind_type_due),
            )
            val checked = types.indexOf(wearKit.womenHealthAbility.getConfig().remindType).coerceAtLeast(0)
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.ds_women_health_remind_type)
                .setSingleChoiceItems(labels, checked) { dialog, which ->
                    setWomenHealth(wearKit.womenHealthAbility.getConfig().copy(remindType = types[which]))
                    dialog.dismiss()
                }
                .show()
        }
        viewBind.itemCycle.setOnClickListener {
            val value = wearKit.womenHealthAbility.getConfig().cycle
            SelectIntDialogFragment.newInstance(
                min = 17,
                max = 60,
                value = value.coerceIn(17, 60),
                title = getString(R.string.ds_women_health_cycle),
                des = getString(R.string.unit_day_count)
            ).show(supportFragmentManager, DIALOG_CYCLE)
        }
        viewBind.itemDuration.setOnClickListener {
            val value = wearKit.womenHealthAbility.getConfig().duration
            SelectIntDialogFragment.newInstance(
                min = 3,
                max = 15,
                value = value.coerceIn(3, 15),
                title = getString(R.string.ds_women_health_duration),
                des = getString(R.string.unit_day_count)
            ).show(supportFragmentManager, DIALOG_DURATION)
        }
        viewBind.itemLatest.setOnClickListener {
            DatePickerDialogFragment.newInstance(
                start = null,
                end = Date(),
                value = wearKit.womenHealthAbility.getConfig().latest,
                title = getString(R.string.ds_women_health_latest)
            ).show(supportFragmentManager, DIALOG_LATEST)
        }
        viewBind.itemMenstruationEnd.setOnClickListener {
            val value = wearKit.womenHealthAbility.getConfig().menstruationEnd
            SelectIntDialogFragment.newInstance(
                min = 0,
                max = 31,
                value = value.coerceIn(0, 31),
                title = getString(R.string.ds_women_health_menstruation_end)
            ).show(supportFragmentManager, DIALOG_MENSTRUATION_END)
        }
        viewBind.itemFlagPeriodStart.getSwitchView().setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed) {
                setWomenHealth(
                    wearKit.womenHealthAbility.getConfig().copy(
                        remindFlags = updateRemindFlag(WKWomenHealthConfig.RemindFlags.MENSTRUATION_PERIOD_START, isChecked)
                    )
                )
            }
        }
        viewBind.itemFlagPeriodEnd.getSwitchView().setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed) {
                setWomenHealth(
                    wearKit.womenHealthAbility.getConfig().copy(
                        remindFlags = updateRemindFlag(WKWomenHealthConfig.RemindFlags.MENSTRUATION_PERIOD_END, isChecked)
                    )
                )
            }
        }
        viewBind.itemFlagFertileStart.getSwitchView().setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed) {
                setWomenHealth(
                    wearKit.womenHealthAbility.getConfig().copy(
                        remindFlags = updateRemindFlag(WKWomenHealthConfig.RemindFlags.FERTILE_PERIOD_START, isChecked)
                    )
                )
            }
        }
        viewBind.itemFlagFertileEnd.getSwitchView().setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed) {
                setWomenHealth(
                    wearKit.womenHealthAbility.getConfig().copy(
                        remindFlags = updateRemindFlag(WKWomenHealthConfig.RemindFlags.FERTILE_PERIOD_END, isChecked)
                    )
                )
            }
        }

        //for test sdk-prototb-adapter. Developer can ignore it.
        if (wearKit.getRawSDK() is PbSDK) {
            viewBind.itemPbTestGetConfig.isVisible = true
            viewBind.itemPbTestGetConfig.setOnClickListener {
                getDispose = (wearKit.getRawSDK() as PbSDK).configGetTest.getWomenHealthConfig()
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

    private fun setWomenHealth(config: WKWomenHealthConfig) {
        setDispose?.dispose()
        setDispose = wearKit.womenHealthAbility.setConfig(config)
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
        if (tag == DIALOG_REMIND_TIME) {
            setWomenHealth(wearKit.womenHealthAbility.getConfig().copy(remindTime = timeMinute))
        }
    }

    override fun onDialogDatePicker(tag: String?, date: Date) {
        if (tag == DIALOG_LATEST) {
            setWomenHealth(wearKit.womenHealthAbility.getConfig().copy(latest = date))
        }
    }

    override fun onDialogSelectInt(tag: String?, selectValue: Int) {
        when (tag) {
            DIALOG_REMIND_ADVANCE -> {
                setWomenHealth(wearKit.womenHealthAbility.getConfig().copy(remindAdvance = selectValue))
            }
            DIALOG_CYCLE -> {
                setWomenHealth(wearKit.womenHealthAbility.getConfig().copy(cycle = selectValue))
            }
            DIALOG_DURATION -> {
                setWomenHealth(wearKit.womenHealthAbility.getConfig().copy(duration = selectValue))
            }
            DIALOG_MENSTRUATION_END -> {
                setWomenHealth(wearKit.womenHealthAbility.getConfig().copy(menstruationEnd = selectValue))
            }
        }
    }

    companion object {
        private const val DIALOG_REMIND_TIME = "remind_time"
        private const val DIALOG_REMIND_ADVANCE = "remind_advance"
        private const val DIALOG_CYCLE = "cycle"
        private const val DIALOG_DURATION = "duration"
        private const val DIALOG_LATEST = "latest"
        private const val DIALOG_MENSTRUATION_END = "menstruation_end"
    }

}
