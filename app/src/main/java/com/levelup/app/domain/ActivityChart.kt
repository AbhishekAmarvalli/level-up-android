package com.levelup.app.domain

import kotlin.math.sqrt

data class ChartSegment(
    val startX: Float,
    val startY: Float,
    val control1X: Float,
    val control1Y: Float,
    val control2X: Float,
    val control2Y: Float,
    val endX: Float,
    val endY: Float,
)

/**
 * Monotone cubic interpolation for the activity curve. Tangents are limited with
 * the Fritsch-Carlson rule, and every control point is clamped to the chart, so a
 * line between two values can never overshoot 0% or 100%.
 */
object ActivityChartMath {
    fun xFor(index: Int, count: Int, left: Float, right: Float): Float {
        if (count <= 1) return (left + right) / 2f
        return left + (right - left) * index.toFloat() / (count - 1).toFloat()
    }

    fun yFor(value: Float, top: Float, bottom: Float): Float {
        val safe = value.coerceIn(0f, 100f) / 100f
        return bottom - (bottom - top) * safe
    }

    fun nearestIndex(x: Float, count: Int, left: Float, right: Float): Int {
        if (count <= 1) return 0
        val fraction = ((x - left) / (right - left)).coerceIn(0f, 1f)
        return (fraction * (count - 1)).toInt().coerceIn(0, count - 1)
    }

    fun segments(
        values: List<Float>,
        left: Float,
        right: Float,
        top: Float,
        bottom: Float,
    ): List<ChartSegment> {
        val count = values.size
        if (count < 2) return emptyList()
        val xs = FloatArray(count) { xFor(it, count, left, right) }
        val ys = FloatArray(count) { yFor(values[it], top, bottom) }
        val dx = xs[1] - xs[0]
        if (dx <= 0f) return emptyList()

        val delta = FloatArray(count - 1) { (ys[it + 1] - ys[it]) / dx }
        val tangents = FloatArray(count)
        tangents[0] = delta[0]
        tangents[count - 1] = delta[count - 2]
        for (i in 1 until count - 1) tangents[i] = (delta[i - 1] + delta[i]) / 2f

        // Fritsch-Carlson monotonicity filter.
        for (i in 0 until count - 1) {
            if (delta[i] == 0f) {
                tangents[i] = 0f
                tangents[i + 1] = 0f
            } else {
                val a = tangents[i] / delta[i]
                val b = tangents[i + 1] / delta[i]
                val magnitude = a * a + b * b
                if (magnitude > 9f) {
                    val scale = 3f / sqrt(magnitude)
                    tangents[i] = scale * a * delta[i]
                    tangents[i + 1] = scale * b * delta[i]
                }
            }
        }

        val third = dx / 3f
        return (0 until count - 1).map { i ->
            ChartSegment(
                startX = xs[i],
                startY = ys[i],
                control1X = xs[i] + third,
                control1Y = (ys[i] + tangents[i] * third).coerceIn(top, bottom),
                control2X = xs[i + 1] - third,
                control2Y = (ys[i + 1] - tangents[i + 1] * third).coerceIn(top, bottom),
                endX = xs[i + 1],
                endY = ys[i + 1],
            )
        }
    }
}
