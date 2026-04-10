package com.screenrecorder.share

import android.app.Activity
import android.content.Intent
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.FileProvider
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.screenrecorder.api.RecordingFile
import java.io.File

internal object ShareBottomSheet {

    fun show(activity: Activity, file: RecordingFile) {
        val dialog = BottomSheetDialog(activity)
        val dp = activity.resources.displayMetrics.density

        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24 * dp).toInt(), (24 * dp).toInt(), (24 * dp).toInt(), (32 * dp).toInt())
        }

        // Title
        val title = TextView(activity).apply {
            text = "Recording Complete"
            textSize = 20f
            setTextColor(0xFF1A1A1A.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        container.addView(title, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = (8 * dp).toInt() })

        // Duration & size info
        val seconds = (file.durationMs / 1000) % 60
        val minutes = (file.durationMs / 1000) / 60
        val sizeMb = file.fileSizeBytes / (1024.0 * 1024.0)
        val info = TextView(activity).apply {
            text = String.format("Duration: %02d:%02d  •  Size: %.1f MB", minutes, seconds, sizeMb)
            textSize = 14f
            setTextColor(0xFF666666.toInt())
        }
        container.addView(info, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = (24 * dp).toInt() })

        // Share button
        val shareButton = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(0xFF2196F3.toInt())
            setPadding((16 * dp).toInt(), (14 * dp).toInt(), (16 * dp).toInt(), (14 * dp).toInt())
            val r = (12 * dp)
            val outline = android.graphics.drawable.GradientDrawable().apply {
                setColor(0xFF2196F3.toInt())
                cornerRadius = r
            }
            background = outline
            isClickable = true
            isFocusable = true

            val icon = ImageView(activity).apply {
                setImageResource(android.R.drawable.ic_menu_share)
                setColorFilter(0xFFFFFFFF.toInt())
            }
            addView(icon, LinearLayout.LayoutParams(
                (24 * dp).toInt(), (24 * dp).toInt()
            ).apply { marginEnd = (12 * dp).toInt() })

            val label = TextView(activity).apply {
                text = "Share Recording"
                textSize = 16f
                setTextColor(0xFFFFFFFF.toInt())
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            addView(label)

            setOnClickListener {
                dialog.dismiss()
                shareFile(activity, file)
            }
        }
        container.addView(shareButton, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = (12 * dp).toInt() })

        // Delete button
        val deleteButton = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((16 * dp).toInt(), (14 * dp).toInt(), (16 * dp).toInt(), (14 * dp).toInt())
            val outline = android.graphics.drawable.GradientDrawable().apply {
                setColor(0x00000000)
                setStroke((1 * dp).toInt(), 0xFFCCCCCC.toInt())
                cornerRadius = 12 * dp
            }
            background = outline
            isClickable = true
            isFocusable = true

            val icon = ImageView(activity).apply {
                setImageResource(android.R.drawable.ic_menu_delete)
                setColorFilter(0xFFFF5252.toInt())
            }
            addView(icon, LinearLayout.LayoutParams(
                (24 * dp).toInt(), (24 * dp).toInt()
            ).apply { marginEnd = (12 * dp).toInt() })

            val label = TextView(activity).apply {
                text = "Discard"
                textSize = 16f
                setTextColor(0xFFFF5252.toInt())
            }
            addView(label)

            setOnClickListener {
                dialog.dismiss()
                try { File(file.path).delete() } catch (_: Exception) {}
            }
        }
        container.addView(deleteButton)

        dialog.setContentView(container)
        dialog.show()
    }

    private fun shareFile(activity: Activity, file: RecordingFile) {
        val recordingFile = File(file.path)
        val authority = "${activity.packageName}.screenrecorder.fileprovider"
        val contentUri = FileProvider.getUriForFile(activity, authority, recordingFile)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activity.startActivity(Intent.createChooser(shareIntent, "Share Recording"))
    }
}
