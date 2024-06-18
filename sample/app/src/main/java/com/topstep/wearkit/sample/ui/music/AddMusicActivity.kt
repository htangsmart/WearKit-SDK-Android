package com.topstep.wearkit.sample.ui.music

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityAddMusicBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import timber.log.Timber

@SuppressLint("CheckResult", "SetTextI18n", "NotifyDataSetChanged")
class AddMusicActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityAddMusicBinding
    private val adapter = MusicAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityAddMusicBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.music_push)
        initView()
        initData()
    }

    private fun initView() {
        viewBind.addMusicRy.layoutManager = LinearLayoutManager(this)
        viewBind.addMusicRy.adapter = adapter

        viewBind.addMusicPush.clickTrigger {
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
                                val intent = Intent()
                                intent.putStringArrayListExtra("addMusic", push)
                                setResult(RESULT_OK, intent)
                                finish()
                            }
                        }
                    }
                }, {
                    Timber.i(it)
                })
        }
    }

    private fun initData() {
        scanPhoneMusic()
    }

    private fun scanPhoneMusic() {
        // scan phone music
        AudioUtils.getLocalAudioFiles(this)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe({
                if (it.isNotEmpty()) {
                    adapter.addMusic(it)
                } else {
                    viewBind.addMusicTv.text = getString(R.string.phone_not_music_files)
                }
            }, {
                Timber.i(it)
            })
    }
}