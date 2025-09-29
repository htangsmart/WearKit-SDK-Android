package com.topstep.wearkit.sample.ui.contacts

import android.content.Intent
import android.os.Bundle
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityContactsHomeBinding
import com.topstep.wearkit.sample.sdk.observeSOS
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.utils.launchRepeatOnStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.asFlow

class ContactsHomeActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityContactsHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityContactsHomeBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.ds_contacts)

        viewBind.btnCommon.clickTrigger {
            if (wearKit.contactsAbility.compat.getContactsCommonMaxNumber() > 0) {
                startActivity(Intent(this, ContactsActivity::class.java))
            } else {
                toast(R.string.tip_un_support)
            }
        }
        viewBind.btnEmergency.clickTrigger {
            if (wearKit.contactsAbility.compat.getContactsEmergencyMaxNumber() > 0) {
                startActivity(Intent(this, EmergencyContactsActivity::class.java))
            } else {
                toast(R.string.tip_un_support)
            }
        }
        viewBind.btnImage.clickTrigger {
            if (wearKit.contactsAbility.compat.getContactsImageMaxNumber() > 0) {
                startActivity(Intent(this, ContactsImageActivity::class.java))
            } else {
                toast(R.string.tip_un_support)
            }
        }

        lifecycle.launchRepeatOnStarted {
            launch {
                wearKit.observeSOS().asFlow().collect {
                    toast("SOS trigger")
                }
            }
        }

    }

}