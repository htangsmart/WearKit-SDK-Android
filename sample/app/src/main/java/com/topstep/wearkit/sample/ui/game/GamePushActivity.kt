package com.topstep.wearkit.sample.ui.game

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topstep.wearkit.apis.model.file.WKGameUIResources
import com.topstep.wearkit.apis.model.file.WKGameUISpace
import com.topstep.wearkit.base.download.AnyDownloader
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityGamePushBinding
import com.topstep.wearkit.sample.databinding.ItemGamePushBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.utils.glideShowImage
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import timber.log.Timber
import java.io.File

@SuppressLint("CheckResult")
class GamePushActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityGamePushBinding

    private val adapter = InnerAdapter()

    private val selectFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        if (result.resultCode == Activity.RESULT_OK) {
            if (uri == null) {
                toast("Select file error!")
            } else {
                install(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityGamePushBinding.inflate(layoutInflater)
        setContentView(viewBind.root)

        viewBind.recyclerView.layoutManager = GridLayoutManager(this, 3)
        viewBind.recyclerView.adapter = adapter
        adapter.listener = object : InnerAdapter.Listener {
            override fun onItemClick(item: WKGameUIResources) {
                install(item.file.uri)
            }
        }

        wearKit.gameUIAbility.requestCloudGameUIResources()
            .flatMap {
                wearKit.gameUIAbility.requestSupportGames().map { supports ->
                    it.filter { supports.contains(it.gameType) }
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_game_push, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId != R.id.menu_local_file) {
            return super.onOptionsItemSelected(item)
        }
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.type = "*/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        try {
            selectFileLauncher.launch(intent)
        } catch (e: Exception) {
            Timber.w(e)
            toast("Select file error!")
        }
        return true
    }

    private fun install(uri: Uri) {
        val loading = ProgressDialog(this).apply {
            setMessage(getString(R.string.action_loading))
            setCancelable(false)
        }
        wearKit.gameUIAbility.requestSpaces()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { loading.show() }
            .subscribe({ spaces ->
                loading.dismiss()
                chooseGameSpace(spaces) { spaceIndex ->
                    push(uri, spaceIndex)
                }
            }, {
                Timber.w(it)
                loading.dismiss()
                toast(R.string.tip_failed)
            })
    }

    private fun chooseGameSpace(spaces: List<WKGameUISpace>, onChosen: (Int) -> Unit) {
        if (spaces.isEmpty()) {
            toast(R.string.tip_failed)
            return
        }
        val items = spaces.mapIndexed { index, space ->
            spaceLabel(index, space)
        }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.game_push_space_select)
            .setItems(items) { _, which ->
                onChosen(which)
            }
            .show()
    }

    private fun spaceLabel(index: Int, space: WKGameUISpace): String {
        val occupant = if (space.gameType == null) {
            getString(R.string.dial_space_empty)
        } else {
            "${space.gameType}#${space.skinNum ?: 0}"
        }
        val free = if (space.free < 0 || space.free == Long.MAX_VALUE) {
            getString(R.string.dial_space_unlimited)
        } else {
            "${space.free / 1024}KB"
        }
        val summary = getString(R.string.dial_space_item_summary, occupant, free)
        return "${getString(R.string.dial_space_item_title, index)} · $summary"
    }

    private fun push(uri: Uri, spaceIndex: Int) {
        val progressDialog = ProgressDialog(this)

        val downloader = AnyDownloader(this, externalCacheDir)
        downloader.download(uri, null, true)
            .lastOrError()
            .map { it.result as File }
            .flatMapObservable {
                wearKit.gameUIAbility.install(it, spaceIndex)
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

    private class InnerViewHolder(val viewBind: ItemGamePushBinding) : RecyclerView.ViewHolder(viewBind.root)

    private class InnerAdapter : RecyclerView.Adapter<InnerViewHolder>() {

        var sources: List<WKGameUIResources>? = null
        var listener: Listener? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InnerViewHolder {
            return InnerViewHolder(
                ItemGamePushBinding.inflate(LayoutInflater.from(parent.context))
            )
        }

        override fun onBindViewHolder(holder: InnerViewHolder, position: Int) {
            val item = sources?.getOrNull(position) ?: return
            glideShowImage(holder.viewBind.image, item.image, true)
            holder.viewBind.tvName.text = "${item.gameName} #${item.skinNum}"
            holder.itemView.clickTrigger {
                listener?.onItemClick(item)
            }
        }

        override fun getItemCount(): Int {
            return sources?.size ?: 0
        }

        interface Listener {
            fun onItemClick(item: WKGameUIResources)
        }
    }

}
