package com.dayushmand.pathsense.core

import kotlin.math.max

internal fun computeMetrics(points: List<PathPoint>): PathMetrics {
    if (points.isEmpty()) {
        val zero = PathPoint(0f, 0f, 0L)
        return PathMetrics(0f, RectF(0f, 0f, 0f, 0f), zero, zero, 0f, 0f, 0f, 0f)
    }
    val length = Resampler.pathLength(points)
    val bbox = Resampler.boundingBox(points)
    val start = points.first()
    val end = points.last()
    val deltaX = end.x - start.x
    val deltaY = end.y - start.y
    val avgDirectionDeg = MathUtils.angleDeg(deltaX, deltaY)
    val durationMs = max(1L, end.tMillis - start.tMillis)
    val avgSpeed = length / (durationMs / 1000f)
    return PathMetrics(length, bbox, start, end, avgDirectionDeg, avgSpeed, deltaX, deltaY)
}
