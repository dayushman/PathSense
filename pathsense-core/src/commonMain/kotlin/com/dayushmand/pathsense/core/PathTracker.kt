package com.dayushmand.pathsense.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToLong
import kotlin.random.Random

class PathTracker(private val config: PathConfig = PathConfig()) {
    var listener: (PathEvent) -> Unit = {}

    private val buffer = PointBuffer(config.maxPoints)
    private val recognizers = LinkedHashSet<GestureRecognizer>()
    private val analysisScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val snapshots = Channel<Snapshot>(Channel.CONFLATED)

    private var sessionId: String? = null
    private var lastSampleTime = 0L
    private var lastAccepted: PathPoint? = null
    private var prevSmoothed1: PathPoint? = null
    private var prevSmoothed2: PathPoint? = null

    init {
        recognizers.add(DollarOneRecognizer())
        analysisScope.launch {
            for (snapshot in snapshots) {
                val metrics = computeMetrics(snapshot.points)
                withContext(MainDispatcher) {
                    if (!snapshot.isFinal) {
                        listener(PathEvent.MetricsUpdated(snapshot.sessionId, metrics))
                    } else {
                        listener(PathEvent.MetricsEnded(snapshot.sessionId, metrics))
                        val match = recognize(snapshot.points)
                        if (match != null) {
                            listener(PathEvent.GestureRecognized(snapshot.sessionId, match))
                        }
                    }
                }
            }
        }
    }

    val currentPoints: List<PathPoint>
        get() = buffer.toList()

    fun addRecognizer(r: GestureRecognizer) {
        recognizers.add(r)
    }

    fun removeRecognizer(r: GestureRecognizer) {
        recognizers.remove(r)
    }

    fun clearPoints() {
        buffer.clear()
        prevSmoothed1 = null
        prevSmoothed2 = null
        lastAccepted = null
    }

    fun onDown(p: PathPoint) {
        val id = newSessionId()
        sessionId = id
        clearPoints()

        val smoothed = smooth(p)
        pushPoint(smoothed)
        lastSampleTime = p.tMillis
        lastAccepted = smoothed

        listener(PathEvent.Started(id, smoothed))
        listener(PathEvent.Updated(id, currentPoints))
        snapshots.trySend(Snapshot(id, currentPoints, isFinal = false))
    }

    fun onMove(p: PathPoint) {
        val id = sessionId ?: return
        val intervalMs = max(1, (1000f / config.samplingHz).roundToLong())
        val last = lastAccepted
        if (last != null) {
            val dt = p.tMillis - lastSampleTime
            if (dt < intervalMs) return
            if (MathUtils.distance(last, p) < config.minDistancePx) return
        }

        val smoothed = smooth(p)
        pushPoint(smoothed)
        lastSampleTime = p.tMillis
        lastAccepted = smoothed

        listener(PathEvent.Updated(id, currentPoints))
        snapshots.trySend(Snapshot(id, currentPoints, isFinal = false))
    }

    fun onUp(p: PathPoint) {
        val id = sessionId ?: return
        val smoothed = smooth(p)
        pushPoint(smoothed)
        lastSampleTime = p.tMillis
        lastAccepted = smoothed

        listener(PathEvent.Ended(id, currentPoints))
        snapshots.trySend(Snapshot(id, currentPoints, isFinal = true))
        sessionId = null
    }

    fun onCancel() {
        val id = sessionId ?: return
        clearPoints()
        listener(PathEvent.Cancelled(id))
        sessionId = null
    }

    private fun pushPoint(point: PathPoint) {
        prevSmoothed2 = prevSmoothed1
        prevSmoothed1 = point
        buffer.add(point)
    }

    private fun smooth(point: PathPoint): PathPoint {
        if (config.smoothingWindow < 3) return point
        val p1 = prevSmoothed1
        val p2 = prevSmoothed2
        if (p1 == null || p2 == null) return point
        val x = (p1.x + p2.x + point.x) / 3f
        val y = (p1.y + p2.y + point.y) / 3f
        return PathPoint(x, y, point.tMillis)
    }

    private fun recognize(points: List<PathPoint>): GestureMatch? {
        var best: GestureMatch? = null
        for (recognizer in recognizers) {
            val match = recognizer.recognize(points) ?: continue
            if (best == null || match.score > best!!.score) {
                best = match
            }
        }
        return best
    }

    private fun newSessionId(): String {
        val time = currentTimeMillis()
        val rand = Random.nextInt(0, 1_000_000)
        return "ps-$time-$rand"
    }

    private data class Snapshot(
        val sessionId: String,
        val points: List<PathPoint>,
        val isFinal: Boolean,
    )
}
