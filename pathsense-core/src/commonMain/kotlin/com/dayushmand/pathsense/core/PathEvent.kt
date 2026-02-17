package com.dayushmand.pathsense.core

sealed class PathEvent {
    data class Started(val sessionId: String, val point: PathPoint) : PathEvent()
    data class Updated(val sessionId: String, val points: List<PathPoint>) : PathEvent()
    data class MetricsUpdated(val sessionId: String, val metrics: PathMetrics) : PathEvent()
    data class Ended(val sessionId: String, val points: List<PathPoint>) : PathEvent()
    data class MetricsEnded(val sessionId: String, val metrics: PathMetrics) : PathEvent()
    data class GestureRecognized(val sessionId: String, val match: GestureMatch) : PathEvent()
    data class Cancelled(val sessionId: String) : PathEvent()
}
