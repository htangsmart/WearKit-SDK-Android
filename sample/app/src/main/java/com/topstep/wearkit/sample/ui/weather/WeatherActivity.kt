package com.topstep.wearkit.sample.ui.weather

import android.annotation.SuppressLint
import android.os.Bundle
import com.topstep.wearkit.apis.model.config.WKFunctionConfig
import com.topstep.wearkit.apis.model.config.toBuilder
import com.topstep.wearkit.apis.model.weather.WKWeatherCode
import com.topstep.wearkit.apis.model.weather.WKWeatherDay
import com.topstep.wearkit.apis.model.weather.WKWeatherHour
import com.topstep.wearkit.apis.model.weather.WKWeatherToday
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityWeatherBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import timber.log.Timber

class WeatherActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityWeatherBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityWeatherBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.title = "Weather"

        viewBind.switchWeather.setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed) {
                setWeather(isChecked)
            }
        }

    }

    @SuppressLint("CheckResult")
    private fun setWeather(checked: Boolean) {
        // Please refer to WKWeatherCode for weather conditions
        if (checked) {
            val config = wearKit.functionAbility.getConfig().toBuilder()
                .setFlagEnabled(WKFunctionConfig.Flag.WEATHER, true).create()

            val weatherToday = WKWeatherToday(
                timestampSeconds = System.currentTimeMillis() / 1000,
                code = WKWeatherCode.CLEAR_DAY, tempMin = 10, tempMax = 20,
                tempCurrent = 18, pressure = 1, humidity = 20, ultraviolet = 2,
                windScale = 3, windSpeed = 5.5f, visibility = 6.6f
            )

            val dayList = ArrayList<WKWeatherDay>()
            for (i in 0 until 5) {
                dayList.add(
                    WKWeatherDay(
                        timestampSeconds = System.currentTimeMillis() / 1000,
                        code = WKWeatherCode.CLOUDY,
                        tempMin = 15 + i, tempMax = 25 + i
                    )
                )
            }

            val hoursList = ArrayList<WKWeatherHour>()
            hoursList.add(
                WKWeatherHour(
                    timestampSeconds = System.currentTimeMillis() / 1000,
                    code = WKWeatherCode.LIGHT_RAIN, tempCurrent = 16,
                )
            )

            wearKit.weatherAbility.setWeather(
                city = "南山区", today = weatherToday,
                futureDays = dayList, futureHours = hoursList
            ).andThen(wearKit.functionAbility.setConfig(config))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    toast(R.string.ds_push_success)
                }, {
                    Timber.w(it)
                })

        } else {
            val config = wearKit.functionAbility.getConfig().toBuilder()
                .setFlagEnabled(WKFunctionConfig.Flag.WEATHER, false).create()

            wearKit.functionAbility.setConfig(config)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    toast(R.string.ds_push_success)
                }, {
                    Timber.w(it)
                })
        }
    }

}