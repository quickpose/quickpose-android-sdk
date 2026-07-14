package ai.quickpose.devapp

import ai.quickpose.camera.QuickPoseCameraSwitchView
import ai.quickpose.core.Feature
import ai.quickpose.core.Landmarks
import ai.quickpose.core.PermissionsHelper
import ai.quickpose.core.QuickPose
import ai.quickpose.core.Side
import ai.quickpose.core.Status
import ai.quickpose.core.Style
import ai.quickpose.devapp.theme.BasicDemoTheme
import android.graphics.Bitmap
import android.graphics.Typeface
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.mutableStateOf
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

private val AccentColor = Color(0xFF5970F6)

/** One overlay style option: a label, the values it offers, and its default. */
private data class StyleOption(val label: String, val values: List<String>, val default: String = values.first())

private val STYLE_OPTIONS =
        listOf(
                StyleOption("Color", listOf("White", "Green", "Red")),
                StyleOption("Line Width", listOf("0.5", "1.0", "1.5", "2.0"), default = "1.0"),
                StyleOption("Line Cap", listOf("Round", "Butt", "Square")),
                StyleOption("Pattern", listOf("Solid", "Dashed", "Dotted")),
                StyleOption("Effect", listOf("None", "Shadow", "Glow")),
                StyleOption("Outline", listOf("Off", "On")),
                StyleOption("Image Fill", listOf("None", "Orange Glow", "Galaxy")),
                StyleOption("Font", listOf("System", "Bungee")),
                StyleOption("Letter Spacing", listOf("0", "0.08", "0.15")),
        )

class MainActivity : ComponentActivity() {
    private var useFrontCamera = mutableStateOf(true)
    private var statusText = mutableStateOf("Powered by QuickPose.ai")
    private var quickPose: QuickPose =
            QuickPose(
                    this,
                    sdkKey = "YOUR SDK KEY HERE"
            ) // register for your free key at https://dev.quickpose.ai
    private var cameraSwitchView: QuickPoseCameraSwitchView? = null
    private var hasPermissions = mutableStateOf(false)

    private var menuOpen = mutableStateOf(false)
    private var openOption = mutableStateOf<String?>(null)
    private var selections = mutableStateOf(STYLE_OPTIONS.associate { it.label to it.default })

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        WindowInsetsCompat.Type.navigationBars()

        hasPermissions.value = PermissionsHelper.checkAndRequestCameraPermissions(this)

        setContent {
            BasicDemoTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (hasPermissions.value) {
                        AndroidView(
                                factory = { ctx ->
                                    cameraSwitchView = QuickPoseCameraSwitchView(ctx, quickPose)
                                    cameraSwitchView!!
                                },
                                modifier = Modifier.fillMaxSize()
                        )

                        Box(
                                modifier =
                                        Modifier.align(Alignment.TopStart)
                                                .padding(top = 48.dp, start = 16.dp)
                        ) {
                            TextButton(
                                    onClick = {
                                        openOption.value = null
                                        menuOpen.value = true
                                    },
                                    modifier =
                                            Modifier.background(
                                                    AccentColor.copy(alpha = 0.8f),
                                                    RoundedCornerShape(22.dp)
                                            )
                            ) {
                                Text(
                                        "\uD83C\uDFA8 Style: ${styleSummary()}",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        maxLines = 1
                                )
                            }
                            DropdownMenu(
                                    expanded = menuOpen.value,
                                    onDismissRequest = { menuOpen.value = false }
                            ) {
                                val open = openOption.value
                                if (open == null) {
                                    STYLE_OPTIONS.forEach { option ->
                                        DropdownMenuItem(onClick = { openOption.value = option.label }) {
                                            Text(option.label)
                                        }
                                    }
                                } else {
                                    val option = STYLE_OPTIONS.first { it.label == open }
                                    val current = selections.value.getValue(option.label)
                                    option.values.forEach { value ->
                                        DropdownMenuItem(
                                                onClick = {
                                                    selections.value =
                                                            selections.value + (option.label to value)
                                                    quickPose.update(selectedFeatures())
                                                    menuOpen.value = false
                                                }
                                        ) { Text(if (value == current) "✓ $value" else value) }
                                    }
                                }
                            }
                        }

                        IconButton(
                                onClick = {
                                    lifecycleScope.launch {
                                        useFrontCamera.value = !useFrontCamera.value
                                        quickPose.stop()
                                        cameraSwitchView?.start(useFrontCamera.value)
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
                    } else {
                        Text(
                                text = "Camera permissions are required",
                                color = Color.White,
                                fontSize = 16.sp,
                                modifier = Modifier.align(Alignment.Center),
                                textAlign = TextAlign.Center
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
    }

    private fun styleSummary(): String {
        val nonDefault =
                STYLE_OPTIONS.mapNotNull { option ->
                    val value = selections.value.getValue(option.label)
                    if (value == option.default) null else value
                }
        return if (nonDefault.isEmpty()) "Default" else nonDefault.joinToString(", ")
    }

    private fun selectedFeatures(): Array<Feature> {
        val style = selectedStyle()
        // The elbow angle keeps a measurement label on screen even when seated,
        // so the font and letter-spacing options are always visible.
        return arrayOf(
                Feature.Overlay(Landmarks.Group.WholeBody(), style),
                Feature.MeasureAngleBody(
                        origin = Landmarks.Body.Elbow(Side.RIGHT),
                        p1 = Landmarks.Body.Shoulder(Side.RIGHT),
                        p2 = Landmarks.Body.Wrist(Side.RIGHT),
                        clockwiseDirection = false,
                        style = style
                ),
        )
    }

    private fun selectedStyle(): Style {
        val values = selections.value
        val baseColor =
                when (values.getValue("Color")) {
                    "Green" -> android.graphics.Color.valueOf(android.graphics.Color.GREEN)
                    "Red" -> android.graphics.Color.valueOf(android.graphics.Color.RED)
                    else -> android.graphics.Color.valueOf(android.graphics.Color.WHITE)
                }
        return Style(
                relativeLineWidth = values.getValue("Line Width").toFloatOrNull() ?: 1.0f,
                color = baseColor,
                lineCap =
                        when (values.getValue("Line Cap")) {
                            "Butt" -> Style.LineCap.BUTT
                            "Square" -> Style.LineCap.SQUARE
                            else -> Style.LineCap.ROUND
                        },
                linePattern =
                        when (values.getValue("Pattern")) {
                            "Dashed" -> Style.LinePattern.DASHED
                            "Dotted" -> Style.LinePattern.DOTTED
                            else -> Style.LinePattern.SOLID
                        },
                shadow =
                        when (values.getValue("Effect")) {
                            "Shadow" ->
                                    Style.Shadow(
                                            color = android.graphics.Color.valueOf(android.graphics.Color.BLACK),
                                            radius = 14f,
                                            offsetX = 0f,
                                            offsetY = 10f
                                    )
                            "Glow" -> Style.Shadow(color = baseColor, radius = 32f, offsetX = 0f, offsetY = 0f)
                            else -> null
                        },
                outline =
                        if (values.getValue("Outline") == "On") {
                            Style.Outline(
                                    color = android.graphics.Color.valueOf(android.graphics.Color.BLACK),
                                    relativeWidth = 0.6f
                            )
                        } else null,
                imageFill =
                        when (values.getValue("Image Fill")) {
                            "Orange Glow" -> orangeGlowImage
                            "Galaxy" ->
                                    android.graphics.BitmapFactory.decodeResource(
                                            resources,
                                            R.drawable.galaxy
                                    )
                            else -> null
                        },
                typeface =
                        if (values.getValue("Font") == "Bungee") {
                            Typeface.createFromAsset(assets, "fonts/Bungee-Regular.ttf")
                        } else null,
                letterSpacing = values.getValue("Letter Spacing").toFloatOrNull() ?: 0f,
        )
    }

    /** Procedural orange radial gradient — any Bitmap works as an image fill. */
    private val orangeGlowImage: Bitmap by lazy {
        val width = 360
        val height = 640
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint()
        paint.shader =
                android.graphics.RadialGradient(
                        width / 2f,
                        height / 2f,
                        height * 0.7f,
                        intArrayOf(
                                android.graphics.Color.rgb(255, 217, 77),
                                android.graphics.Color.rgb(255, 115, 0),
                                android.graphics.Color.rgb(140, 13, 0)
                        ),
                        floatArrayOf(0f, 0.5f, 1f),
                        android.graphics.Shader.TileMode.CLAMP
                )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        bitmap
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
                    selectedFeatures(),
                    onFrame = { status, overlay, features, feedback, landmarks ->
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
