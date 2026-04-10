package com.screenrecorder.api

import com.screenrecorder.engine.*
import kotlinx.coroutines.*

actual class ScreenRecorder {
    actual companion object {
        private var orchestrator: RecordingOrchestrator? = null
        private var controller: RecordingController? = null
        private var config: ScreenRecorderConfig? = null
        private var scope: CoroutineScope? = null

        actual val state: RecordingState
            get() {
                val internal = orchestrator?.currentInternalState
                return when (internal) {
                    InternalState.IDLE, null -> RecordingState.IDLE
                    InternalState.REQUESTING_PERMISSION -> RecordingState.REQUESTING_PERMISSION
                    InternalState.PREPARING -> RecordingState.REQUESTING_PERMISSION
                    InternalState.RECORDING -> RecordingState.RECORDING
                    InternalState.STOPPING -> RecordingState.STOPPING
                    InternalState.FINALIZING -> RecordingState.STOPPING
                }
            }

        fun start(config: ScreenRecorderConfig) {
            this.config = config
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

            val ctrl = RecordingController()
            controller = ctrl

            val stateMachine = RecordingStateMachine()
            val timer = DurationTimer(scope!!)

            orchestrator = RecordingOrchestrator(config, stateMachine, ctrl, timer, scope!!)
        }

        // Called from Swift to trigger record
        fun onBubbleTapRecord() {
            orchestrator?.onBubbleTapRecord()
        }

        // Called from Swift to trigger stop
        fun onBubbleTapStop() {
            orchestrator?.onBubbleTapStop()
        }

        // Called from Swift when permission result arrives
        fun onPermissionResult(granted: Boolean) {
            if (granted) {
                controller?.onAction?.invoke(Action.PermissionGranted)
            } else {
                controller?.onAction?.invoke(Action.PermissionDenied)
            }
        }

        actual fun show() {
            // Bubble show is managed from Swift side
            config?.listener?.invoke(RecordingEvent.BubbleShown)
        }

        actual fun hide() {
            config?.listener?.invoke(RecordingEvent.BubbleHidden)
        }

        actual fun destroy() {
            controller?.release()
            scope?.cancel()
            scope = null
            orchestrator = null
            controller = null
            config = null
        }
    }
}
