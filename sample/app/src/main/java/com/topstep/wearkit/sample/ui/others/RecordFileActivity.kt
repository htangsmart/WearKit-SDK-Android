package com.topstep.wearkit.sample.ui.others

import android.Manifest
import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.apis.model.file.WKFileTransferEvent
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityRecordFileBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import timber.log.Timber
import java.io.File
import java.util.Locale

/**
 * 录音文件拉取示例。
 */
class RecordFileActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityRecordFileBinding

    private var requestFilesDisposable: Disposable? = null
    private var pullFilesDisposable: Disposable? = null

    private var lastSavedFiles: List<String> = emptyList()
    private var mediaPlayer: MediaPlayer? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityRecordFileBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.title = "Record File"

        viewBind.tvSupport.text = buildString {
            append("isSupport: ${wearKit.fileAbility.compat.isSupport()}\n")
            append("isRequireWifi: ${wearKit.fileAbility.compat.isRequireWifi()}")
        }

        viewBind.btnRequestFiles.clickTrigger {
            requestRecordFiles()
        }

        viewBind.btnPullFiles.clickTrigger {
            pullRecordFiles()
        }

        viewBind.btnPlay.clickTrigger {
            playLastPulledFile()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun requestRecordFiles() {
        if (!wearKit.fileAbility.compat.isSupport()) {
            toast(R.string.tip_un_support)
            return
        }
        requestFilesDisposable?.dispose()
        viewBind.tvState.text = "Requesting files..."
        requestFilesDisposable = wearKit.fileAbility.requestFiles()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ files ->
                Timber.i("record files: %s", files.map { "${it.name}(${it.size})" })
                viewBind.tvState.text = if (files.isEmpty()) {
                    "No record files on device"
                } else {
                    files.joinToString(separator = "\n") { "${it.name}  ${it.size} B" }
                }
            }, {
                Timber.w(it, "requestFiles error")
                viewBind.tvState.text = "Failed: ${it.stackTraceToString()}"
            })
    }

    @SuppressLint("SetTextI18n")
    private fun pullRecordFiles() {
        if (!wearKit.fileAbility.compat.isSupport()) {
            toast(R.string.tip_un_support)
            return
        }
        if (wearKit.fileAbility.compat.isRequireWifi()) {
            requestWifiPermissionIfNeeded()
        }
        releaseMediaPlayer()
        pullFilesDisposable?.dispose()
        viewBind.tvState.text = "Pulling..."
        //拉取到应用外部私有目录下，方便查看落盘文件
        val saveDir = File(getExternalFilesDir(null), "record_files").apply { mkdirs() }
        pullFilesDisposable = wearKit.fileAbility.pullFiles(saveDir)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ event ->
                Timber.i("pull event: %s", event)
                if (event is WKFileTransferEvent.OnAllCompleted) {
                    lastSavedFiles = event.savePaths
                }
                viewBind.tvState.text = describe(event)
            }, {
                Timber.w(it, "pullFiles error")
                viewBind.tvState.text = "Failed: ${it.stackTraceToString()}"
            })
    }

    @SuppressLint("SetTextI18n")
    private fun describe(event: WKFileTransferEvent): String {
        return when (event) {
            is WKFileTransferEvent.OnFileProgress -> buildString {
                append("Pulling file ${event.index}/${event.count}  ${event.progress}%\n")
                append("speed: ${"%.1f".format(Locale.US, event.speed / 1024)} KB/s")
            }
            is WKFileTransferEvent.OnFileCompleted -> buildString {
                append("File ${event.index}/${event.count} saved:\n")
                append(event.savePath)
            }
            is WKFileTransferEvent.OnAllCompleted -> buildString {
                append("All done! ${event.devicePaths.size} file(s) saved:\n")
                event.savePaths.forEach { append("\n").append(it) }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun playLastPulledFile() {
        if (lastSavedFiles.isEmpty()) {
            viewBind.tvState.text = "No pulled file yet. Pull first."
            return
        }
        releaseMediaPlayer()
        val file = File(lastSavedFiles.first())
        if (!file.exists()) {
            viewBind.tvState.text = "File not found: ${file.absolutePath}"
            return
        }
        viewBind.tvState.text = "Preparing: ${file.name}..."
        mediaPlayer = runCatching {
            MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnPreparedListener { mp ->
                    mp.start()
                    viewBind.tvState.text = "Playing: ${file.name}"
                }
                setOnCompletionListener {
                    viewBind.tvState.text = "Playback finished: ${file.name}"
                    releaseMediaPlayer()
                }
                setOnErrorListener { _, what, extra ->
                    viewBind.tvState.text = "Play failed: what=$what extra=$extra (unsupported format?)"
                    releaseMediaPlayer()
                    true
                }
                prepareAsync()
            }
        }.getOrElse {
            viewBind.tvState.text = "Cannot open audio: ${it.message}"
            null
        }
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.let {
            runCatching { it.stop() }
            runCatching { it.release() }
        }
        mediaPlayer = null
    }

    private fun requestWifiPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES), 1001)
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseMediaPlayer()
        requestFilesDisposable?.dispose()
        pullFilesDisposable?.dispose()
    }
}
