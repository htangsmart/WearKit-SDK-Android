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
class SportActivity : BaseActivity() {
    private lateinit var viewBind: ActivityStepBinding
    private lateinit var appDatabase: AppDatabase

    private val adapter = SportAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityStepBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.data_sport)
        appDatabase = AppDatabase.getInstance(this)
        initView()
        initData()
    }

    private fun initView() {
        viewBind.stepRy.layoutManager = LinearLayoutManager(this)
        viewBind.stepRy.adapter = adapter
    }

    private fun initData() {
        val sportList =  appDatabase.sportDao().queryAll()
        if (!sportList.isNullOrEmpty()) {
            val reversed = sportList.reversed()
            adapter.addSport(reversed)
        }
    }
}