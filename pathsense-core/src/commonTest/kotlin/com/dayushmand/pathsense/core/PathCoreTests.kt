package com.dayushmand.pathsense.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PathCoreTests {
    @Test
    fun resampleProducesExpectedCount() {
        val points = listOf(
            PathPoint(0f, 0f, 0L),
            PathPoint(10f, 0f, 10L),
            PathPoint(20f, 0f, 20L),
        )
        val resampled = Resampler.resample(points, 8)
        assertEquals(8, resampled.size)
    }

    @Test
    fun ringBufferEvictsOldest() {
        val buffer = PointBuffer(3)
        buffer.add(PathPoint(0f, 0f, 0L))
        buffer.add(PathPoint(1f, 0f, 1L))
        buffer.add(PathPoint(2f, 0f, 2L))
        buffer.add(PathPoint(3f, 0f, 3L))
        val list = buffer.toList()
        assertEquals(3, list.size)
        assertEquals(1f, list.first().x)
        assertEquals(3f, list.last().x)
    }

    @Test
    fun metricsLine() {
        val points = listOf(
            PathPoint(0f, 0f, 0L),
            PathPoint(10f, 0f, 10L),
        )
        val metrics = computeMetrics(points)
        assertTrue(metrics.length >= 10f)
        assertEquals(10f, metrics.deltaX)
        assertEquals(0f, metrics.deltaY)
    }
}
