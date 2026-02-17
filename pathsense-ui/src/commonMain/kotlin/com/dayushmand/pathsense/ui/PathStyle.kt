package com.dayushmand.pathsense.ui

data class PathStyle(
    val gradientStartColor: Long = 0xFFFF3B30,
    val gradientEndColor: Long = 0xFF007AFF,
    val strokeWidthPx: Float = 4f,
    val strokeCap: StrokeCap = StrokeCap.ROUND,
    val fadeOutMs: Long = 300,
    val showBoundingBox: Boolean = false,
    val boundingBoxColor: Long = 0x4400FF00,
)

enum class StrokeCap {
    BUTT,
    ROUND,
    SQUARE,
}
