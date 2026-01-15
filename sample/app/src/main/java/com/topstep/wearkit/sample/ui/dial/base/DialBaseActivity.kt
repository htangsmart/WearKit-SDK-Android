package com.topstep.wearkit.sample.ui.dial.base

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.flywear.sdk.apis.FwSDK
import com.topstep.wearkit.apis.model.dial.WKDialInfo
import com.topstep.wearkit.apis.model.dial.WKDialType
import com.topstep.wearkit.base.download.UriCopyDownloader
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityDialBaseBinding
import com.topstep.wearkit.sample.databinding.ItemDialItemBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import timber.log.Timber
import java.io.File

class DialBaseActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityDialBaseBinding

    private val innerAdapter = InnerAdapter()

    private val selectFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            if (data == null) {
                toast("Select file error!")
                return@registerForActivityResult
            }

            val uris = mutableListOf<Uri>()
            // 处理多选文件
            if (data.clipData != null) {
                // 多个文件
                val clipData = data.clipData!!
                for (i in 0 until clipData.itemCount) {
                    clipData.getItemAt(i)?.uri?.let { uris.add(it) }
                }
            } else if (data.data != null) {
                // 单个文件
                uris.add(data.data!!)
            }

            if (uris.isEmpty()) {
                toast("Select file error!")
            } else {
                startInstall(uris)
            }
        }
    }

    private var refreshDisposable: Disposable? = null
    private var deleteDisposable: Disposable? = null
    private var selectDisposable: Disposable? = null
    private var installDisposable: Disposable? = null

    private fun refreshDials() {
        refreshDisposable?.dispose()
        refreshDisposable = wearKit.dialAbility.requestDials()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ list ->
                innerAdapter.setData(list)
            }, {
                Timber.w(it)
            })
    }

    private fun deleteDial(dialId: String) {
        deleteDisposable?.dispose()
        deleteDisposable = wearKit.dialAbility.uninstall(dialId)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                toast("Delete success")
                //refresh after delete
                refreshDials()
            }, {
                toast("Delete fail")
            })
    }

    private fun selectDial(dialId: String) {
        selectDisposable?.dispose()
        selectDisposable = wearKit.dialAbility.select(dialId)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                toast("Select success")
                //refresh after select
                refreshDials()
            }, {
                toast("Select fail")
            })
    }

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityDialBaseBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.dial_base)

        viewBind.recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        viewBind.recyclerView.adapter = innerAdapter
        innerAdapter.listener = object : InnerListener {

            override fun onDialDelete(dialId: String) {
                deleteDial(dialId)
            }

            override fun onDialSelect(dialId: String) {
                selectDial(dialId)
            }

        }

        viewBind.btnGetAll.clickTrigger {
            refreshDials()
        }

        viewBind.btnDeleteLast.clickTrigger {
            wearKit.dialAbility.requestDials().flatMapCompletable { list ->
                //delete the last none-built-in dial
                val dial = list.findLast { it.dialType != WKDialType.BUILT_IN }
                if (dial == null) {
                    Completable.complete()
                } else {
                    wearKit.dialAbility.uninstall(dial.dialId)
                }
            }.subscribe({
                Timber.i("delete finish")
            }, {
                Timber.w(it)
            })
        }

        viewBind.btnInstall.clickTrigger {
            //Select dial file(s)
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.type = "*/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true) // 支持多选
            try {
                selectFileLauncher.launch(intent)
            } catch (e: Exception) {
                Timber.w(e)
                toast("Select file error!")
            }
        }
    }

    private fun copyTempFile(uri: Uri): File {
        val downloader = UriCopyDownloader(this, externalCacheDir, 30_0000)
        return downloader.download(uri.toString(), null, true).filter {
            it.progress == 100
        }.singleOrError().map { it.result as File }.blockingGet()
    }

    private fun startInstall(uris: List<Uri>) {
        if (wearKit.getRawSDK() is FwSDK) {
            toast("FwSDK dialAbility.install require dialId")
            return
        }
        installDisposable?.dispose()

        var index = 1
        installDisposable = Observable.fromIterable(uris).concatMap {
            val file = copyTempFile(it)
            wearKit.dialAbility.install("", file).doOnComplete {
                index++
                file.delete()
            }
        }.subscribe({
            toast("Install dial $index progress:$it")
        }, {
            Timber.w(it)
            toast("Install dial $index error")
        }, {
            toast("Install success all")
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        refreshDisposable?.dispose()
        deleteDisposable?.dispose()
        selectDisposable?.dispose()
        installDisposable?.dispose()
    }

    private class InnerViewHolder(val viewBind: ItemDialItemBinding) : RecyclerView.ViewHolder(viewBind.root)

    private interface InnerListener {
        fun onDialDelete(dialId: String)
        fun onDialSelect(dialId: String)
    }

    private class InnerAdapter : RecyclerView.Adapter<InnerViewHolder>() {

        private val data = ArrayList<WKDialInfo>()
        var listener: InnerListener? = null

        fun setData(d: List<WKDialInfo>) {
            data.clear()
            data.addAll(d)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InnerViewHolder {
            return InnerViewHolder(
                ItemDialItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }

        override fun onBindViewHolder(holder: InnerViewHolder, position: Int) {
            val item = data[position]
            holder.viewBind.tvDialId.text = "Dial Id:" + item.dialId
            val text = when (item.dialType) {
                WKDialType.BUILT_IN -> "Builtin Dial"
                WKDialType.NORMAL -> "Cloud Dial"
                WKDialType.CUSTOM -> "Custom Dial"
            }
            holder.viewBind.tvDialType.text = text
            if (item.isSelected) {
                holder.viewBind.imgSelect.setImageResource(R.drawable.ic_contacts_select_circle_enabled)
            } else {
                holder.viewBind.imgSelect.setImageResource(R.drawable.ic_contacts_select_circle_disabled)
            }
            holder.itemView.setOnClickListener {
                listener?.onDialSelect(item.dialId)
            }
            holder.itemView.setOnLongClickListener {
                listener?.onDialDelete(item.dialId)
                true
            }
        }

        override fun getItemCount(): Int {
            return data.size
        }

    }

}