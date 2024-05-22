package com.topstep.wearkit.sample.ui.contacts

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.result.contract.ActivityResultContracts
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.apis.model.WKContacts
import com.topstep.wearkit.apis.model.WKContactsEmergency
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityEmergencyBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.utils.permission.PermissionHelper
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import timber.log.Timber

@SuppressLint("CheckResult")
class EmergencyContactsActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityEmergencyBinding

    private var cache: WKContacts? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityEmergencyBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.ds_contacts_emergency)

        initEvent()

        //request
        wearKit.contactsAbility.requestContactsEmergency()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                viewBind.switchEmergency.isChecked = it.isEnabled
                it.items?.firstOrNull()?.let { c ->
                    cache = c
                    viewBind.emergencyNama.text = c.name
                    viewBind.emergencyNumber.text = c.number
                }
            }, {
                Timber.w(it)
                toast(R.string.tip_failed)
            })
    }

    private fun initEvent() {
        viewBind.switchEmergency.setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed) {
                setEmergencyContact(isChecked, cache)
            }
        }

        // add
        viewBind.emergencyAdd.clickTrigger {
            PermissionHelper.requestContacts(this) { granted ->
                if (granted) {
                    pickContact.launch(Intent(Intent.ACTION_PICK).apply {
                        type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
                    })
                }
            }
        }

        //delete
        viewBind.emergencyDelete.clickTrigger {
            cache = null
            setEmergencyContact(viewBind.switchEmergency.isChecked, null)
        }
    }

    private val pickContact = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val contacts = ContactsActivity.onPickContacts(this, result)
        if (contacts != null) {
            viewBind.emergencyNama.text = contacts.name
            viewBind.emergencyNumber.text = contacts.number
            cache = contacts
            setEmergencyContact(viewBind.switchEmergency.isChecked, contacts)
        }
    }

    private fun setEmergencyContact(isSwitch: Boolean, contacts: WKContacts?) {
        wearKit.contactsAbility.setContactsEmergency(WKContactsEmergency(isSwitch, if (contacts == null) emptyList() else listOf(contacts)))
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                toast(getString(R.string.tip_success))
            }, {
                Timber.w(it)
                toast(getString(R.string.tip_failed))
            })
    }

}