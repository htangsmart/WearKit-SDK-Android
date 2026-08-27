package com.topstep.wearkit.sample.ui.dial.style

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topstep.wearkit.apis.ability.dial.DanMuCoord
import com.topstep.wearkit.apis.ability.dial.WKDialStyleAbility
import com.topstep.wearkit.apis.model.dial.WKDialQuality
import com.topstep.wearkit.apis.model.dial.WKDialStyleConstraint
import com.topstep.wearkit.apis.model.dial.WKDialStyleResources
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.MyDialStyleProvider
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityDialDanmuCustomBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.ui.dial.style.adapter.DialDanMuItemAdapter
import com.topstep.wearkit.sample.ui.dial.style.adapter.DialPositionSelectAdapter
import com.topstep.wearkit.sample.ui.dial.style.adapter.DialStyleSelectAdapter
import com.topstep.wearkit.sample.widget.ColorPickerView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

@SuppressLint("CheckResult")
class DialDanMuCustomActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityDialDanmuCustomBinding

    private var styleConstraint: WKDialStyleConstraint? = null
    private val styleAdapter = DialStyleSelectAdapter()
    private val positionAdapter = DialPositionSelectAdapter()
    private val danMuAdapter = DialDanMuItemAdapter()
    private val drafts = mutableListOf<DanMuDraft>()
    private val disposables = CompositeDisposable()
    private var styleColorTint = Color.WHITE
    private var isCreatingDial = false
    private var backgroundColor = Color.BLACK

    private val addDanMu = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        @Suppress("DEPRECATION")
        val draft = result.data?.getParcelableExtra<DanMuDraft>(DialDanMuAddActivity.EXTRA_DRAFT) ?: return@registerForActivityResult
        val editIndex = result.data?.getIntExtra(
            DialDanMuAddActivity.EXTRA_EDIT_INDEX,
            DialDanMuAddActivity.NO_EDIT_INDEX,
        ) ?: DialDanMuAddActivity.NO_EDIT_INDEX
        if (editIndex in drafts.indices) {
            drafts[editIndex] = draft
            danMuAdapter.notifyItemChanged(editIndex)
        } else {
            drafts.add(draft)
            danMuAdapter.notifyItemInserted(drafts.lastIndex)
        }
        refreshPreview()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityDialDanmuCustomBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.dial_custom_style_danmu)

        if (savedInstanceState != null) {
            @Suppress("DEPRECATION")
            savedInstanceState.getParcelableArrayList<DanMuDraft>(STATE_DRAFTS)?.let { saved ->
                drafts.addAll(saved)
            }
        }

        viewBind.btnBgColor.clickTrigger { selectBackgroundColor() }
        viewBind.btnSelectColor.clickTrigger { selectStyleColor() }
        viewBind.btnAddDanmu.clickTrigger { launchAddDanMu() }
        viewBind.btnCreateDial.clickTrigger {
            if (drafts.isEmpty()) {
                toast(R.string.dial_custom_style_danmu_empty)
                return@clickTrigger
            }
            chooseDialQuality(wearKit.dialStyleAbility.compat.getQualityLevels()) { quality ->
                createAndInstall(quality, drafts.toList())
            }
        }

        viewBind.danmuRecyclerView.layoutManager = LinearLayoutManager(this)
        viewBind.danmuRecyclerView.adapter = danMuAdapter
        danMuAdapter.items = drafts
        danMuAdapter.listener = object : DialDanMuItemAdapter.Listener {
            override fun onItemClick(position: Int) {
                launchAddDanMu(drafts.getOrNull(position), position)
            }

            override fun onDelete(position: Int) {
                drafts.removeAt(position)
                danMuAdapter.notifyItemRemoved(position)
                refreshPreview()
            }
        }

        viewBind.styleRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        viewBind.styleRecyclerView.adapter = styleAdapter
        styleAdapter.listener = object : DialStyleSelectAdapter.Listener {
            override fun onItemSelect(position: Int, item: WKDialStyleConstraint.Style) {
                updateCreateButton()
            }
        }

        viewBind.positionRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        viewBind.positionRecyclerView.adapter = positionAdapter

        disposables.add(
            getDialStyleResources().flatMap {
                wearKit.dialStyleAbility.requestConstraint(it)
            }.observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    this.styleConstraint = it
                    updateConstraintUI(it)
                    refreshPreview()
                }, {
                    Timber.w(it)
                    toast(R.string.tip_failed)
                })
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList(STATE_DRAFTS, ArrayList(drafts))
    }

    private fun launchAddDanMu(
        draft: DanMuDraft? = null,
        editIndex: Int = DialDanMuAddActivity.NO_EDIT_INDEX,
    ) {
        val shape = wearKit.deviceAbility.getDeviceInfo().shape
        addDanMu.launch(
            DialDanMuAddActivity.createIntent(
                context = this,
                displayWidth = shape.width,
                displayHeight = shape.height,
                defaultY = nextDefaultY(shape.height),
                draft = draft,
                editIndex = editIndex,
            )
        )
    }

    private fun nextDefaultY(displayHeight: Int): Int {
        val next = 36 + drafts.size * 80
        return next.coerceIn(0, (displayHeight - 40).coerceAtLeast(0))
    }

    private fun getDialStyleResources(): Single<WKDialStyleResources> {
        val deviceInfo = wearKit.deviceAbility.getDeviceInfo()
        return MyDialStyleProvider.getResources(deviceInfo).flatMap {
            val value = it.value
            if (value == null) {
                wearKit.dialStyleAbility.requestCloudDialStyleResources()
            } else {
                Single.just(value)
            }
        }
    }

    private fun updateConstraintUI(constraint: WKDialStyleConstraint) {
        viewBind.viewPreview.shape = wearKit.deviceAbility.getDeviceInfo().shape
        viewBind.btnCreateDial.text = getString(R.string.ds_dial_create, "${constraint.templates.first().size / 1024}KB")

        styleAdapter.items = constraint.styles
        styleAdapter.notifyDataSetChanged()

        val positions = constraint.allowPositions
        viewBind.tvTitlePosition.isVisible = !positions.isNullOrEmpty()
        viewBind.positionRecyclerView.isVisible = !positions.isNullOrEmpty()
        if (!positions.isNullOrEmpty()) {
            positionAdapter.items = positions
        }

        viewBind.tvTitleColor.isVisible = constraint.allowColorTint
        viewBind.btnSelectColor.isVisible = constraint.allowColorTint
        updateCreateButton()
    }

    private fun selectBackgroundColor() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_color_picker, null)
        val colorPickerView = view.findViewById<ColorPickerView>(R.id.color_pick_view)
        MaterialAlertDialogBuilder(this)
            .setView(view)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                backgroundColor = colorPickerView.selectedColor
                refreshPreview()
            }
            .show()
    }

    private fun selectStyleColor() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_color_picker, null)
        val colorPickerView = view.findViewById<ColorPickerView>(R.id.color_pick_view)
        MaterialAlertDialogBuilder(this)
            .setView(view)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                styleColorTint = colorPickerView.selectedColor
            }
            .show()
    }

    private fun refreshPreview() {
        val shape = wearKit.deviceAbility.getDeviceInfo().shape
        val rendered = drafts.map { draft ->
            draft to DanMuTextRenderer.render(
                draft.text,
                draft.fontSizePx.toFloat(),
                DanMuTextRenderer.resolveTypeface(draft.fontStyleIndex),
                draft.textColor,
            )
        }
        try {
            viewBind.viewPreview.background = BitmapDrawable(
                resources,
                createDanMuPreview(shape.width, shape.height, backgroundColor, rendered),
            )
        } finally {
            rendered.forEach { it.second.recycle() }
        }
    }

    private fun createAndInstall(quality: WKDialQuality, items: List<DanMuDraft>) {
        val constraint = styleConstraint ?: return
        if (items.isEmpty()) {
            toast(R.string.dial_custom_style_danmu_empty)
            return
        }
        if (!beginDialCreation()) return
        val style = WKDialStyleAbility.StyleConfig(
            styleIndex = styleAdapter.selectPosition,
            positionIndex = positionAdapter.selectPosition,
            colorTint = styleColorTint,
        )
        val disposable = Single.fromCallable {
            prepareDanMuInput(quality, style, items)
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ (input, directory) ->
                createAndInstall(constraint, input) {
                    directory.deleteRecursively()
                }
            }, {
                finishDialCreation()
                Timber.w(it, "prepare DanMu input failed")
                toast(R.string.tip_failed)
            })
        disposables.add(disposable)
    }

    private fun prepareDanMuInput(
        quality: WKDialQuality,
        style: WKDialStyleAbility.StyleConfig,
        drafts: List<DanMuDraft>,
    ): Pair<WKDialStyleAbility.CreateInput, File> {
        val directory = File(cacheDir, "danmu/${System.nanoTime()}")
        check(directory.mkdirs()) { "Failed to create DanMu cache directory" }
        val animationFiles = drafts.mapNotNull { draft ->
            val uriString = draft.animationUri ?: return@mapNotNull null
            val assetPath = uriString.toAssetPath() ?: return@mapNotNull null
            uriString to copyAssetToCache(directory, assetPath, File(assetPath).name)
        }.toMap()
        val items = drafts.mapIndexed { index, draft ->
            val bitmap = DanMuTextRenderer.render(
                draft.text,
                draft.fontSizePx.toFloat(),
                DanMuTextRenderer.resolveTypeface(draft.fontStyleIndex),
                draft.textColor,
            )
            val imageUri = try {
                val file = File(directory, "danmu_$index.png")
                FileOutputStream(file).use { output ->
                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                        "Failed to save DanMu image"
                    }
                }
                Uri.fromFile(file)
            } finally {
                bitmap.recycle()
            }
            WKDialStyleAbility.DanMuItem(
                imageUri = imageUri,
                imageX = draft.imageX,
                imageY = draft.imageY,
                walkSpeed = draft.walkSpeed.toFloat(),
                animUri = draft.animationUri?.let { uriString ->
                    animationFiles[uriString]?.let { Uri.fromFile(it) } ?: uriString.toUri()
                },
                animX = draft.animX,
                animY = draft.animY,
                ltr = draft.ltr,
            )
        }
        val input = WKDialStyleAbility.CreateInput.danMu(
            items = items,
            style = style,
            backgroundColor = backgroundColor,
        ).apply {
            this.quality = quality
        }
        return input to directory
    }

    private fun copyAssetToCache(directory: File, assetPath: String, fileName: String): File {
        return File(directory, fileName).also { file ->
            assets.open(assetPath).use { input ->
                FileOutputStream(file).use(input::copyTo)
            }
        }
    }

    private fun String.toAssetPath(): String? {
        return if (startsWith(ASSET_URI_PREFIX)) substring(ASSET_URI_PREFIX.length) else null
    }

    private fun createAndInstall(
        constraint: WKDialStyleConstraint,
        input: WKDialStyleAbility.CreateInput,
        cleanup: () -> Unit = {},
    ) {
        val progressDialog = ProgressDialog(this)
        val disposable = wearKit.dialStyleAbility.createCustom(
            constraint = constraint,
            input = input,
        ).flatMapObservable {
            wearKit.dialAbility.install(it.dialId, it.dialFile)
        }.observeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            progressDialog.setCancelable(false)
            progressDialog.setTitle(R.string.dial_installing)
            progressDialog.show()
        }.doFinally {
            cleanup()
            progressDialog.dismiss()
            finishDialCreation()
        }.subscribe({
            progressDialog.progress = it
        }, {
            Timber.w(it)
            toast(R.string.tip_failed)
        }, {})
        disposables.add(disposable)
    }

    private fun beginDialCreation(): Boolean {
        if (isCreatingDial) return false
        isCreatingDial = true
        viewBind.btnCreateDial.isEnabled = false
        return true
    }

    private fun finishDialCreation() {
        isCreatingDial = false
        if (isFinishing || isDestroyed) return
        viewBind.btnCreateDial.isEnabled = true
    }

    private fun updateCreateButton() {
        val templateSize = styleConstraint?.getTemplate(styleAdapter.selectPosition)?.size ?: 0
        viewBind.btnCreateDial.text = getString(R.string.ds_dial_create, "${templateSize / 1024}KB")
    }

    /**
     * Draw each DanMu at its coordinates. Off-screen X is clamped so the preview still shows the text lane.
     */
    private fun createDanMuPreview(
        width: Int,
        height: Int,
        backgroundColor: Int,
        items: List<Pair<DanMuDraft, Bitmap>>,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(backgroundColor)
        items.forEach { (draft, danMuBitmap) ->
            val resolvedX = DanMuCoord.resolveX(draft.imageX, width, danMuBitmap.width)
            val resolvedY = DanMuCoord.resolveY(draft.imageY, height, danMuBitmap.height)
            val drawX = when {
                resolvedX + danMuBitmap.width <= 0 -> 0f
                resolvedX >= width -> (width - danMuBitmap.width).toFloat().coerceAtLeast(0f)
                else -> resolvedX.toFloat()
            }
            canvas.drawBitmap(danMuBitmap, drawX, resolvedY.toFloat(), null)
        }
        return bitmap
    }

    override fun onDestroy() {
        disposables.clear()
        super.onDestroy()
    }

    companion object {
        private const val STATE_DRAFTS = "danmu_drafts"
        private const val ASSET_URI_PREFIX = "file:///android_asset/"
    }
}
