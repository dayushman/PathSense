package com.dayushmand.pathsense.core

import kotlin.math.abs
import kotlin.math.max

internal object Resampler {
    fun resample(points: List<PathPoint>, targetCount: Int): List<PathPoint> {
        if (points.isEmpty()) return emptyList()
        if (points.size == 1) return List(targetCount) { points.first() }

        val pathLength = pathLength(points)
        if (pathLength <= 0f) return List(targetCount) { points.first() }

        val interval = pathLength / (targetCount - 1)
        val resampled = ArrayList<PathPoint>(targetCount)
        resampled.add(points.first())

        var distanceSoFar = 0f
        var i = 1
        var prev = points[0]
        while (i < points.size) {
            val current = points[i]
            val d = MathUtils.distance(prev, current)
            if (isNearlyZero(d)) {
                prev = current
                i++
                continue
            }
            if (distanceSoFar + d >= interval) {
                val t = (interval - distanceSoFar) / d
                val nx = prev.x + t * (current.x - prev.x)
                val ny = prev.y + t * (current.y - prev.y)
                val nt = (prev.tMillis + t * (current.tMillis - prev.tMillis)).toLong()
                val newPoint = PathPoint(nx, ny, nt)
                resampled.add(newPoint)
                prev = newPoint
                distanceSoFar = 0f
            } else {
                distanceSoFar += d
                prev = current
                i++
            }
        }

        while (resampled.size < targetCount) {
            resampled.add(points.last())
        }

        return resampled
    }

    fun pathLength(points: List<PathPoint>): Float {
        var length = 0f
        for (i in 1 until points.size) {
            length += MathUtils.distance(points[i - 1], points[i])
        }
        return length
    }

    fun boundingBox(points: List<PathPoint>): RectF {
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
            minX = 0f
            minY = 0f
            maxX = 0f
            maxY = 0f
        }
        return RectF(minX, minY, maxX, maxY)
    }

    fun centroid(points: List<PathPoint>): PathPoint {
        if (points.isEmpty()) return PathPoint(0f, 0f, 0L)
        var sumX = 0f
        var sumY = 0f
        for (p in points) {
            sumX += p.x
            sumY += p.y
        }
        return PathPoint(sumX / points.size, sumY / points.size, points.last().tMillis)
    }

    fun rotate(points: List<PathPoint>, angleRad: Float): List<PathPoint> {
        val c = centroid(points)
        val cos = kotlin.math.cos(angleRad)
        val sin = kotlin.math.sin(angleRad)
        return points.map { p ->
            val dx = p.x - c.x
            val dy = p.y - c.y
            val nx = dx * cos - dy * sin + c.x
            val ny = dx * sin + dy * cos + c.y
            PathPoint(nx, ny, p.tMillis)
        }
    }

    fun scaleToSquare(points: List<PathPoint>, size: Float): List<PathPoint> {
        val box = boundingBox(points)
        val width = max(1f, box.right - box.left)
        val height = max(1f, box.bottom - box.top)
        return points.map { p ->
            val nx = (p.x - box.left) * (size / width)
            val ny = (p.y - box.top) * (size / height)
            PathPoint(nx, ny, p.tMillis)
        }
    }

    fun translateToOrigin(points: List<PathPoint>): List<PathPoint> {
        val c = centroid(points)
        return points.map { p ->
            PathPoint(p.x - c.x, p.y - c.y, p.tMillis)
        }
    }

    fun indicativeAngle(points: List<PathPoint>): Float {
        if (points.isEmpty()) return 0f
        val c = centroid(points)
        val first = points.first()
        return kotlin.math.atan2((c.y - first.y).toDouble(), (c.x - first.x).toDouble()).toFloat()
    }

    fun vectorize(points: List<PathPoint>): FloatArray {
        val vector = FloatArray(points.size * 2)
        var i = 0
        for (p in points) {
            vector[i++] = p.x
            vector[i++] = p.y
        }
        val magnitude = MathUtils.magnitude(vector)
        if (magnitude > 0f) {
            for (j in vector.indices) {
                vector[j] /= magnitude
            }
        }
        return vector
    }

    fun cosineDistance(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        var i = 0
        while (i < a.size && i < b.size) {
            dot += a[i] * b[i]
            i++
        }
        dot = dot.coerceIn(-1f, 1f)
        return kotlin.math.acos(dot)
    }

    fun isNearlyZero(v: Float): Boolean = abs(v) < 1e-6f
}
