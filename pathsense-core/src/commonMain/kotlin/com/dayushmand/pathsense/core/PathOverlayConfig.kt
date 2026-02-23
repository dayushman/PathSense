package com.dayushmand.pathsense.core

/**
 * Visual overlay configuration for gesture path rendering.
 *
 * All properties are `var` so they can be set individually from both
 * Kotlin and Swift (ObjC interop exports `var` as read-write).
 */
data class PathOverlayConfig(
    var debugOnly: Boolean = true,
    var style: PathStyle = PathStyle(),
    var showCrosshair: Boolean = true,
    var showTouchCircle: Boolean = true,
    var showCoordinateHUD: Boolean = false,
    var hudAlignment: HUDAlignment = HUDAlignment.TOP_LEFT,
    var hudTextColor: Long = 0xFFFFFFFF,      // white
    var hudBackgroundColor: Long = 0xB3000000, // black @ 70% alpha
) {
    /** No-arg constructor for Swift interop — KMM doesn't export default param values to ObjC. */
    constructor() : this(
        debugOnly = true,
        style = PathStyle(),
        showCrosshair = true,
        showTouchCircle = true,
        showCoordinateHUD = false,
        hudAlignment = HUDAlignment.TOP_LEFT,
        hudTextColor = 0xFFFFFFFF,
        hudBackgroundColor = 0xB3000000,
    )
}

enum class HUDAlignment {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    CENTER_LEFT,
    CENTER_RIGHT,
}
