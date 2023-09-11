package io.benkon.sample.image_detection

import android.util.Size
import java.text.SimpleDateFormat
import java.util.Locale

object Constant {
    internal const val MODEL_FILE = "yolov8m_fp32_person_20230725"
    internal val INPUT_SIZE = Size(640, 640)
    internal val OUTPUT_SIZE = intArrayOf(1, 5, 8400)
    internal const val DETECT_THRESHOLD = 0.25f
    internal const val IOU_THRESHOLD = 0.72f
    internal const val IOU_CLASS_DUPLICATED_THRESHOLD = 0.72f

    // after modified:
    // red'=red * CONTRAST + BRIGHTNESS
    // green'=green * CONTRAST + BRIGHTNESS
    // blue'=blue * CONTRAST + BRIGHTNESS
    internal const val CONTRAST = 0.2f // raw image 1
    internal const val BRIGHTNESS = 0f // raw image 0
    val DEFAULT_LOCALE = Locale.US

    const val FRIENDLY_DATE_TIME_FORMAT_STRING = "yyyy-MM-dd HH:mm:ss"
    val FRIENDLY_DATE_TIME_FORMAT: SimpleDateFormat =
        SimpleDateFormat(FRIENDLY_DATE_TIME_FORMAT_STRING, DEFAULT_LOCALE)
}