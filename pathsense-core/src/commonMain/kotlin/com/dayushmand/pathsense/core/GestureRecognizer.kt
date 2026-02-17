package com.dayushmand.pathsense.core

fun interface GestureRecognizer {
    fun recognize(points: List<PathPoint>): GestureMatch?
}
