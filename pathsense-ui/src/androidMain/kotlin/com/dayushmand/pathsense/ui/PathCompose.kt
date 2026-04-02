package com.dayushmand.pathsense.ui

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.dayushmand.pathsense.core.HUDAlignment
import com.dayushmand.pathsense.core.PathOverlayConfig
import com.dayushmand.pathsense.core.PathPoint
import com.dayushmand.pathsense.core.PathTracker
import com.dayushmand.pathsense.core.StrokeCap
import kotlin.math.max

private const val TAP_DISTANCE_THRESHOLD = 1.0

@Composable
fun PathCapture(
    modifier: Modifier = Modifier,
    tracker: PathTracker,
    overlayConfig: PathOverlayConfig = PathOverlayConfig(),
    onEvent: ((com.dayushmand.pathsense.core.PathEvent) -> Unit)? = null,
) {
    val isTouchActive = remember { mutableStateOf(false) }
    val startPoint = remember { mutableStateOf<PathPoint?>(null) }
    val hudText = remember { mutableStateOf(PathOverlayView.HUD_DEFAULT) }

    LaunchedEffect(onEvent) {
        if (onEvent != null) {
            tracker.listener = onEvent
        }
    }

    PathOverlay(
        modifier = modifier.pointerInput(tracker) {
            awaitEachGesture {
                if (!tracker.captureEnabled) return@awaitEachGesture
                val down = awaitFirstDown()
                isTouchActive.value = true
                val downPoint = down.position.toPoint()
                startPoint.value = downPoint
                tracker.onDown(downPoint)
                hudText.value = formatHud(downPoint, downPoint)

                var done = false
                while (!done) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: continue
                    if (change.changedToUp()) {
                        tracker.onUp(change.position.toPoint())
                        resetCapture(tracker, isTouchActive, startPoint, hudText)
                        change.consume()
                        done = true
                    } else if (!change.pressed) {
                        tracker.onCancel()
                        resetCapture(tracker, isTouchActive, startPoint, hudText)
                        change.consume()
                        done = true
                    } else {
                        val movePoint = change.position.toPoint()
                        tracker.onMove(movePoint)
                        hudText.value = formatHud(movePoint, startPoint.value ?: movePoint)
                    }
                }
            }
        },
        tracker = tracker,
        overlayConfig = overlayConfig,
        isTouchActive = isTouchActive,
        hudText = hudText,
    )
}

@Composable
fun PathOverlay(
    modifier: Modifier = Modifier,
    tracker: PathTracker,
    overlayConfig: PathOverlayConfig = PathOverlayConfig(),
    isTouchActive: MutableState<Boolean>? = null,
    hudText: MutableState<String>? = null,
) {
    // When no external hudText is provided, auto-derive from the tracker's events
    val effectiveHudText = hudText ?: if (overlayConfig.showCoordinateHUD) {
        val autoHud = remember { mutableStateOf(PathOverlayView.HUD_DEFAULT) }
        val startPoint = remember { mutableStateOf<PathPoint?>(null) }
        DisposableEffect(tracker) {
            val previous = tracker.listener
            tracker.listener = { event ->
                previous(event)
                when (event) {
                    is com.dayushmand.pathsense.core.PathEvent.Started -> {
                        startPoint.value = event.point
                        autoHud.value = formatHud(event.point, event.point)
                    }
                    is com.dayushmand.pathsense.core.PathEvent.Updated -> {
                        val last = event.points.lastOrNull()
                        if (last != null) {
                            autoHud.value = formatHud(last, startPoint.value ?: last)
                        }
                    }
                    is com.dayushmand.pathsense.core.PathEvent.Ended -> {
                        val last = event.points.lastOrNull()
                        if (last != null) {
                            autoHud.value = formatHud(last, startPoint.value ?: last)
                        }
                    }
                    is com.dayushmand.pathsense.core.PathEvent.Cancelled -> {
                        startPoint.value = null
                        autoHud.value = PathOverlayView.HUD_DEFAULT
                    }
                    else -> {}
                }
            }
            onDispose {
                tracker.listener = previous
            }
        }
        autoHud
    } else null

    val cachedPath = remember { Path() }
    val cachedVersion = remember { mutableStateOf(-1) }

    Box(modifier = modifier.zIndex(PathSense.OVERLAY_PRIORITY_Z)) {
        Canvas(modifier = Modifier.matchParentSize()) {
            if (!isDebugBuild() && overlayConfig.debugOnly) return@Canvas
            val points = tracker.currentPoints
            if (points.isEmpty()) return@Canvas

            val style = overlayConfig.style
            val start = points.first()
            val end = points.last()
            val dx = (end.x - start.x).toDouble()
            val dy = (end.y - start.y).toDouble()
            val isTap = kotlin.math.hypot(dx, dy) < TAP_DISTANCE_THRESHOLD

            if (isTap) {
                val radius = max(style.strokeWidthPx, 4f)
                drawCircle(
                    color = style.gradientStartColor.toComposeColor(),
                    radius = radius,
                    center = Offset(end.x, end.y),
                    alpha = 1f,
                )
            } else {
                val path = cachedPath.apply {
                    if (cachedVersion.value != tracker.pointsVersion) {
                        reset()
                        buildComposePathInto(this, points)
                        cachedVersion.value = tracker.pointsVersion
                    }
                }
                val brush = Brush.linearGradient(
                    colors = listOf(style.gradientStartColor.toComposeColor(), style.gradientEndColor.toComposeColor()),
                    start = Offset(start.x, start.y),
                    end = Offset(end.x, end.y),
                )
                drawPath(
                    path = path,
                    brush = brush,
                    style = Stroke(width = style.strokeWidthPx, cap = style.strokeCap.toComposeCap()),
                    alpha = 1f,
                )
            }

            if (style.showBoundingBox) {
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
                if (minX != Float.POSITIVE_INFINITY) {
                    drawRect(
                        color = style.boundingBoxColor.toComposeColor(),
                        topLeft = Offset(minX, minY),
                        size = androidx.compose.ui.geometry.Size(maxX - minX, maxY - minY),
                        style = Stroke(width = max(2f, style.strokeWidthPx / 2f)),
                        alpha = 1f,
                    )
                }
            }

            val shouldShowCrosshair =
                overlayConfig.showCrosshair && (isTouchActive?.value ?: true)
            if (shouldShowCrosshair) {
                val p = points.last()
                drawLine(
                    color = PathOverlayView.CROSSHAIR_COLOR.toComposeColor(),
                    start = Offset(0f, p.y),
                    end = Offset(size.width, p.y),
                    strokeWidth = 4f,
                    alpha = 1f,
                )
                drawLine(
                    color = PathOverlayView.CROSSHAIR_COLOR.toComposeColor(),
                    start = Offset(p.x, 0f),
                    end = Offset(p.x, size.height),
                    strokeWidth = 4f,
                    alpha = 1f,
                )
            }

            if (overlayConfig.showTouchCircle) {
                val p = points.last()
                val radius = max(16f, style.strokeWidthPx * 3f)
                drawCircle(
                    color = style.gradientStartColor.toComposeColor(),
                    radius = radius,
                    center = Offset(p.x, p.y),
                    alpha = 0.8f,
                    style = Stroke(width = 3f),
                )
            }
        }

        // Coordinate HUD label — only visible while crosshair is showing (touch active)
        if (overlayConfig.showCoordinateHUD && (isTouchActive?.value ?: false)) {
            val alignment = when (overlayConfig.hudAlignment) {
                HUDAlignment.TOP_LEFT -> Alignment.TopStart
                HUDAlignment.TOP_RIGHT -> Alignment.TopEnd
                HUDAlignment.BOTTOM_LEFT -> Alignment.BottomStart
                HUDAlignment.BOTTOM_RIGHT -> Alignment.BottomEnd
                HUDAlignment.CENTER_LEFT -> Alignment.CenterStart
                HUDAlignment.CENTER_RIGHT -> Alignment.CenterEnd
            }
            BasicText(
                text = effectiveHudText?.value ?: PathOverlayView.HUD_DEFAULT,
                modifier = Modifier
                    .align(alignment)
                    .padding(12.dp)
                    .background(
                        overlayConfig.hudBackgroundColor.toComposeColor(),
                        RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                style = TextStyle(
                    color = overlayConfig.hudTextColor.toComposeColor(),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                ),
            )
        }
    }
}

private fun resetCapture(
    tracker: PathTracker,
    isTouchActive: MutableState<Boolean>,
    startPoint: MutableState<PathPoint?>,
    hudText: MutableState<String>,
) {
    isTouchActive.value = false
    tracker.clearPoints()
    startPoint.value = null
    hudText.value = PathOverlayView.HUD_DEFAULT
}

private fun buildComposePathInto(path: Path, points: List<PathPoint>) {
    if (points.isEmpty()) return
    path.moveTo(points.first().x, points.first().y)
    for (i in 1 until points.size) {
        val prev = points[i - 1]
        val curr = points[i]
        val midX = (prev.x + curr.x) / 2f
        val midY = (prev.y + curr.y) / 2f
        path.quadraticBezierTo(prev.x, prev.y, midX, midY)
    }
    val last = points.last()
    path.lineTo(last.x, last.y)
}

private fun Offset.toPoint(): PathPoint {
    return PathPoint(x, y, SystemClock.uptimeMillis())
}

private fun formatHud(current: PathPoint, start: PathPoint): String {
    val dx = current.x - start.x
    val dy = current.y - start.y
    return "x: ${current.x.toInt()}  y: ${current.y.toInt()}  dx: ${dx.toInt()}  dy: ${dy.toInt()}"
}
