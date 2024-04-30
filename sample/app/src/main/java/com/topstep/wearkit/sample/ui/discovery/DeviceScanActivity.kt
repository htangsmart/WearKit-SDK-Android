package com.topstep.wearkit.sample.ui.discovery

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.topstep.wearkit.apis.model.core.WKDeviceType
import com.topstep.wearkit.apis.model.core.WKScanResult
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityDeviceScanBinding
import com.topstep.wearkit.sample.model.DeviceInfo
import com.topstep.wearkit.sample.ui.DeviceActivity
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.utils.flowLocationServiceState
import com.topstep.wearkit.sample.utils.launchRepeatOnStarted
import com.topstep.wearkit.sample.utils.permission.PermissionHelper
import kotlinx.coroutines.launch
import timber.log.Timber

class DeviceScanActivity : BaseActivity() {

    private lateinit var viewBind: ActivityDeviceScanBinding
    private lateinit var type: WKDeviceType
    private lateinit var scannerHelper: ScannerHelper

    /**
     * Avoid repeated requests for permissions at the same time
     */
    private var isRequestingPermission: Boolean = false

    private val adapter: ScanDevicesAdapter = ScanDevicesAdapter().apply {
        listener = object : ScanDevicesAdapter.Listener {
            override fun onItemClick(device: ScanDevice) {
                tryingBind(device.address, device.name)
            }
        }
    }

    private val scannerListener = object : ScannerHelper.Listener {

        override fun requestPermission() {
            lifecycleScope.launchWhenResumed {
                if (!isRequestingPermission) {
                    isRequestingPermission = true
                    PermissionHelper.requestBle(this@DeviceScanActivity) {
                        isRequestingPermission = false
                    }
                }
            }
        }

        override fun bluetoothAlert(show: Boolean) {
            toggleBluetoothAlert(show)
        }

        override fun scanErrorDelayAlert() {
            lifecycleScope.launchWhenStarted {
                ScanErrorDelayDialogFragment().show(supportFragmentManager, null)
            }
        }

        override fun scanErrorRestartAlert() {
            lifecycleScope.launchWhenStarted {
                ScanErrorRestartDialogFragment().show(supportFragmentManager, null)
            }
        }

        override fun onScanStart() {
            viewBind.btnSearch.setText(android.R.string.cancel)
            viewBind.refreshLayout.isRefreshing = true
        }

        override fun onScanStop() {
            viewBind.btnSearch.setText(R.string.action_search)
            viewBind.refreshLayout.isRefreshing = false
        }

        override fun onScanResult(result: WKScanResult) {
            adapter.newScanResult(result)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityDeviceScanBinding.inflate(layoutInflater)
        setContentView(viewBind.root)

        supportActionBar?.setTitle(R.string.main_to_scan)

        type = WKDeviceType.valueOf(intent.getStringExtra(EXTRA_TYPE)!!)
        scannerHelper = ScannerHelper(this, type)
        scannerHelper.listener = scannerListener
        lifecycle.addObserver(scannerHelper)

        viewBind.refreshLayout.setOnRefreshListener {
            //Clear data when using pull to refresh. This is a different strategy than fabScan click event
            adapter.clearItems()
            if (!scannerHelper.start()) {
                viewBind.refreshLayout.isRefreshing = false
            }
        }

        viewBind.recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        viewBind.recyclerView.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))
        viewBind.recyclerView.adapter = adapter

        viewBind.btnSearch.setOnClickListener {
            scannerHelper.toggle()
        }

        lifecycle.launchRepeatOnStarted {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                launch {
                    flowLocationServiceState(this@DeviceScanActivity).collect { isEnabled ->
                        viewBind.layoutLocationService.isVisible = !isEnabled
                    }
                }
            } else {
                viewBind.layoutLocationService.isVisible = false
            }
        }
        viewBind.btnEnableLocationService.setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            } catch (e: Exception) {
                Timber.w(e)
            }
        }
    }

    private fun tryingBind(address: String, name: String?) {
        scannerHelper.stop()
        DeviceActivity.start(
            this, DeviceInfo(
                type, address, if (name.isNullOrEmpty()) {
                    UNKNOWN_DEVICE_NAME
                } else {
                    name
                }
            )
        )
    }

    private var bluetoothSnackbar: Snackbar? = null

    private fun toggleBluetoothAlert(show: Boolean) {
        if (show) {
            val snackbar = bluetoothSnackbar ?: createBluetoothSnackbar().also { bluetoothSnackbar = it }
            if (!snackbar.isShownOrQueued) {
                snackbar.show()
            }
        } else {
            bluetoothSnackbar?.dismiss()
        }
    }

    private fun createBluetoothSnackbar(): Snackbar {
        val snackbar = Snackbar.make(viewBind.root, R.string.device_state_bt_disabled, Snackbar.LENGTH_INDEFINITE)
        snackbar.setAction(R.string.action_turn_on) {
            PermissionHelper.requestBle(this) { granted ->
                if (granted) {
                    startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                }
            }
        }
        return snackbar
    }

    companion object {
        private const val EXTRA_TYPE = "type"
        const val UNKNOWN_DEVICE_NAME = "Unknown"

        fun start(context: Context, type: WKDeviceType) {
            context.startActivity(
                Intent(context, DeviceScanActivity::class.java).apply {
                    putExtra(EXTRA_TYPE, type.name)
                }
            )
        }
    }
}