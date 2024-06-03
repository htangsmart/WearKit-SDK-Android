package com.topstep.wearkit.sample.ui.remind

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.apis.model.WKRemind
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityRemindBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers

@SuppressLint("CheckResult", "Range", "NotifyDataSetChanged")
class RemindActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityRemindBinding
    private lateinit var adapter: RemindAdapter
    private val remindCount = ArrayList<WKRemind>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityRemindBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.title = "remind"
        initView()
        initData()
        initEvent()
    }

    override fun onResume() {
        super.onResume()
        // request remind
        wearKit.remindAbility.requestReminds()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (it.isNotEmpty()) {
                    remindCount.clear()
                    remindCount.addAll(it)
                    adapter.sources = remindCount
                    adapter.notifyDataSetChanged()
                }
            }, {

            })
    }

    private fun initView() {
        adapter = RemindAdapter()
        if (wearKit.remindAbility.compat.isSupportType(WKRemind.Type.Custom::class.java)) {
            viewBind.remindAdd.visibility = View.VISIBLE
        } else {
            viewBind.remindAdd.visibility = View.GONE
        }
    }

    private fun initData() {
        viewBind.remindRy.layoutManager = LinearLayoutManager(this)
        viewBind.remindRy.adapter = adapter
        adapter.listener = object : RemindAdapter.Listener {
            override fun onItemClick(name: String, type: WKRemind.Type) {
                RemindContentActivity.startActivity(this@RemindActivity, RemindContentActivity.type2TransId(type), name, false)
            }
        }
    }

    private fun initEvent() {
        // add remind
        viewBind.remindAdd.clickTrigger {
            var customCount = 0
            val sources = adapter.sources
            if (!sources.isNullOrEmpty()) {
                sources.forEach {
                    if (it.type != WKRemind.Type.Sedentary
                        && it.type != WKRemind.Type.DrinkWater
                        && it.type != WKRemind.Type.TakeMedicine
                    ) {
                        customCount++
                    }
                }
            }
            val customRemindMaxNumber = wearKit.remindAbility.compat.getCustomRemindMaxNumber()
            if (customCount >= customRemindMaxNumber) {
                toast(getString(R.string.ds_remind_add_limit, customRemindMaxNumber))
            } else {
                val createCustomRemind = wearKit.remindAbility.compat.createCustomRemind(adapter.sources)
                if (createCustomRemind != null) {
                    RemindContentActivity.startActivity(this@RemindActivity, RemindContentActivity.type2TransId(createCustomRemind.type), "", true)
                }
            }
        }
    }
}