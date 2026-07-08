package com.topstep.wearkit.sample.ui.raw

import android.content.Intent
import android.os.Bundle
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.sample.databinding.ActivityRawFcSdkBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity

/**
 * Demonstrate how to call the same capability of the underlying SDK
 */
class RawFcSdkActivity : BaseActivity() {

    private lateinit var viewBind: ActivityRawFcSdkBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityRawFcSdkBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.title = "Raw FcSDK"

        viewBind.itemTaxi.clickTrigger {
            startActivity(Intent(this, TaxiActivity::class.java))
        }
    }

}
