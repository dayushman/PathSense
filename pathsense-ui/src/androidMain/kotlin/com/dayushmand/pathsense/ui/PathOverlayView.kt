package com.dayushmand.pathsense.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF as AndroidRectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.SystemClock
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.dayushmand.pathsense.core.PathPoint
import com.dayushmand.pathsense.core.PathTracker
import kotlin.math.max

class PathOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    var tracker: PathTracker? = null
    var overlayConfig: PathOverlayConfig = PathOverlayConfig()
        set(value) {
            field = value
            applyHudConfig()
            invalidate()
        }

    private val path = Path()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private var fadeStartTime: Long? = null
    private var startPoint: PathPoint? = null

    // ---- HUD label (matches GesturePathKit's coordinateLabel) ----
    private val hudLabel: TextView = TextView(context).apply {
        setTypeface(Typeface.MONOSPACE, Typeface.NORMAL)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        text = HUD_DEFAULT
        val dp = context.resources.displayMetrics.density
        val hPad = (12 * dp).toInt()
        val vPad = (6 * dp).toInt()
        setPadding(hPad, vPad, hPad, vPad)
        visibility = View.GONE
    }

    init {
        isClickable = false
        isFocusable = false
        setWillNotDraw(false)
        initDebugContextFrom(context)

        val dp = context.resources.displayMetrics.density
        val margin = (12 * dp).toInt()
        val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        lp.setMargins(margin, margin, margin, margin)
        addView(hudLabel, lp)
        applyHudConfig()

        // Adjust HUD margins to avoid system bars (status bar, navigation bar)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            setOnApplyWindowInsetsListener { _, insets ->
                val hudLp = hudLabel.layoutParams as? LayoutParams
                if (hudLp != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val systemBars = insets.getInsets(
                            android.view.WindowInsets.Type.systemBars()
                        )
                        hudLp.setMargins(
                            margin + systemBars.left,
                            margin + systemBars.top,
                            margin + systemBars.right,
                            margin + systemBars.bottom,
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        hudLp.setMargins(
                            margin + insets.systemWindowInsetLeft,
                            margin + insets.systemWindowInsetTop,
                            margin + insets.systemWindowInsetRight,
                            margin + insets.systemWindowInsetBottom,
                        )
                    }
                    hudLabel.layoutParams = hudLp
                }
                insets
            }
        }
    }

    private fun applyHudConfig() {
        hudLabel.setTextColor(overlayConfig.hudTextColor.toColorInt())
        val bg = GradientDrawable().apply {
            setColor(overlayConfig.hudBackgroundColor.toColorInt())
            val dp = context.resources.displayMetrics.density
            cornerRadius = 8 * dp
        }
        hudLabel.background = bg
        hudLabel.visibility = if (overlayConfig.showCoordinateHUD) View.VISIBLE else View.GONE

        // Position via layout gravity
        val lp = hudLabel.layoutParams as? LayoutParams ?: return
        lp.gravity = when (overlayConfig.hudAlignment) {
            HUDAlignment.TOP_LEFT -> Gravity.TOP or Gravity.START
            HUDAlignment.TOP_RIGHT -> Gravity.TOP or Gravity.END
            HUDAlignment.BOTTOM_LEFT -> Gravity.BOTTOM or Gravity.START
            HUDAlignment.BOTTOM_RIGHT -> Gravity.BOTTOM or Gravity.END
            HUDAlignment.CENTER_LEFT -> Gravity.CENTER_VERTICAL or Gravity.START
            HUDAlignment.CENTER_RIGHT -> Gravity.CENTER_VERTICAL or Gravity.END
        }
        hudLabel.layoutParams = lp
    }

    fun notifyPathStarted() {
        fadeStartTime = null
        invalidate()
    }

    fun notifyPathEnded() {
        if (overlayConfig.style.fadeOutMs > 0) {
            fadeStartTime = SystemClock.uptimeMillis()
            invalidate()
        }
    }

    /** Called by the touch interceptor when a new touch starts. */
    fun notifyTouchStart(point: PathPoint) {
        startPoint = point
        fadeStartTime = null
        updateHudText(point)
        invalidate()
    }

    /** Called by the touch interceptor on each touch move. */
    fun notifyTouchMove(point: PathPoint) {
        updateHudText(point)
        invalidate()
    }

    /** Called by the touch interceptor when the touch ends. */
    fun notifyTouchEnd(point: PathPoint) {
        updateHudText(point)
        if (overlayConfig.style.fadeOutMs > 0) {
            fadeStartTime = SystemClock.uptimeMillis()
            invalidate()
        }
    }

    /** Called by the touch interceptor on cancel. */
    fun notifyTouchCancel() {
        startPoint = null
        hudLabel.text = HUD_DEFAULT
        invalidate()
    }

    fun clearCanvas() {
        tracker?.clearPoints()
        fadeStartTime = null
        startPoint = null
        hudLabel.text = HUD_DEFAULT
        invalidate()
    }

    private fun updateHudText(current: PathPoint) {
        if (!overlayConfig.showCoordinateHUD) return
        val sp = startPoint ?: current
        val dx = current.x - sp.x
        val dy = current.y - sp.y
        hudLabel.text = "x: ${current.x.toInt()}  y: ${current.y.toInt()}  dx: ${dx.toInt()}  dy: ${dy.toInt()}"
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isDebugBuild() && overlayConfig.debugOnly) return

        val points = tracker?.currentPoints.orEmpty()
        if (points.isEmpty()) return

        val fadeAlpha = computeFadeAlpha()
        if (fadeAlpha <= 0f) return

        val style = overlayConfig.style
        paint.strokeWidth = style.strokeWidthPx
        paint.strokeCap = style.strokeCap.toPaintCap()
        paint.alpha = (fadeAlpha * 255).toInt().coerceIn(0, 255)

        val start = points.first()
        val end = points.last()
        paint.shader = LinearGradient(
            start.x,
            start.y,
            end.x,
            end.y,
            style.gradientStartColor.toColorInt(),
            style.gradientEndColor.toColorInt(),
            Shader.TileMode.CLAMP,
        )

        buildPath(points, path)
        canvas.drawPath(path, paint)

        if (style.showBoundingBox) {
            val bbox = computeBoundingBox(points)
            val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.style = Paint.Style.STROKE
                strokeWidth = max(2f, style.strokeWidthPx / 2f)
                color = style.boundingBoxColor.toColorInt()
                this.alpha = (fadeAlpha * 255).toInt().coerceIn(0, 255)
            }
            canvas.drawRect(bbox, boxPaint)
        }

        if (overlayConfig.showCrosshair) {
            drawCrosshair(canvas, end, fadeAlpha)
        }

        if (overlayConfig.showTouchCircle) {
            drawTouchCircle(canvas, end, fadeAlpha)
        }

        // Keep invalidating during fade-out animation
        if (fadeStartTime != null && fadeAlpha > 0f) {
            postInvalidateOnAnimation()
        }
    }

    private fun computeFadeAlpha(): Float {
        val fadeMs = overlayConfig.style.fadeOutMs
        val start = fadeStartTime ?: return 1f
        if (fadeMs <= 0) return 1f
        val elapsed = SystemClock.uptimeMillis() - start
        val t = elapsed.toFloat() / fadeMs.toFloat()
        return (1f - t).coerceIn(0f, 1f)
    }

    private fun buildPath(points: List<PathPoint>, outPath: Path) {
        outPath.reset()
        if (points.isEmpty()) return
        outPath.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            val midX = (prev.x + curr.x) / 2f
            val midY = (prev.y + curr.y) / 2f
            outPath.quadTo(prev.x, prev.y, midX, midY)
        }
        val last = points.last()
        outPath.lineTo(last.x, last.y)
    }

    private fun drawCrosshair(canvas: Canvas, point: PathPoint, alpha: Float) {
        val crossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = overlayConfig.style.gradientEndColor.toColorInt()
            this.alpha = (alpha * 160).toInt().coerceIn(0, 255)
        }
        canvas.drawLine(0f, point.y, width.toFloat(), point.y, crossPaint)
        canvas.drawLine(point.x, 0f, point.x, height.toFloat(), crossPaint)
    }

    private fun drawTouchCircle(canvas: Canvas, point: PathPoint, alpha: Float) {
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = overlayConfig.style.gradientStartColor.toColorInt()
            this.alpha = (alpha * 200).toInt().coerceIn(0, 255)
        }
        val radius = max(16f, overlayConfig.style.strokeWidthPx * 3f)
        canvas.drawCircle(point.x, point.y, radius, circlePaint)
    }

    private fun computeBoundingBox(points: List<PathPoint>): AndroidRectF {
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        for (p in points) {
            minX = minX.coerceAtMost(p.x)
            minY = minY.coerceAtMost(p.y)
            maxX = maxX.coerceAtLeast(p.x)
            maxY = maxY.coerceAtLeast(p.y)
        }
        if (minX == Float.POSITIVE_INFINITY) {
            minX = 0f; minY = 0f; maxX = 0f; maxY = 0f
        }
        return AndroidRectF(minX, minY, maxX, maxY)
    }

    companion object {
        internal const val HUD_DEFAULT = "x: \u2013  y: \u2013  dx: \u2013  dy: \u2013"
    }
}

private fun StrokeCap.toPaintCap(): Paint.Cap {
    return when (this) {
        StrokeCap.BUTT -> Paint.Cap.BUTT
        StrokeCap.ROUND -> Paint.Cap.ROUND
        StrokeCap.SQUARE -> Paint.Cap.SQUARE
    }
}

private fun Long.toColorInt(): Int = this.toInt()
