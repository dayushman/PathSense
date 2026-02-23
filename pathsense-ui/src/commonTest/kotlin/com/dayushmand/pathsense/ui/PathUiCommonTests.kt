package com.dayushmand.pathsense.ui

import com.dayushmand.pathsense.core.PathOverlayConfig
import kotlin.test.Test
import kotlin.test.assertEquals

class PathUiCommonTests {
    @Test
    fun defaultOverlayConfigHasDebugOnly() {
        val config = PathOverlayConfig()
        assertEquals(true, config.debugOnly)
    }
}
