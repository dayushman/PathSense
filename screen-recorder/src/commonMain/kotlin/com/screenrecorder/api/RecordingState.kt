package com.screenrecorder.api

enum class RecordingState {
    IDLE,
    REQUESTING_PERMISSION,
    RECORDING,
    STOPPING,
}
