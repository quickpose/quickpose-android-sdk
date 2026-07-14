package ai.quickpose.camera

import ai.quickpose.core.QuickPose
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.opengl.GLES20
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.content.ContextCompat
import com.google.mediapipe.glutil.EglManager
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine

class QuickPoseCameraView(
        private val isFrontCamera: Boolean,
        private val quickPose: QuickPose,
        private val context: Context,
        private var targetResolution: Size = Size(1920, 1080)
) : SurfaceView(context), SurfaceHolder.Callback {
        companion object {
                private val TAG = QuickPoseCameraView::class.java.simpleName
        }
        private var cameraThread: HandlerThread? = null
        private var cameraHandler: Handler? = null
        private var session: CameraCaptureSession? = null
        private val surfaceReadyDeferred = CompletableDeferred<Surface>()

        var camera: CameraDevice? = null
        var aspectRatio: Float = 0f
        private var ratioWidth = 0
        private var ratioHeight = 0

        init {
                holder.addCallback(this)
        }

        override fun surfaceCreated(holder: SurfaceHolder) {
                Log.d(TAG, "Surface created")
                surfaceReadyDeferred.complete(holder.surface)
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                Log.d(TAG, "Surface changed: format=$format, width=$width, height=$height")
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
                Log.d(TAG, "Surface destroyed")
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec)
                val width = MeasureSpec.getSize(widthMeasureSpec)
                val height = MeasureSpec.getSize(heightMeasureSpec)
                if (ratioWidth == 0 || ratioHeight == 0) {
                        setMeasuredDimension(width, height)
                } else {
                        // Cover: scale to fill entire parent, cropping excess
                        val cameraAspect = ratioWidth.toFloat() / ratioHeight.toFloat()
                        val fitHeight = (width * cameraAspect).roundToInt()
                        if (fitHeight >= height) {
                                setMeasuredDimension(width, fitHeight)
                        } else {
                                val fitWidth = (height / cameraAspect).roundToInt()
                                setMeasuredDimension(fitWidth, height)
                        }
                }
        }

        suspend fun start(): Float {
                if (camera != null) return aspectRatio
                cameraThread = HandlerThread("CameraThread").apply { start() }
                cameraHandler = Handler(cameraThread!!.looper)

                Log.d(TAG, "Waiting for surface to be ready...")
                holder.surface.let {
                        if (it != null && it.isValid) {
                                surfaceReadyDeferred.complete(it)
                        }
                }
                surfaceReadyDeferred.await()
                Log.d(TAG, "Surface is ready.")

                val selectedCameraSize =
                        startCamera(
                                onComplete = { inputTexture, cameraFrameSize, upscale ->
                                        quickPose.onCameraStarted(
                                                isFrontCamera,
                                                cameraFrameSize,
                                                upscale
                                        )
                                        inputTexture!!.setOnFrameAvailableListener(quickPose)
                                }
                        )
                this.aspectRatio =
                        selectedCameraSize.width.toFloat() / selectedCameraSize.height.toFloat()
                return aspectRatio
        }

        private suspend fun startCamera(
                onComplete:
                        ((
                                surfaceTexture: SurfaceTexture?,
                                cameraFrameSize: Size,
                                upscale: Float) -> Unit)?
        ): Size {
                val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                val cameraId = manager.cameraIdList[if (isFrontCamera) 1 else 0]
                val characteristics = manager.getCameraCharacteristics(cameraId)

                val selectedCameraSize = getPreviewOutputSize(
                        this.width, this.height, characteristics, SurfaceTexture::class.java
                )
                Log.d(TAG, "View size: ${this.width}x${this.height}, selected camera size: $selectedCameraSize")

                targetResolution = selectedCameraSize

                // Set surface buffer to match camera output exactly (avoids scaling distortion)
                holder.setFixedSize(selectedCameraSize.width, selectedCameraSize.height)

                // Set aspect ratio for onMeasure to size the view correctly in portrait
                ratioWidth = selectedCameraSize.width
                ratioHeight = selectedCameraSize.height
                requestLayout()

                val camera = openCamera(manager, cameraId, cameraHandler)
                this.camera = camera

                val mlTexture = createSurfaceTexture()
                val mlSurface = Surface(mlTexture)

                val captureRequest =
                        camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                                .apply {
                                        addTarget(this@QuickPoseCameraView.holder.surface)
                                        addTarget(mlSurface)
                                }
                                .build()

                val mainThreadExecutor = ContextCompat.getMainExecutor(this.context)
                session = suspendCoroutine { cont ->
                        val captureSessionCallback =
                                object : CameraCaptureSession.StateCallback() {

                                        override fun onConfigured(session: CameraCaptureSession) {
                                                cont.resume(session)

                                                session.setRepeatingRequest(
                                                        captureRequest,
                                                        null,
                                                        cameraHandler
                                                )

                                                // Detach the SurfaceTexture from the GL context we
                                                // created earlier so that the MediaPipe pipeline can
                                                // attach it.
                                                mlTexture.detachFromGLContext()

                                                mainThreadExecutor.execute {
                                                        val upscale = max(
                                                                width.toFloat() / selectedCameraSize.height.toFloat(),
                                                                height.toFloat() / selectedCameraSize.width.toFloat()
                                                        )
                                                        Log.d(TAG, "View after layout: ${width}x${height}, upscale: $upscale")
                                                        onComplete?.invoke(
                                                                mlTexture,
                                                                Size(selectedCameraSize.height, selectedCameraSize.width),
                                                                upscale
                                                        )
                                                }
                                        }

                                        override fun onConfigureFailed(
                                                session: CameraCaptureSession
                                        ) {
                                                val exc =
                                                        RuntimeException(
                                                                "Camera ${camera.id} session configuration failed"
                                                        )
                                                Log.e(QuickPoseCameraView.TAG, exc.message, exc)
                                                cont.resumeWithException(exc)
                                        }
                                }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {

                                val targets =
                                        listOf(
                                                OutputConfiguration(this.holder.surface),
                                                OutputConfiguration(mlSurface)
                                        )

                                val config =
                                        SessionConfiguration(
                                                SessionConfiguration.SESSION_REGULAR,
                                                targets,
                                                mainThreadExecutor,
                                                captureSessionCallback
                                        )

                                // API 28 and above
                                camera.createCaptureSession(config)
                        } else {
                                // API 26-27
                                val surfaces = listOf(this.holder.surface, mlSurface)
                                val mainHandler = Handler(context.mainLooper)
                                camera.createCaptureSession(
                                        surfaces,
                                        captureSessionCallback,
                                        mainHandler
                                )
                        }
                }

                return selectedCameraSize
        }

        /** Opens the camera and returns the opened device (as the result of the suspend coroutine) */
        @SuppressLint("MissingPermission")
        private suspend fun openCamera(
                manager: CameraManager,
                cameraId: String,
                handler: Handler? = null,
        ): CameraDevice = suspendCancellableCoroutine { cont ->
                manager.openCamera(
                        cameraId,
                        object : CameraDevice.StateCallback() {
                                override fun onOpened(device: CameraDevice) = cont.resume(device)

                                override fun onDisconnected(device: CameraDevice) {
                                        Log.w(
                                                QuickPoseCameraView.TAG,
                                                "Camera $cameraId has been disconnected"
                                        )
                                }

                                override fun onError(device: CameraDevice, error: Int) {
                                        val msg =
                                                when (error) {
                                                        ERROR_CAMERA_DEVICE -> "Fatal (device)"
                                                        ERROR_CAMERA_DISABLED -> "Device policy"
                                                        ERROR_CAMERA_IN_USE -> "Camera in use"
                                                        ERROR_CAMERA_SERVICE -> "Fatal (service)"
                                                        ERROR_MAX_CAMERAS_IN_USE ->
                                                                "Maximum cameras in use"
                                                        else -> "Unknown"
                                                }
                                        val exc =
                                                RuntimeException(
                                                        "Camera $cameraId error: ($error) $msg"
                                                )
                                        Log.e(QuickPoseCameraView.TAG, exc.message, exc)
                                        if (cont.isActive) cont.resumeWithException(exc)
                                }
                        },
                        handler
                )
        }

        private fun createSurfaceTexture(): SurfaceTexture {
                val eglManager = EglManager(null)
                val tempEglSurface = eglManager.createOffscreenSurface(1, 1)
                eglManager.makeCurrent(tempEglSurface, tempEglSurface)
                val textures = IntArray(1)
                GLES20.glGenTextures(1, textures, 0)
                return SurfaceTexture(textures[0])
        }

        fun stop() {
                if (camera == null) return
                Log.w(QuickPoseCameraView.TAG, "Stopping")
                cameraThread?.quit()
                cameraThread = null
                cameraHandler = null

                if (camera != null) {
                        try {
                                camera?.close()
                                camera = null
                        } catch (e: Exception) {
                                Log.w(QuickPoseCameraView.TAG, "Error closing camera", e)
                        }
                }
                session?.close()
                session = null
        }
}
