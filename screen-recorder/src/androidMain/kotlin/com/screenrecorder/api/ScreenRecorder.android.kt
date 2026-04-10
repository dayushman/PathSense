package com.screenrecorder.api

actual class ScreenRecorder {
    actual companion object {
        actual val state: RecordingState get() = RecordingState.IDLE
        actual fun show() {}
        actual fun hide() {}
        actual fun destroy() {}
    }
}
