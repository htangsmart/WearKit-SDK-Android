package com.topstep.wearkit.sample.ui.basic

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
        supportActionBar?.setTitle(R.string.ds_display_weather)

        viewBind.switchWeather.isChecked = wearKit.functionAbility.getConfig().isFlagEnabled(WKFunctionConfig.Flag.WEATHER)
        viewBind.switchWeather.setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed) {
                setWeather(isChecked)
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun setWeather(checked: Boolean) {
        val config = wearKit.functionAbility.getConfig().toBuilder()
            .setFlagEnabled(WKFunctionConfig.Flag.WEATHER, checked).create()

        // Please refer to WKWeatherCode for weather conditions
        if (checked) {
            //Mock today weather
            val weatherToday = WKWeatherToday(
                timestampSeconds = System.currentTimeMillis() / 1000,
                code = WKWeatherCode.CLOUDY,
                tempMin = 10,
                tempMax = 20,
                tempCurrent = 18,
                pressure = 1013,
                humidity = 20,
                ultraviolet = 2,
                windAngle = 180f,
                windScale = 3,
                windSpeed = 5.5f,
                visibility = 6.6f
            )

            //Mock the weather for the next 6 days
            val dayList = ArrayList<WKWeatherDay>()
            val currentTimestamps = System.currentTimeMillis()
            val oneHourInMilliss = 24 * 60 * 60 * 1000L
            for (i in 1 until 7) {
                var data = currentTimestamps + (i * oneHourInMilliss)
                dayList.add(
                    WKWeatherDay(
                        timestampSeconds = data / 1000,
                        code = WKWeatherCode.CLEAR,
                        tempMin = 15 + i, tempMax = 25 + i
                    )
                )
            }

            //Mock the weather for the next 24 hours
            val hoursList = ArrayList<WKWeatherHour>()
            val currentTimestamp = System.currentTimeMillis()
            val oneHourInMillis = 60 * 60 * 1000L
            for (i in 1 until 25) {
                var data = currentTimestamp + (i * oneHourInMillis)
                hoursList.add(
                    WKWeatherHour(
                        timestampSeconds = data / 1000,//Mock next few hours
                        code = WKWeatherCode.HAZE,
                        tempCurrent = 16 + i,
                    )
                )
            }
            //Ensure the weather config is enabled
            wearKit.functionAbility.setConfig(config)
                .andThen(
                    wearKit.weatherAbility.setWeather(
                        city = "ShenZhen",
                        today = weatherToday,
                        futureDays = dayList,
                        futureHours = hoursList
                    )
                )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    toast(R.string.tip_success)
                }, {
                    Timber.w(it)
                    toast(R.string.tip_failed)
                })

        } else {
            wearKit.functionAbility.setConfig(config)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    toast(R.string.tip_success)
                }, {
                    Timber.w(it)
                    toast(R.string.tip_failed)
                })
        }
    }

}