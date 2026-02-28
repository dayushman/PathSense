package com.dayushmand.pathsense.sample.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dayushmand.pathsense.ui.PathSense
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<FloatingActionButton>(R.id.fab_clear).setOnClickListener {
            PathSense.clearCanvas()
        }
    }
}
