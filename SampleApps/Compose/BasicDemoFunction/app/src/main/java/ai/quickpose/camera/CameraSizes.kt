package ai.quickpose.camera

import android.hardware.camera2.CameraCharacteristics
import android.util.Size
import kotlin.math.abs

/**
 * Returns the best available camera output size.
 * Picks the largest 16:9 size available (display sizing is handled by onMeasure/setFixedSize).
 */
fun <T> getPreviewOutputSize(
    viewWidth: Int,
    viewHeight: Int,
    characteristics: CameraCharacteristics,
    targetClass: Class<T>,
): Size {
    val config = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
    val allSizes = config.getOutputSizes(targetClass)

    val targetRatio = 16f / 9f
    val maxWidth = 1920
    val best16x9 = allSizes
        .filter { it.width <= maxWidth }
        .filter { abs((it.width.toFloat() / it.height.toFloat()) - targetRatio) < 0.01f }
        .maxByOrNull { it.width * it.height }

    return best16x9
        ?: allSizes.maxByOrNull { it.width * it.height }
        ?: Size(1920, 1080)
}
