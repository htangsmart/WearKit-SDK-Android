package com.topstep.wearkit.sample.ui.custom.sanag

import android.os.Bundle
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivitySanagDemoBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.utils.launchRepeatOnStarted

/**
 * "塞那"定制功能
 *
 * Add a new feature: implement [SanagDemoFeature] and append it in [createFeatures].
 */
class SanagDemoActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivitySanagDemoBinding
    private lateinit var features: List<SanagDemoFeature>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SanagPreferencesStorage.init(this)
        // Avoid competing with DeviceActivity / other modules on the shared connector
        wearKit.connector.close()
        viewBind = ActivitySanagDemoBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.main_sanag_demo)

        features = createFeatures()
        features.forEach { it.onCreate() }

        lifecycle.launchRepeatOnStarted {
            features.forEach { it.observe(this) }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::features.isInitialized) {
            features.forEach { it.onResume() }
        }
    }

    override fun onDestroy() {
        if (::features.isInitialized) {
            features.asReversed().forEach { it.onDestroy() }
        }
        super.onDestroy()
    }

    private fun createFeatures(): List<SanagDemoFeature> = listOf(
        SanagDeviceFeature(this, viewBind),
        SanagBatteryFeature(this, viewBind),
        SanagVersionOtaFeature(this, viewBind),
        SanagFileFeature(this, viewBind),
        SanagSpeechFeature(this, viewBind),
    )
}
