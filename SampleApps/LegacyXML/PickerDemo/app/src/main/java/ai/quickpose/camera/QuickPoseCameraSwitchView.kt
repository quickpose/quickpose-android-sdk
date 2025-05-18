package ai.quickpose.camera

import ai.quickpose.core.QuickPose
import android.content.Context
import android.util.AttributeSet
import android.util.Size
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout

class QuickPoseCameraSwitchView
@JvmOverloads
constructor(
        context: Context,
        private var quickPose: QuickPose,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        targetResolution: Size = Size(1080, 1920)
) : FrameLayout(context, attrs, defStyleAttr) {

    private var frontCameraView: QuickPoseCameraView? = null
    private var backCameraView: QuickPoseCameraView? = null
    private var frontOverlayView: SurfaceView? = null
    private var backOverlayView: SurfaceView? = null

    var aspectRatio = 1920f / 1080f

    init {
        frontCameraView =
                QuickPoseCameraView(isFrontCamera = true, quickPose, context, targetResolution)
        frontCameraView?.visibility = View.VISIBLE
        addView(frontCameraView)

        backCameraView =
                QuickPoseCameraView(isFrontCamera = false, quickPose, context, targetResolution)
        backCameraView?.visibility = View.INVISIBLE
        addView(backCameraView)

        frontOverlayView = SurfaceView(context)
        frontOverlayView?.visibility = View.VISIBLE
        addView(frontOverlayView)

        backOverlayView = SurfaceView(context)
        backOverlayView?.visibility = View.INVISIBLE
        addView(backOverlayView)
    }

    fun stop() {
        frontCameraView?.stop()
        backCameraView?.stop()
    }

    suspend fun start(useFrontCamera: Boolean): Float {
        if (useFrontCamera) {
            backCameraView?.stop()

            backOverlayView?.visibility = View.INVISIBLE
            backCameraView?.visibility = View.INVISIBLE

            frontCameraView?.visibility = View.VISIBLE
            frontOverlayView?.visibility = View.VISIBLE

            this.aspectRatio = frontCameraView?.start() ?: 1920f / 1080f
            quickPose.setOverlayView(frontOverlayView)
        } else {
            frontCameraView?.stop()
            frontOverlayView?.visibility = View.INVISIBLE
            frontCameraView?.visibility = View.INVISIBLE

            backCameraView?.visibility = View.VISIBLE
            backOverlayView?.visibility = View.VISIBLE

            this.aspectRatio = backCameraView?.start() ?: 1920f / 1080f
            quickPose.setOverlayView(backOverlayView)
        }
        return this.aspectRatio
    }
}
