package com.topstep.wearkit.sample.ui.sync

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.apis.model.data.WKSyncData
import com.topstep.wearkit.apis.util.SyncTimeProviderIncremental
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivitySyncBinding
import com.topstep.wearkit.sample.db.AppDatabase
import com.topstep.wearkit.sample.entity.*
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.utils.AppUtils
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import timber.log.Timber

@SuppressLint("CheckResult")
class SyncDataActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivitySyncBinding
    private lateinit var appDatabase: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivitySyncBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.sync_data)
        appDatabase = AppDatabase.getInstance(this)
        initView()
        initData()
    }

    private fun initView() {
        viewBind.itemStep.clickTrigger(block = blockClick)
        viewBind.itemSleep.clickTrigger(block = blockClick)
        viewBind.itemHeartRate.clickTrigger(block = blockClick)
        viewBind.itemOxygen.clickTrigger(block = blockClick)
        viewBind.itemBloodPressure.clickTrigger(block = blockClick)
        viewBind.itemPressure.clickTrigger(block = blockClick)
        viewBind.itemSport.clickTrigger(block = blockClick)
    }

    private fun initData() {
        viewBind.refreshLayout.setOnRefreshListener {
            val device = wearKit.connector.getDevice()?.address
            if (device.isNullOrEmpty()) {
                toast("Not Connect Device")
                return@setOnRefreshListener
            }
            val timeProvider = SyncTimeProviderIncremental(this, "sample_sync_time", device)
            wearKit.deviceAbility.syncData(timeProvider)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe({
                    saveSyncData(it, timeProvider)
                }, {
                    Timber.i(it)
                    toast(R.string.tip_failed)
                }, {
                    viewBind.refreshLayout.isRefreshing = false
                    viewBind.tvRefreshState.setText(R.string.sync_state_idle)
                    toast(R.string.tip_success)
                })
        }
    }

    private fun saveSyncData(data: WKSyncData, timeProvider: SyncTimeProviderIncremental) {
        when (data.type) {
            // activity
            WKSyncData.Type.ACTIVITY -> {
                data.toActivity()?.map {
                    Timber.e("Sync Activity:$it")
                    ActivityEntity(
                        it.timestampSeconds, it.steps, it.distance.toFloat(),
                        it.calories.toFloat(), it.number, it.duration / 60,
                        it.sportDuration / 60
                    )
                }?.let {
                    appDatabase.activityDao().insert(it)
                }
            }
            // sleep
            WKSyncData.Type.SLEEP -> {
                data.toSleep()?.map {
                    Timber.e("Sync Sleep:$it")
                    SleepEntity(it.timestampSeconds, it.duration, it.deep, it.light, it.awake, it.rem, it.nap)
                }?.let {
                    appDatabase.sleepDao().insert(it)
                }
            }
            // heart rate
            WKSyncData.Type.HEART_RATE -> {
                data.toHeartRate()?.map {
                    Timber.e("Sync HeartRate:$it")
                    HeartRateEntity(it.timestampSeconds, it.heartRate)
                }?.let {
                    appDatabase.heartRateDao().insert(it)
                }
            }
            // blood oxygen
            WKSyncData.Type.BLOOD_OXYGEN -> {
                data.toBloodOxygen()?.map {
                    Timber.e("Sync BloodOxygen:$it")
                    BloodOxygenEntity(it.timestampSeconds, it.oxygen)
                }?.let {
                    appDatabase.bloodOxygenDao().insert(it)
                }
            }
            //blood pressure
            WKSyncData.Type.BLOOD_PRESSURE -> {
                data.toBloodPressure()?.map {
                    Timber.e("Sync BloodPressure:$it")
                    BloodPressureEntity(it.timestampSeconds, it.sbp, it.dbp)
                }?.let {
                    appDatabase.bloodPressureDao().insert(it)
                }
            }
            //pressure
            WKSyncData.Type.PRESSURE -> {
                data.toPressure()?.map {
                    Timber.e("Sync Pressure:$it")
                    PressureEntity(it.timestampSeconds, it.pressure)
                }?.let {
                    appDatabase.pressureDao().insert(it)
                }
            }
            //sport
            WKSyncData.Type.SPORT -> {
                data.toSport()?.map {
                    Timber.e("Sync Sport:$it")
                    SportEntity(
                        it.timestampSeconds,
                        it.sportType,
                        AppUtils.convertSportType(it.sportType, this),
                        it.endTimestampSeconds,
                        it.duration,
                        it.distance,
                        it.calories,
                        it.steps,
                        it.warmUpDuration,
                        it.fatBurningDuration,
                        it.aerobicDuration,
                        it.anaerobicDuration,
                        it.heartLimitDuration,
                        it.avgHeartRate,
                        it.maxHeartRate,
                        it.minHeartRate
                    )
                }?.let {
                    appDatabase.sportDao().insert(it)
                }
            }
            else -> {}
        }
        timeProvider.onItemSyncSuccess(data)
    }

    private val blockClick: (View) -> Unit = { view ->
        when (view) {
            viewBind.itemStep -> {
                startActivity(Intent(this, StepActivity::class.java))
            }
            viewBind.itemSleep -> {
                startActivity(Intent(this, SleepActivity::class.java))
            }
            viewBind.itemHeartRate -> {
                startActivity(Intent(this, HeartRateActivity::class.java))
            }
            viewBind.itemOxygen -> {
                startActivity(Intent(this, BloodOxygenActivity::class.java))
            }
            viewBind.itemBloodPressure -> {
                startActivity(Intent(this, BloodPressureActivity::class.java))
            }
            viewBind.itemPressure -> {
                startActivity(Intent(this, PressureActivity::class.java))
            }
            viewBind.itemSport -> {
                startActivity(Intent(this, SportActivity::class.java))
            }
        }
    }

}