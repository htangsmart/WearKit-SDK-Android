package com.topstep.wearkit.sample.ui.sport

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.apis.model.file.WKSportUIResources
import com.topstep.wearkit.base.download.AnyDownloader
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.databinding.ActivitySportPushBinding
import com.topstep.wearkit.sample.databinding.ItemSportPushBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.utils.glideShowImage
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import timber.log.Timber
import java.io.File

@SuppressLint("CheckResult")
class SportPushActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivitySportPushBinding

    private val adapter = InnerAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivitySportPushBinding.inflate(layoutInflater)
        setContentView(viewBind.root)

        viewBind.recyclerView.layoutManager = GridLayoutManager(this, 3)
        viewBind.recyclerView.adapter = adapter
        adapter.listener = object : InnerAdapter.Listener {
            override fun onItemClick(item: WKSportUIResources) {
                push(item)
            }
        }

        //Request cloud resources and display
        wearKit.sportUIAbility.requestCloudSportUIResources()
            .flatMap {
                wearKit.sportUIAbility.requestSupportSports().map { supports ->
                    it.filter { supports.contains(it.sportType) }
                }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                adapter.sources = it
                adapter.notifyDataSetChanged()
            }, {
                Timber.w(it)
            })
    }

    private fun push(item: WKSportUIResources) {
        val progressDialog = ProgressDialog(this)

        val downloader = AnyDownloader(this, externalCacheDir)
        downloader.download(item.file.uri, null, true)
            .lastOrError()
            .map { it.result as File }
            .flatMapObservable {
                wearKit.sportUIAbility.install(it)
            }
            .observeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
                progressDialog.setCancelable(false)
                progressDialog.setTitle("Installing...")
                progressDialog.show()
            }.subscribe({
                progressDialog.progress = it
            }, {
                Timber.w(it)
                progressDialog.dismiss()
            }, {
                progressDialog.dismiss()
            })
    }

    private class InnerViewHolder(val viewBind: ItemSportPushBinding) : RecyclerView.ViewHolder(viewBind.root)

    private class InnerAdapter : RecyclerView.Adapter<InnerViewHolder>() {

        var sources: List<WKSportUIResources>? = null
        var listener: Listener? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InnerViewHolder {
            return InnerViewHolder(
                ItemSportPushBinding.inflate(LayoutInflater.from(parent.context))
            )
        }

        override fun onBindViewHolder(holder: InnerViewHolder, position: Int) {
            val item = sources?.getOrNull(position) ?: return
            glideShowImage(holder.viewBind.image, item.image, true)
            holder.viewBind.tvName.text = item.sportName
            holder.itemView.clickTrigger {
                listener?.onItemClick(item)
            }
        }

        override fun getItemCount(): Int {
            return sources?.size ?: 0
        }

        interface Listener {
            fun onItemClick(item: WKSportUIResources)
        }
    }


}