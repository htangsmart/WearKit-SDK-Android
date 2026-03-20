package com.topstep.wearkit.sample.ui.others

import android.annotation.SuppressLint
import android.os.Bundle
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityHsdIceBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import timber.log.Timber

class HsdIceActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityHsdIceBinding

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityHsdIceBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.title = "ICE"

        viewBind.btnSave.clickTrigger {
            saveIceLabels()
        }
    }

    private fun saveIceLabels() {
        val label1 = viewBind.editLabel1.text?.toString().orEmpty()
        val label2 = viewBind.editLabel2.text?.toString().orEmpty()
        val label3 = viewBind.editLabel3.text?.toString().orEmpty()

        val labels = listOf(label1, label2, label3).filter { it.isNotBlank() }

        wearKit.b2b.hsdAbility.setIceLabels(labels)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                toast(R.string.tip_success)
            }, {
                Timber.w(it)
                toast(R.string.tip_failed)
            })
    }

}
