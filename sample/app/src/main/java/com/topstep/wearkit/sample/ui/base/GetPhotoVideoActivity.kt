package com.topstep.wearkit.sample.ui.base

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.github.kilnn.tool.storage.MediaStoreUtil
import com.github.kilnn.tool.storage.SAFUtil
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.files.AppFiles
import com.topstep.wearkit.sample.utils.FILE_PROVIDER_AUTHORITY
import com.topstep.wearkit.sample.utils.isVideo
import com.topstep.wearkit.sample.utils.permission.PermissionHelper
import timber.log.Timber
import java.io.File

abstract class GetPhotoVideoActivity : BaseActivity() {

    /**
     * Temporarily store photos or select photo Uri
     * Because taking pictures does not return Uri, but uses the url passed in by itself, so this uri needs to be saved for use after the picture is taken successfully.
     */
    private var photoUri: Uri? = null

    /**
     * The Uri where the cropped photo is stored
     * Compatible processing: Some mobile phones do not return Uri for cropped photos, so save the Uri that is passed in and cropped for output in [tryCropPhoto]. Similar to [photoUri].
     */
    private var cropUri: Uri? = null

    /**
     * Whether to take pictures, true to take pictures, false to select
     */
    private var actionTake = true

    /**
     * Crop Mode
     * [CROP_NONE]
     * [CROP_TRY]
     * [CROP_MUST]
     */
    private var cropMode = CROP_TRY

    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = photoUri
        Timber.tag(TAG).i("take photo result:%d , uri:%s", result.resultCode, uri)
        if (result.resultCode == Activity.RESULT_OK) {
            if (uri != null) {
                tryCropPhoto(uri)
            } else {
                toast(R.string.take_failed)
            }
        }
    }

    private val chooseLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        Timber.tag(TAG).i("choose result:%d , uri:%s", result.resultCode, uri)
        if (result.resultCode == Activity.RESULT_OK) {
            if (uri != null) {
                if (isVideo(uri)) {
                    onGetVideo(uri)
                } else {
                    copyChoosePhoto(uri)
                }
            } else {
                toast(R.string.choose_failed)
            }
        }
    }

    private val cropPhotoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        Timber.tag(TAG).i("crop photo result:%d , uri:%s", result.resultCode, uri)
        if (result.resultCode == Activity.RESULT_OK) {
            handleCropPhotoResult(uri ?: cropUri)//部分手机裁剪不返回uri，使用cropUri
        }
    }

    private val takeVideoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        Timber.tag(TAG).i("take video result:%d , uri:%s", result.resultCode, uri)
        if (result.resultCode == Activity.RESULT_OK) {
            if (uri != null) {
                onGetVideo(uri)
            } else {
                toast(R.string.choose_failed)
            }
        }
    }

    /**
     * Compatible processing: [Intent.ACTION_PICK] only has temporary permissions, so an additional function of copying pictures is made to copy the selected pictures to the APP exclusive directory
     * refer：https://stackoverflow.com/questions/30572261/using-data-from-context-providers-or-requesting-google-photos-read-permission
     */
    private fun copyChoosePhoto(uri: Uri) {
        lifecycleScope.launchWhenStarted {
            val resultUri = AppFiles.copyChoosePhotoUri(this@GetPhotoVideoActivity, uri)
            if (resultUri != null) {
                tryCropPhoto(resultUri)
            }
        }
    }

    private fun tryCropPhoto(uri: Uri) {
        if (cropMode == CROP_NONE) {
            onGetPhoto(uri)
        } else {
            //Save the photo Uri before cropping
            photoUri = uri
            try {
                //call system crop
                val cropFile = getCropPhotoFile() ?: throw NullPointerException()
                val cropParam = getCropPhotoParam() ?: throw NullPointerException()
                val intent = MediaStoreUtil.appSpecialCropIntent(
                    this, FILE_PROVIDER_AUTHORITY, uri, cropFile,
                    cropParam.aspectX, cropParam.aspectY, cropParam.outputX, cropParam.outputY
                )
                cropUri = intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT)
                Timber.tag(TAG).i("create crop uri:%s", cropUri)
                cropPhotoLauncher.launch(intent)
            } catch (e: Exception) {
                Timber.tag(TAG).w(e)
                handleCropPhotoResult(null)
            }
        }
    }

    private fun handleCropPhotoResult(resultUri: Uri?) {
        if (resultUri != null) {
            //crop success
            onGetPhoto(resultUri)
        } else {
            //If it is not force to crop, then use photoUri. else if it is nul, judgment is failure
            val rawUri = if (cropMode == CROP_TRY) photoUri else null
            if (rawUri != null) {
                Timber.tag(TAG).w("crop failed and use rawUri instead")
                onGetPhoto(rawUri)
            } else {
                Timber.tag(TAG).w("crop failed")
                if (actionTake) {
                    //Prompt to take pictures failed
                    toast(R.string.take_failed)
                } else {
                    //Prompt selection failed
                    toast(R.string.choose_failed)
                }
            }
        }
    }

    /**
     * To take pictures
     */
    fun takePhoto(cropMode: Int) {
        this.actionTake = true
        this.cropMode = cropMode
        PermissionHelper.requestSystemCamera(this) {
            if (it) {
                // Call system camera
                val file = getTakePhotoFile()
                if (file == null) {//Generate file error, prompting failed to take pictures
                    Timber.tag(TAG).w("getTakePhotoFile null")
                    toast(R.string.take_failed)
                } else {
                    val intent = MediaStoreUtil.appSpecialCaptureIntent(this, FILE_PROVIDER_AUTHORITY, file)
                    photoUri = intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT)
                    takePhotoLauncher.launch(intent)
                }
            }
        }
    }

    /**
     * To choose Photo
     */
    fun chooseAlbum(cropMode: Int, mediaType: String) {
        this.actionTake = false
        this.cropMode = cropMode
        //Call the system to select a photo
        try {
            chooseLauncher.launch(SAFUtil.openDocumentIntent(mediaType))
        } catch (e: ActivityNotFoundException) {
            Timber.tag(TAG).w(e)
            try {
                chooseLauncher.launch(SAFUtil.getContentIntent(mediaType))
            } catch (e: ActivityNotFoundException) {
                Timber.tag(TAG).w(e)
                toast(R.string.choose_failed)
            }
        }
    }

    fun takeVideo(cropMode: Int) {
        this.actionTake = true
        this.cropMode = cropMode
        PermissionHelper.requestSystemCamera(this) {
            if (it) {
                takeVideoLauncher.launch(Intent(MediaStore.ACTION_VIDEO_CAPTURE))
            }
        }
    }

    abstract fun getTakePhotoFile(): File?
    abstract fun getCropPhotoFile(): File?
    abstract fun getCropPhotoParam(): CropParam?
    abstract fun onGetPhoto(uri: Uri)
    abstract fun onGetVideo(uri: Uri)

    companion object {
        private const val TAG = "GetPhotoVideoActivity"
        const val CROP_NONE = 0//no crop required
        const val CROP_TRY = 1//Try to crop, if not successful, return to the original uri
        const val CROP_MUST = 2//The crop must be successful
    }

}

/**
 * Crop params
 */
data class CropParam(
    /**
     * aspectX
     */
    val aspectX: Int,

    /**
     * aspectY
     */
    val aspectY: Int,

    /**
     * outputX
     */
    val outputX: Int,

    /**
     * outputY
     */
    val outputY: Int,
)