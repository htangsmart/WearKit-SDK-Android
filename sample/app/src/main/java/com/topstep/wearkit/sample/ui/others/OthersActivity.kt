package com.topstep.wearkit.sample.ui.others

import android.content.Intent
import android.os.Bundle
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityOthersBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity

class OthersActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityOthersBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityOthersBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.title = "Others"

        viewBind.itemICE.clickTrigger {
            if (wearKit.b2b.hsdAbility.compat.isSupportIce()) {
                startActivity(Intent(this, HsdIceActivity::class.java))
            } else {
                toast(R.string.unit_km)
            }
        }
    }

}