package com.topstep.wearkit.sample.ui.basic

import android.annotation.SuppressLint
import android.os.Bundle
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.base.utils.LanguageUtil
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityLangBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import timber.log.Timber
import java.util.Locale

@SuppressLint("CheckResult")
class LanguageActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityLangBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityLangBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.ds_language_set)

        viewBind.btnFollowSystem.clickTrigger {
            setLanguage(LanguageUtil.getSystemLanguageType(this))
        }
        viewBind.btnSetChinese.clickTrigger {
            setLanguage(LanguageUtil.getLanguageType(Locale.SIMPLIFIED_CHINESE))

        }
        viewBind.btnSetEnglish.clickTrigger {
            setLanguage(LanguageUtil.getLanguageType(Locale.US))
        }

        wearKit.languageAbility.requestLanguage()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                viewBind.tvLang.text = "Current language code:$it"
            }, {
                Timber.w(it)
                toast(R.string.tip_failed)
            })
    }

    private fun setLanguage(code: Byte) {
        wearKit.languageAbility.setLanguage(code)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                toast(getString(R.string.tip_success))
            }, {
                toast(getString(R.string.tip_failed))
            })
    }

}