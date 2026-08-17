package com.topstep.wearkit.sample.ui.custom.sanag

import com.topstep.wearkit.apis.model.core.WKConnectorState
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import kotlinx.coroutines.CoroutineScope

/**
 * One Sanag demo feature. Add a new implementation and register it in [SanagDemoActivity].
 */
internal interface SanagDemoFeature {
    fun onCreate() {}
    fun observe(scope: CoroutineScope) {}
    fun onResume() {}
    fun onDestroy() {}
}

internal fun SanagDemoActivity.requireDeviceConnected(): Boolean {
    if (MyApplication.wearKit.connector.getConnectorState() != WKConnectorState.CONNECTED) {
        toast(R.string.device_state_disconnected)
        return false
    }
    return true
}
