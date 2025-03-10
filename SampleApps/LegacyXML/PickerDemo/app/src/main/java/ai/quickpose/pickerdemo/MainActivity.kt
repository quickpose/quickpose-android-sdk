package ai.quickpose.pickerdemo

import ai.quickpose.camera.QuickPoseCameraSwitchView
import ai.quickpose.core.*
import ai.quickpose.core.Feature
import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private var useFrontCamera: Boolean = true
    private var cameraButton: View? = null
    private var cameraAndOverlay: ViewGroup? = null

    private var categorySpinner: Spinner? = null
    private var featureSpinner: Spinner? = null

    private var quickPose: QuickPose =
            QuickPose(
                    this,
                    sdkKey = "YOUR SDK KEY HERE"
            ) // register for your free key at https://dev.quickpose.ai
    private var cameraSwitchView: QuickPoseCameraSwitchView? = null
    private var feedbackTextView: TextView? = null
    private var featureTextView: TextView? = null
    private var valueBar: View? = null

    private var counter: QuickPoseThresholdCounter = QuickPoseThresholdCounter()
    private var timer: QuickPoseThresholdTimer = QuickPoseThresholdTimer()

    private var statusTextView: TextView? = null
    private var selectedFeatures: Array<Feature> =
            arrayOf(Feature.RangeOfMotion(RangeOfMotion.Shoulder(Side.LEFT, false)))

    private var selectedComponent: String = "General"
    private var allDemoFeatureComponents: List<String> =
            listOf(
                    "General",
                    // "Input",
                    "Position",
                    "Fitness",
                    "Health",
                     "Conditional",
                    // "Sports",
                    // "Measurement"
                    )

    private fun updateFeatureAdapter() {
        val features = allDemoFeatures(selectedComponent)
        val featureStrings = features.map { it.first().displayString() }
        val featureAdapter =
                ArrayAdapter(this@MainActivity, R.layout.spinner_item_list, featureStrings)
        featureAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        featureSpinner?.adapter = featureAdapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraAndOverlay = findViewById<ViewGroup>(R.id.quickpose_camera_and_overlay_view)

        if (PermissionsHelper.checkAndRequestCameraPermissions(this)) {
            setupCamera()
        }

        setupSpinners()

        statusTextView = findViewById(R.id.status_text_view)
        feedbackTextView = findViewById<TextView>(R.id.feedback_text_view)
        featureTextView = findViewById<TextView>(R.id.feature_text_view)
        valueBar = findViewById(R.id.value_bar)
    }

    private fun setupCamera() {
        cameraSwitchView = QuickPoseCameraSwitchView(this, quickPose)
        cameraAndOverlay?.addView(cameraSwitchView)

        cameraButton = findViewById(R.id.camera_button)
        cameraButton?.setOnClickListener {
            lifecycleScope.launch {
                useFrontCamera = !useFrontCamera
                quickPose.stop()
                cameraSwitchView?.start(useFrontCamera)!!
                quickPose.resume()
            }
        }
    }

    private fun setupSpinners() {
        categorySpinner = findViewById(R.id.category_spinner)
        featureSpinner = findViewById(R.id.feature_spinner)

        val categoryAdapter =
                ArrayAdapter(this, R.layout.spinner_item_list, allDemoFeatureComponents)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        val feature = allDemoFeatureComponents[0]
        val features = allDemoFeatures(feature)
        val featureStrings = features.map { it.first().displayString() }
        val featureAdapter =
                ArrayAdapter(this@MainActivity, R.layout.spinner_item_list, featureStrings)
        featureAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        updateFeatureAdapter()
        categorySpinner?.adapter = categoryAdapter
        featureSpinner?.adapter = featureAdapter
        var isCategorySpinnerInitialized = false

        categorySpinner?.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                            parent: AdapterView<*>,
                            view: View,
                            position: Int,
                            id: Long
                    ) {
                        if (isCategorySpinnerInitialized) {

                            selectedComponent = allDemoFeatureComponents[position]
                            updateFeatureAdapter()
                        } else {
                            isCategorySpinnerInitialized = true
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }

        featureSpinner?.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                            parent: AdapterView<*>,
                            view: View,
                            position: Int,
                            id: Long
                    ) {
                        val selectedFeatures =
                                allDemoFeatures(selectedComponent)[position].toTypedArray()
                        counter.reset()
                        valueBar?.visibility = View.INVISIBLE
                        featureTextView?.text = ""
                        featureTextView?.visibility = View.INVISIBLE
                        quickPose.update(selectedFeatures)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (PermissionsHelper.cameraPermissionsGranted(this)) {
            setupCamera()
        } else {
            println("camera permission not granted")
            val noCameraAccessView = findViewById<View>(R.id.no_camera_access_view)
            noCameraAccessView.visibility = View.VISIBLE
        }
    }

    @SuppressLint("WrongConstant")
    override fun onResume() {
        super.onResume()

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
            windowInsetsController.hide(android.view.WindowInsets.Type.navigationBars())
        } else {
            window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }

        if (!PermissionsHelper.cameraPermissionsGranted(this)) return

        lifecycleScope.launch {
            cameraSwitchView?.start(useFrontCamera)!!
            quickPose.start(
                    selectedFeatures,
                    onStart = {},
                    onFrame = { status, overlay, features, feedback, landmarks ->
                        if (status is Status.Success) {
                            runOnUiThread {
                                statusTextView?.text =
                                        "Powered by QuickPose.ai v${quickPose.quickPoseVersion()}\n${status.fps} fps"
                            }
                        }
                        runOnUiThread {
                            feedbackTextView?.apply {
                                when (status) {
                                    is Status.Success -> {
                                        if (feedback.isNotEmpty() && feedback.values.first().isRequired) {
                                            val feedbackResult = feedback.values.first()
                                            text = feedbackResult.displayString
                                            visibility = View.VISIBLE
                                        } else {
                                            text = ""
                                            visibility = View.GONE
                                        }
                                    }
                                    Status.NoPersonFound -> {
                                        text = "Stand in view"
                                        visibility = View.VISIBLE
                                    }
                                    Status.SdkValidationError -> {
                                        text = "Be back soon"
                                        visibility = View.VISIBLE
                                    }
                                }
                            }
                        }
                        runOnUiThread {
                            if (status is Status.Success) {
                                if (selectedFeatures.firstOrNull() is Feature.Fitness) {
                                    val fitnessFeature =
                                            selectedFeatures.first() as Feature.Fitness
                                    val result = features[selectedFeatures.first()]
                                    result?.let {
                                        val measure = it.value
                                        valueBar?.visibility = View.VISIBLE
                                        valueBar?.let { valueBar ->
                                            val parentWidth = (valueBar.parent as View).width
                                            valueBar.layoutParams?.width =
                                                    (measure * parentWidth).toInt()
                                        }

                                        if (it.stringValue == "plank") {
                                            timer.time(measure)
                                            val timeInPosition =
                                                    String.format("%.2f", timer.getState().time)
                                            featureTextView?.text = timeInPosition
                                            featureTextView?.visibility = View.VISIBLE
                                        } else {
                                            counter.count(measure)
                                            val count = counter.state.count

                                            featureTextView?.text =
                                                    "${fitnessFeature.displayString()}:  ${count}"
                                            featureTextView?.visibility = View.VISIBLE
                                        }
                                    }
                                } else {
                                    featureTextView?.text = ""
                                    featureTextView?.visibility = View.INVISIBLE
                                }

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

    fun allDemoFeatures(component: String): List<List<Feature>> {
        return when (component) {
            // "Measurement" ->
            //         listOf(
            //                 listOf(
            //                         measureLineBody(
            //                                 Landmarks.Body.LEFT_SHOULDER,
            //                                 Landmarks.Body.RIGHT_SHOULDER,
            //                                 null,
            //                                 null
            //                         )
            //                 ),
            //                 listOf(
            //                         measureLineBody(
            //                                 Landmarks.Body.LEFT_SHOULDER,
            //                                 Landmarks.Body.RIGHT_SHOULDER,
            //                                 100,
            //                                 "%.fcm"
            //                         )
            //                 )
            //         )
            "Health" ->
                    listOf(
                            listOf(Feature.RangeOfMotion(RangeOfMotion.Shoulder(Side.LEFT, false))),
                            listOf(Feature.RangeOfMotion(RangeOfMotion.Shoulder(Side.RIGHT, true))),
                            listOf(Feature.RangeOfMotion(RangeOfMotion.Hip(Side.RIGHT, true))),
                            listOf(Feature.RangeOfMotion(RangeOfMotion.Knee(Side.RIGHT, true))),
                            listOf(Feature.RangeOfMotion(RangeOfMotion.Neck(false))),
                            listOf(Feature.RangeOfMotion(RangeOfMotion.Back(false)))
                    )
            "Position" ->
                    listOf(
                            listOf(
                                    Feature.Inside(
                                            RelativeEdgeInsets(0.3f, 0.3f, 0.3f, 0.3f),
                                            Landmarks.Group.Hand(Side.LEFT),
                                    )
                            ),
                            listOf(
                                    Feature.Inside(
                                            RelativeEdgeInsets(0.3f, 0.3f, 0.3f, 0.3f),
                                            Landmarks.Group.Hand(Side.RIGHT)
                                    )
                            )
                    )
            // "Input" -> listOf(listOf(raisedFingers()), listOf(thumbsUp()),
            // listOf(thumbsUpOrDown()))
            "Conditional" -> {
                val greenStyle =
                    Style(
                        conditionalColors =
                        listOf(
                            Style.ConditionalColor(
                                min = 40.0f,
                                max = null,
                                color = Color.valueOf(Color.GREEN)
                            )
                        )
                    )
                val redStyle =
                    Style(
                        conditionalColors =
                        listOf(
                            Style.ConditionalColor(
                                min = 180.0f,
                                max = null,
                                color = Color.valueOf(Color.RED)
                            )
                        )
                    )
                listOf(
                    listOf(
                        Feature.RangeOfMotion(
                            RangeOfMotion.Shoulder(Side.LEFT, true),
                            style = greenStyle
                        )
                    ),
                    listOf(
                        Feature.RangeOfMotion(
                            RangeOfMotion.Knee(Side.RIGHT, true),
                            style = redStyle
                        )
                    )
                )
            }
            "Fitness" ->
                    listOf(
                            listOf(Feature.Fitness(FitnessFeature.Squats)),
                            listOf(Feature.Fitness(FitnessFeature.PushUps)),
                            listOf(Feature.Fitness(FitnessFeature.JumpingJacks)),
                            listOf(Feature.Fitness(FitnessFeature.SumoSquats)),
                            listOf(Feature.Fitness(FitnessFeature.Lunges(Side.LEFT))),
                            listOf(Feature.Fitness(FitnessFeature.Lunges(Side.RIGHT))),
                            listOf(Feature.Fitness(FitnessFeature.SitUps)),
                            listOf(Feature.Fitness(FitnessFeature.CobraWings)),
                            listOf(Feature.Fitness(FitnessFeature.Plank)),
                            listOf(Feature.Fitness(FitnessFeature.BicepCurls)),
                            listOf(Feature.Fitness(FitnessFeature.LegRaises)),
                            listOf(Feature.Fitness(FitnessFeature.GluteBridge)),
                            listOf(Feature.Fitness(FitnessFeature.OverheadDumbbellPress)),
                            listOf(Feature.Fitness(FitnessFeature.VUps)),
                            listOf(Feature.Fitness(FitnessFeature.LateralRaises)),
                            listOf(Feature.Fitness(FitnessFeature.FrontRaises)),
                            listOf(Feature.Fitness(FitnessFeature.SideLunges(Side.LEFT))),
                            listOf(Feature.Fitness(FitnessFeature.SideLunges(Side.RIGHT)))
                    )
            // "Sports" -> {
            //     val bikeStyle =
            //             QuickPose.Style(
            //                     relativeFontSize = 0.33f,
            //                     relativeArcSize = 0.4f,
            //                     relativeLineWidth = 0.3f
            //             )
            //     val feature1 =
            //             rangeOfMotion(Landmarks.Body.RIGHT_SHOULDER, false, style =
            // bikeStyle)
            //     val feature2 = rangeOfMotion(Landmarks.Body.RIGHT_ELBOW, false, style
            // =
            // bikeStyle)
            //     val feature3 = rangeOfMotion(Landmarks.Body.RIGHT_HIP, false, style =
            // bikeStyle)
            //     val feature4 = rangeOfMotion(Landmarks.Body.RIGHT_KNEE, true, style =
            // bikeStyle)
            //     listOf(listOf(feature1, feature2, feature3, feature4))
            // }
            else -> {
                Landmarks.Group.commonLimbs().map { listOf(Feature.Overlay(it)) } +
                        listOf(listOf(Feature.ShowPoints()))
            }
        }
    }
}
