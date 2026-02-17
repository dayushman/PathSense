package com.dayushmand.pathsense.core

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt

internal object MathUtils {
    fun distance(a: PathPoint, b: PathPoint): Float {
        return hypot((b.x - a.x).toDouble(), (b.y - a.y).toDouble()).toFloat()
    }

    fun angleDeg(dx: Float, dy: Float): Float {
        return (atan2(dy.toDouble(), dx.toDouble()) * 180.0 / PI).toFloat()
    }

    fun magnitude(vec: FloatArray): Float {
        var sum = 0f
        for (v in vec) sum += v * v
        return sqrt(sum)
    }
}
