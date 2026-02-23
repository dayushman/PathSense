package com.dayushmand.pathsense.core

/**
 * Visual style for the gesture path trail.
 *
 * All properties are `var` so that they can be set individually from
 * both Kotlin and Swift (ObjC interop exports `var` as read-write).
 */
data class PathStyle(
    var gradientStartColor: Long = 0xFFFF3B30,
    var gradientEndColor: Long = 0xFF007AFF,
    var strokeWidthPx: Float = 4f,
    var strokeCap: StrokeCap = StrokeCap.ROUND,
    var fadeOutMs: Long = 300,
    var showBoundingBox: Boolean = false,
    var boundingBoxColor: Long = 0x4400FF00,
) {
    /** No-arg constructor for Swift interop — KMM doesn't export default param values to ObjC. */
    constructor() : this(
        gradientStartColor = 0xFFFF3B30,
        gradientEndColor = 0xFF007AFF,
        strokeWidthPx = 4f,
        strokeCap = StrokeCap.ROUND,
        fadeOutMs = 300,
        showBoundingBox = false,
        boundingBoxColor = 0x4400FF00,
    )
}

enum class StrokeCap {
    BUTT,
    ROUND,
    SQUARE,
}
