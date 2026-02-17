package com.dayushmand.pathsense.core

data class PathConfig(
    val samplingHz: Int = 120,
    val minDistancePx: Float = 2f,
    val smoothingWindow: Int = 3,
    val resampleSpacingPx: Float = 6f,
    val maxPoints: Int = 500,
)
