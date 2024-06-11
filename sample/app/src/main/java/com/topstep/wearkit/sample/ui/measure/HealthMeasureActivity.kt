package com.topstep.wearkit.sample.ui.measure

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityHealthMeasureBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import timber.log.Timber

@SuppressLint("CheckResult")
class HealthMeasureActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private var healthType: Int? = null
    private var countDownTimer: CountDownTimer? = null
    private lateinit var viewBind: ActivityHealthMeasureBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityHealthMeasureBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        healthType = intent.getIntExtra(health_type, -1)
        supportActionBar?.title = getName()
        startTime()
        when (healthType) {
            1 -> {
                heartRateMeasure()
            }
            2 -> {
                bloodOxygenMeasure()
            }
            3 -> {
                pressureMeasure()
            }
        }
    }

    private fun startTime() {
        countDownTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val progress = millisUntilFinished / 1000
                viewBind.measureView.isMeasuring = true
                viewBind.measureView.progress = progress / 60f
                viewBind.imgInCenter.visibility = View.VISIBLE
                viewBind.layoutResultInCenter.visibility = View.INVISIBLE
                viewBind.tvCountDown.text = progress.toString()
            }

            override fun onFinish() {
                viewBind.measureView.isMeasuring = false
            }
        }
        (countDownTimer as CountDownTimer).start()
    }


    private fun heartRateMeasure() {
        wearKit.heartRateAbility.measureRealtime(60)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                updateValue(it.toString())
            }, {
                Timber.i(it)
            })
    }

    private fun bloodOxygenMeasure() {
        wearKit.bloodOxygenAbility.measureRealtime(60)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                updateValue(it.toString())
            }, {
                Timber.i(it)
            })
    }

    private fun pressureMeasure() {
        wearKit.pressureAbility.measureRealtime(60)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                updateValue(it.toString())
            }, {
                Timber.i(it)
            })
    }

    private fun updateValue(value: String) {
        viewBind.imgInCenter.visibility = View.INVISIBLE
        viewBind.layoutResultInCenter.visibility = View.VISIBLE
        viewBind.headerName.text = getHealthName()
        viewBind.tvValue.text = value
    }

    companion object {
        private const val health_type = "health_type"

        @JvmStatic
        fun startActivity(context: Context?, type: Int?) {
            if (context == null) return
            val intent = Intent(context, HealthMeasureActivity::class.java)
            intent.putExtra(health_type, type)
            context.startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        (countDownTimer as CountDownTimer).cancel()
    }

    private fun getName(): String {
        return when (healthType) {
            1 -> getString(R.string.heartRate_measure)
            2 -> getString(R.string.bloodOxygen_measure)
            3 -> getString(R.string.pressure_measure)
            else -> getString(R.string.heartRate_measure)
        }
    }

    private fun getHealthName(): String {
        return when (healthType) {
            1 -> getString(R.string.heart_rate_value)
            2 -> getString(R.string.blood_oxygen_value)
            3 -> getString(R.string.pres_value)
            else -> getString(R.string.heart_rate_value)
        }
    }
}