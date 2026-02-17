package com.dayushmand.pathsense.ui

import android.view.MotionEvent
import android.view.Window
import com.dayushmand.pathsense.core.PathPoint
import com.dayushmand.pathsense.core.PathTracker

/**
 * Wraps the original [Window.Callback] to transparently observe touch events
 * and feed them to a [PathTracker]. All events are passed through to the
 * original callback — the app's touch handling is completely unaffected.
 */
internal class TouchInterceptWindowCallback(
    private val wrapped: Window.Callback,
    private val tracker: PathTracker,
    private val overlayView: PathOverlayView,
) : Window.Callback by wrapped {

    private var trackingPointerId = -1

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        event?.let { observeTouch(it) }
        return wrapped.dispatchTouchEvent(event)
    }

    private fun observeTouch(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                trackingPointerId = event.getPointerId(0)
                val point = event.toPathPoint(0)
                tracker.onDown(point)
                processHistory(event, 0)
                overlayView.notifyTouchStart(point)
            }

            MotionEvent.ACTION_MOVE -> {
                val idx = event.findPointerIndex(trackingPointerId)
                if (idx >= 0) {
                    val point = event.toPathPoint(idx)
                    processHistory(event, idx)
                    tracker.onMove(point)
                    overlayView.notifyTouchMove(point)
                }
            }

            MotionEvent.ACTION_UP -> {
                val idx = event.findPointerIndex(trackingPointerId)
                if (idx >= 0) {
                    val point = event.toPathPoint(idx)
                    processHistory(event, idx)
                    tracker.onUp(point)
                    overlayView.notifyTouchEnd(point)
                }
                trackingPointerId = -1
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val idx = event.actionIndex
                if (event.getPointerId(idx) == trackingPointerId) {
                    val point = event.toPathPoint(idx)
                    tracker.onUp(point)
                    overlayView.notifyTouchEnd(point)
                    trackingPointerId = -1
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                tracker.onCancel()
                overlayView.notifyTouchCancel()
                trackingPointerId = -1
            }
        }
    }

    private fun processHistory(event: MotionEvent, pointerIndex: Int) {
        for (h in 0 until event.historySize) {
            val point = PathPoint(
                event.getHistoricalX(pointerIndex, h),
                event.getHistoricalY(pointerIndex, h),
                event.getHistoricalEventTime(h),
            )
            tracker.onMove(point)
        }
    }
}

private fun MotionEvent.toPathPoint(pointerIndex: Int): PathPoint =
    PathPoint(getX(pointerIndex), getY(pointerIndex), eventTime)
