package com.topstep.wearkit.sample.ui.dial.style

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topstep.wearkit.apis.ability.dial.WKDialStyleAbility
import com.topstep.wearkit.apis.model.dial.WKDialQuality
import com.topstep.wearkit.apis.model.dial.WKDialStyleConstraint
import com.topstep.wearkit.apis.model.dial.WKDialStyleResources
import com.topstep.wearkit.prototb.internal.ability.dial.DialCreateLocalize
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.MyDialStyleProvider
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityDialVideoCustomBinding
import com.topstep.wearkit.sample.files.AppFiles
import com.topstep.wearkit.sample.ui.base.CropParam
import com.topstep.wearkit.sample.ui.base.GetPhotoVideoActivity
import com.topstep.wearkit.sample.ui.dial.style.adapter.DialPositionSelectAdapter
import com.topstep.wearkit.sample.ui.dial.style.adapter.DialStyleSelectAdapter
import com.topstep.wearkit.sample.ui.dialog.SelectIntDialogFragment
import com.topstep.wearkit.sample.widget.ColorPickerView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import timber.log.Timber
import java.io.File
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@SuppressLint("CheckResult")
class DialVideoCustomActivity : GetPhotoVideoActivity(), SelectIntDialogFragment.Listener {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityDialVideoCustomBinding

    private var styleConstraint: WKDialStyleConstraint? = null
    private var videoUri: Uri? = null
    private val styleAdapter = DialStyleSelectAdapter()
    private val positionAdapter = DialPositionSelectAdapter()
    private var selectedColor = Color.BLACK
    private var videoDurationMillis = DEFAULT_DURATION_MILLIS
    private var packDisposable: Disposable? = null
    private var installDisposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityDialVideoCustomBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.dial_custom_style_video)

        videoDurationMillis = resolveDefaultDurationMillis()
        updateDurationButton()

        getDialStyleResources().flatMap {
            wearKit.dialStyleAbility.requestConstraint(it)
        }.observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                this.styleConstraint = it
                updateUI(it)
            }, {
                Timber.w(it)
                toast(R.string.tip_failed)
            })

        viewBind.btnSelectBackground.clickTrigger {
            selectBackground()
        }

        viewBind.btnVideoDuration.clickTrigger {
            selectDuration()
        }

        viewBind.styleRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        viewBind.styleRecyclerView.adapter = styleAdapter

        viewBind.positionRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        viewBind.positionRecyclerView.adapter = positionAdapter

        viewBind.btnSelectColor.clickTrigger {
            selectColor()
        }

        viewBind.btnCreateSingle.clickTrigger {
            chooseDialQuality(qualityLevels()) { quality ->
                packSingle(quality)
            }
        }

        viewBind.btnCreateAll.clickTrigger {
            packAllThenChoose()
        }
    }

    override fun onDestroy() {
        packDisposable?.dispose()
        installDisposable?.dispose()
        super.onDestroy()
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

    private fun qualityLevels(): List<WKDialQuality> {
        val levels = wearKit.dialStyleAbility.compat.getQualityLevels()
        return levels.ifEmpty { listOf(WKDialQuality.SD) }
    }

    private fun packSingle(quality: WKDialQuality) {
        val constraint = styleConstraint ?: return
        if (!ensureVideoSelected()) return
        val startMs = SystemClock.elapsedRealtime()
        subscribePack(wearKit.dialStyleAbility.createCustom(constraint, newCreateInput(quality))) { output ->
            toastElapsed(startMs)
            installDial(output)
        }
    }

    private fun packAllThenChoose() {
        val constraint = styleConstraint ?: return
        if (!ensureVideoSelected()) return
        val qualities = qualityLevels()
        val startMs = SystemClock.elapsedRealtime()
        subscribePack(
            DialCreateLocalize.make(this, constraint, newCreateInput(qualities.first())).flatMap { local ->
                val sources = qualities.map { quality ->
                    wearKit.dialStyleAbility.createCustom(
                        local.constraint,
                        copyLocalizedInput(local.input, quality),
                    ).map { PackedDial(quality, it) }
                }
                Single.zip(sources) { array ->
                    array.map { it as PackedDial }
                }
            }
        ) { packed ->
            toastElapsed(startMs)
            showPackedChoice(packed)
        }
    }

    private fun <T : Any> subscribePack(source: Single<T>, onSuccess: (T) -> Unit) {
        packDisposable?.dispose()
        setPackButtonsEnabled(false)
        val progressDialog = ProgressDialog(this).apply {
            setMessage(getString(R.string.dial_video_pack_running))
            setCancelable(false)
            show()
        }
        packDisposable = source
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally {
                dismissDialog(progressDialog)
                if (isUiAlive()) setPackButtonsEnabled(true)
            }
            .subscribe({ result ->
                if (isUiAlive()) onSuccess(result)
            }, {
                Timber.w(it)
                if (isUiAlive()) toast(R.string.tip_failed)
            })
    }

    private fun showPackedChoice(packed: List<PackedDial>) {
        if (!isUiAlive()) return
        val labels = packed.map {
            getString(
                R.string.dial_video_pack_choice_item,
                qualityLabel(it.quality),
                formatFileSize(it.output.dialFile.length()),
            )
        }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dial_video_pack_choice)
            .setItems(labels) { _, which ->
                installDial(packed[which].output)
            }
            .show()
    }

    private fun installDial(output: WKDialStyleAbility.CreateOutput) {
        if (!isUiAlive()) return
        installDisposable?.dispose()
        val progressDialog = ProgressDialog(this)
        installDisposable = wearKit.dialAbility.install(output.dialId, output.dialFile)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
                progressDialog.setCancelable(false)
                progressDialog.setTitle(R.string.dial_installing)
                progressDialog.show()
            }
            .doFinally {
                dismissDialog(progressDialog)
            }
            .subscribe({
                if (isUiAlive()) progressDialog.progress = it
            }, {
                Timber.w(it)
                if (isUiAlive()) toast(R.string.tip_failed)
            })
    }

    private fun isUiAlive(): Boolean {
        return !isFinishing && !isDestroyed
    }

    private fun dismissDialog(dialog: ProgressDialog) {
        if (dialog.isShowing) {
            dialog.dismiss()
        }
    }

    private fun chooseDialQuality(
        levels: List<WKDialQuality>,
        onChosen: (WKDialQuality) -> Unit,
    ) {
        when {
            levels.size <= 1 -> onChosen(levels.firstOrNull() ?: WKDialQuality.SD)
            else -> {
                val labels = levels.map { qualityLabel(it) }.toTypedArray()
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.dial_quality_select)
                    .setItems(labels) { _, which -> onChosen(levels[which]) }
                    .show()
            }
        }
    }

    private fun newCreateInput(quality: WKDialQuality): WKDialStyleAbility.CreateInput {
        val uri = videoUri ?: error("videoUri required")
        return WKDialStyleAbility.CreateInput.video(
            backgroundUri = uri,
            style = WKDialStyleAbility.StyleConfig(
                styleIndex = styleAdapter.selectPosition,
                positionIndex = positionAdapter.selectPosition,
                colorTint = selectedColor,
            ),
            videoDurationMillis = videoDurationMillis,
        ).apply {
            this.quality = quality
        }
    }

    /**
     * 并行打包时为每个质量复制一份 input。
     * 1. subscribe 后不要再改这份 input
     * 2. 不要使用相同的 output 路径（可为 null，SDK 会生成唯一路径）
     */
    private fun copyLocalizedInput(
        source: WKDialStyleAbility.CreateInput,
        quality: WKDialQuality,
    ): WKDialStyleAbility.CreateInput {
        return WKDialStyleAbility.CreateInput().apply {
            styleIndex = source.styleIndex
            positionIndex = source.positionIndex
            colorTint = source.colorTint
            backgroundUri = source.backgroundUri
            videoRect = source.videoRect
            videoOffsetMillis = source.videoOffsetMillis
            videoDurationMillis = source.videoDurationMillis
            inputs = source.inputs
            danMuConfig = source.danMuConfig
            multiplePlayIntervalMillis = source.multiplePlayIntervalMillis
            customDialId = source.customDialId
            this.quality = quality
            outputDialFile = source.outputDialFile
            outputPreviewFile = source.outputPreviewFile
        }
    }

    private fun updateUI(constraint: WKDialStyleConstraint) {
        viewBind.viewBackground.shape = wearKit.deviceAbility.getDeviceInfo().shape

        styleAdapter.items = constraint.styles
        styleAdapter.notifyDataSetChanged()

        val positions = constraint.allowPositions
        viewBind.tvTitlePosition.isVisible = !positions.isNullOrEmpty()
        if (!positions.isNullOrEmpty()) {
            positionAdapter.items = positions
        }

        viewBind.tvTitleColor.isVisible = constraint.allowColorTint
        viewBind.btnSelectColor.isVisible = constraint.allowColorTint
    }

    private fun selectBackground() {
        val items = arrayOf(
            getString(R.string.action_take_video),
            getString(R.string.action_choose_video),
        )
        MaterialAlertDialogBuilder(this)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> takeVideo(CROP_TRY)
                    1 -> chooseAlbum(CROP_TRY, "video/mp4")
                }
            }
            .show()
    }

    private fun selectDuration() {
        val maxSeconds = max(1, (getMaxDurationMillis() / 1000L).toInt())
        SelectIntDialogFragment.newInstance(
            min = 1,
            max = maxSeconds,
            multiples = 1000,
            value = videoDurationMillis.toInt().coerceIn(1000, maxSeconds * 1000),
            title = getString(R.string.dial_custom_style_video_duration),
        ).show(supportFragmentManager, DIALOG_VIDEO_DURATION)
    }

    private fun selectColor() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_color_picker, null)
        val colorPickerView = view.findViewById<ColorPickerView>(R.id.color_pick_view)
        MaterialAlertDialogBuilder(this)
            .setView(view)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                selectedColor = colorPickerView.selectedColor
            }
            .show()
    }

    private fun getMaxDurationMillis(): Long {
        return max(1000L, wearKit.dialStyleAbility.compat.getVideoMaxDurationMillis())
    }

    private fun resolveDefaultDurationMillis(): Long {
        return min(DEFAULT_DURATION_MILLIS, getMaxDurationMillis())
    }

    private fun updateDurationButton() {
        viewBind.btnVideoDuration.text =
            getString(R.string.unit_second_param, (videoDurationMillis / 1000L).toInt())
    }

    override fun onDialogSelectInt(tag: String?, selectValue: Int) {
        if (tag == DIALOG_VIDEO_DURATION) {
            videoDurationMillis = selectValue.toLong().coerceAtMost(getMaxDurationMillis())
            updateDurationButton()
        }
    }

    override fun dialogSelectIntFormat(tag: String?, value: Int): String {
        if (tag == DIALOG_VIDEO_DURATION) {
            return getString(R.string.unit_second_param, value / 1000)
        }
        return super.dialogSelectIntFormat(tag, value)
    }

    override fun getTakePhotoFile(): File? {
        return AppFiles.generateJpegFile(this)
    }

    override fun getCropPhotoFile(): File? {
        return AppFiles.generateJpegFile(this)
    }

    override fun getCropPhotoParam(): CropParam {
        val shape = wearKit.deviceAbility.getDeviceInfo().shape
        return CropParam(shape.width, shape.height, shape.width, shape.height)
    }

    override fun onGetPhoto(uri: Uri) {
        // Video dial only supports video background
    }

    override fun onGetVideo(uri: Uri) {
        videoUri = uri
        updateBackground(uri)
    }

    private fun updateBackground(uri: Uri) {
        Glide.with(this)
            .asBitmap()
            .load(uri)
            .into(object : CustomTarget<Bitmap?>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap?>?) {
                    viewBind.viewBackground.background =
                        BitmapDrawable(this@DialVideoCustomActivity.resources, resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    viewBind.viewBackground.background = null
                }
            })
    }

    private fun ensureVideoSelected(): Boolean {
        if (videoUri != null) return true
        toast(R.string.action_select)
        return false
    }

    private fun toastElapsed(startMs: Long): Double {
        val seconds = (SystemClock.elapsedRealtime() - startMs) / 1000.0
        toast(getString(R.string.dial_video_pack_elapsed, seconds))
        return seconds
    }

    private fun setPackButtonsEnabled(enabled: Boolean) {
        viewBind.btnCreateSingle.isEnabled = enabled
        viewBind.btnCreateAll.isEnabled = enabled
    }

    private fun qualityLabel(quality: WKDialQuality): String {
        return when (quality) {
            WKDialQuality.SD -> getString(R.string.dial_quality_sd)
            WKDialQuality.LOSSLESS -> getString(R.string.dial_quality_lossless)
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return String.format(Locale.US, "%dKB", bytes / 1024)
    }

    private data class PackedDial(
        val quality: WKDialQuality,
        val output: WKDialStyleAbility.CreateOutput,
    )

    companion object {
        private const val DIALOG_VIDEO_DURATION = "video_duration"
        private const val DEFAULT_DURATION_MILLIS = 3000L
    }
}
