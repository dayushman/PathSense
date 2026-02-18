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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dayushmand.pathsense.core.PathPoint
import com.dayushmand.pathsense.core.PathTracker
import kotlinx.coroutines.delay
import kotlin.math.max

private const val HUD_DEFAULT = "x: \u2013  y: \u2013  dx: \u2013  dy: \u2013"

@Composable
fun PathCapture(
    modifier: Modifier = Modifier,
    tracker: PathTracker,
    overlayConfig: PathOverlayConfig = PathOverlayConfig(),
    onEvent: ((com.dayushmand.pathsense.core.PathEvent) -> Unit)? = null,
) {
    val fadeStart = remember { mutableStateOf<Long?>(null) }
    val startPoint = remember { mutableStateOf<PathPoint?>(null) }
    val hudText = remember { mutableStateOf(HUD_DEFAULT) }

    LaunchedEffect(onEvent) {
        if (onEvent != null) {
            tracker.listener = onEvent
        }
    }

    PathOverlay(
        modifier = modifier.pointerInput(tracker) {
            awaitEachGesture {
                val down = awaitFirstDown()
                fadeStart.value = null
                val downPoint = down.position.toPoint()
                startPoint.value = downPoint
                tracker.onDown(downPoint)
                hudText.value = formatHud(downPoint, downPoint)

                var done = false
                while (!done) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: continue
                    if (change.changedToUp()) {
                        val upPoint = change.position.toPoint()
                        tracker.onUp(upPoint)
                        hudText.value = formatHud(upPoint, startPoint.value ?: upPoint)
                        fadeStart.value = SystemClock.uptimeMillis()
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
        fadeStartMillis = fadeStart,
        hudText = hudText,
    )
}

@Composable
fun PathOverlay(
    modifier: Modifier = Modifier,
    tracker: PathTracker,
    overlayConfig: PathOverlayConfig = PathOverlayConfig(),
    fadeStartMillis: MutableState<Long?>? = null,
    hudText: MutableState<String>? = null,
) {
    val now = remember { mutableStateOf(SystemClock.uptimeMillis()) }
    val fadeOutMs = overlayConfig.style.fadeOutMs

    // When no external hudText is provided, auto-derive from the tracker's events
    val effectiveHudText = hudText ?: if (overlayConfig.showCoordinateHUD) {
        val autoHud = remember { mutableStateOf(HUD_DEFAULT) }
        val startPoint = remember { mutableStateOf<PathPoint?>(null) }
        LaunchedEffect(tracker) {
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
                        autoHud.value = HUD_DEFAULT
                    }
                    else -> {}
                }
            }
        }
        autoHud
    } else null

    LaunchedEffect(fadeStartMillis?.value, fadeOutMs) {
        val start = fadeStartMillis?.value
        if (start != null && fadeOutMs > 0) {
            while (true) {
                now.value = SystemClock.uptimeMillis()
                if (now.value - start > fadeOutMs) break
                delay(16)
            }
            // Fade finished — clear points so the path won't redraw,
            // then reset HUD text to placeholder.
            tracker.clearPoints()
            effectiveHudText?.value = HUD_DEFAULT
        }
    }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.matchParentSize()) {
            if (!isDebugBuild() && overlayConfig.debugOnly) return@Canvas
            val points = tracker.currentPoints
            if (points.isEmpty()) return@Canvas

            val alpha = computeFadeAlpha(fadeStartMillis?.value, now.value, fadeOutMs)
            if (alpha <= 0f) return@Canvas

            val path = buildComposePath(points)
            val style = overlayConfig.style
            val brush = Brush.linearGradient(
                colors = listOf(style.gradientStartColor.toComposeColor(), style.gradientEndColor.toComposeColor()),
                start = Offset(points.first().x, points.first().y),
                end = Offset(points.last().x, points.last().y),
            )
            drawPath(
                path = path,
                brush = brush,
                style = Stroke(width = style.strokeWidthPx, cap = style.strokeCap.toComposeCap()),
                alpha = alpha,
            )

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
                        alpha = alpha,
                    )
                }
            }

            if (overlayConfig.showCrosshair) {
                val p = points.last()
                drawLine(
                    color = style.gradientEndColor.toComposeColor(),
                    start = Offset(0f, p.y),
                    end = Offset(size.width, p.y),
                    strokeWidth = 2f,
                    alpha = alpha * 0.7f,
                )
                drawLine(
                    color = style.gradientEndColor.toComposeColor(),
                    start = Offset(p.x, 0f),
                    end = Offset(p.x, size.height),
                    strokeWidth = 2f,
                    alpha = alpha * 0.7f,
                )
            }

            if (overlayConfig.showTouchCircle) {
                val p = points.last()
                val radius = max(16f, style.strokeWidthPx * 3f)
                drawCircle(
                    color = style.gradientStartColor.toComposeColor(),
                    radius = radius,
                    center = Offset(p.x, p.y),
                    alpha = alpha * 0.8f,
                    style = Stroke(width = 3f),
                )
            }
        }

        // Coordinate HUD label — real Text composable (matches GesturePathKit's coordinateLabel)
        if (overlayConfig.showCoordinateHUD) {
            val alignment = when (overlayConfig.hudAlignment) {
                HUDAlignment.TOP_LEFT -> Alignment.TopStart
                HUDAlignment.TOP_RIGHT -> Alignment.TopEnd
                HUDAlignment.BOTTOM_LEFT -> Alignment.BottomStart
                HUDAlignment.BOTTOM_RIGHT -> Alignment.BottomEnd
                HUDAlignment.CENTER_LEFT -> Alignment.CenterStart
                HUDAlignment.CENTER_RIGHT -> Alignment.CenterEnd
            }
            val hudAlpha = computeFadeAlpha(fadeStartMillis?.value, now.value, fadeOutMs)
            BasicText(
                text = effectiveHudText?.value ?: HUD_DEFAULT,
                modifier = Modifier
                    .align(alignment)
                    .padding(12.dp)
                    .graphicsLayer { alpha = hudAlpha }
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

private fun buildComposePath(points: List<PathPoint>): Path {
    val path = Path()
    if (points.isEmpty()) return path
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
    return path
}

private fun Offset.toPoint(): PathPoint {
    return PathPoint(x, y, SystemClock.uptimeMillis())
}

private fun computeFadeAlpha(start: Long?, now: Long, fadeOutMs: Long): Float {
    if (start == null || fadeOutMs <= 0) return 1f
    val elapsed = (now - start).coerceAtLeast(0)
    val t = elapsed.toFloat() / fadeOutMs.toFloat()
    return (1f - t).coerceIn(0f, 1f)
}

private fun formatHud(current: PathPoint, start: PathPoint): String {
    val dx = current.x - start.x
    val dy = current.y - start.y
    return "x: ${current.x.toInt()}  y: ${current.y.toInt()}  dx: ${dx.toInt()}  dy: ${dy.toInt()}"
}
