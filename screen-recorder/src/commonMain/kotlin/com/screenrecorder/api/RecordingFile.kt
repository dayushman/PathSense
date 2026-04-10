package com.screenrecorder.api

data class RecordingFile(
    val path: String,
    val durationMs: Long,
    val fileSizeBytes: Long,
    val width: Int,
    val height: Int,
)
