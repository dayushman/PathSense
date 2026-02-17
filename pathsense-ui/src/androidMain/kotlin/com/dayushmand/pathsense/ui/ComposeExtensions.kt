package com.dayushmand.pathsense.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap

internal fun Long.toComposeColor(): Color {
    return Color(this.toInt())
}

internal fun com.dayushmand.pathsense.ui.StrokeCap.toComposeCap(): StrokeCap {
    return when (this) {
        com.dayushmand.pathsense.ui.StrokeCap.BUTT -> StrokeCap.Butt
        com.dayushmand.pathsense.ui.StrokeCap.ROUND -> StrokeCap.Round
        com.dayushmand.pathsense.ui.StrokeCap.SQUARE -> StrokeCap.Square
    }
}
