package com.topstep.wearkit.sample.ui.dial.base

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.apis.model.dial.WKDialInfo
import com.topstep.wearkit.apis.model.dial.WKDialType
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityDialBaseBinding
import com.topstep.wearkit.sample.databinding.ItemDialItemBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.Disposable
import timber.log.Timber

class DialBaseActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityDialBaseBinding

    private val innerAdapter = InnerAdapter()

    private var refreshDisposable: Disposable? = null
    private var deleteDisposable: Disposable? = null
    private var selectDisposable: Disposable? = null

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
//            wearKit.dialAbility.install(
//                dialId = "1111",
//                file = File(""),
//            ).subscribe({ progress ->
//                Timber.i("install:$progress")
//            }, {
//                Timber.w(it)
//            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        refreshDisposable?.dispose()
        deleteDisposable?.dispose()
        selectDisposable?.dispose()
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