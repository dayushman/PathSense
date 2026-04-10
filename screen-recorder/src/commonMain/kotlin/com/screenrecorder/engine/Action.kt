package com.screenrecorder.engine

import com.screenrecorder.api.RecordingError
import com.screenrecorder.api.RecordingFile

internal sealed class Action {
    object TapRecord : Action()
    object TapStop : Action()
    object PermissionGranted : Action()
    object PermissionDenied : Action()
    object EncoderReady : Action()
    object EncoderStopped : Action()
    data class FileReady(val file: RecordingFile) : Action()
    data class Failed(val error: RecordingError) : Action()
    object MaxDuration : Action()
}

internal enum class InternalState {
    IDLE, REQUESTING_PERMISSION, PREPARING, RECORDING, STOPPING, FINALIZING
}
