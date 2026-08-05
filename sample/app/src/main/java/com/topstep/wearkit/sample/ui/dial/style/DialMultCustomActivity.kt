package com.topstep.wearkit.sample.ui.dial.style

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
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
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.MyDialStyleProvider
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityDialMultCustomBinding
import com.topstep.wearkit.sample.files.AppFiles
import com.topstep.wearkit.sample.ui.base.CropParam
import com.topstep.wearkit.sample.ui.base.GetPhotoVideoActivity
import com.topstep.wearkit.sample.ui.dial.style.adapter.DialBackgroundThumbAdapter
import com.topstep.wearkit.sample.ui.dial.style.adapter.DialPositionSelectAdapter
import com.topstep.wearkit.sample.ui.dial.style.adapter.DialStyleSelectAdapter
import com.topstep.wearkit.sample.ui.dialog.SelectIntDialogFragment
import com.topstep.wearkit.sample.widget.ColorPickerView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import timber.log.Timber
import java.io.File

@SuppressLint("CheckResult")
class DialMultCustomActivity : GetPhotoVideoActivity(), SelectIntDialogFragment.Listener {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityDialMultCustomBinding

    private var styleConstraint: WKDialStyleConstraint? = null
    private val backgrounds = ArrayList<BackgroundItem>()
    private val backgroundAdapter = DialBackgroundThumbAdapter()
    private val styleAdapter = DialStyleSelectAdapter()
    private val positionAdapter = DialPositionSelectAdapter()
    private var applyingSelection = false
    private var playIntervalMillis = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityDialMultCustomBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.dial_custom_style_multiple)

        getDialStyleResources().flatMap {
            wearKit.dialStyleAbility.requestConstraint(it)
        }.observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                this.styleConstraint = it
                updateConstraintUI(it)
            }, {
                Timber.w(it)
                toast(R.string.tip_failed)
            })

        viewBind.btnSelectBackground.clickTrigger {
            selectBackground()
        }

        viewBind.btnPlayInterval.clickTrigger {
            selectPlayInterval()
        }

        viewBind.backgroundRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        viewBind.backgroundRecyclerView.adapter = backgroundAdapter
        backgroundAdapter.listener = object : DialBackgroundThumbAdapter.Listener {
            override fun onItemSelect(position: Int, uri: Uri) {
                bindSelectedBackground(position)
            }
        }

        viewBind.styleRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        viewBind.styleRecyclerView.adapter = styleAdapter
        styleAdapter.listener = object : DialStyleSelectAdapter.Listener {
            override fun onItemSelect(position: Int, item: WKDialStyleConstraint.Style) {
                if (applyingSelection) return
                currentBackground()?.styleIndex = position
                updateCreateButton()
            }
        }

        viewBind.positionRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        viewBind.positionRecyclerView.adapter = positionAdapter
        positionAdapter.listener = object : DialPositionSelectAdapter.Listener {
            override fun onItemSelect(position: Int, item: WKDialStyleConstraint.Position) {
                if (applyingSelection) return
                currentBackground()?.positionIndex = position
            }
        }

        viewBind.btnSelectColor.clickTrigger {
            selectColor()
        }

        viewBind.btnCreateDial.clickTrigger {
            chooseDialQuality(wearKit.dialStyleAbility.compat.getQualityLevels()) { quality ->
                createAndInstall(quality)
            }
        }

        refreshBackgroundSection()
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

    private fun createAndInstall(quality: WKDialQuality) {
        val constraint = styleConstraint ?: return
        if (backgrounds.isEmpty()) return
        val progressDialog = ProgressDialog(this)

        wearKit.dialStyleAbility.createCustom(
            constraint = constraint,
            input = WKDialStyleAbility.CreateInput.multiple(
                backgroundUris = backgrounds.map { it.uri },
                styles = backgrounds.map {
                    WKDialStyleAbility.StyleConfig(
                        styleIndex = it.styleIndex,
                        positionIndex = it.positionIndex,
                        colorTint = it.colorTint,
                    )
                },
                playIntervalMillis = playIntervalMillis,
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

    private fun updateConstraintUI(constraint: WKDialStyleConstraint) {
        viewBind.viewBackground.shape = wearKit.deviceAbility.getDeviceInfo().shape
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

        if (backgrounds.isNotEmpty()) {
            bindSelectedBackground(backgroundAdapter.selectPosition)
        }
        updateCreateButton()
    }

    private fun selectBackground() {
        val items = arrayOf(
            getString(R.string.action_take_photo),
            getString(R.string.action_choose_photo),
        )
        MaterialAlertDialogBuilder(this)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> takePhoto(CROP_TRY)
                    1 -> chooseAlbum(CROP_TRY, "image/*")
                }
            }
            .show()
    }

    private fun selectPlayInterval() {
        SelectIntDialogFragment.newInstance(
            min = 0,
            max = 60,
            multiples = 1000,
            value = playIntervalMillis,
            title = getString(R.string.dial_custom_style_multiple_interval),
        ).show(supportFragmentManager, DIALOG_PLAY_INTERVAL)
    }

    private fun selectColor() {
        val current = currentBackground() ?: return
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_color_picker, null)
        val colorPickerView = view.findViewById<ColorPickerView>(R.id.color_pick_view)
        MaterialAlertDialogBuilder(this)
            .setView(view)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                current.colorTint = colorPickerView.selectedColor
            }
            .show()
    }

    private fun addBackground(uri: Uri) {
        backgrounds.add(BackgroundItem(uri = uri))
        backgroundAdapter.items = backgrounds.map { it.uri }
        backgroundAdapter.select(backgrounds.lastIndex)
        refreshBackgroundSection()
        bindSelectedBackground(backgrounds.lastIndex)
    }

    private fun refreshBackgroundSection() {
        val count = backgrounds.size
        viewBind.tvBackgroundCount.text = getString(R.string.dial_custom_style_multiple_count, count)
        viewBind.backgroundRecyclerView.isVisible = count > 0
        viewBind.layoutPlayInterval.isVisible = count > 0
        viewBind.layoutStyleOptions.isVisible = count > 0
        updatePlayIntervalButton()
        updateCreateButton()
    }

    private fun updatePlayIntervalButton() {
        viewBind.btnPlayInterval.text = if (playIntervalMillis <= 0) {
            getString(R.string.dial_custom_style_multiple_interval_off)
        } else {
            getString(R.string.dial_custom_style_multiple_interval_value, playIntervalMillis / 1000)
        }
    }

    private fun bindSelectedBackground(position: Int) {
        val item = backgrounds.getOrNull(position) ?: return
        applyingSelection = true
        styleAdapter.select(item.styleIndex, notifyListener = false)
        if (positionAdapter.items != null) {
            positionAdapter.select(item.positionIndex, notifyListener = false)
        }
        applyingSelection = false
        updatePreview(item.uri)
        updateCreateButton()
    }

    private fun currentBackground(): BackgroundItem? {
        return backgrounds.getOrNull(backgroundAdapter.selectPosition)
    }

    private fun updateCreateButton() {
        val styleIndex = currentBackground()?.styleIndex ?: styleAdapter.selectPosition
        val templateSize = styleConstraint?.getTemplate(styleIndex)?.size ?: 0
        viewBind.btnCreateDial.text = getString(R.string.ds_dial_create, "${templateSize / 1024}KB")
    }

    override fun onDialogSelectInt(tag: String?, selectValue: Int) {
        if (tag == DIALOG_PLAY_INTERVAL) {
            playIntervalMillis = selectValue
            updatePlayIntervalButton()
        }
    }

    override fun dialogSelectIntFormat(tag: String?, value: Int): String {
        if (tag == DIALOG_PLAY_INTERVAL) {
            return if (value <= 0) {
                getString(R.string.dial_custom_style_multiple_interval_off)
            } else {
                getString(R.string.dial_custom_style_multiple_interval_value, value / 1000)
            }
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
        addBackground(uri)
    }

    override fun onGetVideo(uri: Uri) {
        // Multiple background dial only supports images
    }

    private fun updatePreview(uri: Uri) {
        Glide.with(this)
            .asBitmap()
            .load(uri)
            .into(object : CustomTarget<Bitmap?>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap?>?) {
                    viewBind.viewBackground.background =
                        BitmapDrawable(this@DialMultCustomActivity.resources, resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    viewBind.viewBackground.background = null
                }
            })
    }

    private data class BackgroundItem(
        val uri: Uri,
        var styleIndex: Int = 0,
        var positionIndex: Int = 0,
        var colorTint: Int = Color.BLACK,
    )

    companion object {
        private const val DIALOG_PLAY_INTERVAL = "play_interval"
    }
}
