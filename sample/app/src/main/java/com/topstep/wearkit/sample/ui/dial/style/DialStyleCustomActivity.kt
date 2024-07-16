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
import com.topstep.wearkit.sample.databinding.ActivityDialStyleCustomBinding
import com.topstep.wearkit.sample.files.AppFiles
import com.topstep.wearkit.sample.ui.base.CropParam
import com.topstep.wearkit.sample.ui.base.GetPhotoVideoActivity
import com.topstep.wearkit.sample.ui.dial.style.adapter.DialPositionSelectAdapter
import com.topstep.wearkit.sample.ui.dial.style.adapter.DialStyleSelectAdapter
import com.topstep.wearkit.sample.widget.ColorPickerView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import timber.log.Timber
import java.io.File

@SuppressLint("CheckResult")
class DialStyleCustomActivity : GetPhotoVideoActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityDialStyleCustomBinding

    private var styleConstraint: WKDialStyleConstraint? = null
    private var selectBackgroundUri: Uri? = null
    private val styleAdapter = DialStyleSelectAdapter()
    private val positionAdapter = DialPositionSelectAdapter()
    private var selectedColor = Color.BLACK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityDialStyleCustomBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.dial_custom_style)

        //Request custom style constraint(info)
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

        //Background
        viewBind.btnSelectBackground.clickTrigger {
            selectBackground()
        }

        //Style
        viewBind.styleRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        viewBind.styleRecyclerView.adapter = styleAdapter
        styleAdapter.listener = object : DialStyleSelectAdapter.Listener {
            override fun onItemSelect(position: Int, item: WKDialStyleConstraint.Style) {
                val templateSize = styleConstraint?.getTemplate(position)?.size ?: 0
                viewBind.btnCreateDial.text = getString(R.string.ds_dial_create, "${templateSize / 1024}KB")
                //May your should update preview
            }
        }

        //Position
        viewBind.positionRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        viewBind.positionRecyclerView.adapter = positionAdapter
        positionAdapter.listener = object : DialPositionSelectAdapter.Listener {
            override fun onItemSelect(position: Int, item: WKDialStyleConstraint.Position) {
                //May your should update preview
            }
        }

        //Color
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
                //If you don't have your own dial style resources
                //Request sdk by default
                wearKit.dialStyleAbility.requestCloudDialStyleResources()
            } else {
                Single.just(value)
            }
        }
    }

    private fun createAndInstall() {
        val constraint = styleConstraint ?: return
        val backgroundUri = selectBackgroundUri ?: return

        val progressDialog = ProgressDialog(this)

        wearKit.dialStyleAbility.createCustom(
            constraint = constraint,
            input = WKDialStyleAbility.CreateInput().apply {
                this.backgroundUri = backgroundUri
                this.styleIndex = styleAdapter.selectPosition
                this.positionIndex = positionAdapter.selectPosition
                this.colorTint = selectedColor
            }
        ).flatMapObservable {
            wearKit.dialStyleAbility.installCustom(it.dialFile)
        }.observeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            progressDialog.setCancelable(false)
            progressDialog.setTitle("Installing...")
            progressDialog.show()
        }.subscribe({
            progressDialog.progress = it.progress
        }, {
            Timber.w(it)
            progressDialog.dismiss()
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
        val isSupportVideo = wearKit.dialStyleAbility.compat.isSupportVideoBackground()
        val items = if (isSupportVideo) {
            arrayOf(
                getString(R.string.action_take_photo),
                getString(R.string.action_choose_photo),
                getString(R.string.action_take_video),
                getString(R.string.action_choose_video),
            )
        } else {
            arrayOf(
                getString(R.string.action_take_photo),
                getString(R.string.action_choose_photo),
            )
        }
        MaterialAlertDialogBuilder(this)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> {//Take photo
                        takePhoto(CROP_TRY)
                    }
                    1 -> {//Choose photo
                        chooseAlbum(CROP_TRY, "image/*")
                    }
                    2 -> {//Take video
                        takeVideo(CROP_TRY)
                    }
                    3 -> {//Choose video
                        chooseAlbum(CROP_TRY, "video/mp4")
                    }
                }
            }
            .show()
    }

    private fun selectColor() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_color_picker, null)
        val colorPickerView = view.findViewById<ColorPickerView>(R.id.color_pick_view)
        MaterialAlertDialogBuilder(this)
            .setView(view)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                selectedColor = colorPickerView.selectedColor
                //May your should update preview
            }
            .show()
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
        //background changed
        selectBackgroundUri = uri
        updateBackground(uri)
    }

    override fun onGetVideo(uri: Uri) {
        selectBackgroundUri = uri
        updateBackground(uri)
    }

    private fun updateBackground(uri: Uri) {
        Glide.with(this)
            .asBitmap()
            .load(uri)
            .into(object : CustomTarget<Bitmap?>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap?>?) {
                    viewBind.viewBackground.background = BitmapDrawable(this@DialStyleCustomActivity.resources, resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    viewBind.viewBackground.background = null
                }
            })
    }
}