package com.dayushmand.pathsense.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import com.dayushmand.pathsense.core.PathPoint
import com.dayushmand.pathsense.core.PathTracker

class PathCaptureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    var tracker: PathTracker = PathTracker()
        set(value) {
            field = value
            overlayView.tracker = value
        }

    var overlayConfig: PathOverlayConfig
        get() = overlayView.overlayConfig
        set(value) {
            overlayView.overlayConfig = value
        }

    private val overlayView = PathOverlayView(context).apply {
        tracker = this@PathCaptureView.tracker
    }

    init {
        isClickable = true
        initDebugContextFrom(context)
        addView(
            overlayView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT),
        )
    }

    fun clearCanvas() {
        overlayView.clearCanvas()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val point = event.toPathPoint()
                tracker.onDown(point)
                handleHistory(event)
                overlayView.notifyTouchStart(point)
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val point = event.toPathPoint()
                handleHistory(event)
                tracker.onMove(point)
                overlayView.notifyTouchMove(point)
                return true
            }
            MotionEvent.ACTION_UP -> {
                val point = event.toPathPoint()
                handleHistory(event)
                tracker.onUp(point)
                overlayView.notifyTouchEnd(point)
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                tracker.onCancel()
                overlayView.notifyTouchCancel()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun handleHistory(event: MotionEvent) {
        val historySize = event.historySize
        for (i in 0 until historySize) {
            val point = PathPoint(event.getHistoricalX(i), event.getHistoricalY(i), event.getHistoricalEventTime(i))
            tracker.onMove(point)
        }
    }
}

private fun MotionEvent.toPathPoint(): PathPoint {
    return PathPoint(x, y, eventTime)
}
