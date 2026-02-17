package com.dayushmand.pathsense.ui

import com.dayushmand.pathsense.core.PathConfig
import com.dayushmand.pathsense.core.PathEvent

/**
 * Configuration for [PathSense] auto-initialization.
 *
 * @param pathConfig Core path tracking configuration (sampling rate, smoothing, etc.)
 * @param overlayConfig Visual overlay configuration (style, crosshair, HUD, etc.)
 * @param listener Optional global callback for all [PathEvent]s across every Activity
 */
data class PathSenseConfig(
    val pathConfig: PathConfig = PathConfig(),
    val overlayConfig: PathOverlayConfig = PathOverlayConfig(),
    val listener: ((PathEvent) -> Unit)? = null,
)
