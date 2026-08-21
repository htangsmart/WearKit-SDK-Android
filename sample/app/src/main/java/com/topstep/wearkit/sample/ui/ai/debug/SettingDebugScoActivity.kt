package com.topstep.wearkit.sample.ui.ai.debug

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivitySettingDebugScoBinding
import com.topstep.wearkit.sample.databinding.ItemScanDeviceBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.utils.permission.PermissionHelper

class SettingDebugScoActivity : BaseActivity() {

    private lateinit var viewBind: ActivitySettingDebugScoBinding
    private val adapter = BondedDeviceAdapter { device ->
        DebugScoStorage.set(this, device.address, device.name)
        bindSelected()
        toast(R.string.tip_save_success)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivitySettingDebugScoBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.ds_speech_debug_sco)

        viewBind.recyclerView.layoutManager = LinearLayoutManager(this)
        viewBind.recyclerView.adapter = adapter
        viewBind.btnClear.setOnClickListener {
            DebugScoStorage.clear(this)
            bindSelected()
            toast(R.string.tip_success)
        }

        bindSelected()
        PermissionHelper.requestBle(this) { granted ->
            if (granted) {
                loadBondedDevices()
            } else {
                toast(R.string.permission_explain_msg)
            }
        }
    }

    private fun bindSelected() {
        val selected = DebugScoStorage.get(this)
        if (selected == null) {
            viewBind.tvSelectedName.setText(R.string.ds_speech_debug_sco_none)
            viewBind.tvSelectedAddress.text = ""
            viewBind.btnClear.isEnabled = false
        } else {
            viewBind.tvSelectedName.text = selected.name.ifBlank { selected.address }
            viewBind.tvSelectedAddress.text = selected.address
            viewBind.btnClear.isEnabled = true
        }
        adapter.selectedAddress = selected?.address
    }

    @SuppressLint("MissingPermission")
    private fun loadBondedDevices() {
        val bonded = runCatching {
            MyApplication.wearKit.bluetoothAdapter?.bondedDevices.orEmpty()
                .sortedBy { it.name.orEmpty() }
                .map { BondedItem(it.address, it.name.orEmpty().ifBlank { it.address }) }
        }.getOrDefault(emptyList())
        adapter.submit(bonded)
        viewBind.tvEmpty.visibility = if (bonded.isEmpty()) View.VISIBLE else View.GONE
    }

    private data class BondedItem(
        val address: String,
        val name: String,
    )

    private class BondedDeviceAdapter(
        private val onClick: (BondedItem) -> Unit,
    ) : RecyclerView.Adapter<BondedDeviceAdapter.Holder>() {

        private var items: List<BondedItem> = emptyList()
        var selectedAddress: String? = null
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        fun submit(list: List<BondedItem>) {
            items = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            return Holder(
                ItemScanDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = items[position]
            holder.bind(item, item.address == selectedAddress)
            holder.itemView.setOnClickListener { onClick(item) }
        }

        override fun getItemCount(): Int = items.size

        class Holder(
            private val viewBind: ItemScanDeviceBinding,
        ) : RecyclerView.ViewHolder(viewBind.root) {
            fun bind(item: BondedItem, selected: Boolean) {
                viewBind.tvName.text = item.name
                viewBind.tvAddress.text = item.address
                viewBind.tvRssi.visibility = View.GONE
                viewBind.root.isSelected = selected
            }
        }
    }
}
