package com.topstep.wearkit.sample.ui.custom.sanag

import com.topstep.wearkit.apis.ability.file.WKFileAbility
import com.topstep.wearkit.apis.model.file.WKFileTransferEvent
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivitySanagDemoBinding
import com.topstep.wearkit.sample.utils.permission.PermissionHelper
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import timber.log.Timber

/**
 * Request file count and pull files from device.
 */
internal class SanagFileFeature(
    private val activity: SanagDemoActivity,
    private val viewBind: ActivitySanagDemoBinding,
) : SanagDemoFeature {

    private val wearKit = MyApplication.wearKit
    private var countDisposable: Disposable? = null
    private var pullDisposable: Disposable? = null

    override fun onCreate() {
        viewBind.btnFileCount.setOnClickListener {
            if (!activity.requireDeviceConnected()) return@setOnClickListener
            val fileAbility = wearKit.fileAbility
            if (!fileAbility.compat.isSupport()) {
                activity.toast(R.string.tip_un_support)
                return@setOnClickListener
            }
            countDisposable?.dispose()
            countDisposable = fileAbility.requestFilesCount()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    viewBind.tvFileState.text = activity.getString(R.string.ds_file_count_result, it)
                }, {
                    Timber.w(it)
                    viewBind.tvFileState.text = it.message ?: activity.getString(R.string.tip_failed)
                    activity.toast(R.string.tip_failed)
                })
        }

        viewBind.btnFilePull.setOnClickListener {
            if (!activity.requireDeviceConnected()) return@setOnClickListener
            val fileAbility = wearKit.fileAbility
            if (!fileAbility.compat.isSupport()) {
                activity.toast(R.string.tip_un_support)
                return@setOnClickListener
            }
            ensureFileWifiReady(fileAbility) {
                startPull(fileAbility)
            }
        }
    }

    override fun onDestroy() {
        countDisposable?.dispose()
        countDisposable = null
        pullDisposable?.dispose()
        pullDisposable = null
    }

    private fun startPull(fileAbility: WKFileAbility) {
        pullDisposable?.dispose()
        viewBind.btnFilePull.isEnabled = false
        pullDisposable = fileAbility.pullFiles(null)
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally { viewBind.btnFilePull.isEnabled = true }
            .subscribe({ event ->
                Timber.i("pullFiles event:%s", event)
                when (event) {
                    is WKFileTransferEvent.OnFileProgress -> {
                        viewBind.tvFileState.text = activity.getString(
                            R.string.ds_file_pull_progress,
                            event.index + 1,
                            event.count,
                            event.progress,
                        )
                    }
                    is WKFileTransferEvent.OnFileCompleted -> {
                        Timber.i("pull file completed: %s -> %s", event.devicePath, event.savePath)
                    }
                    is WKFileTransferEvent.OnAllCompleted -> {
                        viewBind.tvFileState.text = activity.getString(
                            R.string.ds_file_pull_done,
                            event.savePaths.size,
                        )
                        activity.toast(R.string.tip_success)
                    }
                }
            }, {
                Timber.w(it, "pullFiles error")
                viewBind.tvFileState.text = it.message ?: activity.getString(R.string.tip_failed)
                activity.toast(R.string.tip_failed)
            })
    }

    private fun ensureFileWifiReady(fileAbility: WKFileAbility, onReady: () -> Unit) {
        if (!fileAbility.compat.isRequireWifi()) {
            onReady()
            return
        }
        PermissionHelper.requestFileWifi(activity) { granted ->
            if (granted) {
                onReady()
            } else {
                activity.toast(R.string.ds_file_wifi_denied)
            }
        }
    }
}
