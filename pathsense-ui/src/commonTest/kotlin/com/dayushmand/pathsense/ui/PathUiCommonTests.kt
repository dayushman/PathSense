package com.dayushmand.pathsense.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class PathUiCommonTests {
    @Test
    fun defaultOverlayConfigHasDebugOnly() {
        val config = PathOverlayConfig()
        assertEquals(true, config.debugOnly)
    }
}
