package com.topstep.wearkit.sample.ui.custom.sanag

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import com.topstep.wearkit.apis.model.config.WKDeviceInfo
import com.topstep.wearkit.apis.model.core.WKConnectorState
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivitySanagDemoBinding
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.asFlow
import timber.log.Timber
import java.io.File

/**
 * Show project / firmware version and local OTA.
 */
internal class SanagVersionOtaFeature(
    private val activity: SanagDemoActivity,
    private val viewBind: ActivitySanagDemoBinding,
) : SanagDemoFeature {

    private val wearKit = MyApplication.wearKit
    private var otaDisposable: Disposable? = null
    private var otaDialog: ProgressDialog? = null

    private val selectOtaFileLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        if (result.resultCode == Activity.RESULT_OK) {
            if (uri == null) {
                activity.toast(R.string.ds_ota_select_file_error)
            } else {
                startOta(uri)
            }
        }
    }

    override fun onCreate() {
        viewBind.btnOta.setOnClickListener {
            if (!activity.requireDeviceConnected()) return@setOnClickListener
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            try {
                selectOtaFileLauncher.launch(intent)
            } catch (e: Exception) {
                Timber.w(e)
                activity.toast(R.string.ds_ota_select_file_error)
            }
        }
    }

    override fun observe(scope: CoroutineScope) {
        scope.launch {
            wearKit.connector.observeConnectorState()
                .startWithItem(wearKit.connector.getConnectorState())
                .asFlow()
                .collect { state ->
                    if (state == WKConnectorState.CONNECTED) {
                        updateVersionText(wearKit.deviceAbility.getDeviceInfo())
                    }
                }
        }
        scope.launch {
            wearKit.deviceAbility.observeDeviceInfo(false)
                .asFlow()
                .catch { Timber.w(it) }
                .collect { updateVersionText(it) }
        }
    }

    override fun onDestroy() {
        otaDisposable?.dispose()
        otaDisposable = null
        dismissOtaDialog()
    }

    private fun updateVersionText(info: WKDeviceInfo) {
        viewBind.tvProjectNum.text = activity.getString(R.string.ds_project_num, info.model)
        viewBind.tvFirmwareVersion.text = activity.getString(R.string.ds_firmware_version, info.version)
    }

    private fun startOta(uri: Uri) {
        otaDisposable?.dispose()
        showOtaDialog()
        otaDisposable = copyUriToFile(uri)
            .flatMapObservable { wearKit.otaAbility.ota(it) }
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally { dismissOtaDialog() }
            .subscribe({
                otaDialog?.apply {
                    progress = it
                    setMessage(activity.getString(R.string.ds_ota_progress, it))
                }
                viewBind.tvOtaState.text = activity.getString(R.string.ds_ota_progress, it)
            }, {
                Timber.w(it)
                viewBind.tvOtaState.text = it.message ?: activity.getString(R.string.tip_failed)
                activity.toast(R.string.tip_failed)
            }, {
                viewBind.tvOtaState.text = activity.getString(R.string.tip_success)
                activity.toast(R.string.tip_success)
            })
    }

    private fun copyUriToFile(uri: Uri): Single<File> {
        return Single.fromCallable {
            val dest = File(activity.cacheDir, "sanag_ota.bin")
            activity.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: throw IllegalArgumentException("Cannot open uri: $uri")
            dest
        }.subscribeOn(Schedulers.io())
    }

    private fun showOtaDialog() {
        dismissOtaDialog()
        otaDialog = ProgressDialog(activity).apply {
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            setTitle(activity.getString(R.string.action_ota))
            setMessage(activity.getString(R.string.ds_dfu_preparing))
            max = 100
            progress = 0
            show()
        }
    }

    private fun dismissOtaDialog() {
        otaDialog?.let {
            if (it.isShowing) {
                it.dismiss()
            }
        }
        otaDialog = null
    }
}
