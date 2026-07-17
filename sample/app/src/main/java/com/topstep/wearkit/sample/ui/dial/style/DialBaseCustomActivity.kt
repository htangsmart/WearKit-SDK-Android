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
class DialBaseCustomActivity : GetPhotoVideoActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityDialStyleCustomBinding

    private var styleConstraint: WKDialStyleConstraint? = null
    private var photoUri: Uri? = null
    private val styleAdapter = DialStyleSelectAdapter()
    private val positionAdapter = DialPositionSelectAdapter()
    private var selectedColor = Color.BLACK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityDialStyleCustomBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.dial_custom_style_base)

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

        viewBind.styleRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        viewBind.styleRecyclerView.adapter = styleAdapter
        styleAdapter.listener = object : DialStyleSelectAdapter.Listener {
            override fun onItemSelect(position: Int, item: WKDialStyleConstraint.Style) {
                val templateSize = styleConstraint?.getTemplate(position)?.size ?: 0
                viewBind.btnCreateDial.text = getString(R.string.ds_dial_create, "${templateSize / 1024}KB")
            }
        }

        viewBind.positionRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
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
        val uri = photoUri ?: return
        val progressDialog = ProgressDialog(this)

        wearKit.dialStyleAbility.createCustom(
            constraint = constraint,
            input = WKDialStyleAbility.CreateInput.base(
                backgroundUri = uri,
                style = WKDialStyleAbility.StyleConfig(
                    styleIndex = styleAdapter.selectPosition,
                    positionIndex = positionAdapter.selectPosition,
                    colorTint = selectedColor,
                ),
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
        photoUri = uri
        updateBackground(uri)
    }

    override fun onGetVideo(uri: Uri) {
        // Base dial only supports a single image background
    }

    private fun updateBackground(uri: Uri) {
        Glide.with(this)
            .asBitmap()
            .load(uri)
            .into(object : CustomTarget<Bitmap?>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap?>?) {
                    viewBind.viewBackground.background = BitmapDrawable(this@DialBaseCustomActivity.resources, resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    viewBind.viewBackground.background = null
                }
            })
    }
}
