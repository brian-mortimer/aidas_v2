package com.brianm135.aidas_v2

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private var threshold: Float = 0.5f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val modelSpinner: Spinner = findViewById(R.id.modelSelectionSpinner)
        val galleryBtn: Button = findViewById(R.id.galleryBtn)
        val liveBtn: Button = findViewById(R.id.liveBtn)
        val increaseThresholdBtn: Button = findViewById(R.id.plusThreshold)
        val decreaseThresholdBtn: Button = findViewById(R.id.minusThreshold)
        val thresholdTxt: TextView = findViewById(R.id.thresholdText)

        ArrayAdapter.createFromResource(this, R.array.models,android.R.layout.simple_spinner_item).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            modelSpinner.adapter = adapter
        }


        galleryBtn.setOnClickListener {
            val selectedModel = getSelectedModel(modelSpinner)

            // Handle switching to Gallery Mode
            val intent = Intent(this, GalleryActivity::class.java)
            intent.putExtra("model", selectedModel)
            intent.putExtra("threshold", threshold)
            startActivity(intent)
        }

        liveBtn.setOnClickListener {
            val selectedModel = getSelectedModel(modelSpinner)

            // Handle switching to Live Mode.
            val intent = Intent(this, LiveActivity::class.java)
            intent.putExtra("model", selectedModel)
            intent.putExtra("threshold", threshold)
            startActivity(intent)
        }

        increaseThresholdBtn.setOnClickListener {
            increaseThreshold()
            thresholdTxt.text = String.format("%.2f", threshold)
        }

        decreaseThresholdBtn.setOnClickListener {
            decreaseThreshold()
            thresholdTxt.text = String.format("%.2f", threshold)
        }
    }

    /**
     * Get the Selected models ID from the modelSelectionDropDown spinner.
     * @property modelSelectionDropdown Spinnner representing the model selection dropdown list.
     * @return Integer value representing modelID.
     */
    private fun getSelectedModel(modelSelectionDropdown: Spinner): Int {
        return modelSelectionDropdown.selectedItemId.toInt()
    }

    /**
     * Increase the Confidence Threshold for selected model.
     */
    private fun increaseThreshold() {
        threshold += 0.1f
        if(threshold >= 1.0){
            threshold = 1.0f
        }
    }

    /**
     * Decrease the confidence Threshold for selected model.
     */
    private fun decreaseThreshold(){
        threshold -= 0.1f
        if (threshold <= 0.0){
            threshold = 0.0f
        }
    }
}