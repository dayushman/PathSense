package com.screenrecorder.bubble

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager

internal class BubbleManager(
    private val context: Context,
    private val tintColor: Long,
    private val onRecordTap: () -> Unit,
    private val onStopTap: () -> Unit,
) {
    private var windowManager: WindowManager? = null
    private var bubbleView: BubbleView? = null
    private var isAttached = false

    fun attach() {
        if (isAttached) return
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        bubbleView = BubbleView(context, tintColor, onRecordTap, onStopTap)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16
            y = 200
        }

        windowManager?.addView(bubbleView, params)
        isAttached = true
    }

    fun detach() {
        if (!isAttached) return
        try {
            windowManager?.removeView(bubbleView)
        } catch (_: Exception) {}
        bubbleView = null
        isAttached = false
    }

    fun setRecording(isRecording: Boolean) {
        bubbleView?.setRecording(isRecording)
    }

    fun updateDuration(durationMs: Long) {
        bubbleView?.updateDuration(durationMs)
    }
}
