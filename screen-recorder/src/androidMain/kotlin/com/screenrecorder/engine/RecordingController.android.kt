package com.screenrecorder.engine

import com.screenrecorder.api.ScreenRecorderConfig

internal actual class RecordingController {
    actual var onAction: (Action) -> Unit = {}
    actual fun requestPermissions() {}
    actual fun prepare(config: ScreenRecorderConfig) {}
    actual fun startCapture() {}
    actual fun stopCapture() {}
    actual fun release() {}
}
