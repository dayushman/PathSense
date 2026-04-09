package com.screenrecorder

import com.screenrecorder.api.BubblePosition
import com.screenrecorder.api.OutputFormat
import com.screenrecorder.api.RecordingState
import com.screenrecorder.api.ScreenRecorderConfig
import com.screenrecorder.api.VideoQuality
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertFalse

class ConfigTest {

    // ── Default values ──────────────────────────────────────────────────

    @Test
    fun defaultConfig_hasTintColorRed() {
        val config = ScreenRecorderConfig()
        assertEquals(0xFFFF3B30, config.tintColor)
    }

    @Test
    fun defaultConfig_hasBubblePositionTrailingCenter() {
        val config = ScreenRecorderConfig()
        assertEquals(BubblePosition.TRAILING_CENTER, config.bubblePosition)
    }

    @Test
    fun defaultConfig_hasAudioDisabled() {
        val config = ScreenRecorderConfig()
        assertFalse(config.audioEnabled)
    }

    @Test
    fun defaultConfig_hasVideoQualityHD720() {
        val config = ScreenRecorderConfig()
        assertEquals(VideoQuality.HD_720, config.videoQuality)
    }

    @Test
    fun defaultConfig_hasMaxDuration300Seconds() {
        val config = ScreenRecorderConfig()
        assertEquals(300, config.maxDurationSec)
    }

    @Test
    fun defaultConfig_hasOutputFormatMP4() {
        val config = ScreenRecorderConfig()
        assertEquals(OutputFormat.MP4, config.outputFormat)
    }

    @Test
    fun defaultConfig_hasNullListener() {
        val config = ScreenRecorderConfig()
        assertNull(config.listener)
    }

    // ── No-arg constructor matches parameterized defaults ───────────────

    @Test
    fun noArgConstructor_matchesParameterizedDefaults() {
        val noArg = ScreenRecorderConfig()
        val parameterized = ScreenRecorderConfig(
            tintColor = 0xFFFF3B30,
            bubblePosition = BubblePosition.TRAILING_CENTER,
            audioEnabled = false,
            videoQuality = VideoQuality.HD_720,
            maxDurationSec = 300,
            outputFormat = OutputFormat.MP4,
            listener = null,
        )
        assertEquals(parameterized.tintColor, noArg.tintColor)
        assertEquals(parameterized.bubblePosition, noArg.bubblePosition)
        assertEquals(parameterized.audioEnabled, noArg.audioEnabled)
        assertEquals(parameterized.videoQuality, noArg.videoQuality)
        assertEquals(parameterized.maxDurationSec, noArg.maxDurationSec)
        assertEquals(parameterized.outputFormat, noArg.outputFormat)
        assertEquals(parameterized.listener, noArg.listener)
    }

    // ── Enum values exist ───────────────────────────────────────────────

    @Test
    fun bubblePosition_hasAllExpectedValues() {
        val values = BubblePosition.entries
        assertEquals(6, values.size)
        val names = values.map { it.name }.toSet()
        assertEquals(
            setOf(
                "LEADING_CENTER", "TRAILING_CENTER",
                "LEADING_TOP", "TRAILING_TOP",
                "LEADING_BOTTOM", "TRAILING_BOTTOM",
            ),
            names,
        )
    }

    @Test
    fun videoQuality_hasAllExpectedValues() {
        val values = VideoQuality.entries
        assertEquals(3, values.size)
        val names = values.map { it.name }.toSet()
        assertEquals(setOf("SD_480", "HD_720", "FHD_1080"), names)
    }

    @Test
    fun outputFormat_hasAllExpectedValues() {
        val values = OutputFormat.entries
        assertEquals(2, values.size)
        val names = values.map { it.name }.toSet()
        assertEquals(setOf("MP4", "MOV"), names)
    }

    @Test
    fun recordingState_hasAllExpectedValues() {
        val values = RecordingState.entries
        assertEquals(4, values.size)
        val names = values.map { it.name }.toSet()
        assertEquals(setOf("IDLE", "REQUESTING_PERMISSION", "RECORDING", "STOPPING"), names)
    }

    // ── VideoQuality properties ─────────────────────────────────────────

    @Test
    fun videoQuality_sd480_hasCorrectDimensions() {
        assertEquals(854, VideoQuality.SD_480.width)
        assertEquals(480, VideoQuality.SD_480.height)
        assertEquals(2f, VideoQuality.SD_480.bitrateMbps)
    }

    @Test
    fun videoQuality_hd720_hasCorrectDimensions() {
        assertEquals(1280, VideoQuality.HD_720.width)
        assertEquals(720, VideoQuality.HD_720.height)
        assertEquals(5f, VideoQuality.HD_720.bitrateMbps)
    }

    @Test
    fun videoQuality_fhd1080_hasCorrectDimensions() {
        assertEquals(1920, VideoQuality.FHD_1080.width)
        assertEquals(1080, VideoQuality.FHD_1080.height)
        assertEquals(8f, VideoQuality.FHD_1080.bitrateMbps)
    }

    // ── OutputFormat file extensions ────────────────────────────────────

    @Test
    fun outputFormat_mp4_hasCorrectExtension() {
        assertEquals("mp4", OutputFormat.MP4.fileExtension)
    }

    @Test
    fun outputFormat_mov_hasCorrectExtension() {
        assertEquals("mov", OutputFormat.MOV.fileExtension)
    }
}
