package ai.quickpose.devapp

import ai.quickpose.devapp.theme.BasicDemoTheme
import ai.quickpose.core.Feature
import ai.quickpose.core.PermissionsHelper
import ai.quickpose.core.QuickPose
import ai.quickpose.core.QuickPoseCameraSwitchView
import ai.quickpose.core.RangeOfMotion
import ai.quickpose.core.Side
import ai.quickpose.core.Status
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge


import androidx.compose.foundation.layout.Box
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

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
        QuickPose(this, sdkKey = "YOUR SDK KEY HERE") // register for your free key at https://dev.quickpose.ai
    private var cameraSwitchView: QuickPoseCameraSwitchView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        WindowInsetsCompat.Type.navigationBars()
        if (!PermissionsHelper.checkAndRequestCameraPermissions(this)) {
            println("camera permission not granted")
        }


        setContent {
            BasicDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                        AndroidView(
                            factory = { context ->
                                cameraSwitchView = QuickPoseCameraSwitchView(context, quickPose)
                                lifecycleScope.launch {
                                    cameraSwitchView?.startCamera(useFrontCamera.value)
                                }
                                cameraSwitchView!!
                            })

                        IconButton(
                            onClick = {
                                lifecycleScope.launch {
                                    useFrontCamera.value = !useFrontCamera.value
                                    quickPose.stop()
                                    cameraSwitchView?.startCamera(useFrontCamera.value)
                                    quickPose.resume()
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd) // Position it in the top-right corner
                                .padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                tint = Color.White,
                                contentDescription = "Switch Camera"
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 64.dp),
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

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.setDecorFitsSystemWindows(false)
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.hide(android.view.WindowInsets.Type.navigationBars())


        lifecycleScope.launch {
            cameraSwitchView?.startCamera(useFrontCamera.value)
            quickPose.start(
                arrayOf(Feature.RangeOfMotion(RangeOfMotion.Shoulder(Side.LEFT, false))),
                onFrame = { status, featureResults, feedback, landmarks ->
                    println("$status, $featureResults")
                    if (status is Status.Success) {
                        statusText.value =
                            "Powered by QuickPose.ai v${quickPose.quickPoseVersion()}\n${status.fps} fps"
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