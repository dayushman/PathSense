package com.screenrecorder.api

expect class ScreenRecorder {
    companion object {
        val state: RecordingState
        fun show()
        fun hide()
        fun destroy()
    }
}
