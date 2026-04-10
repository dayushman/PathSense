package com.screenrecorder.bubble

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView

@SuppressLint("ViewConstructor")
internal class BubbleView(
    context: Context,
    private val tintColor: Long,
    private val onRecordTap: () -> Unit,
    private val onStopTap: () -> Unit,
    private val onDragStart: () -> Unit,
    private val onLongPress: () -> Unit,
) : FrameLayout(context) {

    private val bubbleSize = (44 * context.resources.displayMetrics.density).toInt()
    private val iconSize = (20 * context.resources.displayMetrics.density).toInt()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var isRecording = false
    private var isDragging = false
    private var longPressTriggered = false
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var initialX = 0
    private var initialY = 0

    private val longPressHandler = Handler(Looper.getMainLooper())
    private val longPressRunnable = Runnable {
        if (!isDragging) {
            longPressTriggered = true
            onLongPress()
        }
    }

    private val durationLabel: TextView
    private var pulseAnimator: ValueAnimator? = null
    private var pulseAlpha = 255

    init {
        setWillNotDraw(false)
        minimumWidth = bubbleSize
        minimumHeight = bubbleSize

        durationLabel = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 10f
            gravity = Gravity.CENTER
            visibility = View.GONE
            setBackgroundColor(Color.argb(180, 0, 0, 0))
            setPadding(8, 2, 8, 2)
        }
        addView(durationLabel, LayoutParams(
            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
            topMargin = bubbleSize + 4
        })
    }

    fun setRecording(recording: Boolean) {
        isRecording = recording
        durationLabel.visibility = if (recording) View.VISIBLE else View.GONE
        if (recording) startPulse() else stopPulse()
        invalidate()
    }

    fun updateDuration(durationMs: Long) {
        val seconds = (durationMs / 1000) % 60
        val minutes = (durationMs / 1000) / 60
        durationLabel.text = String.format("%02d:%02d", minutes, seconds)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = bubbleSize
        val height = bubbleSize + (if (isRecording) (20 * context.resources.displayMetrics.density).toInt() else 0)
        setMeasuredDimension(width, height)
        measureChildren(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = bubbleSize / 2f
        val cy = bubbleSize / 2f
        val radius = bubbleSize / 2f

        // Background circle
        paint.color = if (isRecording) Color.RED else tintColor.toInt()
        paint.alpha = pulseAlpha
        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy, radius, paint)

        // Icon
        iconPaint.color = Color.WHITE
        iconPaint.style = Paint.Style.FILL
        if (isRecording) {
            // Stop square
            val iconHalf = iconSize / 3f
            canvas.drawRoundRect(
                cx - iconHalf, cy - iconHalf,
                cx + iconHalf, cy + iconHalf,
                4f, 4f, iconPaint
            )
        } else {
            // Record circle
            canvas.drawCircle(cx, cy, iconSize / 3f, iconPaint)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val params = layoutParams as? WindowManager.LayoutParams ?: return false
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = false
                longPressTriggered = false
                lastTouchX = event.rawX
                lastTouchY = event.rawY
                initialX = params.x
                initialY = params.y
                longPressHandler.postDelayed(longPressRunnable, 500)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - lastTouchX
                val dy = event.rawY - lastTouchY
                if (!isDragging && (dx * dx + dy * dy) > 100) {
                    isDragging = true
                    longPressHandler.removeCallbacks(longPressRunnable)
                    onDragStart()
                }
                if (isDragging) {
                    params.x = initialX - dx.toInt()
                    params.y = initialY + dy.toInt()
                    wm.updateViewLayout(this, params)
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                longPressHandler.removeCallbacks(longPressRunnable)
                if (longPressTriggered) {
                    // Long press already handled
                } else if (!isDragging) {
                    if (isRecording) onStopTap() else onRecordTap()
                } else {
                    snapToEdge(params, wm)
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                longPressHandler.removeCallbacks(longPressRunnable)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun snapToEdge(params: WindowManager.LayoutParams, wm: WindowManager) {
        val display = context.resources.displayMetrics
        val screenWidth = display.widthPixels
        val currentX = params.x

        // Snap to nearest edge (x=16 margin)
        val targetX = if (currentX > screenWidth / 2) 16 else screenWidth - bubbleSize - 16

        ValueAnimator.ofInt(currentX, targetX).apply {
            duration = 200
            addUpdateListener {
                params.x = it.animatedValue as Int
                try { wm.updateViewLayout(this@BubbleView, params) } catch (_: Exception) {}
            }
            start()
        }
    }

    private fun startPulse() {
        pulseAnimator = ValueAnimator.ofInt(255, 180, 255).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                pulseAlpha = it.animatedValue as Int
                invalidate()
            }
            start()
        }
    }

    private fun stopPulse() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        pulseAlpha = 255
        invalidate()
    }
}
