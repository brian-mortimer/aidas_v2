package com.brianm135.aidas_v2

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult

class ObjectDetectionHelper (
    var threshold: Float = THRESHOLD_DEFAULT,
    var maxResults: Int = MAX_RESULTS_DEFAULT,
    var currentDelegate: Int = DELEGATE_CPU,
    var currentModel: Int = MODEL_EFFICIENTDET_LITE0,
    var runningMode: RunningMode = RunningMode.IMAGE,
    val context: Context,
    // The listener is only used when running in RunningMode.LIVE_STREAM
    var objectDetectorListener: DetectorListener? = null
) {

    private var objectDetector: ObjectDetector? = null
    private var imageRotation = 0
    private lateinit var imageProcessingOptions: ImageProcessingOptions

    init{
        setupObjectDetector()
    }

    fun isClosed(): Boolean {
        return objectDetector == null
    }

    fun clearObjectDetector() {
        objectDetector?.close()
        objectDetector = null
    }

    /**
     * Setup the object detector
     *
     * Initialise the base detector depending on Live stream mode or gallery mode.
     */
    fun setupObjectDetector() {
        val baseOptionsBuilder = BaseOptions.builder()

        // Default to CPU
        when (currentDelegate) {
            DELEGATE_CPU -> baseOptionsBuilder.setDelegate(Delegate.CPU)
            DELEGATE_GPU -> baseOptionsBuilder.setDelegate(Delegate.GPU)
        }

        val modelName = getModelName(currentModel)
        baseOptionsBuilder.setModelAssetPath(modelName)

        // Ensure runningMode matches with objectDetectorListener
        when (runningMode) {
            RunningMode.LIVE_STREAM -> {
                if (objectDetectorListener == null) {
                    throw IllegalStateException(
                        "objectDetectorListener must be set when runningMode is LIVE_STREAM."
                    )
                }
            }
            RunningMode.IMAGE, RunningMode.VIDEO -> {
                // no-op
            }
        }

        try {
            val optionsBuilder = ObjectDetector.ObjectDetectorOptions.builder()
                .setBaseOptions(baseOptionsBuilder.build())
                .setScoreThreshold(threshold).setRunningMode(runningMode)
                .setMaxResults(maxResults)

            if(currentModel == MODEL_MOBILENET_V1 || currentModel == MODEL_EFFICIENTDET_LITE0 || currentModel == MODEL_SSD_MOBILENET_V1){
                optionsBuilder.setCategoryAllowlist(mutableListOf("person"))
            }


            imageProcessingOptions = ImageProcessingOptions.builder().setRotationDegrees(imageRotation).build()

            when (runningMode) {
                RunningMode.IMAGE, RunningMode.VIDEO -> optionsBuilder.setRunningMode(runningMode)

                // Set result and error listeners for live stream mode.
                RunningMode.LIVE_STREAM -> optionsBuilder.setRunningMode(runningMode)
                    .setResultListener(this::returnLivestreamResult)
                    .setErrorListener(this::returnLivestreamError)
            }
            // build options and create object detector.
            val options = optionsBuilder.build()
            objectDetector = ObjectDetector.createFromOptions(context, options)


        } catch (e: IllegalStateException) {
            objectDetectorListener?.onError("Object detector failed to initialize. See logs for details")
            Log.e(TAG, "TFLite failed to load model with error: " + e.message)
        } catch (e: RuntimeException) {
            objectDetectorListener?.onError("Object detector failed to initialize. See logs for details")
            Log.e(TAG, "Object detector failed to load model with error: " + e.message)
        }
    }

    /***
     * Detect Livestream frames and update results.
     */
    fun detectLivestreamFrame(imageProxy: ImageProxy) {

        // Ensure Runningmode is livestream.
        if (runningMode != RunningMode.LIVE_STREAM) {
            throw IllegalArgumentException(
                "Attempting to call detectLivestreamFrame" + " while not using RunningMode.LIVE_STREAM"
            )
        }

        val frameTime = SystemClock.uptimeMillis()

        // Copy out RGB bits from the frame to a bitmap buffer
        val bitmapBuffer = Bitmap.createBitmap(
            imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888
        )
        imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
        imageProxy.close()

        // if rotation doesn't match specified rotation then clear and setup object detector again.
        if (imageProxy.imageInfo.rotationDegrees != imageRotation) {
            imageRotation = imageProxy.imageInfo.rotationDegrees
            clearObjectDetector()
            setupObjectDetector()
            return
        }

        // Convert the input Bitmap object to an MPImage object to run inference
        val mpImage = BitmapImageBuilder(bitmapBuffer).build()

        detectAsync(mpImage, frameTime)
    }

    /**
     * Run object Detection using MediaPipe Object Detector API
     * @property mpImage media pipe image to run inference on.
     * @property frameTime timestamp of frame
     */
    @VisibleForTesting
    fun detectAsync(mpImage: MPImage, frameTime: Long) {
        // Result returned in returnLivestreamResult function
        objectDetector?.detectAsync(mpImage, imageProcessingOptions, frameTime)
    }

    /**
     * Result of detectLivestreamFrame is return in this function
     */
    private fun returnLivestreamResult(result: ObjectDetectorResult, input: MPImage) {
        val finishTimeMs = SystemClock.uptimeMillis()
        val inferenceTime = finishTimeMs - result.timestampMs()

        // Set results to a resultBundle custom object.
        objectDetectorListener?.onResults(
            ResultBundle(
                listOf(result),
                inferenceTime,
                input.height,
                input.width,
                imageRotation
            )
        )
    }


    private fun returnLivestreamError(error: RuntimeException) {
        objectDetectorListener?.onError(error.message ?: "An unknown error has occurred")
    }


    /**
     * Perform inference on a single bitmap image.
     * @property image image to run inference on.
     * @return ResultBundle containing result of inference
     */
    fun detectImage(image: Bitmap): ResultBundle? {
        if (runningMode != RunningMode.IMAGE) {
            throw IllegalArgumentException(
                "Attempting to call detectImage while not using RunningMode.IMAGE"
            )
        }
        if (objectDetector == null) return null

        val startTime = SystemClock.uptimeMillis()

        // Convert the input Bitmap object to an MPImage object to run inference
        val mpImage = BitmapImageBuilder(image).build()

        // Run object detection using MediaPipe Object Detector API
        objectDetector?.detect(mpImage)?.also { detectionResult ->
            val inferenceTimeMs = SystemClock.uptimeMillis() - startTime
            return ResultBundle(
                listOf(detectionResult),
                inferenceTimeMs,
                image.height,
                image.width
            )
        }
        return null
    }


    /**
     * Get the Model file name using Int ID value.
     * @property modelId integer value representing the model.
     * @return String containing model file name.
     */
    fun getModelName(modelId: Int): String {
        // Additional models should be added to this when block
        val modelName = when(modelId) {
            MODEL_TRAFFIC_SIGN_V1 -> "traffic_sign_detection_model_v1.tflite"
            MODEL_TRAFFIC_SIGN_V2 -> "traffic_sign_detection_model_v2.tflite"
            MODEL_TRAFFIC_SIGN_V3 -> "traffic_sign_detection_model_v3.tflite"
            MODEL_TRAFFIC_SIGN_V4 -> "traffic_sign_detection_model_v4.tflite"
            MODEL_EFFICIENTDET_LITE0 -> "efficientdet-lite0.tflite"
            MODEL_MOBILENET_V1 -> "mobilenetv1.tflite"
            MODEL_SSD_MOBILENET_V1 -> "ssd_mobilenet_v1.tflite"
            else -> "ssd_mobilenet_v1.tflite"
        }
        return modelName
    }

    /**
     * Result Bundle class is a custom class for representing detection results.
     */
    data class ResultBundle(
        val results: List<ObjectDetectorResult>,
        val inferenceTime: Long,
        val inputImageHeight: Int,
        val inputImageWidth: Int,
        val inputImageRotation: Int = 0
    )

    interface DetectorListener {
        fun onError(error: String)
        fun onResults(resultBundle: ResultBundle)
    }

    companion object {
        // Models: Additional models should be added here

        const val MODEL_TRAFFIC_SIGN_V1 = 0
        const val MODEL_TRAFFIC_SIGN_V2 = 1
        const val MODEL_TRAFFIC_SIGN_V3 = 2
        const val MODEL_TRAFFIC_SIGN_V4 = 3
        const val MODEL_EFFICIENTDET_LITE0 = 4
        const val MODEL_MOBILENET_V1 = 5
        const val MODEL_SSD_MOBILENET_V1 =6


        // Object Detector Defaults
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val MAX_RESULTS_DEFAULT = 5
        const val THRESHOLD_DEFAULT = 0.5F

        const val TAG = "ObjectDetectorHelper"
    }
}