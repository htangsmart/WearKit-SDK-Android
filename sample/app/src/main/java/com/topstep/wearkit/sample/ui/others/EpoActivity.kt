package com.topstep.wearkit.sample.ui.others

import android.annotation.SuppressLint
import android.os.Bundle
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.apis.model.gps.WKLocation
import com.topstep.wearkit.apis.provider.WKLocationProvider
import com.topstep.wearkit.base.utils.Optional
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.databinding.ActivityEpoBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * EPO示例。
 *
 * 步骤：
 * 1. 注册一个 [WKLocationProvider]，供 SDK 在更新 EPO / 设备主动请求定位时拉取当前定位。
 * 2. 调用 [com.topstep.wearkit.apis.ability.file.WKLocationMapAbility.updateEpo] 触发更新，观察 0..100 进度。
 *
 * 简化说明：这里用「模拟定位」（固定经纬度）。实际 App 应返回设备当前真实定位。
 * 本示例使用最简单的 updateEpo(force)——从 SDK 内置服务器获取 EPO 文件。
 */
class EpoActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityEpoBinding

    private var updateDisposable: Disposable? = null
    private var timeDisposable: Disposable? = null

    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    //模拟定位 provider：实际 App 应返回真实定位
    private val locationProvider = object : WKLocationProvider {
        override fun requestLocation(): Single<Optional<WKLocation>> {
            return Single.fromCallable {
                //TODO 用固定坐标模拟，实际应请求设备真实定位
                Optional(WKLocation(lat = 22.525030, lng = 113.921000, timestampMillis = System.currentTimeMillis()))
            }.subscribeOn(Schedulers.io())
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityEpoBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.title = "EPO"

        //注册定位 provider（更新 EPO / 设备主动请求定位时，SDK 会回调它拉取定位）
        wearKit.locationMapAbility.setLocation(locationProvider)

        viewBind.tvSupport.text = "isSupportEpo: ${wearKit.locationMapAbility.compat.isSupportEpo()}"

        //force=true：跳过"是否需要更新"的检查，强制更新
        viewBind.btnUpdateForce.clickTrigger {
            startUpdate(force = true)
        }

        //force=false：由 SDK 判断是否需要更新（如有效期内会返回 ERROR_NOT_NECESSARY）
        viewBind.btnUpdateNormal.clickTrigger {
            startUpdate(force = false)
        }

        //调试：查询设备 EPO 有效期 + 本地上次更新时间
        viewBind.btnRequestTime.clickTrigger {
            timeDisposable?.dispose()
            timeDisposable = wearKit.locationMapAbility.requestEpoTime()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    viewBind.tvState.text = "validTime: ${formatTime(it.validTimeMillis)}" +
                            "\nupdateTime: ${formatTime(it.updateTimeMillis)}"
                }, {
                    Timber.w(it)
                    viewBind.tvState.text = "Failed:" + it.stackTraceToString()
                })
        }

        //调试：清除 EPO 缓存
        viewBind.btnClear.clickTrigger {
            wearKit.locationMapAbility.clearEpo()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    toast("Cleared")
                }, {
                    toast("Clear fail")
                    Timber.w(it)
                })

        }
    }

    private fun formatTime(time: Long): String {
        return if (time <= 0) "0" else timeFormat.format(Date(time))
    }

    @SuppressLint("SetTextI18n")
    private fun startUpdate(force: Boolean) {
        if (!wearKit.locationMapAbility.compat.isSupportEpo()) {
            toast("UnSupport EPO!")
            return
        }
        updateDisposable?.dispose()
        viewBind.tvState.text = "Updating(force=$force)..."
        updateDisposable = wearKit.locationMapAbility.updateEpo(force)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                viewBind.tvState.text = "Progress:$it"
            }, {
                Timber.w(it)
                viewBind.tvState.text = "Failed:" + it.stackTraceToString()
            }, {
                viewBind.tvState.text = "Success"
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        updateDisposable?.dispose()
        timeDisposable?.dispose()
        //注销 provider
        wearKit.locationMapAbility.setLocation(null as WKLocationProvider?)
    }

}
