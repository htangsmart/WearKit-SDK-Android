package com.topstep.wearkit.sample.ui.discovery

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import androidx.recyclerview.widget.SortedListAdapterCallback
import com.topstep.wearkit.apis.model.core.WKScanResult
import com.topstep.wearkit.sample.databinding.ItemScanDeviceBinding
import kotlin.math.abs

class ScanDevicesAdapter : RecyclerView.Adapter<ScanDevicesAdapter.DeviceViewHolder>() {

    private val sorter = SortedList(ScanDevice::class.java, object : SortedListAdapterCallback<ScanDevice>(this) {
        override fun compare(o1: ScanDevice, o2: ScanDevice): Int {
            return o2.rssi.compareTo(o1.rssi)
        }

        override fun areContentsTheSame(oldItem: ScanDevice, newItem: ScanDevice): Boolean {
            return oldItem.name == newItem.name && oldItem.rssi == newItem.rssi
        }

        override fun areItemsTheSame(item1: ScanDevice, item2: ScanDevice): Boolean {
            return item1.address == item2.address
        }
    })

    var listener: Listener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        return DeviceViewHolder(
            ItemScanDeviceBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(sorter[position])
        holder.viewBind.root.setOnClickListener {
            listener?.onItemClick(sorter[holder.bindingAdapterPosition])
        }
    }

    override fun getItemCount(): Int {
        return sorter.size()
    }

    fun newScanResult(result: WKScanResult) {
        /**
         * ToNote:The data in [SortedList] is sorted, so the [SortedList.indexOf] method uses binary search to improve efficiency.
         * Unfortunately, this only works if the primary keys match the sort keys.That is, the [SortedListAdapterCallback.areItemsTheSame] method and [SortedListAdapterCallback.compare] need to maintain consistency.
         * We use [ScanDevice.address] as primary key. And [ScanDevice.rssi] as sort key. So never use [SortedList.indexOf] to find a item.
         */
        var existIndex = SortedList.INVALID_POSITION
        for (i in 0 until sorter.size()) {
            if (result.device.address == sorter[i].address) {
                existIndex = i
                break
            }
        }
        val exist = if (existIndex != SortedList.INVALID_POSITION) {
            sorter.get(existIndex)
        } else {
            null
        }
        if (exist != null) {
            //If it exists, then update the rssi and the name that may change
            //ToNote:In rare cases, the name may change
            val nameChanged = exist.name != result.name && !result.name.isNullOrEmpty()
            //ToNote:Not updated when the rssi difference is small. This is to avoid frequent drawing of View when there are a large number of devices around
            val rssiChanged = abs(exist.rssi - result.rssi) > 5
            if (nameChanged || rssiChanged) {
                val newItem = ScanDevice(
                    exist.address,
                    if (nameChanged) result.name else exist.name,
                    if (rssiChanged) result.rssi else exist.rssi
                )
                sorter.updateItemAt(existIndex, newItem)
            }
        } else {
            //If it does not exist, then add
            sorter.add(ScanDevice(result.device.address, result.name, result.rssi))
        }
    }

    fun clearItems() {
        sorter.clear()
    }

    interface Listener {
        fun onItemClick(device: ScanDevice)
    }

    class DeviceViewHolder(val viewBind: ItemScanDeviceBinding) : RecyclerView.ViewHolder(viewBind.root) {
        fun bind(result: ScanDevice) {
            viewBind.tvName.text = if (result.name.isNullOrEmpty()) {
                DeviceScanActivity.UNKNOWN_DEVICE_NAME
            } else {
                result.name
            }
            viewBind.tvAddress.text = result.address
            viewBind.tvRssi.text = "${result.rssi}"
        }
    }

}

class ScanDevice(
    val address: String,
    var name: String?,
    var rssi: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ScanDevice

        if (address != other.address) return false

        return true
    }

    override fun hashCode(): Int {
        return address.hashCode()
    }

    override fun toString(): String {
        return "ScanDevice(address='$address', name='$name', rssi=$rssi)"
    }
}