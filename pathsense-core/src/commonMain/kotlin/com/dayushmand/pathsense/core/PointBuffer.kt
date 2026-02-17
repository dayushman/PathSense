package com.dayushmand.pathsense.core

internal class PointBuffer(private val maxPoints: Int) {
    private val deque = ArrayDeque<PathPoint>()

    val size: Int
        get() = deque.size

    fun add(point: PathPoint) {
        if (maxPoints <= 0) return
        if (deque.size >= maxPoints) {
            deque.removeFirst()
        }
        deque.addLast(point)
    }

    fun clear() {
        deque.clear()
    }

    fun lastOrNull(): PathPoint? = deque.lastOrNull()

    fun toList(): List<PathPoint> = deque.toList()
}
