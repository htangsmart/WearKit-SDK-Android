package com.topstep.wearkit.sample.ui.special

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import androidx.annotation.StringRes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
    /** 最近一次 request 结果；set 为全量替换，增删改需基于此合并 */
    private var latestMap: FixedHashMap<String, String> = FixedHashMap()

    @StringRes
    protected abstract fun titleRes(): Int

    protected abstract fun requestSupport(): Single<FixedHashMap<String, String>>

    protected abstract fun setQrCode(map: FixedHashMap<String, String>): Completable

    /** 是否允许新增自定义条目（如 Titan） */
    protected abstract fun allowAdd(): Boolean

    /** 是否允许删除已有条目（如 Titan） */
    protected abstract fun allowDelete(): Boolean

    protected open fun demoContent(key: String): String {
        return getString(R.string.qr_code_ability_demo_content, key)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityQrCodeAbilityBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(titleRes())
        viewBind.tvTips.setText(
            if (allowAdd() || allowDelete()) {
                R.string.qr_code_ability_tips_editable
            } else {
                R.string.qr_code_ability_tips
            }
        )
        viewBind.btnAdd.visibility = if (allowAdd()) View.VISIBLE else View.GONE
        viewBind.btnAdd.setOnClickListener { showAddDialog() }
        loadSupport()
    }

    private fun loadSupport() {
        viewBind.progressBar.visibility = View.VISIBLE
        viewBind.layoutMain.visibility = View.GONE
        viewBind.tvEmpty.visibility = View.GONE
        viewBind.tvEmpty.setOnClickListener(null)
        requestDispose?.dispose()
        requestDispose = requestSupport()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ map ->
                viewBind.progressBar.visibility = View.GONE
                latestMap = map
                if (map.isEmpty() && !allowAdd()) {
                    viewBind.tvEmpty.visibility = View.VISIBLE
                    viewBind.tvEmpty.text = getString(R.string.tip_current_no_data)
                } else {
                    viewBind.layoutMain.visibility = View.VISIBLE
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
            itemBind.tvTitle.text = key.ifEmpty { getString(R.string.tip_none) }
            itemBind.tvValue.text = value.ifBlank { getString(R.string.tip_none) }
            itemBind.root.setOnClickListener {
                applyFullMap { copy ->
                    if (copy.containsKey(key)) {
                        copy[key] = demoContent(key)
                    } else {
                        copy.putUnLimit(key, demoContent(key))
                    }
                }
            }
            if (allowDelete()) {
                itemBind.btnDelete.visibility = View.VISIBLE
                itemBind.btnDelete.setOnClickListener {
                    confirmDelete(key)
                }
            } else {
                itemBind.btnDelete.visibility = View.GONE
            }
            viewBind.layoutContent.addView(itemBind.root)
        }
    }

    private fun showAddDialog() {
        val padding = (16 * resources.displayMetrics.density).toInt()
        val input = EditText(this).apply {
            hint = getString(R.string.qr_code_ability_add_hint)
            setSingleLine()
        }
        val container = FrameLayout(this).apply {
            setPadding(padding, padding / 2, padding, 0)
            addView(input)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.qr_code_ability_add_title)
            .setView(container)
            .setPositiveButton(R.string.action_add) { _, _ ->
                val name = input.text?.toString()?.trim().orEmpty()
                when {
                    name.isEmpty() -> toast(R.string.qr_code_ability_name_empty)
                    latestMap.containsKey(name) -> toast(R.string.qr_code_ability_name_exists)
                    else -> applyFullMap { it.putUnLimit(name, demoContent(name)) }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun confirmDelete(key: String) {
        MaterialAlertDialogBuilder(this)
            .setMessage(getString(R.string.qr_code_ability_delete_confirm, key.ifEmpty { getString(R.string.tip_none) }))
            .setPositiveButton(R.string.action_delete) { _, _ ->
                applyFullMap { it.removeUnLimit(key) }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun applyFullMap(mutate: (FixedHashMap<String, String>) -> Unit) {
        val map = FixedHashMap<String, String>()
        for ((k, v) in latestMap) {
            map.putUnLimit(k, v)
        }
        mutate(map)
        submitMap(map)
    }

    private fun submitMap(map: FixedHashMap<String, String>) {
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
