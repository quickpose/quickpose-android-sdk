package ai.quickpose.devapp

import ai.quickpose.camera.QuickPoseCameraSwitchView
import ai.quickpose.core.Feature
import ai.quickpose.core.QuickPose
import ai.quickpose.core.RangeOfMotion
import ai.quickpose.core.Side
import ai.quickpose.core.Status
import ai.quickpose.devapp.theme.BasicDemoTheme
import android.app.Activity
import android.content.Intent
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
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
    var overlaySurfaceView by remember { mutableStateOf<SurfaceView?>(null) }
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
        Box(modifier = Modifier.fillMaxSize()) {
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
                                overlaySurfaceView = overlay
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
                modifier = Modifier.fillMaxSize()
            )
            // Share screenshot
            IconButton(
                onClick = {
                    val csv = cameraSwitchView ?: return@IconButton
                    val overlay = overlaySurfaceView
                    var cameraSV: SurfaceView? = null
                    for (i in 0 until csv.childCount) {
                        val child = csv.getChildAt(i)
                        if (child is SurfaceView && child.visibility == View.VISIBLE && child !== overlay) {
                            cameraSV = child; break
                        }
                    }
                    if (cameraSV == null) return@IconButton
                    QuickPose.captureFrame(cameraSV, overlay) { bitmap ->
                        if (bitmap != null) {
                            val file = File(context.cacheDir, "quickpose_screenshot_${System.currentTimeMillis()}.jpg")
                            FileOutputStream(file).use { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, it) }
                            bitmap.recycle()
                            (context as Activity).runOnUiThread {
                                try {
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/jpeg"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Screenshot"))
                                } catch (_: Exception) {}
                            }
                        }
                    }
                },
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 48.dp, end = 64.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    tint = Color.White,
                    contentDescription = "Share Screenshot"
                )
            }
            // Switch camera
            IconButton(
                onClick = {
                    lifecycleOwner.lifecycleScope.launch {
                        useFrontCamera.value = !useFrontCamera.value
                        quickPose.stop()
                        cameraAspectRatio.value = cameraSwitchView?.start(useFrontCamera.value) ?: 1.0f;
                        quickPose.resume()
                    }
                },
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 48.dp, end = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    tint = Color.White,
                    contentDescription = "Switch Camera"
                )
            }
            Text(
                text = statusText.value,
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp).fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}
