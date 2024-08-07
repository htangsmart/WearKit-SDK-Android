package com.topstep.wearkit.sample.ui.music

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.apis.ability.file.addFile
import com.topstep.wearkit.apis.model.file.WKFileOp
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityMusicBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import timber.log.Timber
import java.io.File

@SuppressLint("CheckResult", "SetTextI18n", "NotifyDataSetChanged")
class MusicActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityMusicBinding
    private val watchMusicAdapter = WatchMusicAdapter()
    private var pushMusicDisposable: Disposable? = null

    private val addMusic = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val pushMusicList = result.data?.getStringArrayListExtra("addMusic")
            if (pushMusicList != null && pushMusicList.size > 0) {
                pushMusic(pushMusicList)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityMusicBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.music_push)
        initView()
        initData()
        viewBind.musicAdd.clickTrigger {
            addMusic.launch(Intent(this, AddMusicActivity::class.java))
        }

        wearKit.musicAbility.observeFileChange().observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (it.op == WKFileOp.DELETE) {
                    val musicName = it.name
                    val sources = watchMusicAdapter.sources
                    if (sources != null) {
                        for (i in sources.size - 1 downTo 0) {
                            if (sources[i].name == musicName) {
                                sources.removeAt(i)
                                watchMusicAdapter.notifyItemRemoved(i)
                                watchMusicAdapter.notifyItemRangeChanged(0, sources.size)
                            }
                        }
                        watchMusicAdapter.notifyDataSetChanged()
                    }
                } else if (it.op == WKFileOp.CLEAR) {
                    watchMusicAdapter.sources?.clear()
                    watchMusicAdapter.notifyDataSetChanged()
                }
            }, {
                Timber.w(it)
            })
    }

    private fun initView() {

        viewBind.watchMusicRy.layoutManager = LinearLayoutManager(this)
        viewBind.watchMusicRy.adapter = watchMusicAdapter

        watchMusicAdapter.listener = object : WatchMusicAdapter.Listener {
            override fun onItemDelete(name: String, pos: Int) {
                // delete music
                wearKit.musicAbility.deleteFile(name)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        val sources = watchMusicAdapter.sources
                        if (sources != null) {
                            for (i in sources.size - 1 downTo 0) {
                                if (pos == i) {
                                    sources.removeAt(i)
                                    watchMusicAdapter.notifyItemRemoved(i)
                                    watchMusicAdapter.notifyItemRangeChanged(0, sources.size)
                                }
                            }
                            watchMusicAdapter.notifyDataSetChanged()
                        }
                        toast(R.string.tip_success)
                    }, {
                        toast(R.string.tip_failed)
                        Timber.w(it)
                    })
            }
        }
    }

    private fun initData() {
        requestWatchMusic()
    }

    private fun requestWatchMusic() {
        // request watch music
        wearKit.musicAbility.requestFiles()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (it.isNotEmpty()) {
                    viewBind.watchMusicTv.visibility = View.VISIBLE
                    viewBind.notMusic.visibility = View.GONE
                    watchMusicAdapter.addMusic(it.toMutableList())
                } else {
                    viewBind.watchMusicTv.visibility = View.GONE
                    viewBind.notMusic.visibility = View.VISIBLE
                }
            }, {
                toast(R.string.tip_failed)
                Timber.w(it)
            })
    }

    //push music to watch
    private fun pushMusic(filePaths: MutableList<String>) {
        viewBind.musicLayout.visibility = View.VISIBLE
        var index = 0
        var failCount = 0
        pushMusicDisposable = Observable.fromIterable(filePaths).concatMapCompletable {
            index++
            wearKit.musicAbility.addFile(File(it))
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { progress ->
                    viewBind.musicProgress.progress = progress
                    viewBind.musicCount.text = "$index / ${filePaths.size}"
                    viewBind.tvProgress.text = "$progress %"
                }
                .doOnError { err ->
                    Timber.w(err)
                    failCount++
                }
                .onErrorComplete()
                .ignoreElements()
        }.doOnComplete {
            viewBind.musicLayout.visibility = View.GONE
            viewBind.musicProgress.progress = 0
            viewBind.tvProgress.text = ""
            viewBind.musicCount.text = ""
            toast(R.string.ds_push_success)
        }.subscribe({
            val successCount = filePaths.size - failCount
            toast("Success:$successCount  Fail:$failCount")
            if (successCount > 0) {
                requestWatchMusic()
            }
        }, {
            Timber.w(it)
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        pushMusicDisposable?.dispose()
    }

}