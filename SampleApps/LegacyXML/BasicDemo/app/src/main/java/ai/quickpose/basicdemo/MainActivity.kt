package ai.quickpose.basicdemo

import ai.quickpose.core.*
import ai.quickpose.core.Feature
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private var useFrontCamera: Boolean = true
    private var cameraButton: View? = null
    private var quickPose: QuickPose =
        QuickPose(this, sdkKey = "YOUR SDK KEY HERE") // register for your free key at https://dev.quickpose.ai
    private var cameraSwitchView: QuickPoseCameraSwitchView? = null
    private var statusTextView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!PermissionsHelper.checkAndRequestCameraPermissions(this)) {
            println("camera permission not granted")
        }

        setContentView(R.layout.activity_main)
        val viewGroup = findViewById<ViewGroup>(R.id.preview_display_layout)
        cameraSwitchView = QuickPoseCameraSwitchView(this, quickPose)
        viewGroup.addView(cameraSwitchView)

        cameraButton = findViewById(R.id.camera_button)
        cameraButton?.setOnClickListener {
            lifecycleScope.launch {
                useFrontCamera = !useFrontCamera
                quickPose.stop()
                cameraSwitchView?.startCamera(useFrontCamera)!!
                quickPose.resume()
            }
        }

        statusTextView = findViewById(R.id.status_text_view)
    }

    override fun onResume() {
        super.onResume()

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }

        lifecycleScope.launch {
            cameraSwitchView?.startCamera(useFrontCamera)!!
            quickPose.start(
                arrayOf(Feature.RangeOfMotion(RangeOfMotion.Shoulder(Side.LEFT, false))),
                onFrame = { status, featureResults, feedback, landmarks ->
                    println("$status, $featureResults")
                }
            )
        }
    }

    override fun onPause() {
        super.onPause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        quickPose.stop()
        cameraSwitchView?.stop()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (!PermissionsHelper.cameraPermissionsGranted(this)) {
            println("camera permission not granted")
            val noCameraAccessView = findViewById<View>(R.id.no_camera_access_view)
            noCameraAccessView.visibility = View.VISIBLE
        }
    }
}
