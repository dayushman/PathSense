package com.dayushmand.pathsense.sample.view

import android.app.Application
import android.util.Log
import com.screenrecorder.api.RecordingEvent
import com.screenrecorder.api.ScreenRecorder
import com.screenrecorder.api.ScreenRecorderConfig

class SampleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ScreenRecorder.init(this, ScreenRecorderConfig().apply {
            audioEnabled = false
            listener = { event ->
                when (event) {
                    is RecordingEvent.RecordingStarted -> Log.d("ScreenRecorder", "Recording started: ${event.sessionId}")
                    is RecordingEvent.DurationUpdate -> Log.d("ScreenRecorder", "Duration: ${event.elapsedMs}ms")
                    is RecordingEvent.RecordingStopped -> Log.d("ScreenRecorder", "Saved to: ${event.file.path}")
                    is RecordingEvent.RecordingFailed -> Log.e("ScreenRecorder", "Error: ${event.error.message}")
                    is RecordingEvent.PermissionRequired -> Log.d("ScreenRecorder", "Permission needed: ${event.type}")
                    is RecordingEvent.PermissionGranted -> Log.d("ScreenRecorder", "Permission granted: ${event.type}")
                    is RecordingEvent.PermissionDenied -> Log.e("ScreenRecorder", "Permission denied: ${event.type}")
                    is RecordingEvent.BubbleShown -> Log.d("ScreenRecorder", "Bubble shown")
                    is RecordingEvent.BubbleHidden -> Log.d("ScreenRecorder", "Bubble hidden")
                }
            }
        })
    }
}
