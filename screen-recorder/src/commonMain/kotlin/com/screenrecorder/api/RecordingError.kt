package com.screenrecorder.api

sealed class RecordingError(val message: String) {
    class PermissionDenied(message: String) : RecordingError(message)
    class EncoderFailed(message: String) : RecordingError(message)
    class DiskFull(message: String) : RecordingError(message)
    class MaxDurationReached : RecordingError("Max duration reached")
    class SystemUnavailable(message: String) : RecordingError(message)
}
