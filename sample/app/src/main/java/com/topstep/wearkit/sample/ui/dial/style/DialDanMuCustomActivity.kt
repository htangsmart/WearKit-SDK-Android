package com.topstep.wearkit.sample.ui.dial.style

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topstep.wearkit.apis.ability.dial.WKDialStyleAbility
import com.topstep.wearkit.apis.model.dial.WKDialQuality
import com.topstep.wearkit.apis.model.dial.WKDialStyleConstraint
import com.topstep.wearkit.apis.model.dial.WKDialStyleResources
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.MyDialStyleProvider
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityDialDanmuCustomBinding
import com.topstep.wearkit.sample.files.AppFiles
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.ui.dial.style.adapter.DialPositionSelectAdapter
import com.topstep.wearkit.sample.ui.dial.style.adapter.DialStyleSelectAdapter
import com.topstep.wearkit.sample.ui.dialog.SelectIntDialogFragment
import com.topstep.wearkit.sample.utils.FILE_PROVIDER_AUTHORITY
import com.topstep.wearkit.sample.widget.ColorPickerView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

@SuppressLint("CheckResult")
class DialDanMuCustomActivity : BaseActivity(), SelectIntDialogFragment.Listener {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityDialDanmuCustomBinding

    private var styleConstraint: WKDialStyleConstraint? = null
    private val styleAdapter = DialStyleSelectAdapter()
    private val positionAdapter = DialPositionSelectAdapter()
    private var styleColorTint = Color.WHITE

    private var backgroundColor = Color.BLACK
    private var fontStyleIndex = 0
    private var fontSizePx = DEFAULT_FONT_SIZE_PX
    private var walkSpeedPxPerSec = DEFAULT_WALK_SPEED_PX_PER_SEC
    private var textBitmap: Bitmap? = null
    private var textUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityDialDanmuCustomBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.dial_custom_style_danmu)

        viewBind.editDanmuText.setText(R.string.dial_custom_style_danmu_text_default)
        viewBind.editDanmuText.setSelection(viewBind.editDanmuText.text?.length ?: 0)
        viewBind.editDanmuText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                refreshPreview()
            }
        })

        viewBind.btnBgColor.clickTrigger { selectBackgroundColor() }
        viewBind.btnFontStyle.clickTrigger { selectFontStyle() }
        viewBind.btnFontSize.clickTrigger { selectFontSize() }
        viewBind.btnWalkSpeed.clickTrigger { selectWalkSpeed() }
        viewBind.btnSelectColor.clickTrigger { selectStyleColor() }
        viewBind.btnCreateDial.clickTrigger {
            chooseDialQuality(wearKit.dialStyleAbility.compat.getQualityLevels()) { quality ->
                createAndInstall(quality)
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

        updateFontStyleButton()
        updateFontSizeButton()
        updateWalkSpeedButton()

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

    private fun selectFontStyle() {
        val items = arrayOf(
            getString(R.string.dial_custom_style_danmu_font_normal),
            getString(R.string.dial_custom_style_danmu_font_bold),
            getString(R.string.dial_custom_style_danmu_font_italic),
            getString(R.string.dial_custom_style_danmu_font_bold_italic),
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dial_custom_style_danmu_font_style)
            .setSingleChoiceItems(items, fontStyleIndex) { dialog, which ->
                fontStyleIndex = which
                updateFontStyleButton()
                refreshPreview()
                dialog.dismiss()
            }
            .show()
    }

    private fun selectFontSize() {
        SelectIntDialogFragment.newInstance(
            min = MIN_FONT_SIZE_PX,
            max = MAX_FONT_SIZE_PX,
            value = fontSizePx,
            title = getString(R.string.dial_custom_style_danmu_font_size),
        ).show(supportFragmentManager, DIALOG_FONT_SIZE)
    }

    private fun selectWalkSpeed() {
        SelectIntDialogFragment.newInstance(
            min = MIN_WALK_SPEED_PX_PER_SEC,
            max = MAX_WALK_SPEED_PX_PER_SEC,
            value = walkSpeedPxPerSec,
            title = getString(R.string.dial_custom_style_danmu_walk_speed),
        ).show(supportFragmentManager, DIALOG_WALK_SPEED)
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
        val text = viewBind.editDanmuText.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) {
            textBitmap = null
            textUri = null
            viewBind.viewPreview.background = BitmapDrawable(
                resources,
                createDanMuPreview(shape.width, shape.height, backgroundColor, null)
            )
            return
        }
        val rendered = renderDanMuText(text, fontSizePx.toFloat(), resolveTypeface(fontStyleIndex))
        textBitmap = rendered
        textUri = saveBitmapAsPng(rendered)
        viewBind.viewPreview.background = BitmapDrawable(
            resources,
            createDanMuPreview(shape.width, shape.height, backgroundColor, rendered)
        )
    }

    private fun createAndInstall(quality: WKDialQuality) {
        val constraint = styleConstraint ?: return
        val uri = textUri ?: run {
            toast(R.string.dial_custom_style_danmu_text_hint)
            return
        }
        val progressDialog = ProgressDialog(this)
        val walkSpeed = walkSpeedPxPerSec / 1000f

        wearKit.dialStyleAbility.createCustom(
            constraint = constraint,
            input = WKDialStyleAbility.CreateInput.danMu(
                textUri = uri,
                style = WKDialStyleAbility.StyleConfig(
                    styleIndex = styleAdapter.selectPosition,
                    positionIndex = positionAdapter.selectPosition,
                    colorTint = styleColorTint,
                ),
                backgroundColor = backgroundColor,
                walkSpeed = walkSpeed,
            ).apply {
                this.quality = quality
            }
        ).flatMapObservable {
            wearKit.dialAbility.install(it.dialId, it.dialFile)
        }.observeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            progressDialog.setCancelable(false)
            progressDialog.setTitle(R.string.dial_installing)
            progressDialog.show()
        }.subscribe({
            progressDialog.progress = it
        }, {
            Timber.w(it)
            progressDialog.dismiss()
            toast(R.string.tip_failed)
        }, {
            progressDialog.dismiss()
        })
    }

    private fun updateCreateButton() {
        val templateSize = styleConstraint?.getTemplate(styleAdapter.selectPosition)?.size ?: 0
        viewBind.btnCreateDial.text = getString(R.string.ds_dial_create, "${templateSize / 1024}KB")
    }

    private fun updateFontStyleButton() {
        viewBind.btnFontStyle.text = when (fontStyleIndex) {
            1 -> getString(R.string.dial_custom_style_danmu_font_bold)
            2 -> getString(R.string.dial_custom_style_danmu_font_italic)
            3 -> getString(R.string.dial_custom_style_danmu_font_bold_italic)
            else -> getString(R.string.dial_custom_style_danmu_font_normal)
        }
    }

    private fun updateFontSizeButton() {
        viewBind.btnFontSize.text = getString(R.string.dial_custom_style_danmu_font_size_value, fontSizePx)
    }

    private fun updateWalkSpeedButton() {
        viewBind.btnWalkSpeed.text =
            getString(R.string.dial_custom_style_danmu_walk_speed_value, walkSpeedPxPerSec)
    }

    override fun onDialogSelectInt(tag: String?, selectValue: Int) {
        when (tag) {
            DIALOG_FONT_SIZE -> {
                fontSizePx = selectValue
                updateFontSizeButton()
                refreshPreview()
            }
            DIALOG_WALK_SPEED -> {
                walkSpeedPxPerSec = selectValue
                updateWalkSpeedButton()
            }
        }
    }

    override fun dialogSelectIntFormat(tag: String?, value: Int): String {
        return when (tag) {
            DIALOG_FONT_SIZE -> getString(R.string.dial_custom_style_danmu_font_size_value, value)
            DIALOG_WALK_SPEED -> getString(R.string.dial_custom_style_danmu_walk_speed_value, value)
            else -> super.dialogSelectIntFormat(tag, value)
        }
    }

    private fun resolveTypeface(styleIndex: Int): Typeface {
        return when (styleIndex) {
            1 -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            2 -> Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            3 -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
            else -> Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
    }

    private fun renderDanMuText(text: String, fontSizePx: Float, typeface: Typeface): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = fontSizePx
            this.typeface = typeface
            this.color = Color.WHITE
        }
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        val width = (bounds.width() + TEXT_PADDING * 2).coerceAtLeast(1)
        val height = (bounds.height() + TEXT_PADDING * 2).coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val x = -bounds.left.toFloat() + TEXT_PADDING
        val y = -bounds.top.toFloat() + TEXT_PADDING
        canvas.drawText(text, x, y, paint)
        return bitmap
    }

    /**
     * Same layout as SDK: fill [backgroundColor], draw danmu text left-aligned and vertically centered.
     */
    private fun createDanMuPreview(
        width: Int,
        height: Int,
        backgroundColor: Int,
        danMuBitmap: Bitmap?,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(backgroundColor)
        if (danMuBitmap != null) {
            val top = ((height - danMuBitmap.height) / 2f).coerceAtLeast(0f)
            canvas.drawBitmap(danMuBitmap, 0f, top, null)
        }
        return bitmap
    }

    private fun saveBitmapAsPng(bitmap: Bitmap): Uri? {
        val dir = AppFiles.dirPicture(this) ?: return null
        val file = File(dir, "danmu_${System.currentTimeMillis()}.png")
        return runCatching {
            FileOutputStream(file).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            FileProvider.getUriForFile(this, FILE_PROVIDER_AUTHORITY, file)
        }.onFailure {
            Timber.w(it, "save danmu png failed")
        }.getOrNull()
    }

    companion object {
        private const val DIALOG_FONT_SIZE = "font_size"
        private const val DIALOG_WALK_SPEED = "walk_speed"
        private const val DEFAULT_FONT_SIZE_PX = 32
        private const val MIN_FONT_SIZE_PX = 12
        private const val MAX_FONT_SIZE_PX = 96
        private const val DEFAULT_WALK_SPEED_PX_PER_SEC = 60
        private const val MIN_WALK_SPEED_PX_PER_SEC = 1
        private const val MAX_WALK_SPEED_PX_PER_SEC = 200
        private const val TEXT_PADDING = 4
    }
}
