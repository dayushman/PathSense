package com.screenrecorder.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class RecordingStateMachine {
    private val _state = MutableStateFlow(InternalState.IDLE)
    val state: StateFlow<InternalState> = _state.asStateFlow()

    fun transition(action: Action): InternalState {
        val current = _state.value
        val next = resolve(current, action)
        if (next == current) return current
        _state.value = next
        return next
    }

    fun reset() {
        _state.value = InternalState.IDLE
    }

    private fun resolve(state: InternalState, action: Action): InternalState {
        return when (state) {
            InternalState.IDLE -> when (action) {
                is Action.TapRecord -> InternalState.REQUESTING_PERMISSION
                else -> state
            }
            InternalState.REQUESTING_PERMISSION -> when (action) {
                is Action.PermissionGranted -> InternalState.PREPARING
                is Action.PermissionDenied -> InternalState.IDLE
                else -> state
            }
            InternalState.PREPARING -> when (action) {
                is Action.EncoderReady -> InternalState.RECORDING
                is Action.Failed -> InternalState.IDLE
                else -> state
            }
            InternalState.RECORDING -> when (action) {
                is Action.TapStop -> InternalState.STOPPING
                is Action.MaxDuration -> InternalState.STOPPING
                is Action.Failed -> InternalState.IDLE
                else -> state
            }
            InternalState.STOPPING -> when (action) {
                is Action.EncoderStopped -> InternalState.FINALIZING
                else -> state
            }
            InternalState.FINALIZING -> when (action) {
                is Action.FileReady -> InternalState.IDLE
                is Action.Failed -> InternalState.IDLE
                else -> state
            }
        }
    }
}
