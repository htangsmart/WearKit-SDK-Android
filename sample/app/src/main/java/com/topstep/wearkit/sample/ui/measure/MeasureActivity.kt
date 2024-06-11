package com.topstep.wearkit.sample.ui.measure

import android.os.Bundle
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityMeasureBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity

class MeasureActivity : BaseActivity() {

    private lateinit var viewBind: ActivityMeasureBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityMeasureBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.health_realtime)
        // heartRate measure
        viewBind.itemHeartrate.clickTrigger {
            HealthMeasureActivity.startActivity(this, 1)
        }

        //bloodOxygen measure
        viewBind.itemBloodOxygen.clickTrigger {
            HealthMeasureActivity.startActivity(this, 2)
        }

        //pressure measure
        viewBind.itemPressure.clickTrigger {
            HealthMeasureActivity.startActivity(this, 3)
        }
    }
}