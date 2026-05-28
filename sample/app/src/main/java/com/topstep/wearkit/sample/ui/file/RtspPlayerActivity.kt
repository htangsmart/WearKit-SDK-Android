package com.topstep.wearkit.sample.ui.file

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.view.View
import com.permissionx.guolindev.PermissionX
import com.topstep.wearkit.abmate.apis.AbMateSDK
import com.topstep.wearkit.apis.ability.file.WKFileAbility
import com.topstep.wearkit.apis.exception.WKUnsupportedException
import com.topstep.wearkit.apis.model.core.WKConnectorState
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.databinding.ActivityRtspPlayerBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import timber.log.Timber

class RtspPlayerActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityRtspPlayerBinding

    private var fileAbility: WKFileAbility? = null
    private var libVLC: LibVLC? = null
    private var mediaPlayer: MediaPlayer? = null
    private var rtspDisposable: Disposable? = null
    private var currentUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityRtspPlayerBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.title = "RTSP Stream"

        if (wearKit.connector.getConnectorState() != WKConnectorState.CONNECTED) {
            toast("Device not connected!")
            finish()
            return
        }

        // rtsp() 目前仅 AbMate 设备支持
        val rawSdk = wearKit.getRawSDK() as? AbMateSDK
        if (rawSdk == null) {
            toast("Only AbMate device supports RTSP")
            finish()
            return
        }
        if (!rawSdk.fileAbility.compat.isSupport()) {
            toast("UnSupport!")
            finish()
            return
        }
        fileAbility = rawSdk.fileAbility

        setupPlayer()
    }

    private fun setupPlayer() {
        // libVLC 启动参数：强制 RTP-over-TCP，降低网络缓冲提升实时性
        val options = arrayListOf(
            "--rtsp-tcp",
            "--network-caching=200",
            "--no-drop-late-frames",
            "--no-skip-frames",
            "-vv",
        )
        val vlc = LibVLC(this, options)
        val mp = MediaPlayer(vlc)
        mp.attachViews(viewBind.videoLayout, null, false, false)
        mp.setEventListener { event ->
            when (event.type) {
                MediaPlayer.Event.Buffering -> {
                    viewBind.progress.visibility = if (event.buffering < 100f) View.VISIBLE else View.GONE
                }
                MediaPlayer.Event.Playing -> {
                    viewBind.progress.visibility = View.GONE
                }
                MediaPlayer.Event.EncounteredError -> {
                    Timber.w("VLC EncounteredError")
                    viewBind.tvStatus.text = "Player error (libVLC)"
                }
                MediaPlayer.Event.EndReached -> {
                    Timber.w("VLC EndReached")
                }
            }
        }
        libVLC = vlc
        mediaPlayer = mp
    }

    override fun onStart() {
        super.onStart()
        requestWifiPermissions { granted ->
            if (!granted) {
                viewBind.tvStatus.text = "Wifi permissions denied"
                return@requestWifiPermissions
            }
            if (!isWifiEnabled()) {
                viewBind.tvStatus.text = "Please enable Wifi"
                return@requestWifiPermissions
            }
            subscribeRtsp()
        }
    }

    override fun onStop() {
        super.onStop()
        // dispose 才会断开设备 wifi 直连
        rtspDisposable?.dispose()
        rtspDisposable = null
        currentUrl = null
        mediaPlayer?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.let {
            it.detachViews()
            it.release()
        }
        mediaPlayer = null
        libVLC?.release()
        libVLC = null
    }

    @SuppressLint("SetTextI18n")
    private fun subscribeRtsp() {
        if (rtspDisposable?.isDisposed == false) return
        val ability = fileAbility ?: return
        viewBind.tvStatus.text = "Connecting to device wifi..."
        viewBind.progress.visibility = View.VISIBLE

        rtspDisposable = ability.rtsp()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ url ->
                Timber.i("RTSP url: %s", url)
                if (url == currentUrl) return@subscribe
                currentUrl = url
                viewBind.tvStatus.text = url
                playUrl(url)
            }, { err ->
                Timber.w(err, "rtsp() failed")
                viewBind.progress.visibility = View.GONE
                viewBind.tvStatus.text = when (err) {
                    is WKUnsupportedException -> "Unsupported"
                    is SecurityException -> "Permission denied: ${err.message}"
                    else -> "Failed: ${err.message}"
                }
            })
    }

    private fun playUrl(url: String) {
        val vlc = libVLC ?: return
        val mp = mediaPlayer ?: return
        val media = Media(vlc, Uri.parse(url))
        media.setHWDecoderEnabled(true, false)
        media.addOption(":network-caching=200")
        media.addOption(":clock-jitter=0")
        media.addOption(":clock-synchro=0")
        mp.media = media
        media.release()
        mp.play()
    }

    private fun isWifiEnabled(): Boolean {
        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return false
        return wm.isWifiEnabled
    }

    private fun requestWifiPermissions(grantResult: (Boolean) -> Unit) {
        val perms = mutableListOf(
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms += Manifest.permission.NEARBY_WIFI_DEVICES
        } else {
            perms += Manifest.permission.ACCESS_FINE_LOCATION
        }
        PermissionX.init(this)
            .permissions(perms)
            .request { allGranted, _, _ -> grantResult(allGranted) }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, RtspPlayerActivity::class.java))
        }
    }
}
