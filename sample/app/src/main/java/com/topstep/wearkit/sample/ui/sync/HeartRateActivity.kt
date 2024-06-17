package com.topstep.wearkit.sample.ui.sync

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityStepBinding
import com.topstep.wearkit.sample.db.AppDatabase
import com.topstep.wearkit.sample.entity.*
import com.topstep.wearkit.sample.ui.base.BaseActivity


@SuppressLint("CheckResult")
class HeartRateActivity : BaseActivity() {
    private lateinit var viewBind: ActivityStepBinding
    private lateinit var appDatabase: AppDatabase

    private val adapter = HeartRateAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityStepBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.data_heart_rate)
        appDatabase = AppDatabase.getInstance(this)
        initView()
        initData()
    }

    private fun initView() {
        viewBind.stepRy.layoutManager = LinearLayoutManager(this)
        viewBind.stepRy.adapter = adapter
    }

    private fun initData() {
        val heartList = appDatabase.heartRateDao().queryAll()
        if (!heartList.isNullOrEmpty()) {
            adapter.addActivity(heartList)
        }
    }
}