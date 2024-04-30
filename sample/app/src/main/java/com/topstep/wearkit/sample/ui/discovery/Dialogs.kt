package com.topstep.wearkit.sample.ui.discovery

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topstep.wearkit.sample.R

class ScanErrorDelayDialogFragment : AppCompatDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.device_scan_tips_error)
            .setMessage(R.string.device_scan_tips_delay)
            .setPositiveButton(R.string.tip_i_know, null)
            .create()
    }
}

class ScanErrorRestartDialogFragment : AppCompatDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.device_scan_tips_error)
            .setMessage(R.string.device_scan_tips_restart)
            .setPositiveButton(R.string.tip_i_know, null)
            .create()
    }
}

class OthersDeviceDialogFragment {

//    private val otherDevicesAdapter: OtherDevicesAdapter = OtherDevicesAdapter().apply {
//        listener = object : OtherDevicesAdapter.Listener {
//            override fun onItemClick(device: ScanDevice) {
//                tryingBind(device.address, device.name)
//            }
//        }
//    }
//    viewBind.otherDevicesRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
//    viewBind.otherDevicesRecyclerView.adapter = otherDevicesAdapter
//    private fun refreshOtherDevices(checkPermission: Boolean) {
//        if (checkPermission && !PermissionHelper.hasBle(requireContext())) {
//            return
//        }
//        PermissionHelper.requestBle(this) { granted ->
//            if (granted) {
//                otherDevicesAdapter.bonded = OtherDevicesAdapter.devices(bluetoothManager.adapter.bondedDevices)
//                otherDevicesAdapter.connected = OtherDevicesAdapter.devices(bluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER))
//                otherDevicesAdapter.notifyDataSetChanged()
//            }
//        }
//    }
}