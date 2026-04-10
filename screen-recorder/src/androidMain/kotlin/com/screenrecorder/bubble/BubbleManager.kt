package com.screenrecorder.bubble

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager

internal class BubbleManager(
    private val context: Context,
    private val tintColor: Long,
    initialAudioEnabled: Boolean,
    private val onStartRecording: () -> Unit,
    private val onStopTap: () -> Unit,
    private val onGetMoreInfo: () -> Unit,
    private val onAudioToggle: (Boolean) -> Unit,
    initialPathSenseEnabled: Boolean,
    private val onPathSenseToggle: (Boolean) -> Unit,
) {
    private var windowManager: WindowManager? = null
    private var bubbleView: BubbleView? = null
    private var popoverView: PopoverMenuView? = null
    private var isAttached = false
    private var isPopoverShown = false
    private var currentAudioEnabled = initialAudioEnabled
    private var currentPathSenseEnabled = initialPathSenseEnabled

    private val dp = context.resources.displayMetrics.density
    private val bubbleSize = (44 * dp).toInt()

    fun attach() {
        if (isAttached) return
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        bubbleView = BubbleView(
            context, tintColor,
            onRecordTap = { showPopover() },
            onStopTap = onStopTap,
            onDragStart = { hidePopover() },
        )

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
        hidePopoverImmediate()
        if (!isAttached) return
        try { windowManager?.removeView(bubbleView) } catch (_: Exception) {}
        bubbleView = null
        isAttached = false
    }

    fun setRecording(isRecording: Boolean) {
        hidePopoverImmediate()
        bubbleView?.setRecording(isRecording)
    }

    fun updateDuration(durationMs: Long) {
        bubbleView?.updateDuration(durationMs)
    }

    fun showPopover() {
        if (isPopoverShown) return
        val bv = bubbleView ?: return
        val params = bv.layoutParams as? WindowManager.LayoutParams ?: return
        val screenWidth = context.resources.displayMetrics.widthPixels
        val isBubbleOnRight = params.x < screenWidth / 2
        val bubbleLeft = screenWidth - params.x - bubbleSize

        popoverView = PopoverMenuView(
            context = context,
            bubbleLeft = bubbleLeft,
            bubbleTop = params.y,
            bubbleSize = bubbleSize,
            isBubbleOnRight = isBubbleOnRight,
            audioEnabled = currentAudioEnabled,
            onStartRecording = {
                hidePopoverImmediate()
                onStartRecording()
            },
            onGetMoreInfo = {
                hidePopoverImmediate()
                onGetMoreInfo()
            },
            onAudioToggle = { enabled ->
                currentAudioEnabled = enabled
                onAudioToggle(enabled)
            },
            pathSenseEnabled = currentPathSenseEnabled,
            onPathSenseToggle = { enabled ->
                currentPathSenseEnabled = enabled
                onPathSenseToggle(enabled)
            },
            onDismiss = { hidePopover() },
        )

        val overlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        )

        windowManager?.addView(popoverView, overlayParams)
        isPopoverShown = true
    }

    fun hidePopover() {
        if (!isPopoverShown) return
        popoverView?.animateOut {
            removePopoverView()
        }
    }

    private fun hidePopoverImmediate() {
        if (!isPopoverShown) return
        removePopoverView()
    }

    private fun removePopoverView() {
        try { windowManager?.removeView(popoverView) } catch (_: Exception) {}
        popoverView = null
        isPopoverShown = false
    }
}
