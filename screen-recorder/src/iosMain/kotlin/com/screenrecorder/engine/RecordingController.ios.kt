package com.screenrecorder.engine

import com.screenrecorder.api.*

internal actual class RecordingController {
    actual var onAction: (Action) -> Unit = {}

    // Bridge callbacks that Swift sets
    var onRequestPermissions: (() -> Unit)? = null
    var onPrepare: ((ScreenRecorderConfig) -> Unit)? = null
    var onStartCapture: (() -> Unit)? = null
    var onStopCapture: (() -> Unit)? = null
    var onRelease: (() -> Unit)? = null

    actual fun requestPermissions() {
        onRequestPermissions?.invoke()
    }

    actual fun prepare(config: ScreenRecorderConfig) {
        onPrepare?.invoke(config)
    }

    actual fun startCapture() {
        onStartCapture?.invoke()
    }

    actual fun stopCapture() {
        onStopCapture?.invoke()
    }

    actual fun release() {
        onRelease?.invoke()
    }

    // Called from Swift to feed actions back
    fun reportEncoderReady() {
        onAction(Action.EncoderReady)
    }

    fun reportEncoderStopped() {
        onAction(Action.EncoderStopped)
    }

    fun reportFileReady(path: String, durationMs: Long, fileSizeBytes: Long, width: Int, height: Int) {
        onAction(Action.FileReady(RecordingFile(
            path = path,
            durationMs = durationMs,
            fileSizeBytes = fileSizeBytes,
            width = width,
            height = height,
        )))
    }

    fun reportFailed(message: String) {
        onAction(Action.Failed(RecordingError.EncoderFailed(message)))
    }
}
