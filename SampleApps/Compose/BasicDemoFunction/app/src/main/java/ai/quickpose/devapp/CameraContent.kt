package ai.quickpose.devapp

import ai.quickpose.camera.QuickPoseCameraSwitchView
import ai.quickpose.core.Feature
import ai.quickpose.core.QuickPose
import ai.quickpose.core.RangeOfMotion
import ai.quickpose.core.Side
import ai.quickpose.core.Status
import ai.quickpose.devapp.theme.BasicDemoTheme
import android.app.Activity
import android.view.WindowManager
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

@Composable
fun CameraContent() {
    val context = LocalContext.current
    val useFrontCamera = remember { mutableStateOf(true) }
    val statusText = remember { mutableStateOf("Powered by QuickPose.ai") }
    val quickPose = remember {
        QuickPose(
            context,
            sdkKey = "YOUR SDK KEY HERE"
        ) // register for your free key at https://dev.quickpose.ai
    }
    var cameraSwitchView by remember { mutableStateOf<QuickPoseCameraSwitchView?>(null) }
    val cameraAspectRatio = remember { mutableStateOf<Float>(1.0f) }

    val lifecycleOwner = LocalLifecycleOwner.current

    //  Lifecycle aware startup for camera and pose detection
    DisposableEffect(lifecycleOwner) {
        val lifecycle = lifecycleOwner.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    (context as Activity).window.addFlags(
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    )
                    (context).window.setDecorFitsSystemWindows(false)
                }
                Lifecycle.Event.ON_PAUSE -> {
                    (context as Activity).window.clearFlags(
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    )
                    quickPose.stop()
                    cameraSwitchView?.stop()
                }
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    // UI
    BasicDemoTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(bottom = 60.dp)) {
                AndroidView(
                    factory = { ctx ->
                        cameraSwitchView = QuickPoseCameraSwitchView(ctx, quickPose)
                        lifecycleOwner.lifecycleScope.launch {
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
                                        (context as Activity).runOnUiThread {
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
                        lifecycleOwner.lifecycleScope.launch {
                            cameraAspectRatio.value = view.aspectRatio
                        }
                    },
                    modifier =
                    Modifier.matchParentSize()
                        .aspectRatio(cameraAspectRatio.value)
                )
                IconButton(
                    onClick = {
                        lifecycleOwner.lifecycleScope.launch {
                            useFrontCamera.value = !useFrontCamera.value
                            quickPose.stop()
                            cameraAspectRatio.value = cameraSwitchView?.start(useFrontCamera.value) ?: 1.0f;
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
            }
            Box(
                modifier = Modifier.fillMaxSize().padding(bottom = 64.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    text = statusText.value,
                    color = Color.Blue,
                    fontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
