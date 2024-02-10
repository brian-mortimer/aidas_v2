package com.brianm135.aidas_v2

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.brianm135.aidas_v2.databinding.ActivityTestBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mediapipe.tasks.vision.core.RunningMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.math.log

class TestActivity() : AppCompatActivity(), ObjectDetectionHelper.DetectorListener {


    private lateinit var objectDetectionHelper: ObjectDetectionHelper
    private lateinit var resultHolder: LinearLayout
    private lateinit var imageView: ImageView
    private lateinit var viewBinding: ActivityTestBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityTestBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        val modelNameTextView: TextView = findViewById(R.id.modelNameTxt)
        val runTestButton: Button = findViewById(R.id.runTestBtn)
        resultHolder = findViewById(R.id.resultView)
        imageView = findViewById(R.id.testImage)

        val currentModel = intent.getSerializableExtra("model") as Int
        val threshold = intent.getSerializableExtra("threshold") as Float


        objectDetectionHelper = ObjectDetectionHelper(
            context = this,
            currentModel = currentModel,
            objectDetectorListener = this,
            threshold = threshold,
            runningMode = RunningMode.IMAGE
        )

        if (objectDetectionHelper.isClosed()) {
            objectDetectionHelper.setupObjectDetector()
        }
        val modelName = objectDetectionHelper.getModelName(currentModel)
        modelNameTextView.text = "Model: ${modelName}"


        runTestButton.setOnClickListener {
            runTestButton.isEnabled = false
            CoroutineScope(Dispatchers.Main).launch {
                val testImages = parseJSONFromString(readJSONFromAsset("test_data.json"))
                val testResult =  runTest(modelName, testImages)

                if (testResult != null) {
                    displayResult(testResult)
                }
            }

        }
    }

    suspend fun runTest(modelName: String, testImages: List<TestImage>): TestResult? {

        val detections = mutableListOf<Detection>()
        var detectionCorrectness: DetectionCorrectness

        for(testImg: TestImage in testImages){
            Log.i(TAG, "Image Path: ${testImg.imgPath}")

            detectionCorrectness = DetectionCorrectness.MISSING_LABEL

            // Load the image bitmap and display
            var bitmap: Bitmap = loadBitmapFromAsset(testImg.imgPath) ?: continue
            imageView.setImageBitmap(bitmap)

            // run the detection
            var resultBundle = objectDetectionHelper.detectImage(bitmap)?.let { resultBundle ->
                Log.i("result", "Inference Time ${resultBundle.inferenceTime}")
                val detectionResult  = resultBundle.results[0]
                viewBinding.overlayView3.setResults(
                    detectionResult,
                    resultBundle.inputImageHeight,
                    resultBundle.inputImageWidth,
                    resultBundle.inputImageRotation
                )

                try {
                    var label = detectionResult.detections()!![0].categories()[0].categoryName()
                    detectionCorrectness = if(label.lowercase() == testImg.label.lowercase()) {
                        DetectionCorrectness.CORRRECT
                    }else{
                        DetectionCorrectness.INCORRECT_LABEL
                    }
                }catch (e: Exception){
                    detectionCorrectness = DetectionCorrectness.MISSING_LABEL
                }

            }

            val detection = Detection(
                imgPath = testImg.imgPath,
                detectionCorrectness = detectionCorrectness,
                resultBundle = resultBundle
            )

            detections.add(0, detection)

            // Add Result to ResultLayout
            val textView = TextView(this)
            textView.text = "${detection.imgPath} - ${detection.detectionCorrectness}"
            resultHolder.addView(textView)

            delay(IMAGE_PAUSE_TIME)

        }

        return TestResult(
            modelName = modelName,
            detections = detections,
            avgInferenceTime = 80,
            accuracy = 60
        )
    }

    private fun loadBitmapFromAsset(imgPath: String): Bitmap? {
        return try {
            this.assets.open(imgPath).use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }

        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun readJSONFromAsset(filename: String): String{
        return this.assets.open(filename).bufferedReader().use { it.readText() }
    }

    private fun parseJSONFromString(text: String):List<TestImage>{
        val gson = Gson()
        return gson.fromJson(text, object : TypeToken<List<TestImage>>() {}.type)
    }

    /**
     * Custom Class Object for holding test result.
     */
    data class TestResult(
        val modelName: String,
        val detections: List<Detection>,
        val avgInferenceTime: Long,
        val accuracy: Long
    )

    data class Detection(
        val imgPath:String,
        val detectionCorrectness: DetectionCorrectness,
        val resultBundle: Unit?
    )
    
    // Correct, Incorrect Label, Missed Label, Incorrect Annotation
    enum class DetectionCorrectness {
        CORRRECT, INCORRECT_LABEL, MISSING_LABEL, INCORRECT_ANNOTATION
    }


    data class TestImage (
        val imgPath: String,
        val label: String,
        val boundingBox: List<Int>
    )

    override fun onError(error: String) {
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
    }

    override fun onResults(resultBundle: ObjectDetectionHelper.ResultBundle) {
        // Empty
    }

    companion object{
        const val TAG="TESTING"
        const val IMAGE_PAUSE_TIME:Long=2000
    }
}