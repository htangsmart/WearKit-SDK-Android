package com.topstep.wearkit.sample.ui.special

import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import com.topstep.wearkit.base.utils.FixedHashMap
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityQrCodeAbilityBinding
import com.topstep.wearkit.sample.databinding.ItemQrCodeTypeBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import timber.log.Timber

abstract class QrCodeAbilityDemoActivity : BaseActivity() {

    protected val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityQrCodeAbilityBinding
    private var requestDispose: Disposable? = null
    private var setDispose: Disposable? = null

    @StringRes
    protected abstract fun titleRes(): Int

    protected abstract fun requestSupport(): Single<FixedHashMap<String, String>>

    protected abstract fun setQrCode(map: FixedHashMap<String, String>): Completable

    protected open fun demoContent(key: String): String {
        return getString(R.string.qr_code_ability_demo_content, key)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityQrCodeAbilityBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(titleRes())
        viewBind.tvTips.setText(R.string.qr_code_ability_tips)
        loadSupport()
    }

    private fun loadSupport() {
        viewBind.progressBar.visibility = View.VISIBLE
        viewBind.scrollView.visibility = View.GONE
        viewBind.tvEmpty.visibility = View.GONE
        viewBind.tvEmpty.setOnClickListener(null)
        requestDispose?.dispose()
        requestDispose = requestSupport()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ map ->
                viewBind.progressBar.visibility = View.GONE
                if (map.isEmpty()) {
                    viewBind.tvEmpty.visibility = View.VISIBLE
                    viewBind.tvEmpty.text = getString(R.string.tip_current_no_data)
                } else {
                    viewBind.scrollView.visibility = View.VISIBLE
                    bindItems(map)
                }
            }, {
                Timber.w(it)
                viewBind.progressBar.visibility = View.GONE
                viewBind.tvEmpty.visibility = View.VISIBLE
                viewBind.tvEmpty.text = getString(R.string.tip_load_error)
                viewBind.tvEmpty.setOnClickListener {
                    loadSupport()
                }
            })
    }

    private fun bindItems(map: FixedHashMap<String, String>) {
        viewBind.layoutContent.removeAllViews()
        for ((key, value) in map) {
            val itemBind = ItemQrCodeTypeBinding.inflate(layoutInflater, viewBind.layoutContent, false)
            itemBind.tvTitle.text = key
            itemBind.tvValue.text = value.ifBlank { getString(R.string.tip_none) }
            itemBind.root.setOnClickListener {
                setQrCodeItem(key)
            }
            viewBind.layoutContent.addView(itemBind.root)
        }
    }

    private fun setQrCodeItem(key: String) {
        val map = FixedHashMap<String, String>()
        map.putUnLimit(key, demoContent(key))
        setDispose?.dispose()
        setDispose = setQrCode(map)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                toast(R.string.tip_setting_success)
                loadSupport()
            }, {
                Timber.w(it)
                toast(it.message ?: getString(R.string.tip_failed))
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        requestDispose?.dispose()
        setDispose?.dispose()
    }
}
