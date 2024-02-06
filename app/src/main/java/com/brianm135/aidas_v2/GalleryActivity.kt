package com.brianm135.aidas_v2

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContentProviderCompat.requireContext
import com.brianm135.aidas_v2.databinding.ActivityGalleryBinding
import com.brianm135.aidas_v2.databinding.ActivityLiveBinding
import com.google.android.material.tabs.TabLayout.TabGravity
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectionResult
import java.io.FileDescriptor
import java.io.IOException

class GalleryActivity : AppCompatActivity(), ObjectDetectionHelper.DetectorListener {
    private lateinit var viewBinding: ActivityGalleryBinding
    private lateinit var objectDetectionHelper: ObjectDetectionHelper
    private lateinit var imageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        viewBinding.overlayView.setRunningMode(RunningMode.IMAGE)

        val modelNameTextView: TextView = findViewById(R.id.modelNameTxt)
        val selectFromGalleryBtn: Button = findViewById(R.id.selectFromGalleryBtn)
        imageView = findViewById(R.id.galleryImageView)

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

        modelNameTextView.text = "Model: ${objectDetectionHelper.getModelName(currentModel)}"


        val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if(uri != null) {
                Log.d("MediaPicker", "Selected URI $uri")

                if (objectDetectionHelper.isClosed()) {
                    objectDetectionHelper.setupObjectDetector()
                }

                val inputImage = uriToBitmap(uri)
                imageView.setImageBitmap(inputImage)
                if (inputImage != null) {
                    viewBinding.overlayView.clear()

                    objectDetectionHelper.detectImage(inputImage)?.let { resultBundle ->
                        Log.i("result", "Inference Time ${resultBundle.inferenceTime}")
                        val detectionResult  = resultBundle.results[0]
                        viewBinding.overlayView.setResults(
                            detectionResult,
                            resultBundle.inputImageHeight,
                            resultBundle.inputImageWidth,
                            resultBundle.inputImageRotation
                        )
                    }

                    objectDetectionHelper.clearObjectDetector()

                }
            }
            else {
                Log.d("MediaPicker", "No Media Selected")
            }
        }

        selectFromGalleryBtn.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
        }

    }

    private fun uriToBitmap(selectedFileUri: Uri): Bitmap? {
        try {
            val parcelFileDescriptor = contentResolver.openFileDescriptor(selectedFileUri, "r")
            val fileDescriptor: FileDescriptor = parcelFileDescriptor!!.fileDescriptor
            val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
            parcelFileDescriptor.close()
            return image
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    override fun onError(error: String) {
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
    }

    override fun onResults(resultBundle: ObjectDetectionHelper.ResultBundle) {
        // Empty
    }
}