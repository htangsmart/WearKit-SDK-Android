package com.topstep.wearkit.sample.ui.others

import android.annotation.SuppressLint
import android.os.Bundle
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.databinding.ActivityEpoBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * EPO示例。
 *
 * 定位由 [com.topstep.wearkit.sample.WearKitInit] 全局注册的
 * [com.topstep.wearkit.sample.location.SampleLocationProvider] 提供。
 * 本页调用 [com.topstep.wearkit.apis.ability.file.WKLocationMapAbility.updateEpo]，观察 0..100 进度。
 */
class EpoActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityEpoBinding

    private var updateDisposable: Disposable? = null
    private var timeDisposable: Disposable? = null

    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityEpoBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.title = "EPO"

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
    }

}
