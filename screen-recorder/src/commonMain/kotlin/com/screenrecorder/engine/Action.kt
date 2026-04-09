package com.screenrecorder.engine

import com.screenrecorder.api.RecordingError
import com.screenrecorder.api.RecordingFile

internal sealed class Action {
    data object TapRecord : Action()
    data object TapStop : Action()
    data object PermissionGranted : Action()
    data object PermissionDenied : Action()
    data object EncoderReady : Action()
    data object EncoderStopped : Action()
    data class FileReady(val file: RecordingFile) : Action()
    data class Failed(val error: RecordingError) : Action()
    data object MaxDuration : Action()
}

internal enum class InternalState {
    IDLE, REQUESTING_PERMISSION, PREPARING, RECORDING, STOPPING, FINALIZING
}
