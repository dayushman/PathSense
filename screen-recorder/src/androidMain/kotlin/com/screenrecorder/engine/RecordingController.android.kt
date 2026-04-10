package com.screenrecorder.engine

import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Build
import com.screenrecorder.api.*
import java.io.File

internal actual class RecordingController {
    actual var onAction: (Action) -> Unit = {}

    private var context: Context? = null
    private var mediaProjection: MediaProjection? = null
    private var mediaRecorder: MediaRecorder? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var outputFile: File? = null
    private var config: ScreenRecorderConfig? = null

    fun setContext(context: Context) {
        this.context = context
    }

    fun setMediaProjection(projection: MediaProjection) {
        this.mediaProjection = projection
    }

    actual fun requestPermissions() {
        // Permission flow is handled by ScreenRecorderService via Activity result.
        // The service triggers the permission flow and calls onAction when done.
    }

    actual fun prepare(config: ScreenRecorderConfig) {
        this.config = config
        val ctx = context ?: run {
            onAction(Action.Failed(RecordingError.SystemUnavailable("Context not available")))
            return
        }

        try {
            val dir = File(ctx.cacheDir, "screen-recorder")
            if (!dir.exists()) dir.mkdirs()
            outputFile = File(dir, "rec_${System.currentTimeMillis()}.${config.outputFormat.fileExtension}")

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(ctx)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder!!.apply {
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                if (config.audioEnabled) {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                }
                setOutputFormat(
                    if (config.outputFormat == OutputFormat.MP4) MediaRecorder.OutputFormat.MPEG_4
                    else MediaRecorder.OutputFormat.MPEG_4 // MOV uses same container on Android
                )
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                if (config.audioEnabled) {
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                }
                setVideoSize(config.videoQuality.width, config.videoQuality.height)
                setVideoEncodingBitRate((config.videoQuality.bitrateMbps * 1_000_000).toInt())
                setVideoFrameRate(30)
                setOutputFile(outputFile!!.absolutePath)
                prepare()
            }
            onAction(Action.EncoderReady)
        } catch (e: Exception) {
            onAction(Action.Failed(RecordingError.EncoderFailed(e.message ?: "Failed to prepare recorder")))
        }
    }

    actual fun startCapture() {
        val projection = mediaProjection ?: run {
            onAction(Action.Failed(RecordingError.SystemUnavailable("MediaProjection not available")))
            return
        }
        val recorder = mediaRecorder ?: return
        val ctx = context ?: return

        try {
            val metrics = ctx.resources.displayMetrics
            virtualDisplay = projection.createVirtualDisplay(
                "ScreenRecorder",
                config!!.videoQuality.width,
                config!!.videoQuality.height,
                metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                recorder.surface,
                null, null
            )
            recorder.start()
        } catch (e: Exception) {
            onAction(Action.Failed(RecordingError.EncoderFailed(e.message ?: "Failed to start capture")))
        }
    }

    actual fun stopCapture() {
        try {
            mediaRecorder?.stop()
        } catch (e: RuntimeException) {
            onAction(Action.Failed(RecordingError.EncoderFailed("No frames captured")))
            return
        }

        virtualDisplay?.release()
        virtualDisplay = null
        mediaProjection?.stop()
        mediaProjection = null

        val file = outputFile ?: run {
            onAction(Action.Failed(RecordingError.EncoderFailed("Output file missing")))
            return
        }

        val durationMs = try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            duration?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        }

        onAction(Action.EncoderStopped)
        onAction(Action.FileReady(RecordingFile(
            path = file.absolutePath,
            durationMs = durationMs,
            fileSizeBytes = file.length(),
            width = config?.videoQuality?.width ?: 0,
            height = config?.videoQuality?.height ?: 0,
        )))
    }

    actual fun release() {
        try { mediaRecorder?.release() } catch (_: Exception) {}
        try { virtualDisplay?.release() } catch (_: Exception) {}
        try { mediaProjection?.stop() } catch (_: Exception) {}
        mediaRecorder = null
        virtualDisplay = null
        mediaProjection = null
        outputFile = null
    }
}
