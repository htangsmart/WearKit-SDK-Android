package com.topstep.wearkit.sample.ui.others

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import com.topstep.opus.tool.OpusDecoder
import com.topstep.wearkit.apis.model.speech.WKAudioFormat
import com.topstep.wearkit.apis.model.camera.WKLiveSession
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.databinding.ActivityLiveBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import timber.log.Timber

class LiveActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityLiveBinding

    private var session: WKLiveSession? = null
    private val disposables = CompositeDisposable()
    private var opusDecoder: OpusDecoder? = null
    private var audioTrack: AudioTrack? = null
    private var previewBitmap: Bitmap? = null
    private var frameCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityLiveBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.title = "Live Stream"
        viewBind.tvState.text = "Opening…"
    }

    override fun onStart() {
        super.onStart()
        val opened = wearKit.cameraAbility.openLive()
        if (opened == null) {
            toast("openLive failed")
            finish()
            return
        }
        session = opened
        viewBind.tvState.text = "Starting…"
        disposables.add(
            opened.video()
                .toFlowable(BackpressureStrategy.LATEST)
                .observeOn(Schedulers.computation(), false, 1)
                .map { frame ->
                    val bitmap = BitmapFactory.decodeByteArray(frame.jpeg, 0, frame.jpeg.size)
                    bitmap to frame.ptsMs
                }
                .observeOn(AndroidSchedulers.mainThread(), false, 1)
                .subscribe({ (bitmap, ptsMs) ->
                    if (session == null) {
                        bitmap?.recycle()
                        return@subscribe
                    }
                    frameCount++
                    showPreview(bitmap)
                    viewBind.tvState.text = "frames=$frameCount pts=$ptsMs"
                }, {
                    Timber.w(it, "live video error")
                    toast("Live error: ${it.message}")
                    finish()
                })
        )
        disposables.add(
            opened.audio()
                .observeOn(Schedulers.computation())
                .subscribe({ packet ->
                    playAudio(opened, packet.data)
                }, {
                    Timber.w(it, "live audio error")
                })
        )
    }

    private fun showPreview(bitmap: Bitmap?) {
        if (bitmap == null) return
        val old = previewBitmap
        previewBitmap = bitmap
        viewBind.ivPreview.setImageBitmap(bitmap)
        if (old != null && old !== bitmap && !old.isRecycled) {
            old.recycle()
        }
    }

    private fun playAudio(session: WKLiveSession, data: ByteArray) {
        val format = session.audioFormat ?: return
        val pcm = when (format) {
            is WKAudioFormat.OPUS -> {
                val decoder = opusDecoder ?: OpusDecoder().also { opusDecoder = it }
                val pcmList = decoder.decode(data) ?: return
                if (pcmList.isEmpty()) return
                if (pcmList.size == 1) pcmList[0] else pcmList.reduce { acc, bytes -> acc + bytes }
            }
            is WKAudioFormat.PCM -> data
        }
        val track = ensureTrack(format) ?: return
        track.write(pcm, 0, pcm.size)
    }

    private fun ensureTrack(format: WKAudioFormat): AudioTrack? {
        val existing = audioTrack
        if (existing != null) return existing
        val channelConfig = if (format.channels <= 1) {
            AudioFormat.CHANNEL_OUT_MONO
        } else {
            AudioFormat.CHANNEL_OUT_STEREO
        }
        val minBuffer = AudioTrack.getMinBufferSize(
            format.sampleRate,
            channelConfig,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuffer <= 0) return null
        val track = AudioTrack(
            AudioManager.STREAM_MUSIC,
            format.sampleRate,
            channelConfig,
            AudioFormat.ENCODING_PCM_16BIT,
            minBuffer,
            AudioTrack.MODE_STREAM,
        )
        track.play()
        audioTrack = track
        return track
    }

    override fun onStop() {
        disposables.clear()
        session?.release()
        session = null
        viewBind.ivPreview.setImageBitmap(null)
        previewBitmap?.takeUnless { it.isRecycled }?.recycle()
        previewBitmap = null
        audioTrack?.release()
        audioTrack = null
        opusDecoder = null
        super.onStop()
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, LiveActivity::class.java))
        }
    }
}
