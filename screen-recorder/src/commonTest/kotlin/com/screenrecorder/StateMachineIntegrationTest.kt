package com.screenrecorder

import com.screenrecorder.api.PermissionType
import com.screenrecorder.api.RecordingError
import com.screenrecorder.api.RecordingEvent
import com.screenrecorder.api.RecordingFile
import com.screenrecorder.engine.Action
import com.screenrecorder.engine.InternalState
import com.screenrecorder.engine.RecordingStateMachine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Integration-style tests that verify the state machine transitions produce
 * the correct state + action pairs that the orchestrator would map to events.
 *
 * Since RecordingController is an expect class, we cannot instantiate
 * RecordingOrchestrator in commonTest. Instead, we replicate the orchestrator's
 * event-mapping logic here to verify the state machine drives the correct events.
 */
class StateMachineIntegrationTest {

    /**
     * Simulates the event the orchestrator would emit for a given action + resulting state.
     * This mirrors RecordingOrchestrator.handleAction logic.
     */
    private fun mapToEvent(action: Action, newState: InternalState, sessionId: String): RecordingEvent? {
        return when (newState) {
            InternalState.PREPARING -> RecordingEvent.PermissionGranted(PermissionType.SCREEN_CAPTURE)
            InternalState.RECORDING -> RecordingEvent.RecordingStarted(sessionId)
            InternalState.IDLE -> when (action) {
                is Action.FileReady -> RecordingEvent.RecordingStopped(sessionId, action.file)
                is Action.Failed -> RecordingEvent.RecordingFailed(sessionId, action.error)
                is Action.PermissionDenied -> RecordingEvent.PermissionDenied(PermissionType.SCREEN_CAPTURE)
                else -> null
            }
            else -> null
        }
    }

    @Test
    fun permissionGranted_emitsPermissionGrantedEvent() {
        val sm = RecordingStateMachine()
        sm.transition(Action.TapRecord)
        val newState = sm.transition(Action.PermissionGranted)

        val event = mapToEvent(Action.PermissionGranted, newState, "session1")
        assertIs<RecordingEvent.PermissionGranted>(event)
        assertEquals(PermissionType.SCREEN_CAPTURE, event.type)
    }

    @Test
    fun encoderReady_emitsRecordingStartedEvent() {
        val sm = RecordingStateMachine()
        sm.transition(Action.TapRecord)
        sm.transition(Action.PermissionGranted)
        val newState = sm.transition(Action.EncoderReady)

        val event = mapToEvent(Action.EncoderReady, newState, "session1")
        assertIs<RecordingEvent.RecordingStarted>(event)
        assertEquals("session1", event.sessionId)
    }

    @Test
    fun fileReady_emitsRecordingStoppedEvent() {
        val sm = RecordingStateMachine()
        sm.transition(Action.TapRecord)
        sm.transition(Action.PermissionGranted)
        sm.transition(Action.EncoderReady)
        sm.transition(Action.TapStop)
        sm.transition(Action.EncoderStopped)

        val file = RecordingFile("/tmp/video.mp4", 5000L, 2048L, 1280, 720)
        val action = Action.FileReady(file)
        val newState = sm.transition(action)

        val event = mapToEvent(action, newState, "session1")
        assertIs<RecordingEvent.RecordingStopped>(event)
        assertEquals("session1", event.sessionId)
        assertEquals(file, event.file)
    }

    @Test
    fun failed_emitsRecordingFailedEvent() {
        val sm = RecordingStateMachine()
        sm.transition(Action.TapRecord)
        sm.transition(Action.PermissionGranted)
        sm.transition(Action.EncoderReady)

        val error = RecordingError.EncoderFailed("codec error")
        val action = Action.Failed(error)
        val newState = sm.transition(action)

        assertEquals(InternalState.IDLE, newState)
        val event = mapToEvent(action, newState, "session1")
        assertIs<RecordingEvent.RecordingFailed>(event)
        assertEquals("session1", event.sessionId)
        assertEquals("codec error", event.error.message)
    }

    @Test
    fun permissionDenied_emitsPermissionDeniedEvent() {
        val sm = RecordingStateMachine()
        sm.transition(Action.TapRecord)

        val action = Action.PermissionDenied
        val newState = sm.transition(action)

        assertEquals(InternalState.IDLE, newState)
        val event = mapToEvent(action, newState, "session1")
        assertIs<RecordingEvent.PermissionDenied>(event)
        assertEquals(PermissionType.SCREEN_CAPTURE, event.type)
    }

    @Test
    fun maxDuration_transitionsToStopping() {
        val sm = RecordingStateMachine()
        sm.transition(Action.TapRecord)
        sm.transition(Action.PermissionGranted)
        sm.transition(Action.EncoderReady)

        val newState = sm.transition(Action.MaxDuration)
        assertEquals(InternalState.STOPPING, newState)
    }

    @Test
    fun fullHappyPath_producesCorrectEventSequence() {
        val sm = RecordingStateMachine()
        val sessionId = "test_session"
        val events = mutableListOf<RecordingEvent>()

        // IDLE -> REQUESTING_PERMISSION (TapRecord doesn't produce an event via handleAction)
        sm.transition(Action.TapRecord)

        // REQUESTING_PERMISSION -> PREPARING
        var state = sm.transition(Action.PermissionGranted)
        mapToEvent(Action.PermissionGranted, state, sessionId)?.let { events.add(it) }

        // PREPARING -> RECORDING
        state = sm.transition(Action.EncoderReady)
        mapToEvent(Action.EncoderReady, state, sessionId)?.let { events.add(it) }

        // RECORDING -> STOPPING
        state = sm.transition(Action.TapStop)
        mapToEvent(Action.TapStop, state, sessionId)?.let { events.add(it) }

        // STOPPING -> FINALIZING
        state = sm.transition(Action.EncoderStopped)
        mapToEvent(Action.EncoderStopped, state, sessionId)?.let { events.add(it) }

        // FINALIZING -> IDLE
        val file = RecordingFile("/videos/out.mp4", 30_000L, 4096L, 1920, 1080)
        val fileAction = Action.FileReady(file)
        state = sm.transition(fileAction)
        mapToEvent(fileAction, state, sessionId)?.let { events.add(it) }

        assertEquals(3, events.size)
        assertIs<RecordingEvent.PermissionGranted>(events[0])
        assertIs<RecordingEvent.RecordingStarted>(events[1])
        assertIs<RecordingEvent.RecordingStopped>(events[2])
    }

    @Test
    fun failedDuringPreparing_producesFailedEvent() {
        val sm = RecordingStateMachine()
        val sessionId = "fail_session"

        sm.transition(Action.TapRecord)
        sm.transition(Action.PermissionGranted)

        val error = RecordingError.SystemUnavailable("hardware encoder not available")
        val action = Action.Failed(error)
        val newState = sm.transition(action)

        assertEquals(InternalState.IDLE, newState)
        val event = mapToEvent(action, newState, sessionId)
        assertIs<RecordingEvent.RecordingFailed>(event)
        assertTrue(event.error.message.contains("hardware encoder"))
    }

    @Test
    fun failedDuringFinalizing_producesFailedEvent() {
        val sm = RecordingStateMachine()
        sm.transition(Action.TapRecord)
        sm.transition(Action.PermissionGranted)
        sm.transition(Action.EncoderReady)
        sm.transition(Action.TapStop)
        sm.transition(Action.EncoderStopped)

        val error = RecordingError.DiskFull("not enough space to write file")
        val action = Action.Failed(error)
        val newState = sm.transition(action)

        assertEquals(InternalState.IDLE, newState)
        val event = mapToEvent(action, newState, "session")
        assertIs<RecordingEvent.RecordingFailed>(event)
        assertIs<RecordingError.DiskFull>(event.error)
    }
}
