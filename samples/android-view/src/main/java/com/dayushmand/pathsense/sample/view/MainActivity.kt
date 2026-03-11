package com.dayushmand.pathsense.sample.view

import android.os.Bundle
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.dayushmand.pathsense.ui.PathSense
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<FloatingActionButton>(R.id.fab_clear).setOnClickListener {
            PathSense.clearCanvas()
        }

        findViewById<MaterialButton>(R.id.btn_open_dialog).setOnClickListener {
            showPathSenseDialog()
        }

        findViewById<MaterialButton>(R.id.btn_open_bottom_sheet).setOnClickListener {
            showPathSenseBottomSheet()
        }
    }

    private fun showPathSenseDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Dialog Test")
            .setMessage("Draw on this dialog and verify dots/path render. Then dismiss.")
            .setPositiveButton("Dismiss", null)
            .show()
    }

    private fun showPathSenseBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val container = FrameLayout(this).apply {
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }
        val messageView = TextView(this).apply {
            text = "Draw on this bottom sheet and verify dots/path render."
            textSize = 18f
        }
        container.addView(
            messageView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        bottomSheetDialog.setContentView(container)
        bottomSheetDialog.show()
    }
}
