package com.topstep.wearkit.sample.ui.custom.sanag

import android.annotation.SuppressLint
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topstep.wearkit.apis.ability.file.WKFileAbility
import com.topstep.wearkit.apis.model.file.WKFileTransferEvent
import com.topstep.wearkit.base.DebugFlags
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
@SuppressLint("RestrictedApi")
internal class SanagFileFeature(
    private val activity: SanagDemoActivity,
    private val viewBind: ActivitySanagDemoBinding,
) : SanagDemoFeature {

    private val wearKit = MyApplication.wearKit
    private var countDisposable: Disposable? = null
    private var pullDisposable: Disposable? = null
    private var clearDisposable: Disposable? = null

    override fun onCreate() {
        refreshPullTypeButton()
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

        viewBind.btnFileClear.setOnClickListener {
            if (!activity.requireDeviceConnected()) return@setOnClickListener
            val fileAbility = wearKit.fileAbility
            if (!fileAbility.compat.isSupport()) {
                activity.toast(R.string.tip_un_support)
                return@setOnClickListener
            }
            clearDisposable?.dispose()
            countDisposable?.dispose()
            clearDisposable = fileAbility.clearFile()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    activity.toast(R.string.tip_success)
                    countDisposable = fileAbility.requestFilesCount()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            viewBind.tvFileState.text = activity.getString(R.string.ds_file_count_result, it)
                        }, {
                            Timber.w(it)
                            viewBind.tvFileState.text = it.message ?: activity.getString(R.string.tip_failed)
                        })
                }, {
                    Timber.w(it)
                    viewBind.tvFileState.text = it.message ?: activity.getString(R.string.tip_failed)
                    activity.toast(R.string.tip_failed)
                })
        }

        viewBind.btnFilePullType.setOnClickListener {
            showPullTypeDialog()
        }
    }

    override fun onDestroy() {
        DebugFlags.debugPullType = DebugFlags.DEBUG_PULL_TYPE_DEFAULT
        countDisposable?.dispose()
        countDisposable = null
        pullDisposable?.dispose()
        pullDisposable = null
        clearDisposable?.dispose()
        clearDisposable = null
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
                        activity.toast("pull file:" + event.devicePath + "\n" + event.extraJson)
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

    private fun showPullTypeDialog() {
        val types = intArrayOf(
            DebugFlags.DEBUG_PULL_TYPE_DEFAULT,
            DebugFlags.DEBUG_PULL_TYPE_BLE,
            DebugFlags.DEBUG_PULL_TYPE_P2P,
            DebugFlags.DEBUG_PULL_TYPE_AP,
            DebugFlags.DEBUG_PULL_TYPE_STATION,
        )
        val items = arrayOf(
            activity.getString(R.string.ds_file_pull_type_default),
            activity.getString(R.string.ds_file_pull_type_ble),
            activity.getString(R.string.ds_file_pull_type_p2p),
            activity.getString(R.string.ds_file_pull_type_ap),
            activity.getString(R.string.ds_file_pull_type_station),
        )
        val checked = types.indexOf(DebugFlags.debugPullType).coerceAtLeast(0)
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.ds_file_pull_type)
            .setSingleChoiceItems(items, checked) { dialog, which ->
                DebugFlags.debugPullType = types[which]
                refreshPullTypeButton()
                dialog.dismiss()
            }
            .show()
    }

    private fun refreshPullTypeButton() {
        val label = when (DebugFlags.debugPullType) {
            DebugFlags.DEBUG_PULL_TYPE_BLE -> activity.getString(R.string.ds_file_pull_type_ble)
            DebugFlags.DEBUG_PULL_TYPE_P2P -> activity.getString(R.string.ds_file_pull_type_p2p)
            DebugFlags.DEBUG_PULL_TYPE_AP -> activity.getString(R.string.ds_file_pull_type_ap)
            DebugFlags.DEBUG_PULL_TYPE_STATION -> activity.getString(R.string.ds_file_pull_type_station)
            else -> activity.getString(R.string.ds_file_pull_type_default)
        }
        viewBind.btnFilePullType.text = activity.getString(R.string.ds_file_pull_type) + "：" + label
    }

    private fun ensureFileWifiReady(fileAbility: WKFileAbility, onReady: () -> Unit) {
        val requireWifi = when (DebugFlags.debugPullType) {
            DebugFlags.DEBUG_PULL_TYPE_BLE -> false
            DebugFlags.DEBUG_PULL_TYPE_P2P,
            DebugFlags.DEBUG_PULL_TYPE_AP,
            DebugFlags.DEBUG_PULL_TYPE_STATION -> true
            else -> fileAbility.compat.isRequireWifi()
        }
        if (!requireWifi) {
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
