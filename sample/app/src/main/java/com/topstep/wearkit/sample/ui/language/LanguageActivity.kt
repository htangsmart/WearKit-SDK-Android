package com.topstep.wearkit.sample.ui.language

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityLangBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers

class LanguageActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityLangBinding
    private var popupWindow: PopupWindow? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityLangBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.title = "language"
        initView()
        initData()
        initEvent()
    }

    private fun initView() {
        popupWindow = PopupWindow().apply {
            contentView = layoutInflater.inflate(R.layout.item_popwindow, null)
            width = ViewGroup.LayoutParams.MATCH_PARENT
            height = ViewGroup.LayoutParams.WRAP_CONTENT
            isFocusable = true
            //设置按钮
            val btnTv1 = contentView.findViewById<TextView>(R.id.tv1)
            val btnTv2 = contentView.findViewById<TextView>(R.id.tv2)
            val btnTv3 = contentView.findViewById<TextView>(R.id.tv3)
            btnTv1.setOnClickListener {
                setLanguage("system")
                dismiss()
            }

            btnTv2.setOnClickListener {
                setLanguage("zh")
                dismiss()
            }

            btnTv3.setOnClickListener {
                setLanguage("en")
                dismiss()
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun initData() {
        wearKit.languageAbility.requestLanguage().observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                viewBind.tvLang.text = "当前语音:${LangConfig.convertWmLanguage(it)}"
            }, {
                viewBind.tvLang.text = "当前语音获取失败"
            })
    }

    @SuppressLint("CheckResult")
    private fun initEvent() {

        viewBind.btnSetLang.clickTrigger {
            popupWindow!!.showAsDropDown(it)
        }
    }

    @SuppressLint("CheckResult")
    fun setLanguage(lang: String) {
        wearKit.languageAbility.setLanguage(LangConfig.getLanguageId(lang))
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                toast(getString(R.string.tip_success))
            }, {
                toast(getString(R.string.tip_failed))
            })
    }


    private fun toast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }
}