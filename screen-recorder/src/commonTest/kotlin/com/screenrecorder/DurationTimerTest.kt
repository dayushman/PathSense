package com.screenrecorder

import com.screenrecorder.engine.DurationTimer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for DurationTimer.
 *
 * DurationTimer internally uses MainDispatcher (an expect val).
 * In commonTest, this dispatcher is not readily available, so we cannot
 * directly test DurationTimer.start() without platform-specific test support.
 *
 * These tests verify the timer's stop/cancel contract and basic construction.
 * Full tick-based testing should be done in platform-specific test source sets
 * where MainDispatcher is properly defined.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DurationTimerTest {

    @Test
    fun timer_canBeCreatedWithScope() = runTest {
        val timer = DurationTimer(this)
        // Timer is created successfully; no crash
        timer.stop()
    }

    @Test
    fun stop_beforeStart_doesNotThrow() = runTest {
        val timer = DurationTimer(this)
        // Calling stop without starting should be a safe no-op
        timer.stop()
        timer.stop() // double stop also safe
    }
}
