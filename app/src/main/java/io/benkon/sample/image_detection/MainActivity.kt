package io.benkon.sample.image_detection

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toFile
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import io.benkon.sample.image_detection.databinding.ActivityMainBinding
import io.benkon.sample.image_detection.yolo.Yolov5TFLiteDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import lib.folderpicker.FolderPicker
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {
    companion object {
        val TAG = "MainActivity"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var yoloV5TFLiteDetector: Yolov5TFLiteDetector

    private var isCanceled = false

    var classInstance: PyObject? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.btnClearLog.setOnClickListener {
            binding.tvLog.text = ""
        }
        binding.btnSelectImage.setOnClickListener {
            updateUI(true)

//            if (!::yoloV5TFLiteDetector.isInitialized) {
//                yoloV5TFLiteDetector = Yolov5TFLiteDetector()
////                yoloV5TFLiteDetector.addGPUDelegate()
//                yoloV5TFLiteDetector.initialModel(this)
//                logToUI(
//                    "Success loading yolo model\n" +
//                            "- model: ${Constant.MODEL_FILE}\n" +
//                            "- input: ${Constant.INPUT_SIZE}\n" +
//                            "- output: ${Constant.OUTPUT_SIZE.joinToString()}\n" +
//                            "- detect threshold: ${Constant.DETECT_THRESHOLD}\n" +
//                            "- IOU threshold: ${Constant.IOU_THRESHOLD}\n" +
//                            "- IOU dup threshold: ${Constant.IOU_CLASS_DUPLICATED_THRESHOLD}\n"+
//                    "- Contrast: ${Constant.CONTRAST}\n"+
//                    "- Brightness: ${Constant.BRIGHTNESS}"
//                )
//                CoroutineScope(Dispatchers.Main).launch {
//                    binding.tvModelInfo.text = "Model: ${Constant.MODEL_FILE}\n" +
//                            "- input: ${Constant.INPUT_SIZE}\n" +
//                            "- output: ${Constant.OUTPUT_SIZE.joinToString()}\n" +
//                            "- detect threshold: ${Constant.DETECT_THRESHOLD}\n" +
//                            "- IOU threshold: ${Constant.IOU_THRESHOLD}\n" +
//                            "- IOU dup threshold: ${Constant.IOU_CLASS_DUPLICATED_THRESHOLD}\n" +
//                            "- IOU dup threshold: ${Constant.IOU_CLASS_DUPLICATED_THRESHOLD}\n"+
//                            "- Contrast: ${Constant.CONTRAST}\n"+
//                            "- Brightness: ${Constant.BRIGHTNESS}"
//                }
//            } else {
//                logToUI("Use existing loaded yolo model")
//            }

            binding.tvProgress.text = ""
            isCanceled = false
            selectImageFolder()
        }
        binding.btnStop.setOnClickListener {
            binding.btnStop.visibility = View.GONE
            isCanceled = true
            logToUI("Canceled")
        }
        binding.tvAppVersion.text = BuildConfig.VERSION_NAME
    }

    private fun updateUI(processing: Boolean) {
        CoroutineScope(Dispatchers.Main).launch {
            if (processing) {
                binding.btnSelectImage.visibility = View.GONE
                binding.btnStop.visibility = View.VISIBLE
                binding.cbResetModel.isEnabled = false
            } else {
                binding.btnSelectImage.visibility = View.VISIBLE
                binding.btnStop.visibility = View.GONE
                binding.cbResetModel.isEnabled = true
                logToUI("End Session\n==============\n")
            }
        }
    }

    override fun onResume() {
        super.onResume()

        Log.e(TAG, "onResume")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:io.benkon.sample.image_detection")
                    )
                )
            }
        } else {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1111)
            }
        }

//        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
//            != PackageManager.PERMISSION_GRANTED) {
//            requestPermissions(
//                arrayOf(
//                    Manifest.permission.READ_EXTERNAL_STORAGE,
//                    Manifest.permission.WRITE_EXTERNAL_STORAGE
//                ), 11111
//            )
//        }

        CoroutineScope(Dispatchers.IO).launch {
            if (!Python.isStarted()) {
                Log.e("TEST", "start init python")
                Python.start(AndroidPlatform(this@MainActivity))
                Log.e("TEST", "end init python")
            }

            if (classInstance == null) {
                Log.e("TEST", "start init class")
                val py: Python = Python.getInstance()
                val pyo: PyObject = py.getModule("yolo_module")
                classInstance = pyo.callAttr("YOLO_object", "20230725_yolom8v.tflite")
                Log.e("TEST", "end init class")
            }
        }
    }

    val startForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { fileUri ->
//
//                contentResolver.openOutputStream(fileUri,"rw")
//                    ?.write("hello".toByteArray())

                File(fileUri.toFile(), "test.csv").apply {
                    Log.e(TAG, absolutePath)
                    createNewFile()
                    writeText("hello")
                }
            }
        }
    }


    val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            CoroutineScope(Dispatchers.IO).launch {
                result.data?.extras?.getString("data")?.let { folderLocation ->
                    logToUI("Process images in $folderLocation")

                    processImageInFolder(folderLocation)
                }
            }
        } else {
            updateUI(false)
        }
    }

    private fun processImageInFolder(folderLocation: String) {
        val imageFiles = File(folderLocation).listFiles { file ->
            Log.e(TAG, "${file.absolutePath}, ${file.extension}")
            file.extension == "png"
                    || file.extension == "jpg"
                    || file.extension == "jpeg"
//            true
        }?.sortedBy { file ->
            file.nameWithoutExtension
        }
        logToUI("Found ${imageFiles?.size ?: 0} images in $folderLocation\n")

        if (!imageFiles.isNullOrEmpty()) {
            CoroutineScope(Dispatchers.Main).launch {
                binding.btnStop.visibility = View.VISIBLE
            }
            val imageCount = imageFiles.size
            loop@ for (i in imageFiles.indices) {
                if (isCanceled) {
                    break@loop
                }

                CoroutineScope(Dispatchers.Main).launch {
                    binding.tvProgress.text = "Processing ${i}/${imageCount} in $folderLocation"
                }
//                if (binding.cbResetModel.isChecked) {
//                    yoloV5TFLiteDetector.reset()
//                    logToUI("Reset detector")
//                }

                val imageFile = imageFiles[i]
                val originalBitmap = BitmapFactory.decodeFile(imageFile.path).let {
                    it.copy(it.config, true)
                }
//                val colorModifiedBitmap = changeBitmapContrastBrightness(originalBitmap,
//                    Constant.CONTRAST,
//                    Constant.BRIGHTNESS)
//                originalBitmap.recycle()
//                val rawInputBitmapContentFile = File(
//                    folderLocation,
//                    "${imageFile.nameWithoutExtension}_input_bitmap.txt"
//                ).apply {
//                    logToUI("Write raw input bipmap to ${this.name}")
//                    val rawInputBitmapContent = StringBuilder()
//                    for (row in 0 until originalBitmap.height) {
//                        for (col in 0 until originalBitmap.width) {
//
//                            val pixel = originalBitmap.getColor(col, row)
//                            rawInputBitmapContent.append(
//                                "$col, $row, " +
//                                        "${pixel.alpha()}, " +
//                                        "${pixel.red()}, " +
//                                        "${pixel.green()}, " +
//                                        "${pixel.blue()}\n"
//                            )
//                        }
//                    }
//                    writeText(rawInputBitmapContent.toString())
//                }

                logToUI("Start detect ${imageFile.name}")
                val startDetectTimestamp = System.currentTimeMillis()
//                val detectResult = yoloV5TFLiteDetector.detect(colorModifiedBitmap)

                val stream = ByteArrayOutputStream()
                originalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                val imageByteArray = stream.toByteArray()
                val imageBase64 = Base64.encodeToString(imageByteArray, Base64.DEFAULT)
                val str: String = classInstance?.callAttr("main",
                    imageBase64
                ).toString()
                Log.e("TEST", str)
                val finishDetectTimestamp = System.currentTimeMillis()

//                val recognitionArray = detectResult.results

                logToUI(
                    "Finish detect ${imageFile.name}: " +
//                            "${recognitionArray.size} person, " +
                            "${finishDetectTimestamp - startDetectTimestamp}ms. Writing output..."
                )
//                val processedBitmap = detectResult.processedBitmap
//                val rawProcessedBitmapContentFile = File(
//                    folderLocation,
//                    "${imageFile.nameWithoutExtension}_processed_bitmap.txt"
//                ).apply {
//                    logToUI("Write processed bipmap to ${this.name}")
//                    val rawInputBitmapContent = StringBuilder()
//                    for (row in 0 until processedBitmap.height) {
//                        for (col in 0 until processedBitmap.width) {
//
//                            val pixel = processedBitmap.getColor(col, row)
//                            rawInputBitmapContent.append(
//                                "$col, $row, " +
//                                        "${pixel.alpha()}, " +
//                                        "${pixel.red()}, " +
//                                        "${pixel.green()}, " +
//                                        "${pixel.blue()}\n"
//                            )
//                        }
//                    }
//                    writeText(rawInputBitmapContent.toString())
//                }
                logToUI("Start writing output")

//                val labelFile = File(
//                    folderLocation,
//                    "${imageFile.nameWithoutExtension}.txt"
//                ).apply {
//                    try {
//                        createNewFile()
//                    } catch (ex: Throwable) {
//                        Log.e(TAG, ex.stackTraceToString())
//                    }
//                }
//                val labelLogFile = File(
//                    folderLocation,
//                    "${imageFile.nameWithoutExtension}.log"
//                ).apply {
//                    try {
//                        createNewFile()
//                    } catch (ex: Throwable) {
//                        Log.e(TAG, ex.stackTraceToString())
//                    }
//                }
//                val labelImageFile = File(
//                    folderLocation,
//                    "${imageFile.nameWithoutExtension}_labeled.jpeg"
//                )
//                var labeledFileOutputStream: BufferedOutputStream? = null
//                labeledFileOutputStream = BufferedOutputStream(FileOutputStream(labelImageFile))
//
//                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
//                paint.color = Color.RED
//                paint.textSize = 8f
//                paint.strokeWidth = 1f
//                paint.style = Paint.Style.STROKE
//
//                //                val detectedSize = min(originalBitmap.width, originalBitmap.height)
//                //                val labelBitmap = Bitmap.createBitmap(originalBitmap, originalBitmap.width - size - (originalBitmap.width-size)/2,  originalBitmap.height - size - (originalBitmap.height-size)/2, size,size)
//                val scaleX = 1f //originalBitmap.width * 1f / detectedSize
//                val scaleY = 1f //originalBitmap.height * 1f / detectedSize
//
//                Canvas(colorModifiedBitmap).apply {
//                    recognitionArray.forEach { detection ->
//                        val box = detection.location
//                        labelFile.appendText(
//                            "${detection.labelId} " +
//                                    "${box.centerX() / colorModifiedBitmap.width} " +
//                                    "${box.centerY() / colorModifiedBitmap.height} " +
//                                    "${box.width() / colorModifiedBitmap.width} " +
//                                    "${box.height() / colorModifiedBitmap.height}\n"
//                        )
//                        val log = StringBuilder()
//                        for (row in 0 until Constant.OUTPUT_SIZE[2]) {
//                            for (column in 0 until Constant.OUTPUT_SIZE[1]) {
//                                if (column == (Constant.OUTPUT_SIZE[1] - 1)) {
//                                    log.append(
//                                        "%.9f\n".format(
//                                            Constant.DEFAULT_LOCALE,
//                                            detection.recognitionArray[row + column * 8400]
//                                        )
//                                    )
////                                    labelLogFile.appendText(
////                                        "%.9f\n".format(
////                                            Constant.DEFAULT_LOCALE,
////                                            detection.recognitionArray[row + column * 8400]
////                                        )
////                                    )
//                                } else {
//                                    log.append(
//                                        "%.9f,".format(
//                                            Constant.DEFAULT_LOCALE,
//                                            detection.recognitionArray[row + column * 8400]
//                                        )
//                                    )
////                                    labelLogFile.appendText(
////                                        "%.9f,".format(
////                                            Constant.DEFAULT_LOCALE,
////                                            detection.recognitionArray[row + column * 8400]
////                                        )
////                                    )
//                                }
//                            }
//                        }
//                        labelLogFile.writeText(log.toString())
//
//                        drawRect(
//                            RectF(
//                                box.left * scaleX, box.top * scaleY,
//                                box.right * scaleX, box.bottom * scaleY
//                            ), paint
//                        )
//                        drawText(
//                            "${detection.labelName} - " +
//                                    String.format(
//                                        Constant.DEFAULT_LOCALE,
//                                        "%.3f",
//                                        detection.confidence
//                                    ),
//                            box.left * scaleX,
//                            box.top * scaleY,
//                            paint
//                        )
//                    }
//                }.save()
//                colorModifiedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, labeledFileOutputStream)

                logToUI("Finish writing output\n")
            }
        }
        updateUI(false)
    }

    fun changeBitmapContrastBrightness(orgBitmap: Bitmap, contrast: Float, brightness: Float): Bitmap {
        val cm = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,
                0f, contrast, 0f, 0f, brightness,
                0f, 0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            )
        )
        val ret = Bitmap.createBitmap(orgBitmap.width, orgBitmap.height, orgBitmap.config)
        val canvas = Canvas(ret)
        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(orgBitmap, 0f, 0f, paint)
        return ret
    }

    private fun logToUI(
        message: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            binding.tvLog.append("${Constant.FRIENDLY_DATE_TIME_FORMAT.format(timestamp)}: $message\n")
        }
        Log.i(TAG, "${Constant.FRIENDLY_DATE_TIME_FORMAT.format(timestamp)}: $message")
    }


    private fun selectImageFolder() {
//        val externalFileDir = Environment.getExternalStorageDirectory()
//
//        Log.e(TAG, externalFileDir.absolutePath)
//        Log.e(TAG, "${externalFileDir.list()?.joinToString()}")
//        File(externalFileDir, "Download/test.csv").writeText("hello")

//        val i = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
//        i.addCategory(Intent.CATEGORY_DEFAULT)
//        startForResult.launch(Intent.createChooser(i, "Choose directory"))


        folderPickerLauncher.launch(Intent(this, FolderPicker::class.java))
    }
}