package com.brianm135.aidas_v2

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val modelSpinner: Spinner = findViewById(R.id.modelSelectionSpinner)
        val galleryBtn: Button = findViewById(R.id.galleryBtn)
        val liveBtn: Button = findViewById(R.id.liveBtn)

        ArrayAdapter.createFromResource(this, R.array.models,android.R.layout.simple_spinner_item).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            modelSpinner.adapter = adapter
        }


        galleryBtn.setOnClickListener {
            val selectedModel = getSelectedModel(modelSpinner)

            val intent = Intent(this, GalleryActivity::class.java)
            intent.putExtra("model", selectedModel)
            startActivity(intent)
        }

        liveBtn.setOnClickListener {
            val selectedModel = getSelectedModel(modelSpinner)

            val intent = Intent(this, LiveActivity::class.java)
            intent.putExtra("model", selectedModel)
            startActivity(intent)
        }
    }

    private fun getSelectedModel(modelSelectionDropdown: Spinner): Int {
        return modelSelectionDropdown.selectedItemId.toInt()
    }
}