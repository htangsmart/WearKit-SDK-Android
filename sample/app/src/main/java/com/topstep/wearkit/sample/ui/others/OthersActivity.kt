package com.topstep.wearkit.sample.ui.others

import android.content.Intent
import android.os.Bundle
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topstep.wearkit.abmate.apis.AbMateSDK
import com.topstep.wearkit.apis.ability.file.WKFileAbility
import com.topstep.wearkit.apis.model.core.WKConnectorState
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityOthersBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.ui.file.RtspPlayerActivity
import com.topstep.wearkit.sample.utils.permission.PermissionHelper
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.Disposable
import timber.log.Timber

class OthersActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityOthersBinding

    private var pushOfflineMapDisposable: Disposable? = null
    private var listOfflineMapDisposable: Disposable? = null
    private var deleteOfflineMapDisposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityOthersBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.title = "Others"

        viewBind.itemICE.clickTrigger {
            if (wearKit.b2b.hsdAbility.compat.isSupportIce()) {
                startActivity(Intent(this, HsdIceActivity::class.java))
            } else {
                toast(R.string.tip_un_support)
            }
        }

        viewBind.itemParentalMode.clickTrigger {
            if (wearKit.b2b.hsdAbility.compat.isSupportParentalMode()) {
                startActivity(Intent(this, HsdParentalModeActivity::class.java))
            } else {
                toast(R.string.tip_un_support)
            }
        }

        viewBind.itemClassRoomMode.clickTrigger {
            if (wearKit.b2b.hsdAbility.compat.isSupportClassRoomMode()) {
                startActivity(Intent(this, HsdClassRoomModeActivity::class.java))
            } else {
                toast(R.string.tip_un_support)
            }
        }

        viewBind.itemTaskReward.clickTrigger {
            if (wearKit.b2b.hsdAbility.compat.getTaskMaxNumber() > 0) {
                startActivity(Intent(this, HsdTaskActivity::class.java))
            } else {
                toast(R.string.tip_un_support)
            }
        }

        viewBind.itemHabit.clickTrigger {
            if (wearKit.b2b.hsdAbility.compat.getHabitMaxNumber() > 0) {
                startActivity(Intent(this, HsdHabitActivity::class.java))
            } else {
                toast(R.string.tip_un_support)
            }
        }

        viewBind.itemUsageInfo.clickTrigger {
            if (wearKit.b2b.hsdAbility.compat.isSupportUsageInfo()) {
                startActivity(Intent(this, HsdUsageInfoActivity::class.java))
            } else {
                toast(R.string.tip_un_support)
            }
        }

        viewBind.itemGame.clickTrigger {
            if (wearKit.b2b.hsdAbility.compat.isSupportGame()) {
                startActivity(Intent(this, HsdGameActivity::class.java))
            } else {
                toast(R.string.tip_un_support)
            }
        }

        viewBind.itemTitanQrcode.clickTrigger {
            if (wearKit.b2b.titanAbility.compat.isSupportQrCode()) {
                startActivity(Intent(this, TitanQrCodeActivity::class.java))
            } else {
                toast(R.string.tip_un_support)
            }
        }

        viewBind.itemPushOfflineMap.clickTrigger {
            if (wearKit.locationMapAbility.compat.isSupportOfflineMap()) {
                if (pushOfflineMapDisposable?.isDisposed != false) {
                    showRadiusChoiceDialog { radius ->
                        //Push a "TestMap" to device
                        pushOfflineMapDisposable = wearKit.locationMapAbility.setOfflineMap(
                            22.5445741, 114.0545429, radius, name = "TestMap"
                        ).observeOn(AndroidSchedulers.mainThread())
                            .subscribe({
                                toast("push map progress:$it")
                            }, {
                                Timber.w(it)
                                toast(it.stackTraceToString())
                            })
                    }
                }
            } else {
                toast(R.string.tip_un_support)
            }
        }

        viewBind.itemListOfflineMap.clickTrigger {
            if (wearKit.locationMapAbility.compat.isSupportOfflineMap()) {
                listOfflineMapDisposable?.dispose()
                listOfflineMapDisposable = wearKit.locationMapAbility.listOfflineMap().subscribe({
                    val s = StringBuilder()
                        .append("Maps on device:")
                    for (name in it) {
                        s.append(name)
                        s.append("   ")
                    }
                    toast(s.toString())
                }, {
                    Timber.w(it)
                    toast(it.stackTraceToString())
                })
            } else {
                toast(R.string.tip_un_support)
            }
        }

        viewBind.itemDeleteOfflineMap.clickTrigger {
            if (wearKit.locationMapAbility.compat.isSupportOfflineMap()) {
                deleteOfflineMapDisposable?.dispose()
                deleteOfflineMapDisposable = wearKit.locationMapAbility.listOfflineMap()
                    .observeOn(AndroidSchedulers.mainThread())
                    .flatMapCompletable { maps ->
                        if (maps.isEmpty()) {
                            toast("没有地图")
                            Completable.complete()
                        } else {
                            val name = maps.first()
                            val remaining = maps.size - 1
                            wearKit.locationMapAbility.deleteOfflineMap(name)
                                .andThen(
                                    Completable.fromAction {
                                        toast("删除了 $name 地图，还剩 $remaining 个")
                                    }
                                )
                        }
                    }
                    .subscribe({
                        // empty list case already toasted
                    }, {
                        Timber.w(it)
                        toast(it.stackTraceToString())
                    })
            } else {
                toast(R.string.tip_un_support)
            }
        }

        viewBind.itemEpo.clickTrigger {
            startActivity(Intent(this, EpoActivity::class.java))
        }

        abmateWifiTest()
    }

    private var getFileNamesDisposable: Disposable? = null
    private var pullFilesDisposable: Disposable? = null

    private fun abmateWifiTest() {
        viewBind.itemAbmateTakePhoto.clickTrigger {
            val rawSdk = wearKit.getRawSDK() as? AbMateSDK
            if (rawSdk == null) {
                toast("Only AbMate device supports this function!")
                return@clickTrigger
            }
            rawSdk.abMateManager.turnOnCamera(0, onSuccess = {}, onFail = {})
        }
        viewBind.itemAbmateGetFileNames.clickTrigger {
            val fileAbility = wearKit.fileAbility
            if (!fileAbility.compat.isSupport()) {
                toast(R.string.tip_un_support)
                return@clickTrigger
            }
            ensureFileWifiReady(fileAbility) {
                getFileNamesDisposable?.dispose()
                getFileNamesDisposable = fileAbility.requestFiles()
                    .subscribe({
                        it.forEach { f ->
                            Timber.i("requestFiles path:%s", f.path)
                        }
                    }, {
                        Timber.w(it, "requestFiles error")
                    })
            }
        }
        viewBind.itemAbmatePullFiles.clickTrigger {
            val fileAbility = wearKit.fileAbility
            if (!fileAbility.compat.isSupport()) {
                toast(R.string.tip_un_support)
                return@clickTrigger
            }
            ensureFileWifiReady(fileAbility) {
                pullFilesDisposable?.dispose()
                pullFilesDisposable = fileAbility.pullFiles(null)
                    .subscribe({
                        Timber.i("pullFiles event:%s", it)
                    }, {
                        Timber.w(it, "pullFiles error")
                    })
            }
        }
        viewBind.itemAbmateRtsp.clickTrigger {
            if (MyApplication.isFlavorLite()) {
                toast("Please use full build version!")
                return@clickTrigger
            }
            val fileAbility = wearKit.fileAbility
            if (!fileAbility.compat.isSupportRtsp()) {
                toast(R.string.tip_un_support)
                return@clickTrigger
            }
            if (wearKit.connector.getConnectorState() != WKConnectorState.CONNECTED) {
                toast("Device not connected!")
                return@clickTrigger
            }
            ensureFileWifiReady(fileAbility) {
                RtspPlayerActivity.start(this)
            }
        }
    }

    private fun ensureFileWifiReady(fileAbility: WKFileAbility, onReady: () -> Unit) {
        if (!fileAbility.compat.isRequireWifi()) {
            onReady()
            return
        }
        PermissionHelper.requestFileWifi(this) { granted ->
            if (granted) {
                onReady()
            } else {
                toast("Wifi permissions denied")
            }
        }
    }

    private fun showRadiusChoiceDialog(onSelected: (radius: Int) -> Unit) {
        val items = Array(10) { "${it + 1} km" }
        MaterialAlertDialogBuilder(this)
            .setTitle("Select radius(500k per 1km)")
            .setItems(items) { _, which ->
                onSelected(which + 1)
            }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        pushOfflineMapDisposable?.dispose()
        listOfflineMapDisposable?.dispose()
        deleteOfflineMapDisposable?.dispose()
        getFileNamesDisposable?.dispose()
        pullFilesDisposable?.dispose()
    }

}