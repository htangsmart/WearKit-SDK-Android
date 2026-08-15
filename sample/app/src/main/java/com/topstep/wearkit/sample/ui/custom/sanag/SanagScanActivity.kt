package com.topstep.wearkit.sample.ui.custom.sanag

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.topstep.wearkit.apis.model.core.WKScanResult
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivitySanagScanBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.ui.discovery.*
import com.topstep.wearkit.sample.utils.flowLocationServiceState
import com.topstep.wearkit.sample.utils.launchRepeatOnStarted
import com.topstep.wearkit.sample.utils.permission.PermissionHelper
import kotlinx.coroutines.launch
import timber.log.Timber

class SanagScanActivity : BaseActivity() {

    private lateinit var viewBind: ActivitySanagScanBinding
    private lateinit var scannerHelper: ScannerHelper

    private var isRequestingPermission: Boolean = false

    private val adapter: ScanDevicesAdapter = ScanDevicesAdapter().apply {
        listener = object : ScanDevicesAdapter.Listener {
            override fun onItemClick(device: ScanDevice) {
                selectDevice(device.address, device.name)
            }
        }
    }

    private val scannerListener = object : ScannerHelper.Listener {

        override fun requestPermission() {
            lifecycleScope.launchWhenResumed {
                if (!isRequestingPermission) {
                    isRequestingPermission = true
                    PermissionHelper.requestBle(this@SanagScanActivity) {
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
        viewBind = ActivitySanagScanBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.main_to_scan)

        // type = null → raw RxBleClient scan (no WKDeviceType filter)
        scannerHelper = ScannerHelper(this)
        scannerHelper.listener = scannerListener
        lifecycle.addObserver(scannerHelper)

        viewBind.refreshLayout.setOnRefreshListener {
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
                    flowLocationServiceState(this@SanagScanActivity).collect { isEnabled ->
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

    private fun selectDevice(address: String, name: String?) {
        scannerHelper.stop()
        SanagPreferencesStorage.setLastDevice(
            SanagDeviceInfo(
                address = address,
                name = if (name.isNullOrEmpty()) ScanDevicesAdapter.UNKNOWN_DEVICE_NAME else name,
            )
        )
        setResult(RESULT_OK)
        finish()
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

}
