package com.screenrecorder.api

data class ScreenRecorderConfig(
    var tintColor: Long = 0xFFFF3B30,
    var bubblePosition: BubblePosition = BubblePosition.TRAILING_CENTER,
    var audioEnabled: Boolean = false,
    var videoQuality: VideoQuality = VideoQuality.HD_720,
    var maxDurationSec: Int = 300,
    var outputFormat: OutputFormat = OutputFormat.MP4,
    var listener: ((RecordingEvent) -> Unit)? = null,
) {
    constructor() : this(
        tintColor = 0xFFFF3B30,
        bubblePosition = BubblePosition.TRAILING_CENTER,
        audioEnabled = false,
        videoQuality = VideoQuality.HD_720,
        maxDurationSec = 300,
        outputFormat = OutputFormat.MP4,
        listener = null,
    )
}

enum class BubblePosition {
    LEADING_CENTER, TRAILING_CENTER,
    LEADING_TOP, TRAILING_TOP,
    LEADING_BOTTOM, TRAILING_BOTTOM,
}

enum class VideoQuality(val width: Int, val height: Int, val bitrateMbps: Float) {
    SD_480(854, 480, 2f),
    HD_720(1280, 720, 5f),
    FHD_1080(1920, 1080, 8f),
}

enum class OutputFormat(val fileExtension: String) {
    MP4("mp4"),
    MOV("mov"),
}
