package ai.quickpose.devapp

import ai.quickpose.camera.QuickPoseCameraSwitchView
import ai.quickpose.core.Feature
import ai.quickpose.core.PermissionsHelper
import ai.quickpose.core.QuickPose
import ai.quickpose.core.RangeOfMotion
import ai.quickpose.core.Side
import ai.quickpose.core.Status
import ai.quickpose.devapp.theme.BasicDemoTheme
import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var useFrontCamera = mutableStateOf(true)
    private var statusText = mutableStateOf("Powered by QuickPose.ai")
    private var quickPose: QuickPose =
            QuickPose(
                    this,
                    sdkKey = "01GS5AG41TSVT9Y3CYD0EG9FX4"
            ) // register for your free key at https://dev.quickpose.ai
    private var cameraSwitchView: QuickPoseCameraSwitchView? = null
    private var hasPermissions = mutableStateOf(false)
    private var cameraAspectRatio = mutableStateOf<Float>(1.0f)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        WindowInsetsCompat.Type.navigationBars()

        hasPermissions.value = PermissionsHelper.checkAndRequestCameraPermissions(this)

        setContent {
            BasicDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                        if (hasPermissions.value) {
                            AndroidView(
                                factory = { ctx ->
                                    cameraSwitchView = QuickPoseCameraSwitchView(ctx, quickPose)
                                    lifecycleScope.launch {
                                        cameraAspectRatio.value = cameraSwitchView?.start(useFrontCamera.value) ?: 1.0f;
                                        quickPose.start(
                                            arrayOf(
                                                Feature.RangeOfMotion(
                                                    RangeOfMotion.Shoulder(Side.LEFT, false)
                                                )
                                            ),
                                            onFrame = { status, overlay, features, feedback, landmarks ->
                                                println("$status, $features")
                                                if (status is Status.Success) {
                                                    runOnUiThread {
                                                        statusText.value =
                                                            "Powered by QuickPose.ai v${quickPose.quickPoseVersion()}\n${status.fps} fps"
                                                    }
                                                }
                                            }
                                        )
                                    }
                                    cameraSwitchView!!
                                },
                                update = { view ->
                                    lifecycleScope.launch {
                                        cameraAspectRatio.value = view.aspectRatio
                                    }
                                },
                                modifier =
                                Modifier.matchParentSize()
                                    .aspectRatio(cameraAspectRatio.value)
                            )

                            IconButton(
                                    onClick = {
                                        lifecycleScope.launch {
                                            useFrontCamera.value = !useFrontCamera.value
                                            quickPose.stop()
                                            cameraSwitchView?.start(useFrontCamera.value)
                                            quickPose.resume()
                                        }
                                    },
                                    modifier = Modifier.align(Alignment.TopEnd).padding(32.dp)
                            ) {
                                Icon(
                                        imageVector = Icons.Default.Settings,
                                        tint = Color.White,
                                        contentDescription = "Switch Camera"
                                )
                            }
                        } else {
                            Text(
                                    text = "Camera permissions are required",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    modifier = Modifier.align(Alignment.Center),
                                    textAlign = TextAlign.Center
                            )
                        }

                        Box(
                                modifier = Modifier.fillMaxSize().padding(bottom = 64.dp),
                                contentAlignment = Alignment.BottomCenter
                        ) {
                            Text(
                                    text = statusText.value,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        hasPermissions.value = PermissionsHelper.checkAndRequestCameraPermissions(this)
        if (!hasPermissions.value) {
            return
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.setDecorFitsSystemWindows(false)
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.hide(android.view.WindowInsets.Type.navigationBars())

        lifecycleScope.launch {
            cameraSwitchView?.start(useFrontCamera.value)
            quickPose.start(
                    arrayOf(Feature.RangeOfMotion(RangeOfMotion.Shoulder(Side.LEFT, false))),
                    onFrame = { status, overlay, features, feedback, landmarks ->
                        println("$status, $features")
                        if (status is Status.Success) {
                            runOnUiThread {
                                statusText.value =
                                        "Powered by QuickPose.ai v${quickPose.quickPoseVersion()}\n${status.fps} fps"
                            }
                        }
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
}
