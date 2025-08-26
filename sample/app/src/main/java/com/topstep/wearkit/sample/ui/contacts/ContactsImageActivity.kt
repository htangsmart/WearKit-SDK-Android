package com.topstep.wearkit.sample.ui.contacts

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.apis.model.message.WKTelephonyType
import com.topstep.wearkit.base.download.UriCopyDownloader
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityContactsImageBinding
import com.topstep.wearkit.sample.databinding.ItemContactsImageBinding
import com.topstep.wearkit.sample.files.AppFiles
import com.topstep.wearkit.sample.sdk.observeSOS
import com.topstep.wearkit.sample.ui.base.CropParam
import com.topstep.wearkit.sample.ui.base.GetPhotoVideoActivity
import com.topstep.wearkit.sample.utils.launchRepeatOnStarted
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.asFlow
import java.io.File

@SuppressLint("CheckResult")
class ContactsImageActivity : GetPhotoVideoActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityContactsImageBinding

    private var selectImageUri: Uri? = null
    private val adapter = InnerAdapter()

    private fun doDelete(number: String) {
        wearKit.contactsAbility.deleteContactsImage(number).observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                doRefresh()
            }, {
                toast("Refresh error:${it.stackTraceToString()}")
            })
    }

    private fun doRefresh() {
        wearKit.contactsAbility.requestContactsHasImage().observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                adapter.setData(it)
            }, {
                toast("Refresh error:${it.stackTraceToString()}")
            })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityContactsImageBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.ds_contacts)

        adapter.listener = object : InnerAdapter.Listener {
            override fun onDelete(number: String) {
                doDelete(number)
            }
        }

        viewBind.recyclerView.adapter = adapter
        viewBind.recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        viewBind.btnSelect.clickTrigger {
            chooseAlbum(CROP_TRY, "image/*")
        }

        viewBind.btnSetting.clickTrigger {
            val number = viewBind.edit.text.trim().toString()
            if (number.isEmpty()) {
                toast("Please input a phone number")
                return@clickTrigger
            }
            if (selectImageUri == null) {
                toast("Please select a image")
                return@clickTrigger
            }
            val downloader = UriCopyDownloader(this, externalCacheDir, 30_0000)
            downloader.download(selectImageUri!!.toString(), null, true).filter {
                it.progress == 100
            }.singleOrError().flatMapObservable {
                wearKit.contactsAbility.setContactsImage(number, it.result as File)
            }.observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    toast("Set contacts progress:$it")
                }, {
                    toast("Set contacts error:${it.stackTraceToString()}")
                })
        }
        viewBind.btnMock.clickTrigger {
            val number = viewBind.edit.text.trim().toString()
            if (number.isEmpty()) {
                toast("Please input a phone number")
                return@clickTrigger
            }
            wearKit.notificationAbility.sendTelephonyNotification(WKTelephonyType.INCOMING, number, "Abcdefg")
                .onErrorComplete().subscribe()
        }
        viewBind.btnRefresh.clickTrigger {
            doRefresh()
        }

        lifecycle.launchRepeatOnStarted {
            launch {
                wearKit.observeSOS().asFlow().collect {
                    toast("SOS trigger")
                }
            }
        }

    }

    override fun getTakePhotoFile(): File? {
        return AppFiles.generateJpegFile(this)
    }

    override fun getCropPhotoFile(): File? {
        return AppFiles.generateJpegFile(this)
    }

    override fun getCropPhotoParam(): CropParam {
        val shape = wearKit.deviceAbility.getDeviceInfo().shape
        return CropParam(shape.width, shape.height, shape.width, shape.height)
    }

    override fun onGetPhoto(uri: Uri) {
        //background changed
        selectImageUri = uri
        Glide.with(this)
            .asBitmap()
            .load(uri)
            .into(object : CustomTarget<Bitmap?>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap?>?) {
                    viewBind.image.background = resource.toDrawable(this@ContactsImageActivity.resources)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    viewBind.image.background = null
                }
            })
    }

    override fun onGetVideo(uri: Uri) {

    }

    private class ItemViewHolder(val viewBind: ItemContactsImageBinding) : RecyclerView.ViewHolder(viewBind.root)

    private class InnerAdapter : RecyclerView.Adapter<ItemViewHolder>() {

        private val data = mutableListOf<String>()

        interface Listener {
            fun onDelete(number: String)
        }

        var listener: Listener? = null

        fun setData(d: List<String>) {
            data.clear()
            data.addAll(d)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            return ItemViewHolder(
                ItemContactsImageBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
        }

        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            val item = data[position]
            holder.viewBind.tvNumber.text = item

            holder.viewBind.imgDelete.clickTrigger {
                listener?.onDelete(item)
            }
        }

        override fun getItemCount(): Int {
            return data.size
        }

    }

}