package com.screenrecorder

import com.screenrecorder.api.RecordingError
import com.screenrecorder.api.RecordingFile
import com.screenrecorder.engine.Action
import com.screenrecorder.engine.InternalState
import com.screenrecorder.engine.RecordingStateMachine
import kotlin.test.Test
import kotlin.test.assertEquals

class StateMachineTest {

    private fun createStateMachine() = RecordingStateMachine()

    // ── Valid transitions ────────────────────────────────────────────────

    @Test
    fun idle_tapRecord_transitionsToRequestingPermission() {
        val sm = createStateMachine()
        val result = sm.transition(Action.TapRecord)
        assertEquals(InternalState.REQUESTING_PERMISSION, result)
        assertEquals(InternalState.REQUESTING_PERMISSION, sm.state.value)
    }

    @Test
    fun requestingPermission_permissionGranted_transitionsToPreparing() {
        val sm = createStateMachine()
        sm.transition(Action.TapRecord)
        val result = sm.transition(Action.PermissionGranted)
        assertEquals(InternalState.PREPARING, result)
        assertEquals(InternalState.PREPARING, sm.state.value)
    }

    @Test
    fun requestingPermission_permissionDenied_transitionsToIdle() {
        val sm = createStateMachine()
        sm.transition(Action.TapRecord)
        val result = sm.transition(Action.PermissionDenied)
        assertEquals(InternalState.IDLE, result)
        assertEquals(InternalState.IDLE, sm.state.value)
    }

    @Test
    fun preparing_encoderReady_transitionsToRecording() {
        val sm = createStateMachine()
        sm.transition(Action.TapRecord)
        sm.transition(Action.PermissionGranted)
        val result = sm.transition(Action.EncoderReady)
        assertEquals(InternalState.RECORDING, result)
        assertEquals(InternalState.RECORDING, sm.state.value)
    }

    @Test
    fun preparing_failed_transitionsToIdle() {
        val sm = createStateMachine()
        sm.transition(Action.TapRecord)
        sm.transition(Action.PermissionGranted)
        val result = sm.transition(Action.Failed(RecordingError.EncoderFailed("encoder init failed")))
        assertEquals(InternalState.IDLE, result)
        assertEquals(InternalState.IDLE, sm.state.value)
    }

    @Test
    fun recording_tapStop_transitionsToStopping() {
        val sm = createStateMachine()
        driveToRecording(sm)
        val result = sm.transition(Action.TapStop)
        assertEquals(InternalState.STOPPING, result)
        assertEquals(InternalState.STOPPING, sm.state.value)
    }

    @Test
    fun recording_maxDuration_transitionsToStopping() {
        val sm = createStateMachine()
        driveToRecording(sm)
        val result = sm.transition(Action.MaxDuration)
        assertEquals(InternalState.STOPPING, result)
        assertEquals(InternalState.STOPPING, sm.state.value)
    }

    @Test
    fun recording_failed_transitionsToIdle() {
        val sm = createStateMachine()
        driveToRecording(sm)
        val result = sm.transition(Action.Failed(RecordingError.DiskFull("no space")))
        assertEquals(InternalState.IDLE, result)
        assertEquals(InternalState.IDLE, sm.state.value)
    }

    @Test
    fun stopping_encoderStopped_transitionsToFinalizing() {
        val sm = createStateMachine()
        driveToStopping(sm)
        val result = sm.transition(Action.EncoderStopped)
        assertEquals(InternalState.FINALIZING, result)
        assertEquals(InternalState.FINALIZING, sm.state.value)
    }

    @Test
    fun finalizing_fileReady_transitionsToIdle() {
        val sm = createStateMachine()
        driveToFinalizing(sm)
        val file = RecordingFile("/tmp/video.mp4", 5000L, 1024L, 1280, 720)
        val result = sm.transition(Action.FileReady(file))
        assertEquals(InternalState.IDLE, result)
        assertEquals(InternalState.IDLE, sm.state.value)
    }

    @Test
    fun finalizing_failed_transitionsToIdle() {
        val sm = createStateMachine()
        driveToFinalizing(sm)
        val result = sm.transition(Action.Failed(RecordingError.EncoderFailed("mux failed")))
        assertEquals(InternalState.IDLE, result)
        assertEquals(InternalState.IDLE, sm.state.value)
    }

    // ── Invalid transitions (no-ops) ────────────────────────────────────

    @Test
    fun idle_tapStop_staysIdle() {
        val sm = createStateMachine()
        val result = sm.transition(Action.TapStop)
        assertEquals(InternalState.IDLE, result)
        assertEquals(InternalState.IDLE, sm.state.value)
    }

    @Test
    fun idle_permissionGranted_staysIdle() {
        val sm = createStateMachine()
        val result = sm.transition(Action.PermissionGranted)
        assertEquals(InternalState.IDLE, result)
        assertEquals(InternalState.IDLE, sm.state.value)
    }

    @Test
    fun idle_encoderReady_staysIdle() {
        val sm = createStateMachine()
        val result = sm.transition(Action.EncoderReady)
        assertEquals(InternalState.IDLE, result)
    }

    @Test
    fun idle_encoderStopped_staysIdle() {
        val sm = createStateMachine()
        val result = sm.transition(Action.EncoderStopped)
        assertEquals(InternalState.IDLE, result)
    }

    @Test
    fun idle_maxDuration_staysIdle() {
        val sm = createStateMachine()
        val result = sm.transition(Action.MaxDuration)
        assertEquals(InternalState.IDLE, result)
    }

    @Test
    fun recording_tapRecord_staysRecording() {
        val sm = createStateMachine()
        driveToRecording(sm)
        val result = sm.transition(Action.TapRecord)
        assertEquals(InternalState.RECORDING, result)
        assertEquals(InternalState.RECORDING, sm.state.value)
    }

    @Test
    fun recording_permissionGranted_staysRecording() {
        val sm = createStateMachine()
        driveToRecording(sm)
        val result = sm.transition(Action.PermissionGranted)
        assertEquals(InternalState.RECORDING, result)
    }

    @Test
    fun requestingPermission_tapStop_staysRequestingPermission() {
        val sm = createStateMachine()
        sm.transition(Action.TapRecord)
        val result = sm.transition(Action.TapStop)
        assertEquals(InternalState.REQUESTING_PERMISSION, result)
    }

    @Test
    fun preparing_tapStop_staysPreparing() {
        val sm = createStateMachine()
        sm.transition(Action.TapRecord)
        sm.transition(Action.PermissionGranted)
        val result = sm.transition(Action.TapStop)
        assertEquals(InternalState.PREPARING, result)
    }

    @Test
    fun stopping_tapStop_staysStopping() {
        val sm = createStateMachine()
        driveToStopping(sm)
        val result = sm.transition(Action.TapStop)
        assertEquals(InternalState.STOPPING, result)
    }

    @Test
    fun stopping_failed_staysStopping() {
        val sm = createStateMachine()
        driveToStopping(sm)
        val result = sm.transition(Action.Failed(RecordingError.EncoderFailed("err")))
        assertEquals(InternalState.STOPPING, result)
    }

    @Test
    fun finalizing_tapStop_staysFinalizing() {
        val sm = createStateMachine()
        driveToFinalizing(sm)
        val result = sm.transition(Action.TapStop)
        assertEquals(InternalState.FINALIZING, result)
    }

    // ── reset() ─────────────────────────────────────────────────────────

    @Test
    fun reset_fromRecording_returnsToIdle() {
        val sm = createStateMachine()
        driveToRecording(sm)
        sm.reset()
        assertEquals(InternalState.IDLE, sm.state.value)
    }

    @Test
    fun reset_fromPreparing_returnsToIdle() {
        val sm = createStateMachine()
        sm.transition(Action.TapRecord)
        sm.transition(Action.PermissionGranted)
        sm.reset()
        assertEquals(InternalState.IDLE, sm.state.value)
    }

    @Test
    fun reset_fromStopping_returnsToIdle() {
        val sm = createStateMachine()
        driveToStopping(sm)
        sm.reset()
        assertEquals(InternalState.IDLE, sm.state.value)
    }

    @Test
    fun reset_fromFinalizing_returnsToIdle() {
        val sm = createStateMachine()
        driveToFinalizing(sm)
        sm.reset()
        assertEquals(InternalState.IDLE, sm.state.value)
    }

    @Test
    fun reset_fromRequestingPermission_returnsToIdle() {
        val sm = createStateMachine()
        sm.transition(Action.TapRecord)
        sm.reset()
        assertEquals(InternalState.IDLE, sm.state.value)
    }

    @Test
    fun reset_fromIdle_staysIdle() {
        val sm = createStateMachine()
        sm.reset()
        assertEquals(InternalState.IDLE, sm.state.value)
    }

    // ── StateFlow reflects current state ────────────────────────────────

    @Test
    fun stateFlow_initiallyIdle() {
        val sm = createStateMachine()
        assertEquals(InternalState.IDLE, sm.state.value)
    }

    @Test
    fun stateFlow_updatesAfterEachTransition() {
        val sm = createStateMachine()
        assertEquals(InternalState.IDLE, sm.state.value)

        sm.transition(Action.TapRecord)
        assertEquals(InternalState.REQUESTING_PERMISSION, sm.state.value)

        sm.transition(Action.PermissionGranted)
        assertEquals(InternalState.PREPARING, sm.state.value)

        sm.transition(Action.EncoderReady)
        assertEquals(InternalState.RECORDING, sm.state.value)
    }

    // ── Full happy path ─────────────────────────────────────────────────

    @Test
    fun fullHappyPath_idleThroughRecordingBackToIdle() {
        val sm = createStateMachine()
        assertEquals(InternalState.IDLE, sm.state.value)

        assertEquals(InternalState.REQUESTING_PERMISSION, sm.transition(Action.TapRecord))
        assertEquals(InternalState.PREPARING, sm.transition(Action.PermissionGranted))
        assertEquals(InternalState.RECORDING, sm.transition(Action.EncoderReady))
        assertEquals(InternalState.STOPPING, sm.transition(Action.TapStop))
        assertEquals(InternalState.FINALIZING, sm.transition(Action.EncoderStopped))

        val file = RecordingFile("/recordings/video.mp4", 10_000L, 2048L, 1920, 1080)
        assertEquals(InternalState.IDLE, sm.transition(Action.FileReady(file)))
        assertEquals(InternalState.IDLE, sm.state.value)
    }

    // ── Helper functions ────────────────────────────────────────────────

    private fun driveToRecording(sm: RecordingStateMachine) {
        sm.transition(Action.TapRecord)
        sm.transition(Action.PermissionGranted)
        sm.transition(Action.EncoderReady)
    }

    private fun driveToStopping(sm: RecordingStateMachine) {
        driveToRecording(sm)
        sm.transition(Action.TapStop)
    }

    private fun driveToFinalizing(sm: RecordingStateMachine) {
        driveToStopping(sm)
        sm.transition(Action.EncoderStopped)
    }
}
