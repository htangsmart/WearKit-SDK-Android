package com.topstep.wearkit.sample.ui.others

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.apis.model.b2b.HsdParentalControl
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityHsdParentalControlBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import timber.log.Timber

@SuppressLint("NotifyDataSetChanged")
class HsdParentalControlActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityHsdParentalControlBinding
    private val adapter = HsdParentalControlAdapter()

    private val addItemLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val item = HsdParentalControlAddActivity.parseResult(result.data) ?: return@registerForActivityResult
            upsertItem(item)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityHsdParentalControlBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.title = "Parental Control"

        viewBind.recyclerView.layoutManager = LinearLayoutManager(this)
        viewBind.recyclerView.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )
        viewBind.recyclerView.adapter = adapter
        adapter.listener = object : HsdParentalControlAdapter.Listener {
            override fun onItemClick(item: HsdParentalControl.Item) {
                addItemLauncher.launch(HsdParentalControlAddActivity.createIntent(this@HsdParentalControlActivity, item))
            }

            override fun onItemDelete(position: Int) {
                adapter.sources.removeAt(position)
                adapter.notifyDataSetChanged()
            }
        }

        viewBind.btnAdd.clickTrigger {
            addItemLauncher.launch(HsdParentalControlAddActivity.createIntent(this))
        }
        viewBind.btnRequest.clickTrigger {
            requestParentalControl()
        }
        viewBind.btnSave.clickTrigger {
            saveParentalControl()
        }
    }

    private fun upsertItem(item: HsdParentalControl.Item) {
        val index = adapter.sources.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            adapter.sources[index] = item
        } else {
            adapter.sources.add(item)
        }
        adapter.notifyDataSetChanged()
    }

    @SuppressLint("CheckResult")
    private fun requestParentalControl() {
        wearKit.b2b.hsdAbility.requestParentalControl()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ control ->
                viewBind.itemEnabled.getSwitchView().isChecked = control.isEnabled
                adapter.sources = control.items.toMutableList()
                adapter.notifyDataSetChanged()
                toast(R.string.tip_success)
            }, {
                Timber.w(it)
                toast(R.string.tip_failed)
            })
    }

    @SuppressLint("CheckResult")
    private fun saveParentalControl() {
        val control = HsdParentalControl(
            isEnabled = viewBind.itemEnabled.getSwitchView().isChecked,
            items = adapter.sources.toList(),
        )
        wearKit.b2b.hsdAbility.setParentalControl(control)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                toast(R.string.tip_success)
            }, {
                Timber.w(it)
                toast(it.message ?: getString(R.string.tip_failed))
            })
    }
}
