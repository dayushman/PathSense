package com.dayushmand.pathsense.core

data class GestureMatch(
    val type: GestureType,
    val score: Float,
    val algorithm: String,
)
