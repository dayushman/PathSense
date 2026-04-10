package com.screenrecorder.engine

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual val MainDispatcher: CoroutineDispatcher = Dispatchers.Main
