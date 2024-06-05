package com.topstep.wearkit.sample.ui.music

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.apis.model.file.toWKFileInfo
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityMusicBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import timber.log.Timber
import java.io.File

@SuppressLint("CheckResult", "SetTextI18n", "NotifyDataSetChanged")
class MusicActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityMusicBinding
    private val adapter = MusicAdapter()
    private val watchMusicAdapter = WatchMusicAdapter()
    private var pushMusicDisposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityMusicBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.music_push)
        initView()
        initData()
        viewBind.musicPush.clickTrigger {
            // request dir space
            wearKit.musicAbility.requestDirSpace()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (it.free < 0) {
                        toast(R.string.ds_dfu_error_insufficient_storage)
                    } else {
                        if (adapter.pushIndexes.size == 0) {
                            toast(getString(R.string.no_music_push_file))
                        } else {
                            val sources = adapter.sources
                            val push = ArrayList<String>()
                            if (sources != null) {
                                for (i in sources.size - 1 downTo 0) {
                                    if (adapter.pushIndexes.contains(i)) {
                                        push.add(sources[i].path!!)
                                    }
                                }
                                pushMusic(push)
                            }
                        }
                    }
                }, {
                    toast("")
                })
        }
    }

    private fun initView() {
        viewBind.musicRy.layoutManager = LinearLayoutManager(this)
        viewBind.musicRy.adapter = adapter

        viewBind.watchMusicRy.layoutManager = LinearLayoutManager(this)
        viewBind.watchMusicRy.adapter = watchMusicAdapter

        watchMusicAdapter.listener = object : WatchMusicAdapter.Listener {
            override fun onItemDelete(name: String, position: Int) {
                // delete music
                wearKit.musicAbility.deleteFile(name)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        val sources = watchMusicAdapter.sources
                        if (sources != null) {
                            for (i in sources.size - 1 downTo 0) {
                                if (position == i) {
                                    sources.removeAt(i)
                                    watchMusicAdapter.notifyItemRemoved(i)
                                    watchMusicAdapter.notifyItemRangeChanged(0, sources.size)
                                }
                            }
                            watchMusicAdapter.notifyDataSetChanged()
                        }
                        toast(R.string.tip_success)
                    }, {
                        Timber.i(it)
                    })
            }
        }
    }

    private fun initData() {
        scanPhoneMusic()
        requestWatchMusic()
    }

    private fun scanPhoneMusic() {
        // scan phone music
        Single.create {
            it.onSuccess(AudioUtils.getLocalAudioFiles(this))
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                adapter.addMusic(it)
            }, {
                Timber.w(it)
            })
    }

    private fun requestWatchMusic() {
        // request watch music
        wearKit.musicAbility.requestFiles()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (it.isNotEmpty()) {
                    viewBind.watchMusicTv.visibility = View.VISIBLE
                    watchMusicAdapter.addMusic(it.toMutableList())
                } else {
                    viewBind.watchMusicTv.visibility = View.GONE
                }
            }, {
                Timber.i(it)
            })
    }

    //push music to watch
    private fun pushMusic(filePaths: MutableList<String>) {
        var index = 0
        pushMusicDisposable = Observable.fromIterable(filePaths).concatMapCompletable {
            index++
            val srcFile = File(it)
            wearKit.musicAbility.addFile(srcFile, srcFile.toWKFileInfo())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { progress ->
                    viewBind.musicProgress.progress = progress
                    viewBind.musicCount.text = "$index / ${filePaths.size}"
                    viewBind.tvProgress.text = "$progress %"
                }
                .doOnError {

                }
                .doOnComplete {
                }
                .onErrorComplete()
                .ignoreElements()
        }.doOnSubscribe {

        }.doOnComplete {
            viewBind.musicProgress.progress = 0
            viewBind.tvProgress.text = ""
            viewBind.musicCount.text = ""
            adapter.pushIndexes.clear()
            adapter.notifyDataSetChanged()
            toast(R.string.ds_push_success)
            requestWatchMusic()
        }.doOnError {

        }.subscribe({

        }, {
            Timber.i(it)
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        pushMusicDisposable?.dispose()
    }

}