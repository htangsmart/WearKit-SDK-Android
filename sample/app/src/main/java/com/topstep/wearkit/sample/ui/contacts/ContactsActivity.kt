package com.topstep.wearkit.sample.ui.contacts

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.apis.model.WKContacts
import com.topstep.wearkit.apis.model.WKContactsCommon
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityContactsBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.utils.permission.PermissionHelper
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import timber.log.Timber

@SuppressLint("CheckResult")
class ContactsActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private val adapter: ContactsAdapter = ContactsAdapter()
    private lateinit var viewBind: ActivityContactsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityContactsBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.ds_contacts_common)

        viewBind.recyclerView.layoutManager = LinearLayoutManager(this)
        viewBind.recyclerView.adapter = adapter

        initEvent()

        // request
        wearKit.contactsAbility.requestContactsCommon()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                adapter.sources = it.items?.toMutableList()
                adapter.notifyDataSetChanged()
            }, {
                Timber.w(it)
                toast(R.string.tip_failed)
            })
    }

    private fun initEvent() {
        // add
        viewBind.contactAdd.clickTrigger {
            val contactMaxNumber = wearKit.contactsAbility.compat.getContactsCommonMaxNumber()
            if (adapter.itemCount >= contactMaxNumber) {
                toast(getString(R.string.ds_contacts_add_limit, contactMaxNumber))
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
            if (adapter.deleteIndexes.size == 0) {
                toast(R.string.ds_contacts_delete)
            } else {
                val sources = adapter.sources
                if (sources != null) {
                    for (i in sources.size - 1 downTo 0) {
                        //val fwContacts = sources[i]
                        if (adapter.deleteIndexes.contains(i)) {
                            sources.removeAt(i)
                            adapter.notifyItemRemoved(i)
                            adapter.notifyItemRangeChanged(0, sources.size)
                        }
                    }
                    adapter.notifyDataSetChanged()
                    setContact(adapter.sources)
                }
            }
        }
    }

    private val pickContact = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val contacts = onPickContacts(this, result)
        if (contacts != null && adapter.addContacts(contacts)) {
            setContact(adapter.sources)
        }
    }

    private fun setContact(con: List<WKContacts>?) {
        //setContact
        wearKit.contactsAbility.setContactsCommon(WKContactsCommon(con))
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                adapter.deleteIndexes.clear()
                adapter.notifyDataSetChanged()
                toast(getString(R.string.tip_success))
            }, {
                Timber.w(it)
                toast(getString(R.string.tip_failed))
            })
    }

    companion object {
        fun onPickContacts(context: Context, result: ActivityResult): WKContacts? {
            val uri = result.data?.data
            if (uri == null || result.resultCode != Activity.RESULT_OK) return null
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            )
            context.contentResolver.query(uri, projection, null, null, null)?.use {
                if (it.moveToFirst()) {
                    val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val number = it.getString(numberIndex)?.replace(" ", "")
                    val name = it.getString(nameIndex)
                    if (!name.isNullOrEmpty() && !number.isNullOrEmpty()) {
                        return WKContacts(name, number)
                    }
                }
            }
            return null
        }
    }
}