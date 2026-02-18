package com.dayushmand.pathsense.ui

data class PathOverlayConfig(
    val debugOnly: Boolean = true,
    val style: PathStyle = PathStyle(),
    val showCrosshair: Boolean = true,
    val showTouchCircle: Boolean = true,
    val showCoordinateHUD: Boolean = true,
    val hudAlignment: HUDAlignment = HUDAlignment.TOP_LEFT,
    val hudTextColor: Long = 0xFFFFFFFF,
    val hudBackgroundColor: Long = 0xB3000000,
)

enum class HUDAlignment {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    CENTER_LEFT,
    CENTER_RIGHT,
}
