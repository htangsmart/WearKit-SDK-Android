package com.topstep.wearkit.sample.ui.discovery

import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import com.polidea.rxandroidble3.RxBleClient
import com.polidea.rxandroidble3.exceptions.BleScanException
import com.polidea.rxandroidble3.scan.ScanSettings
import com.topstep.wearkit.apis.model.core.WKDeviceType
import com.topstep.wearkit.apis.model.core.WKScanResult
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.utils.flowBluetoothAdapterState
import com.topstep.wearkit.sample.utils.permission.PermissionHelper
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.concurrent.TimeUnit

/**
 * @param type Device type for SDK filtered scan. When null, scans all BLE devices via [RxBleClient].
 */
class ScannerHelper(
    private val context: Context,
    private val type: WKDeviceType? = null,
) : DefaultLifecycleObserver {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    private val wearKit = MyApplication.wearKit

    private var stateJob: Job? = null
    private var scanDisposable: Disposable? = null

    /**
     * Whether an auto scan has been performed.
     * Auto scan only performed once.
     */
    private var isAutoScanned = false

    /**
     * The number of consecutive occurrences of an unknown error
     */
    private var errorUnknownCount = 0

    var listener: Listener? = null

    private val flowPermissionsState = flow {
        val hasPermissions = PermissionHelper.hasBle(context)
        emit(hasPermissions)
        if (!hasPermissions) {
            while (currentCoroutineContext().isActive && !PermissionHelper.hasBle(context)) {
                delay(1000)
            }
            //Delay for a while. Sometimes there will be errors in scanning immediately
            delay(500)
            emit(true)
        }
    }.flowOn(Dispatchers.Default)

    private val flowState: Flow<Int> = flowBluetoothAdapterState(context)
        .combine(flowPermissionsState) { isAdapterEnabled, hasPermissions ->
            if (!hasPermissions) {
                STATE_NO_PERMISSION
            } else if (!isAdapterEnabled) {
                STATE_BT_DISABLED
            } else {
                STATE_READY
            }
        }

    private fun getState(): Int {
        val hasPermissions = PermissionHelper.hasBle(context)
        return if (!hasPermissions) {
            STATE_NO_PERMISSION
        } else if (!bluetoothManager.adapter.isEnabled) {
            STATE_BT_DISABLED
        } else {
            STATE_READY
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        stateJob = owner.lifecycle.coroutineScope.launch {
            flowState.collect {
                checkState(it, true)
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        //Cancel scan when onStop
        scanDisposable?.dispose()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        stateJob?.cancel()
    }

    /**
     * @return Whether scanning is started
     */
    private fun checkState(state: Int, performedByAuto: Boolean): Boolean {
        if (state == STATE_NO_PERMISSION) {
            listener?.requestPermission()
        } else {
            listener?.bluetoothAlert(state == STATE_BT_DISABLED)
            if (state == STATE_READY) {
                if (!performedByAuto || !isAutoScanned) {
                    isAutoScanned = true
                    scan()
                    return true
                }
            }
        }
        return false
    }

    private fun scan() {
        if (scanDisposable?.isDisposed != false) {
            //It is recommended not to set the scan duration too short
            scanDisposable = createScanObservable()
                .doOnSubscribe {
                    listener?.onScanStart()
                }
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally {
                    listener?.onScanStop()
                }
                .subscribe({
                    listener?.onScanResult(it)
                }, {
                    //Analysis error
                    analysisScanError(it)
                }, {
                    errorUnknownCount = 0
                })
        }
    }

    private fun createScanObservable(): Observable<WKScanResult> {
        val deviceType = type
        return if (deviceType != null) {
            // Keep legacy duration arg used by DeviceScanActivity
            wearKit.scanner.scan(deviceType, 120 * 1000, checkLocationService = false, acceptEmptyName = true)
        } else {
            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setShouldCheckLocationServicesState(false)
                .build()
            val hasConnectPermission = MyApplication.rxBleClient.isConnectRuntimePermissionGranted
            MyApplication.rxBleClient.scanBleDevices(scanSettings)
                .take(120 * 1000, TimeUnit.SECONDS)
                .map { result ->
                    WKScanResult(
                        device = result.bleDevice.bluetoothDevice,
                        name = if (hasConnectPermission) result.bleDevice.name else null,
                        rssi = result.rssi,
                    )
                }
                .filter {
                    // Intentionally drop empty names: raw scan has no WKDeviceType filter,
                    // so the list would be too noisy for manual selection.
                    val name = it.name
                    name != null && name.isNotEmpty()
                }
        }
    }

    private fun analysisScanError(throwable: Throwable) {
        val reason = if (throwable is BleScanException) {
            throwable.reason
        } else {
            BleScanException.UNKNOWN_ERROR_CODE
        }
        when (reason) {
            BleScanException.BLUETOOTH_DISABLED,
            BleScanException.BLUETOOTH_NOT_AVAILABLE,
            BleScanException.LOCATION_PERMISSION_MISSING,
            BleScanException.LOCATION_SERVICES_DISABLED,
                -> {
                //Ignore these error states because it is handled elsewhere, or has been checked before the scan starts
            }
            BleScanException.SCAN_FAILED_ALREADY_STARTED,
            BleScanException.UNDOCUMENTED_SCAN_THROTTLE,
                -> {
                //Prompt the user to re-search in a few seconds
                listener?.scanErrorDelayAlert()
            }
            else -> {
                errorUnknownCount++
                if (errorUnknownCount <= 3) {
                    listener?.scanErrorDelayAlert()
                } else {
                    //Prompt the user to restart Bluetooth or Mobile-Phone
                    listener?.scanErrorRestartAlert()
                }
            }
        }
    }

    /**
     * @return Whether scanning is started
     */
    fun start(): Boolean {
        return checkState(getState(), false)
    }

    fun stop() {
        scanDisposable?.dispose()
    }

    fun toggle() {
        if (scanDisposable?.isDisposed != false) {
            start()
        } else {
            stop()
        }
    }

    interface Listener {
        fun requestPermission()
        fun bluetoothAlert(show: Boolean)

        fun scanErrorDelayAlert()
        fun scanErrorRestartAlert()

        fun onScanStart()
        fun onScanStop()
        fun onScanResult(result: WKScanResult)
    }

    companion object {
        private const val STATE_NO_PERMISSION = 0
        private const val STATE_BT_DISABLED = 1
        private const val STATE_READY = 2
    }
}
