package com.screenrecorder.api

sealed class RecordingEvent {
    data class PermissionRequired(val type: PermissionType) : RecordingEvent()
    data class PermissionGranted(val type: PermissionType) : RecordingEvent()
    data class PermissionDenied(val type: PermissionType) : RecordingEvent()
    data class RecordingStarted(val sessionId: String) : RecordingEvent()
    data class DurationUpdate(val sessionId: String, val elapsedMs: Long) : RecordingEvent()
    data class RecordingStopped(val sessionId: String, val file: RecordingFile) : RecordingEvent()
    data class RecordingFailed(val sessionId: String, val error: RecordingError) : RecordingEvent()
    data object BubbleShown : RecordingEvent()
    data object BubbleHidden : RecordingEvent()
}

enum class PermissionType { OVERLAY, SCREEN_CAPTURE, MICROPHONE }
