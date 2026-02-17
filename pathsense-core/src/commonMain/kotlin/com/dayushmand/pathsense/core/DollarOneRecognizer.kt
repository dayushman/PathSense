package com.dayushmand.pathsense.core

import kotlin.math.PI

internal class DollarOneRecognizer(
    private val templates: List<Template> = Template.defaults(),
    private val threshold: Float = 0.75f,
) : GestureRecognizer {

    override fun recognize(points: List<PathPoint>): GestureMatch? {
        if (points.size < 2) return null
        val candidate = normalize(points)
        val candidateVector = Resampler.vectorize(candidate)

        var bestScore = -1f
        var bestType = GestureType.UNKNOWN

        for (template in templates) {
            val dist = Resampler.cosineDistance(candidateVector, template.vector)
            val score = 1f - (dist / (PI.toFloat() / 2f))
            if (score > bestScore) {
                bestScore = score
                bestType = template.type
            }
        }

        val clamped = bestScore.coerceIn(0f, 1f)
        return if (clamped >= threshold) {
            GestureMatch(bestType, clamped, "dollar1")
        } else {
            GestureMatch(GestureType.UNKNOWN, clamped, "dollar1")
        }
    }

    private fun normalize(points: List<PathPoint>): List<PathPoint> {
        val resampled = Resampler.resample(points, 64)
        val angle = Resampler.indicativeAngle(resampled)
        val rotated = Resampler.rotate(resampled, -angle)
        val scaled = Resampler.scaleToSquare(rotated, 1f)
        return Resampler.translateToOrigin(scaled)
    }

    data class Template(val type: GestureType, val vector: FloatArray) {
        companion object {
            fun defaults(): List<Template> {
                return listOf(
                    buildLine(),
                    buildCircle(),
                    buildRectangle(),
                    buildZigZag(),
                )
            }

            private fun buildLine(): Template {
                val points = listOf(
                    PathPoint(0f, 0f, 0L),
                    PathPoint(100f, 0f, 10L),
                )
                return Template(GestureType.LINE, vectorize(points))
            }

            private fun buildCircle(): Template {
                val points = ArrayList<PathPoint>()
                val steps = 64
                for (i in 0 until steps) {
                    val t = (2 * PI * i / steps).toFloat()
                    points.add(PathPoint(kotlin.math.cos(t) * 50f, kotlin.math.sin(t) * 50f, i.toLong()))
                }
                return Template(GestureType.CIRCLE, vectorize(points))
            }

            private fun buildRectangle(): Template {
                val points = listOf(
                    PathPoint(0f, 0f, 0L),
                    PathPoint(100f, 0f, 10L),
                    PathPoint(100f, 60f, 20L),
                    PathPoint(0f, 60f, 30L),
                    PathPoint(0f, 0f, 40L),
                )
                return Template(GestureType.RECTANGLE, vectorize(points))
            }

            private fun buildZigZag(): Template {
                val points = listOf(
                    PathPoint(0f, 0f, 0L),
                    PathPoint(30f, 20f, 10L),
                    PathPoint(60f, -20f, 20L),
                    PathPoint(90f, 20f, 30L),
                    PathPoint(120f, -20f, 40L),
                )
                return Template(GestureType.ZIGZAG, vectorize(points))
            }

            private fun vectorize(points: List<PathPoint>): FloatArray {
                val normalized = normalize(points)
                return Resampler.vectorize(normalized)
            }

            private fun normalize(points: List<PathPoint>): List<PathPoint> {
                val resampled = Resampler.resample(points, 64)
                val angle = Resampler.indicativeAngle(resampled)
                val rotated = Resampler.rotate(resampled, -angle)
                val scaled = Resampler.scaleToSquare(rotated, 1f)
                return Resampler.translateToOrigin(scaled)
            }
        }
    }
}
