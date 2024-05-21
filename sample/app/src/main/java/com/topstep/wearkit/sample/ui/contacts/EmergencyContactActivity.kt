package com.topstep.wearkit.sample.ui.contacts

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.view.View
import android.widget.Toast
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

class EmergencyContactActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityEmergencyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityEmergencyBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.title = "Emergency Contacts"
        initView()
        initData()
        initEvent()
    }

    @SuppressLint("CheckResult", "NotifyDataSetChanged")
    private fun initView() {

        // request
        wearKit.contactsAbility.requestContactsEmergency()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                viewBind.emergencySw.isChecked = it.isEnabled
                val items = it.items
                if (!items.isNullOrEmpty()) {
                    val name = items[0].name
                    val number = items[0].number
                    viewBind.emergencyNama.text = name
                    viewBind.emergencyNumber.text = number
                }

            }, {

            })
    }

    private fun initData() {
        viewBind.emergencySw.setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed) {
                viewBind.emergencySw.isChecked = isChecked
            }
        }

    }


    @SuppressLint("CheckResult")
    private fun initEvent() {
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
            setEmergencyContact(viewBind.emergencySw.isChecked, ArrayList())
        }

    }

    @SuppressLint("CheckResult")
    private val pickContact =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val uri = result.data?.data
            if (result.resultCode == Activity.RESULT_OK && uri != null) {
                val projection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                )
                val cursor = contentResolver.query(uri, projection, null, null, null)
                if (cursor != null && cursor.moveToFirst()) {
                    val numberIndex =
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val nameIndex =
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    var number = cursor.getString(numberIndex)
                    var name = cursor.getString(nameIndex)
                    cursor.close()
                    if (!name.isNullOrEmpty() && !number.isNullOrEmpty()) {
                        number = number.replace(" ".toRegex(), "")
                        viewBind.emergencyNama.text = name
                        viewBind.emergencyNumber.text = number
                        val common = ArrayList<WKContacts>()
                        val fwContacts = WKContacts(name, number)
                        common.add(fwContacts)
                        setEmergencyContact(viewBind.emergencySw.isChecked, common)
                    }
                }
            }
        }

    @SuppressLint("CheckResult")
    fun setEmergencyContact(isSwitch: Boolean, con: ArrayList<WKContacts>) {
        //setEmergencyContact
        wearKit.contactsAbility.setContactsEmergency(WKContactsEmergency(isSwitch, con))
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                viewBind.emergencyNama.visibility = View.VISIBLE
                viewBind.emergencyNumber.visibility = View.VISIBLE
                toast(getString(R.string.tip_success))
            }, {
                viewBind.emergencyNama.visibility = View.GONE
                viewBind.emergencyNumber.visibility = View.GONE
                toast(getString(R.string.tip_failed))
            })
    }

}