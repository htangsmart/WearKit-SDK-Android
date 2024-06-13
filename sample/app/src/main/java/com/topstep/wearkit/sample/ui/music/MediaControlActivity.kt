package com.topstep.wearkit.sample.ui.music

import android.annotation.SuppressLint
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.topstep.wearkit.apis.model.message.WKMediaMessage
import com.topstep.wearkit.base.utils.media.AbsMediaController
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityMediaBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.ui.notification.MyNotificationListenerService
import com.topstep.wearkit.sample.utils.permission.PermissionHelper
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import timber.log.Timber


@SuppressLint("CheckResult")
class MediaControlActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private var myMediaController = MyApplication.myMediaController
    private lateinit var viewBind: ActivityMediaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityMediaBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.media_control)

        viewBind.switchMedia.setOnCheckedChangeListener { buttonView, _ ->
            if (buttonView.isPressed) {
                if (NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)) {
                    PermissionHelper.requestTelephony(this) {
                        startService(Intent(this, MyNotificationListenerService::class.java))
                    }
                } else {
                    try {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
                            startActivity(Intent("android.settings.NOTIFICATION_LISTENER_SETTINGS"))
                        } else {
                            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }
                        viewBind.switchMedia.isChecked = false
                    } catch (e: Exception) {
                        Timber.w(e)
                    }
                }
            }
        }
        initView()
        initData()
    }

    private fun initView() {
        myMediaController.setOnMusicChangeListener(object : AbsMediaController.OnMusicChangeListener {
            override fun onMusicInfoChange(title: String, artist: String, duration: Long) {
                wearKit.mediaAbility.setMusicInfo(
                    title, artist, duration
                ).onErrorComplete().subscribe()
            }

            override fun onMusicStateChange(state: Int, position: Long, speed: Float) {
                wearKit.mediaAbility.setMusicState(
                    state, position, speed
                ).onErrorComplete().subscribe()
            }
        })
    }

    private fun initData() {
        wearKit.mediaAbility.observeMediaMessage()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                when (it) {
                    is WKMediaMessage.KeyCode -> myMediaController.applyKeyEvent(it.keyCode)
                    is WKMediaMessage.SetVolume -> myMediaController.setVolume(AudioManager.STREAM_MUSIC, it.volume)
                    is WKMediaMessage.SetSilentMode -> myMediaController.setSilentMode(it.enabled)
                    is WKMediaMessage.Others -> {
                        if (it.type == WKMediaMessage.OtherType.REQUEST_MUSIC_INFO) {
                            myMediaController.requestMusicInfo()
                        } else if (it.type == WKMediaMessage.OtherType.REQUEST_MUSIC_STATE) {
                            myMediaController.requestMusicState()
                        }
                    }
                }
            }, {
                Timber.i(it)
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this, MyNotificationListenerService::class.java))
    }

}