package ai.quickpose.camera

import ai.quickpose.core.QuickPose
import android.content.Context
import android.util.AttributeSet
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
) : FrameLayout(context, attrs, defStyleAttr) {

    private var frontCameraView: QuickPoseCameraView? = null
    private var backCameraView: QuickPoseCameraView? = null
    private var frontOverlayView: SurfaceView? = null
    private var backOverlayView: SurfaceView? = null

    init {
        frontCameraView = QuickPoseCameraView(isFrontCamera = true, quickPose, context)
        addView(frontCameraView)

        backCameraView = QuickPoseCameraView(isFrontCamera = false, quickPose, context)
        backCameraView?.visibility = View.INVISIBLE
        addView(backCameraView)

        frontOverlayView = SurfaceView(context)
        addView(frontOverlayView)

        backOverlayView = SurfaceView(context)
        backOverlayView?.visibility = View.INVISIBLE
        addView(backOverlayView)
    }

    fun stop() {
        frontCameraView?.stop()
        backCameraView?.stop()
    }

    suspend fun start(useFrontCamera: Boolean) {
        if (useFrontCamera) {
            backCameraView?.stop()

            backOverlayView?.visibility = View.INVISIBLE
            backCameraView?.visibility = View.INVISIBLE

            frontCameraView?.visibility = View.VISIBLE
            frontOverlayView?.visibility = View.VISIBLE

            frontCameraView?.start()
            quickPose.setOverlayView(frontOverlayView)
        } else {
            frontCameraView?.stop()
            frontOverlayView?.visibility = View.INVISIBLE
            frontCameraView?.visibility = View.INVISIBLE

            backCameraView?.visibility = View.VISIBLE
            backOverlayView?.visibility = View.VISIBLE

            backCameraView?.start()
            quickPose.setOverlayView(backOverlayView)
        }
    }
}
