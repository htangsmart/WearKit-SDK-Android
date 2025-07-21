package com.topstep.wearkit.sample.ui.camera

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.window.layout.WindowMetricsCalculator
import com.github.kilnn.tool.storage.FileUtil
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.shenju.cameracapturer.FrameData
import com.shenju.cameracapturer.OSIJni
import com.topstep.wearkit.apis.WKWearKit
import com.topstep.wearkit.apis.model.message.WKCameraMessage
import com.topstep.wearkit.base.utils.BytesUtil
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityCameraBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import timber.log.Timber
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.nio.ByteBuffer
import java.util.ArrayDeque
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Helper type alias used for analysis use case callbacks */
typealias LumaListener = (luma: Double) -> Unit

@SuppressLint("CheckResult")
class CameraActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityCameraBinding

    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService

    private val displayManager by lazy {
        getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }

    /**
     * We need a display listener for orientation changes that do not trigger a configuration
     * change, for example if we choose to override config change in manifest or for 180-degree
     * orientation changes.
     */
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = viewBind.viewFinder.let { view ->
            if (displayId == this@CameraActivity.displayId) {
                imageCapture?.targetRotation = view.display.rotation
                imageAnalyzer?.targetRotation = view.display.rotation
            }
        }
    }

    private var displayId: Int = -1
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null

    // Preview
    private var isSupportPreview = false
    private val osiJni: OSIJni = OSIJni()

    private fun dirDownload(context: Context): File? {
        val dir = ContextCompat.getExternalFilesDirs(context, Environment.DIRECTORY_DOWNLOADS).firstOrNull() ?: return null
        if (!dir.exists() && !dir.mkdirs()) {
            return null
        }
        return dir
    }

    private fun getVideoFile(context: Context): File? {
        val dir = dirDownload(context)
        if (dir == null) return null
        return File(dir, "test.h264")
    }

    private val file = getVideoFile(MyApplication.instance)!!
    private val fileWriter = if (TEST_MODE == 1) BufferedWriter(FileWriter(file)) else null
    private val fileReader = if (TEST_MODE == 0) FileCycleReader(file) else if (TEST_MODE == 2) FileFixReader(MyApplication.instance) else null

    override fun onPause() {
        super.onPause()
        viewBind.countDownView.cancelCountDown()
    }

    private var observeCameraDisposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.ds_camera_control)

        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Every time the orientation of device changes, update rotation for use cases
        displayManager.registerDisplayListener(displayListener, null)

        // Wait for the views to be properly laid out
        viewBind.viewFinder.post {
            // Keep track of the display in which this view is attached
            displayId = viewBind.viewFinder.display.displayId

            // Build UI controls
            updateCameraUi()

            // Set up the camera and its use cases
            setUpCamera()
        }

        observeCameraDisposable = wearKit.cameraAbility.observeCameraMessage()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ type ->
                when (type) {
                    WKCameraMessage.CLOSE -> {
                        finish()
                    }

                    WKCameraMessage.TAKE_PHOTO -> {
                        viewBind.btnShutter.simulateClick()
                    }

                    WKCameraMessage.CAMERA_BACK -> {
                        if (hasBackCamera()) {
                            viewBind.imgFacing.setImageResource(R.drawable.ic_baseline_camera_rear_24)
                            lensFacing = CameraSelector.LENS_FACING_BACK
                            bindCameraUseCases()
                        }
                    }

                    WKCameraMessage.CAMERA_FRONT -> {
                        if (hasFrontCamera()) {
                            viewBind.imgFacing.setImageResource(R.drawable.ic_baseline_camera_front_24)
                            lensFacing = CameraSelector.LENS_FACING_FRONT
                            bindCameraUseCases()
                        }
                    }

                    WKCameraMessage.FLASH_OFF -> {
                        imageCapture?.let {
                            it.flashMode = ImageCapture.FLASH_MODE_OFF
                            viewBind.imgFlash.setImageResource(R.drawable.ic_baseline_flash_off_24)
                        }
                    }

                    WKCameraMessage.FLASH_AUTO -> {
                        imageCapture?.let {
                            it.flashMode = ImageCapture.FLASH_MODE_AUTO
                            viewBind.imgFlash.setImageResource(R.drawable.ic_baseline_flash_auto_24)
                        }
                    }

                    WKCameraMessage.FLASH_ON -> {
                        imageCapture?.let {
                            it.flashMode = ImageCapture.FLASH_MODE_ON
                            viewBind.imgFlash.setImageResource(R.drawable.ic_baseline_flash_on_24)
                        }
                    }
                }
            }, {
                Timber.w(it)
            })

        setPhotographMode(true)

        //preview
        isSupportPreview = wearKit.cameraAbility.compat.isSupportPreview()
        if (isSupportPreview) {
            val size = wearKit.cameraAbility.compat.getPreviewSize()
            osiJni.initEncoder(size.x, size.y, 350, PREVIEW_FPS, 1)
            wearKit.cameraAbility.startPreview(PREVIEW_FPS).onErrorComplete()
                .doOnError {
                    Timber.w(it)
                }
                .subscribe()
        }
    }

    private fun setPhotographMode(mode: Boolean) {
        wearKit.cameraAbility.setCameraStatus(mode)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
            }, {
                Timber.w(it)
            })
    }

    /**
     * Inflate camera controls and update the UI manually upon config changes to avoid removing
     * and re-adding the view finder from the view hierarchy; this provides a seamless rotation
     * transition on devices that support it.
     *
     * NOTE: The flag is supported starting in Android 8 but there still is a small flash on the
     * screen for devices that run Android 9 or below.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Rebind the camera with the updated display metrics
        bindCameraUseCases()

        // Enable or disable switching between cameras
        updateCameraSwitchButton()
    }

    /** Initialize CameraX, and prepare to bind the camera use cases  */
    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            // CameraProvider
            cameraProvider = cameraProviderFuture.get()

            // Select lensFacing depending on the available cameras
            lensFacing = when {
                hasBackCamera() -> {
                    viewBind.imgFacing.setImageResource(R.drawable.ic_baseline_camera_rear_24)
                    CameraSelector.LENS_FACING_BACK
                }
                hasFrontCamera() -> {
                    viewBind.imgFacing.setImageResource(R.drawable.ic_baseline_camera_front_24)
                    CameraSelector.LENS_FACING_FRONT
                }
                else -> throw IllegalStateException("Back and front camera are unavailable")
            }

            // Enable or disable switching between cameras
            updateCameraSwitchButton()

            // Build and bind the camera use cases
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    /** Declare and bind preview, capture and analysis use cases */
    private fun bindCameraUseCases() {

        // Get screen metrics used to setup camera for full screen resolution
        val metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(this).bounds
        Timber.tag(TAG).d("Screen metrics: ${metrics.width()} x ${metrics.height()}")

        val screenAspectRatio = aspectRatio(metrics.width(), metrics.height())
        Timber.tag(TAG).d("Preview aspect ratio: $screenAspectRatio")

        val rotation = viewBind.viewFinder.display.rotation

        // CameraProvider
        val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        // CameraSelector
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        // Preview
        preview = Preview.Builder()
            // We request aspect ratio but no resolution
            .setTargetAspectRatio(screenAspectRatio)
            // Set initial target rotation
            .setTargetRotation(rotation)
            .build()

        // ImageCapture
        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            // We request aspect ratio but no resolution to match preview config, but letting
            // CameraX optimize for whatever specific resolution best fits our use cases
            .setTargetAspectRatio(screenAspectRatio)
            // Set initial target rotation, we will have to call this again if rotation changes
            // during the lifecycle of this use case
            .setTargetRotation(rotation)
            .build().also { this.imageCapture = it }

        when (imageCapture.flashMode) {
            ImageCapture.FLASH_MODE_AUTO -> viewBind.imgFlash.setImageResource(R.drawable.ic_baseline_flash_auto_24)
            ImageCapture.FLASH_MODE_ON -> viewBind.imgFlash.setImageResource(R.drawable.ic_baseline_flash_on_24)
            ImageCapture.FLASH_MODE_OFF -> viewBind.imgFlash.setImageResource(R.drawable.ic_baseline_flash_off_24)
            else -> viewBind.imgFlash.isEnabled = false
        }

        // ImageAnalysis
        imageAnalyzer = ImageAnalysis.Builder()
            // We request aspect ratio but no resolution
            .setTargetAspectRatio(screenAspectRatio)
            // Set initial target rotation, we will have to call this again if rotation changes
            // during the lifecycle of this use case
            .setTargetRotation(rotation)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            // The analyzer can then be assigned to the instance
            .also {
                it.setAnalyzer(cameraExecutor, LuminosityAnalyzer(wearKit, osiJni, lensFacing == CameraSelector.LENS_FACING_FRONT, isSupportPreview, fileWriter, fileReader))
            }

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        camera?.let {
            // Must remove observers from the previous camera instance
            removeCameraStateObservers(it.cameraInfo)
        }

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture, imageAnalyzer
            )

            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(viewBind.viewFinder.surfaceProvider)
            camera?.let {
                observeCameraState(it.cameraInfo)
            }
        } catch (exc: Exception) {
            Timber.tag(TAG).w(exc, "Use case binding failed")
        }
    }

    private fun removeCameraStateObservers(cameraInfo: CameraInfo) {
        cameraInfo.cameraState.removeObservers(this)
    }

    private fun observeCameraState(cameraInfo: CameraInfo) {
        cameraInfo.cameraState.observe(this) { cameraState ->
            Timber.tag(TAG).i("cameraState:%s", cameraState.type)
            cameraState.error?.let { _ ->
                // promptToast.showFailed("Camera error:${error.code}")
            }
        }
    }

    /**
     *  [androidx.camera.core.ImageAnalysis.Builder] requires enum value of
     *  [androidx.camera.core.AspectRatio]. Currently it has values of 4:3 & 16:9.
     *
     *  Detecting the most suitable ratio for dimensions provided in @params by counting absolute
     *  of preview ratio to one of the provided values.
     *
     *  @param width - preview width
     *  @param height - preview height
     *  @return suitable aspect ratio
     */
    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    /** Method used to re-draw the camera UI controls, called every time configuration changes. */
    private fun updateCameraUi() {
        // Listener for button used to capture photo
        viewBind.btnShutter.clickTrigger {
            prepareShutter()
        }

        // Setup for button used to switch cameras

        viewBind.imgFacing.isEnabled = false// Disable the button until the camera is set up
        viewBind.imgFacing.clickTrigger {
            lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
                wearKit.cameraAbility.sendCameraMessage(WKCameraMessage.CAMERA_BACK).onErrorComplete().subscribe()
                viewBind.imgFacing.setImageResource(R.drawable.ic_baseline_camera_rear_24)
                CameraSelector.LENS_FACING_BACK
            } else {
                wearKit.cameraAbility.sendCameraMessage(WKCameraMessage.CAMERA_FRONT).onErrorComplete().subscribe()
                viewBind.imgFacing.setImageResource(R.drawable.ic_baseline_camera_front_24)
                CameraSelector.LENS_FACING_FRONT
            }

            // Re-bind use cases to update selected camera
            bindCameraUseCases()
        }

        viewBind.imgFlash.clickTrigger {
            imageCapture?.let {
                when (it.flashMode) {
                    ImageCapture.FLASH_MODE_OFF -> {
                        //off 切换为 auto
                        wearKit.cameraAbility.sendCameraMessage(WKCameraMessage.FLASH_AUTO).onErrorComplete().subscribe()
                        it.flashMode = ImageCapture.FLASH_MODE_AUTO
                        viewBind.imgFlash.setImageResource(R.drawable.ic_baseline_flash_auto_24)
                    }
                    ImageCapture.FLASH_MODE_AUTO -> {
                        //auto 切换为 on
                        wearKit.cameraAbility.sendCameraMessage(WKCameraMessage.FLASH_ON).onErrorComplete().subscribe()
                        it.flashMode = ImageCapture.FLASH_MODE_ON
                        viewBind.imgFlash.setImageResource(R.drawable.ic_baseline_flash_on_24)
                    }
                    ImageCapture.FLASH_MODE_ON -> {
                        // on 切换为 off
                        wearKit.cameraAbility.sendCameraMessage(WKCameraMessage.FLASH_OFF).onErrorComplete().subscribe()
                        it.flashMode = ImageCapture.FLASH_MODE_OFF
                        viewBind.imgFlash.setImageResource(R.drawable.ic_baseline_flash_off_24)
                    }
                    else -> {}
                }
            }
        }

        viewBind.imgFile.clickTrigger {
            try {
                val intent = IntentCompat.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_GALLERY)
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                startActivity(intent)
            } catch (e: Exception) {
                Timber.tag(TAG).w(e)
                try {
                    //打开系统gallery
                    //https://stackoverflow.com/questions/19436366/android-open-gallery-app
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("content://media/internal/images/media"))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    startActivity(intent)
                } catch (e2: Exception) {
                    Timber.tag(TAG).w(e2)
                }
            }
        }

        viewBind.countDownView.setCountDownFinishedListener { shutter() }
    }

    private fun prepareShutter() {
        if (imageCapture == null) return
        if (viewBind.countDownView.isCountingDown) {
            viewBind.countDownView.cancelCountDown()
        }
        viewBind.countDownView.startCountDown(3, true)
    }

    private fun shutter() {
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            return
        }
        val imageCapture = this.imageCapture ?: return

        // Setup image capture metadata
        val metadata = ImageCapture.Metadata().apply {
            // Mirror image when using the front camera
            isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
        }

        // Create output options object which contains file + metadata
        val contentValues = makePublicContentValues(this)
        if (contentValues == null) {
            toast(R.string.take_failed)
            return
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).setMetadata(metadata).build()

        // Setup image capture listener which is triggered after photo has been taken
        imageCapture.takePicture(outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                Timber.tag(TAG).e(exc, "Photo capture failed: ${exc.message}")
                lifecycleScope.launchWhenStarted {
                    toast(R.string.take_failed)
                }
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                wearKit.cameraAbility.sendCameraMessage(WKCameraMessage.TAKE_PHOTO).onErrorComplete().subscribe()

                viewBind.countDownView.playBeepShutter()
                viewBind.root.displayFlashAnim()

                val savedUri = output.savedUri
                Timber.tag(TAG).i("Photo capture succeeded: $savedUri")
                lifecycleScope.launchWhenStarted {
                    toast(R.string.take_success)
                }
            }
        })
    }

    /** Enabled or disabled a button to switch cameras depending on the available cameras */
    private fun updateCameraSwitchButton() {
        try {
            viewBind.imgFacing.isEnabled = hasBackCamera() && hasFrontCamera()
        } catch (exception: CameraInfoUnavailableException) {
            viewBind.imgFacing.isEnabled = false
        }
    }

    /** Returns true if the device has an available back camera. False otherwise */
    private fun hasBackCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
    }

    /** Returns true if the device has an available front camera. False otherwise */
    private fun hasFrontCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
    }

    /**
     * Our custom image analysis class.
     *
     * <p>All we need to do is override the function `analyze` with our desired operations. Here,
     * we compute the average luminosity of the image by looking at the Y plane of the YUV frame.
     */
    private class LuminosityAnalyzer(
        private val wearKit: WKWearKit,
        private val osiJni: OSIJni,
        private val isFront: Boolean,
        private val isSupportPreview: Boolean,
        private val fileWriter: BufferedWriter?,
        private val fileReader: FileTestReader?,
        listener: LumaListener? = null,
    ) : ImageAnalysis.Analyzer {
        private val frameRateWindow = 8
        private val frameTimestamps = ArrayDeque<Long>(5)
        private val listeners = ArrayList<LumaListener>().apply { listener?.let { add(it) } }
        private var lastAnalyzedTimestamp = 0L
        var framesPerSecond: Double = -1.0
            private set

        private var sumSize = 0
        private val startTimestamp = System.currentTimeMillis()

        /**
         * Used to add listeners that will be called with each luma computed
         */
        fun onFrameAnalyzed(listener: LumaListener) = listeners.add(listener)

        /**
         * Helper extension function used to extract a byte array from an image plane buffer
         */
        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }

        /**
         * Analyzes an image to produce a result.
         *
         * <p>The caller is responsible for ensuring this analysis method can be executed quickly
         * enough to prevent stalls in the image acquisition pipeline. Otherwise, newly available
         * images will not be acquired and analyzed.
         *
         * <p>The image passed to this method becomes invalid after this method returns. The caller
         * should not store external references to this image, as these references will become
         * invalid.
         *
         * @param image image being analyzed VERY IMPORTANT: Analyzer method implementation must
         * call image.close() on received images when finished using them. Otherwise, new images
         * may not be received or the camera may stall, depending on back pressure setting.
         *
         */
        override fun analyze(image: ImageProxy) {
            if (isSupportPreview) {
                sendPreviewToWatch(image)
            }

            // If there are no listeners attached, we don't need to perform analysis
            if (listeners.isEmpty()) {
                image.close()
                return
            }

            // Keep track of frames analyzed
            val currentTime = System.currentTimeMillis()
            frameTimestamps.push(currentTime)

            // Compute the FPS using a moving average
            while (frameTimestamps.size >= frameRateWindow) frameTimestamps.removeLast()
            val timestampFirst = frameTimestamps.peekFirst() ?: currentTime
            val timestampLast = frameTimestamps.peekLast() ?: currentTime
            framesPerSecond = 1.0 / ((timestampFirst - timestampLast) /
                    frameTimestamps.size.coerceAtLeast(1).toDouble()) * 1000.0

            // Analysis could take an arbitrarily long amount of time
            // Since we are running in a different thread, it won't stall other use cases

            lastAnalyzedTimestamp = frameTimestamps.first

            // Since format in ImageAnalysis is YUV, image.planes[0] contains the luminance plane
            val buffer = image.planes[0].buffer

            // Extract image data from callback object
            val data = buffer.toByteArray()

            // Convert the data into an array of pixel values ranging 0-255
            val pixels = data.map { it.toInt() and 0xFF }

            // Compute average luminance for the image
            val luma = pixels.average()

            // Call all listeners with new value
            listeners.forEach { it(luma) }

            image.close()
        }

        private var lastPreviewSentTimestamp = 0L

        private fun sendPreviewToWatch(image: ImageProxy) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastPreviewSentTimestamp < 1000 / PREVIEW_FPS) {
                return
            }
            lastPreviewSentTimestamp = currentTime

            if ((TEST_MODE == 0 || TEST_MODE == 2) && fileReader != null) {
                val frameType = fileReader.readLine().also {
                    Timber.w("file read:%s", it)
                }?.toIntOrNull()
                if (frameType != null) {
                    val str = fileReader.readLine().also {
                        Timber.w("file read:%s", it)
                    }
                    if (!str.isNullOrEmpty()) {
                        val frameData = BytesUtil.hexStr2Bytes(str)
                        if (frameData != null) {
                            wearKit.cameraAbility.updatePreview(frameType, frameData).onErrorComplete().doOnError { Timber.w(it) }.subscribe()
                        }
                    }
                }
                return
            }

            val planes = image.planes
            val yPlane = planes[0]
            val uPlane = planes[1]
            val vPlane = planes[2]

            val yBytes: ByteArray = YuvUtil.extractPlaneData(yPlane, image.width, image.height)
            val uBytes: ByteArray
            val vBytes: ByteArray?

            if (uPlane.pixelStride == 2) { // UV数据交错
                uBytes = YuvUtil.extractUVPlaneData(uPlane, image.width / 2, image.height / 2) // 注意宽度和高度
                vBytes = null
            } else {
                uBytes = YuvUtil.extractPlaneData(uPlane, image.width / 2, image.height / 2) // 注意宽度和高度
                vBytes = YuvUtil.extractPlaneData(vPlane, image.width / 2, image.height / 2) // 注意宽度和高度
            }

            synchronized(osiJni) {
                val h264Data = FrameData()
                val ret = osiJni.runEncoder(
                    yBytes, uBytes, vBytes, image.width, image.height,
                    image.imageInfo.rotationDegrees, h264Data, isFront
                )
                if (ret == 0) {
                    sumSize += h264Data.frameData.size
                    val useTime = System.currentTimeMillis() - startTimestamp
                    //计算传输速度
                    if (useTime > 0) {
                        val speed = (sumSize / 1024.0) / (useTime / 1000.0)
                        Timber.e("speed:$speed kb/s")
                    }
                    wearKit.cameraAbility.updatePreview(h264Data.frameType, h264Data.frameData).onErrorComplete().doOnError { Timber.w(it) }.subscribe()
                    if (TEST_MODE == 1 && fileWriter != null) {
                        fileWriter.write(
                            StringBuilder().append(h264Data.frameType).append("\n").toString()
                                .also {
                                    Timber.w("file write:%s", it)
                                })
                        fileWriter.write(
                            StringBuilder().append(BytesUtil.internalBytes2HexStr(h264Data.frameData)).append("\n").toString()
                                .also {
                                    Timber.w("file write:%s", it)
                                })
                    }
                }
            }

        }

    }

    companion object {
        private const val TAG = "Camera"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0

        private const val PREVIEW_FPS = 30
        val TEST_MODE: Int? = null//null 正常模式，0读模式，1写模式，2读assets模式

        fun makePublicContentValues(context: Context): ContentValues? {
            val contentValues = ContentValues()
            val appName = context.getString(R.string.app_name).replace(" ", "")
            var dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), appName)
            if (!dir.exists() && !dir.mkdirs()) {
                Timber.tag(TAG).w("dir create fail 1:%s", dir.absolutePath)
                dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                if (!dir.exists() && !dir.mkdirs()) {
                    Timber.tag(TAG).w("dir create fail 2:%s", dir.absolutePath)
                    return null
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, FileUtil.generateFileName())
                contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/$appName")
            } else {
                val filename = FileUtil.generateImageFileName()
                contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                val file = File(dir, filename)
                contentValues.put(MediaStore.Images.Media.DATA, file.absolutePath)
            }
            return contentValues
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        observeCameraDisposable?.dispose()
        setPhotographMode(false)
        cameraExecutor.shutdown()
        displayManager.unregisterDisplayListener(displayListener)
        synchronized(osiJni) {
            osiJni.closeEncoder()
        }
        runCatching { fileWriter?.close() }
        runCatching { fileReader?.close() }
    }

}