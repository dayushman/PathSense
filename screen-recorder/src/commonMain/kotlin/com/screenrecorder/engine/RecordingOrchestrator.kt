package com.screenrecorder.engine

import com.screenrecorder.api.PermissionType
import com.screenrecorder.api.RecordingEvent
import com.screenrecorder.api.ScreenRecorderConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class RecordingOrchestrator(
    private val config: ScreenRecorderConfig,
    private val stateMachine: RecordingStateMachine,
    private val controller: RecordingController,
    private val durationTimer: DurationTimer,
    private val scope: CoroutineScope,
) {
    val currentInternalState: InternalState
        get() = stateMachine.state.value

    private var sessionId: String = ""

    init {
        controller.onAction = ::handleAction
    }

    fun onBubbleTapRecord() {
        sessionId = "rec_${currentTimeMillis()}"
        val newState = stateMachine.transition(Action.TapRecord)
        if (newState == InternalState.REQUESTING_PERMISSION) {
            controller.requestPermissions()
        }
    }

    fun onBubbleTapStop() {
        val newState = stateMachine.transition(Action.TapStop)
        if (newState == InternalState.STOPPING) {
            durationTimer.stop()
            controller.stopCapture()
        }
    }

    fun handleAction(action: Action) {
        val newState = stateMachine.transition(action)
        when (newState) {
            InternalState.PREPARING -> {
                emitEvent(RecordingEvent.PermissionGranted(PermissionType.SCREEN_CAPTURE))
                controller.prepare(config)
            }
            InternalState.RECORDING -> {
                controller.startCapture()
                // Only emit RecordingStarted and start timer if we're still in RECORDING
                // (startCapture might have failed and pushed us back to IDLE)
                if (stateMachine.state.value != InternalState.RECORDING) return
                emitEvent(RecordingEvent.RecordingStarted(sessionId))
                durationTimer.start { elapsed ->
                    emitEvent(RecordingEvent.DurationUpdate(sessionId, elapsed))
                    if (config.maxDurationSec > 0 && elapsed >= config.maxDurationSec * 1000L) {
                        durationTimer.stop()
                        controller.stopCapture()
                        stateMachine.transition(Action.MaxDuration)
                    }
                }
            }
            InternalState.IDLE -> {
                // Always stop timer when returning to IDLE (covers error paths too)
                durationTimer.stop()
                when (action) {
                    is Action.FileReady -> emitEvent(RecordingEvent.RecordingStopped(sessionId, action.file))
                    is Action.Failed -> emitEvent(RecordingEvent.RecordingFailed(sessionId, action.error))
                    is Action.PermissionDenied -> emitEvent(RecordingEvent.PermissionDenied(PermissionType.SCREEN_CAPTURE))
                    else -> {}
                }
            }
            else -> {}
        }
    }

    private fun emitEvent(event: RecordingEvent) {
        scope.launch(MainDispatcher) {
            config.listener?.invoke(event)
        }
    }
}
