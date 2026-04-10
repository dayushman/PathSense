package com.screenrecorder.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class DurationTimer(private val scope: CoroutineScope) {
    private var job: Job? = null

    fun start(onTick: (elapsedMs: Long) -> Unit) {
        val startTime = currentTimeMillis()
        job = scope.launch(MainDispatcher) {
            while (isActive) {
                delay(1000)
                onTick(currentTimeMillis() - startTime)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
