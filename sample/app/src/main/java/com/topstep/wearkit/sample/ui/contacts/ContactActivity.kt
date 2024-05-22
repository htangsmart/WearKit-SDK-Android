package com.topstep.wearkit.sample.ui.contacts

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.apis.model.WKContacts
import com.topstep.wearkit.apis.model.WKContactsCommon
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityContactsBinding
import com.topstep.wearkit.sample.model.ContactsBean
import com.topstep.wearkit.sample.ui.adapter.ContactsAdapter
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.utils.permission.PermissionHelper
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import timber.log.Timber

class ContactActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityContactsBinding
    private lateinit var adapter: ContactsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityContactsBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.title = "Contacts"
        initView()
        initData()
        initEvent()
    }

    @SuppressLint("CheckResult", "NotifyDataSetChanged")
    private fun initView() {
        adapter = ContactsAdapter()
        var mList = ArrayList<ContactsBean>()
        // request
        wearKit.contactsAbility.requestContactsCommon()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                val items = it.items
                if (!items.isNullOrEmpty()) {
                    items.forEach { item ->
                        mList.add(ContactsBean(item.name, item.number))
                    }
                }
                adapter.sources = mList
                adapter.notifyDataSetChanged()
            }, {

            })
    }

    private fun initData() {
        viewBind.contactRy.layoutManager = LinearLayoutManager(this)
        viewBind.contactRy.adapter = adapter
    }


    @SuppressLint("CheckResult", "NotifyDataSetChanged")
    private fun initEvent() {
        // add
        viewBind.contactAdd.clickTrigger {
            val contactMaxNumber = wearKit.contactsAbility.compat.getContactsCommonMaxNumber()
            if ((adapter.sources?.size ?: 0) >= contactMaxNumber) {
                toast(getString(R.string.ds_contacts_add_limit))
            } else {
                PermissionHelper.requestContacts(this) { granted ->
                    if (granted) {
                        pickContact.launch(Intent(Intent.ACTION_PICK).apply {
                            type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
                        })
                    }
                }
            }
        }

        //delete
        viewBind.contactDelete.clickTrigger {
            if (adapter.mDeleteIndex.size == 0) {
                toast(getString(R.string.ds_delete_contact))
            } else {
                val sources = adapter.sources
                if (sources != null) {
                    for (i in sources.size - 1 downTo 0) {
                        //val fwContacts = sources[i]
                        if (adapter.mDeleteIndex.contains(i)) {
                            sources.removeAt(i)
                            adapter.notifyItemRemoved(i)
                            adapter.notifyItemRangeChanged(0, sources.size)
                        }
                    }
                    adapter.notifyDataSetChanged()
                    val common = ArrayList<WKContacts>()
                    sources.forEach {
                        val fwContacts = WKContacts(it.name, it.number)
                        common.add(fwContacts)
                    }
                    // setContact
                    setContact(common)
                }

            }
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
                    val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    var number = cursor.getString(numberIndex)
                    var name = cursor.getString(nameIndex)
                    cursor.close()
                    if (!name.isNullOrEmpty() && !number.isNullOrEmpty()) {
                        number = number.replace(" ".toRegex(), "")
                        try {
                            val sources = adapter.sources
                            val common = ArrayList<WKContacts>()
                            if (sources != null) {
                                if (sources.size > 0) {
                                    var exist = false
                                    for (item in sources) {
                                        if (item.number == number) {
                                            exist = true
                                            break
                                        }
                                    }
                                    if (!exist) {
                                        sources.add(ContactsBean(name, number))
                                    }
                                } else {
                                    sources.add(ContactsBean(name, number))
                                }
                                sources.forEach {
                                    val fwContacts = WKContacts(it.name, it.number)
                                    common.add(fwContacts)
                                }
                                setContact(common)
                            }

                        } catch (e: Exception) {
                            Timber.w(e)
                        }

                    }
                }
            }
        }

    @SuppressLint("CheckResult", "NotifyDataSetChanged")
    fun setContact(con: ArrayList<WKContacts>) {
        //setContact
        wearKit.contactsAbility.setContactsCommon(WKContactsCommon(con))
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                adapter.mDeleteIndex.clear()
                adapter.notifyDataSetChanged()
                toast(getString(R.string.tip_success))
            }, {
                toast(getString(R.string.tip_failed))
            })
    }

}