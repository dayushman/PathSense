package com.screenrecorder.engine

import com.screenrecorder.api.ScreenRecorderConfig

internal expect class RecordingController {
    fun requestPermissions()
    fun prepare(config: ScreenRecorderConfig)
    fun startCapture()
    fun stopCapture()
    fun release()
    var onAction: (Action) -> Unit
}
