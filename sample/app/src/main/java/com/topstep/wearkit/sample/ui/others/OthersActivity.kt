package com.topstep.wearkit.sample.ui.others

import android.content.Intent
import android.os.Bundle
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityOthersBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import timber.log.Timber

class OthersActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityOthersBinding

    private var pushOfflineMapDisposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityOthersBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.title = "Others"

        viewBind.itemICE.clickTrigger {
            if (wearKit.b2b.hsdAbility.compat.isSupportIce()) {
                startActivity(Intent(this, HsdIceActivity::class.java))
            } else {
                toast(R.string.unit_km)
            }
        }

        viewBind.itemParentalMode.clickTrigger {
            if (wearKit.b2b.hsdAbility.compat.isSupportParentalMode()) {
                startActivity(Intent(this, HsdParentalModeActivity::class.java))
            } else {
                toast(R.string.tip_failed)
            }
        }

        viewBind.itemClassRoomMode.clickTrigger {
            if (wearKit.b2b.hsdAbility.compat.isSupportClassRoomMode()) {
                startActivity(Intent(this, HsdClassRoomModeActivity::class.java))
            } else {
                toast(R.string.tip_failed)
            }
        }

        viewBind.itemTaskReward.clickTrigger {
            if (wearKit.b2b.hsdAbility.compat.getTaskMaxNumber() > 0) {
                startActivity(Intent(this, HsdTaskActivity::class.java))
            } else {
                toast(R.string.tip_failed)
            }
        }

        viewBind.itemHabit.clickTrigger {
            if (wearKit.b2b.hsdAbility.compat.getHabitMaxNumber() > 0) {
                startActivity(Intent(this, HsdHabitActivity::class.java))
            } else {
                toast(R.string.tip_failed)
            }
        }

        viewBind.itemUsageInfo.clickTrigger {
            if (wearKit.b2b.hsdAbility.compat.isSupportUsageInfo()) {
                startActivity(Intent(this, HsdUsageInfoActivity::class.java))
            } else {
                toast(R.string.tip_failed)
            }
        }

        viewBind.itemGame.clickTrigger {
            if (wearKit.b2b.hsdAbility.compat.isSupportGame()) {
                startActivity(Intent(this, HsdGameActivity::class.java))
            } else {
                toast(R.string.tip_failed)
            }
        }

        viewBind.itemOfflineMap.clickTrigger {
            if (wearKit.locationMapAbility.compat.isSupportOfflineMap()) {
                if (pushOfflineMapDisposable?.isDisposed != false) {
                    pushOfflineMapDisposable = wearKit.locationMapAbility.pushOfflineMap(
                        22.5445741, 114.0545429, 10
                    ).observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            toast("push map progress:$it")
                        }, {
                            Timber.w(it)
                            toast(it.stackTraceToString())
                        })
                }
            } else {
                toast(R.string.tip_failed)
            }
        }
    }

}