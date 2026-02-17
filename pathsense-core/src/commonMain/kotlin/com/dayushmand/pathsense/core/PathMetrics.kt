package com.dayushmand.pathsense.core

data class PathMetrics(
    val length: Float,
    val bbox: RectF,
    val start: PathPoint,
    val end: PathPoint,
    val avgDirectionDeg: Float,
    val avgSpeed: Float,
    val deltaX: Float,
    val deltaY: Float,
)
