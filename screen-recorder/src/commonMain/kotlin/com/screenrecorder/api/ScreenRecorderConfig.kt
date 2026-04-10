package com.screenrecorder.api

data class ScreenRecorderConfig(
    var tintColor: Long = 0xFFFF3B30,
    var bubblePosition: BubblePosition = BubblePosition.TRAILING_CENTER,
    var audioEnabled: Boolean = false,
    var videoQuality: VideoQuality = VideoQuality.DEVICE_NATIVE,
    var maxDurationSec: Int = 300,
    var outputFormat: OutputFormat = OutputFormat.MP4,
    var listener: ((RecordingEvent) -> Unit)? = null,
    var pathSenseEnabled: Boolean = true,
) {
    constructor() : this(
        tintColor = 0xFFFF3B30,
        bubblePosition = BubblePosition.TRAILING_CENTER,
        audioEnabled = false,
        videoQuality = VideoQuality.DEVICE_NATIVE,
        maxDurationSec = 300,
        outputFormat = OutputFormat.MP4,
        listener = null,
        pathSenseEnabled = true,
    )
}

enum class BubblePosition {
    LEADING_CENTER, TRAILING_CENTER,
    LEADING_TOP, TRAILING_TOP,
    LEADING_BOTTOM, TRAILING_BOTTOM,
}

enum class VideoQuality(val width: Int, val height: Int, val bitrateMbps: Float) {
    DEVICE_NATIVE(0, 0, 8f),
    SD_480(854, 480, 2f),
    HD_720(1280, 720, 5f),
    FHD_1080(1920, 1080, 8f),
}

enum class OutputFormat(val fileExtension: String) {
    MP4("mp4"),
    MOV("mov"),
}
