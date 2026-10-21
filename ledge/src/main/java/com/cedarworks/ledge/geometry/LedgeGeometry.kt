package com.cedarworks.ledge.geometry

import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Side-drop outline. One edge is a straight line that sits on the screen edge.
 * The other edge leaves that line at a point, flares through a concave neck
 * into a round shoulder, runs straight down the body, and returns to a point.
 * Top and bottom match.
 */
internal object LedgeGeometry {
    private val knotT = floatArrayOf(
        0.00f, 0.04f, 0.08f, 0.12f, 0.15f, 0.17f, 0.185f, 0.20f, 0.22f, 0.24f, 0.27f, 0.50f,
    )
    private val knotW = floatArrayOf(
        0.00f, 0.03f, 0.07f, 0.13f, 0.22f, 0.32f, 0.46f, 0.72f, 0.89f, 0.96f, 1.00f, 1.00f,
    )
    private val knotSlope = monotoneSlopes(knotT, knotW)

    /** Width of the drop at [y], as a fraction of [width]. The tips are points. */
    fun span(y: Float, width: Float, height: Float): Float {
        if (width <= 0f || height <= 0f) return 0f
        val t = (y / height).coerceIn(0f, 1f)
        return width * widthFraction(t)
    }

    /**
     * [stretch] is `-1` while the drop thins back toward the screen edge, `0`
     * at rest, and `1` while the neck fills in. The straight edge does not move,
     * and the outline stays inside the panel.
     */
    fun trace(
        width: Float,
        height: Float,
        steps: Int = 200,
        stretch: Float = 0f,
        emergeFromEnd: Boolean = true,
    ): List<Pair<Float, Float>> {
        if (width <= 1f || height <= 1f || steps < 2) return emptyList()
        val clamped = stretch.coerceIn(-1f, 1f)
        val points = ArrayList<Pair<Float, Float>>(steps * 2 + 2)
        val outerEdge = ArrayList<Pair<Float, Float>>(steps + 1)
        val innerEdge = ArrayList<Pair<Float, Float>>(steps + 1)
        for (index in 0..steps) {
            val y = height * index / steps
            val t = (y / height).coerceIn(0f, 1f)
            val base = widthFraction(t)
            val belly = sin(PI.toFloat() * t).coerceAtLeast(0f)
            val fraction = if (clamped >= 0f) {
                base + (1f - base) * clamped * 0.35f * belly
            } else {
                base * (1f + clamped * 0.28f * belly)
            }.coerceIn(0f, 1f)
            val span = width * fraction
            if (emergeFromEnd) {
                outerEdge += width to y
                innerEdge += (width - span) to y
            } else {
                outerEdge += 0f to y
                innerEdge += span to y
            }
        }
        points += outerEdge
        for (index in innerEdge.lastIndex downTo 0) {
            points += innerEdge[index]
        }
        return points
    }

    private fun widthFraction(t: Float): Float {
        val end = min(t, 1f - t).coerceIn(0f, 0.5f)
        return hermite(end).coerceIn(0f, 1f)
    }

    private fun hermite(t: Float): Float {
        var index = 0
        while (index < knotT.lastIndex && knotT[index + 1] < t) index++
        if (index >= knotT.lastIndex) return knotW.last()
        val step = knotT[index + 1] - knotT[index]
        if (step <= 0f) return knotW[index]
        val u = ((t - knotT[index]) / step).coerceIn(0f, 1f)
        val u2 = u * u
        val u3 = u2 * u
        return (2f * u3 - 3f * u2 + 1f) * knotW[index] +
            (u3 - 2f * u2 + u) * step * knotSlope[index] +
            (-2f * u3 + 3f * u2) * knotW[index + 1] +
            (u3 - u2) * step * knotSlope[index + 1]
    }
}

/** Fritsch–Carlson slopes, so the neck flares without dipping back. */
private fun monotoneSlopes(xs: FloatArray, ys: FloatArray): FloatArray {
    val count = xs.size
    val delta = FloatArray(count - 1)
    for (index in 0 until count - 1) {
        delta[index] = (ys[index + 1] - ys[index]) / (xs[index + 1] - xs[index])
    }
    val slope = FloatArray(count)
    slope[0] = delta[0]
    slope[count - 1] = delta[count - 2]
    for (index in 1 until count - 1) {
        slope[index] = if (delta[index - 1] == 0f || delta[index] == 0f || delta[index - 1] * delta[index] < 0f) {
            0f
        } else {
            (delta[index - 1] + delta[index]) / 2f
        }
    }
    for (index in 0 until count - 1) {
        if (delta[index] == 0f) {
            slope[index] = 0f
            slope[index + 1] = 0f
            continue
        }
        val alpha = slope[index] / delta[index]
        val beta = slope[index + 1] / delta[index]
        val sum = alpha * alpha + beta * beta
        if (sum > 9f) {
            val tau = 3f / sqrt(sum)
            slope[index] = tau * alpha * delta[index]
            slope[index + 1] = tau * beta * delta[index]
        }
    }
    return slope
}
