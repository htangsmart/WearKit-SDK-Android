package com.topstep.wearkit.sample.ui.dial.base

import android.annotation.SuppressLint
import android.os.Bundle
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.apis.model.dial.WKDialType
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityDialBaseBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import io.reactivex.rxjava3.core.Completable
import timber.log.Timber

class DialBaseActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityDialBaseBinding

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityDialBaseBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.dial_base)

        viewBind.btnGetAll.clickTrigger {
            wearKit.dialAbility.requestDials()
                .subscribe({ list ->
                    list.forEach {
                        Timber.i("dialInfo:$it")
                    }
                }, {
                    Timber.w(it)
                })
        }

        viewBind.btnDeleteLast.clickTrigger {
            wearKit.dialAbility.requestDials().flatMapCompletable { list ->
                //delete the last none-built-in dial
                val dial = list.findLast { it.dialType != WKDialType.BUILT_IN }
                if (dial == null) {
                    Completable.complete()
                } else {
                    wearKit.dialAbility.uninstall(dial.dialId)
                }
            }.subscribe({
                Timber.i("delete finish")
            }, {
                Timber.w(it)
            })
        }

        viewBind.btnInstall.clickTrigger {
//            wearKit.dialAbility.install(
//                dialId = "1111",
//                file = File(""),
//            ).subscribe({ progress ->
//                Timber.i("install:$progress")
//            }, {
//                Timber.w(it)
//            })
        }
    }

}