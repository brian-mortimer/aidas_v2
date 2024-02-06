package com.brianm135.aidas_v2

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.brianm135.aidas_v2.databinding.ActivityLiveBinding
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class LiveActivity : AppCompatActivity(), ObjectDetectionHelper.DetectorListener {

    private lateinit var objectDetectionHelper: ObjectDetectionHelper
    private lateinit var viewBinding: ActivityLiveBinding
    private lateinit var previewView: PreviewView
    private lateinit var cameraExecutor: ExecutorService


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityLiveBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        val modelNameTextView: TextView = findViewById(R.id.modelNameTxt)
        previewView = findViewById(R.id.previewView)

        val currentModel = intent.getSerializableExtra("model") as Int
        val threshold = intent.getSerializableExtra("threshold") as Float

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
            Log.println(Log.INFO, "Permissions", "Permissions Granted")
        } else {
            requestPermissions()
        }
        cameraExecutor = Executors.newSingleThreadExecutor()


        objectDetectionHelper = ObjectDetectionHelper(
            context = this,
            currentModel = currentModel,
            objectDetectorListener = this,
            threshold = threshold,
            runningMode = RunningMode.LIVE_STREAM
        )

        if (objectDetectionHelper.isClosed()) {
            objectDetectionHelper.setupObjectDetector()
        }

        modelNameTextView.text = "Model: ${objectDetectionHelper.getModelName(currentModel)}"
    }

    /*
     * Start Camera and perform detections on each frame.
     */
    private fun startCamera(){
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(viewBinding.previewView.display.rotation)
                .build()
                .also { it.setSurfaceProvider(viewBinding.previewView.surfaceProvider) }

            // Initialise Image Analyzer, include running detections on each frame
            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(viewBinding.previewView.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, objectDetectionHelper::detectLivestreamFrame)
                }
            try {
                cameraProvider.unbindAll()
                // Setup camera lifecycle.
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)

            }catch (exc: Exception){
                Log.e("CAMERA", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
        Log.i("CAMERA", "Camera Successfully Started.")
    }


    /***
     * Check All permissions are granted for camera.
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    /***
     * Request Camera permissions.
     */
    private fun requestPermissions(){
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions())
    { permissions ->
        // Handle Permission granted/rejected
        var permissionGranted = true
        permissions.entries.forEach {
            if (it.key in REQUIRED_PERMISSIONS && it.value == false)
                permissionGranted = false
        }
        if (!permissionGranted) {
            Toast.makeText(baseContext,
                "Permission request denied",
                Toast.LENGTH_SHORT).show()
        } else {
            startCamera()
        }
    }



    override fun onError(error: String) {
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
    }

    override fun onResults(resultBundle: ObjectDetectionHelper.ResultBundle) {
        // Assign detection results to the overlay.
        val detectionResult = resultBundle.results[0]
        viewBinding.overlayView2.setResults(
                detectionResult,
                resultBundle.inputImageHeight,
                resultBundle.inputImageWidth,
                resultBundle.inputImageRotation
        )

        // Force overlay to redraw results
        viewBinding.overlayView2.invalidate()
    }

    companion object {
        private val REQUIRED_PERMISSIONS = mutableListOf (
            Manifest.permission.CAMERA,
        ).apply {
            // Add WRITE_EXTERNAL_STORAGE permission for older Android versions
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }
}