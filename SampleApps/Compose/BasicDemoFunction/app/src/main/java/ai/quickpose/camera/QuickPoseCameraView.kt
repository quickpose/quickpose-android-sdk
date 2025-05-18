package ai.quickpose.camera

import ai.quickpose.core.QuickPose
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.hardware.camera2.params.StreamConfigurationMap
import android.opengl.GLES20
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Gravity
import android.view.Surface
import android.view.SurfaceView
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.google.mediapipe.glutil.EglManager
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.max
import kotlinx.coroutines.suspendCancellableCoroutine

class QuickPoseCameraView(
        private val isFrontCamera: Boolean,
        private val quickPose: QuickPose,
        private val context: Context,
        private val targetResolution: Size = Size(1080, 1920)
) : SurfaceView(context) {
        companion object {
                private val TAG = QuickPoseCameraView::class.java.simpleName
        }
        private var cameraThread: HandlerThread? = null
        private var cameraHandler: Handler? = null
        private var session: CameraCaptureSession? = null

        var camera: CameraDevice? = null
        var aspectRatio: Float = 1.0f

        suspend fun start(): Float {
                cameraThread = HandlerThread("CameraThread").apply { start() }
                cameraHandler = Handler(cameraThread!!.looper)

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
                this.aspectRatio = selectedCameraSize.width.toFloat() / selectedCameraSize.height.toFloat()
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
                val camera =
                        openCamera(
                                manager,
                                manager.cameraIdList[if (isFrontCamera) 1 else 0],
                                cameraHandler
                        )

                val cameraCharacteristics: CameraCharacteristics =
                        manager.getCameraCharacteristics(camera.id)
                val map: StreamConfigurationMap? =
                        cameraCharacteristics.get<StreamConfigurationMap>(
                                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
                        )
                val outputSizes = map?.getOutputSizes(SurfaceTexture::class.java)
                // outputSizes?.forEach { println(it) }

                val selectedCameraSize = chooseOptimalSize(outputSizes, targetResolution)

                val cameraViewSize = this.holder.getSurfaceFrame()
                val upscale =
                        max(
                                cameraViewSize.width().toFloat() /
                                        selectedCameraSize.width.toFloat(),
                                cameraViewSize.height().toFloat() /
                                        selectedCameraSize.height.toFloat()
                        )

                this.layoutParams =
                        FrameLayout.LayoutParams(
                                (selectedCameraSize.width * upscale).toInt(),
                                (selectedCameraSize.height * upscale).toInt(),
                                Gravity.CENTER
                        )
                this.camera = camera
                val mlTexture = createSurfaceTexture()
                val mlSurface = Surface(mlTexture)

                // val characteristics = manager.getCameraCharacteristics(camera.id)
                // val fpsRanges =
                //
                // characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)

                // // Log available FPS ranges
                // fpsRanges?.forEach { range ->
                //     Log.d(TAG, "Available FPS range: ${range.lower} - ${range.upper}")
                // }

                // // Choose the range with highest upper bound
                // val maxFpsRange = fpsRanges?.maxByOrNull { it.upper } ?: Range(20, 60)

                val captureRequest =
                        camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                                .apply {
                                        addTarget(this@QuickPoseCameraView.holder.surface)
                                        addTarget(mlSurface)
                                        // set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                                        // maxFpsRange)
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
                                                // created
                                                // earlier so that
                                                // the MediaPipe pipeline can attach it.
                                                // Only needed if MediaPipe pipeline doesn't provide
                                                // a
                                                // SurfaceTexture.
                                                mlTexture.detachFromGLContext()

                                                mainThreadExecutor.execute {
                                                        onComplete?.invoke(
                                                                mlTexture,
                                                                selectedCameraSize,
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

        /**
         * Opens the camera and returns the opened device (as the result of the suspend coroutine)
         */
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
        private fun chooseOptimalSize(choices: Array<Size>?, targetResolution: Size): Size {
                choices ?: return targetResolution

                val targetRatio = targetResolution.height.toFloat() / targetResolution.width
                val bestChoice =
                        choices
                                .filter { it.height.toFloat() / it.width == targetRatio }
                                .minByOrNull {
                                        Math.abs(it.width - targetResolution.width) +
                                                Math.abs(it.height - targetResolution.height)
                                }
                bestChoice?.let {
                        return bestChoice
                }
                return targetResolution
        }
        private fun createSurfaceTexture(): SurfaceTexture {
                //        // Create a temporary surface to make the context current.
                val eglManager = EglManager(null)
                val tempEglSurface = eglManager.createOffscreenSurface(1, 1)
                eglManager.makeCurrent(tempEglSurface, tempEglSurface)
                val textures = IntArray(1)
                GLES20.glGenTextures(1, textures, 0)
                return SurfaceTexture(textures[0])
        }

        fun stop() {
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
