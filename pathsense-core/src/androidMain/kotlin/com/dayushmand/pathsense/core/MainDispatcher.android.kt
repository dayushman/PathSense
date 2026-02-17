package com.dayushmand.pathsense.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual val MainDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
