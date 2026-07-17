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
import com.topstep.wearkit.apis.model.dial.WKDialStyleConstraint
import com.topstep.wearkit.apis.model.dial.WKDialStyleResources
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
import timber.log.Timber
import java.io.File
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
        styleAdapter.listener = object : DialStyleSelectAdapter.Listener {
            override fun onItemSelect(position: Int, item: WKDialStyleConstraint.Style) {
                val templateSize = styleConstraint?.getTemplate(position)?.size ?: 0
                viewBind.btnCreateDial.text =
                    getString(R.string.ds_dial_create, "${templateSize / 1024}KB")
            }
        }

        viewBind.positionRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        viewBind.positionRecyclerView.adapter = positionAdapter

        viewBind.btnSelectColor.clickTrigger {
            selectColor()
        }

        viewBind.btnCreateDial.clickTrigger {
            createAndInstall()
        }
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

    private fun createAndInstall() {
        val constraint = styleConstraint ?: return
        val uri = videoUri ?: return
        val progressDialog = ProgressDialog(this)

        wearKit.dialStyleAbility.createCustom(
            constraint = constraint,
            input = WKDialStyleAbility.CreateInput.video(
                backgroundUri = uri,
                style = WKDialStyleAbility.StyleConfig(
                    styleIndex = styleAdapter.selectPosition,
                    positionIndex = positionAdapter.selectPosition,
                    colorTint = selectedColor,
                ),
                videoDurationMillis = videoDurationMillis,
            )
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

    companion object {
        private const val DIALOG_VIDEO_DURATION = "video_duration"
        private const val DEFAULT_DURATION_MILLIS = 3000L
    }
}
