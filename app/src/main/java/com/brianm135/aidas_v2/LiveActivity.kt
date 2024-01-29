package com.brianm135.aidas_v2

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.mediapipe.tasks.vision.core.RunningMode

class LiveActivity : AppCompatActivity(), ObjectDetectionHelper.DetectorListener {

    private lateinit var objectDetectionHelper: ObjectDetectionHelper
    private lateinit var imageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live)

        val modelNameTextView: TextView = findViewById(R.id.modelNameTxt)
        imageView = findViewById(R.id.liveImageView)

        val currentModel = intent.getSerializableExtra("model") as Int

        objectDetectionHelper = ObjectDetectionHelper(
            context = this,
            currentModel = currentModel,
            objectDetectorListener = this,
            runningMode = RunningMode.LIVE_STREAM
        )

        if (objectDetectionHelper.isClosed()) {
            objectDetectionHelper.setupObjectDetector()
        }

        modelNameTextView.setText("Model: ${objectDetectionHelper.getModelName(currentModel)}")


    }

    override fun onError(error: String) {
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
    }

    override fun onResults(resultBundle: ObjectDetectionHelper.ResultBundle) {

    }
}