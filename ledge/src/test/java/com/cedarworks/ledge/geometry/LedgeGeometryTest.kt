package com.cedarworks.ledge.geometry

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LedgeGeometryTest {
    private val width = 108f
    private val height = 460f

    @Test
    fun tipsArePointsAndTheBodyRunsFullWidth() {
        val tip = LedgeGeometry.span(0f, width, height)
        val neck = LedgeGeometry.span(height * 0.10f, width, height)
        val shoulder = LedgeGeometry.span(height * 0.20f, width, height)
        val body = LedgeGeometry.span(height * 0.50f, width, height)
        assertTrue(tip < width * 0.02f)
        assertTrue(neck < width * 0.12f)
        assertTrue(shoulder > width * 0.65f)
        assertTrue(shoulder > neck)
        assertEquals(width, body, 0.5f)
    }

    @Test
    fun silhouetteIsVerticallySymmetric() {
        val upper = LedgeGeometry.span(height * 0.18f, width, height)
        val lower = LedgeGeometry.span(height * 0.82f, width, height)
        assertEquals(upper, lower, 0.05f)
    }

    @Test
    fun theScreenEdgeIsAStraightLine() {
        assertStraightOuterEdge(emergeFromEnd = true, outerAt = width)
        assertStraightOuterEdge(emergeFromEnd = false, outerAt = 0f)
    }

    @Test
    fun traceStaysInsideThePanelBounds() {
        val points = LedgeGeometry.trace(width, height)
        assertTrue(points.size > 40)
        points.forEach { (x, y) ->
            assertTrue("x=$x", x in -0.5f..(width + 0.5f))
            assertTrue("y=$y", y in -0.5f..(height + 0.5f))
        }
    }

    @Test
    fun emergingStretchFillsTheNeckWithoutMovingTheOuterEdge() {
        val resting = band(0.20f, stretch = 0f)
        val emerging = band(0.20f, stretch = 1f)
        assertEquals(width, resting.second, 0.05f)
        assertEquals(width, emerging.second, 0.05f)
        assertTrue(emerging.second - emerging.first > resting.second - resting.first + 1f)
    }

    @Test
    fun closingStretchThinsTheBodyBackTowardTheEdge() {
        val resting = band(0.50f, stretch = 0f)
        val closing = band(0.50f, stretch = -1f)
        assertEquals(width, resting.second, 0.05f)
        assertEquals(width, closing.second, 0.05f)
        assertTrue(closing.second - closing.first < resting.second - resting.first - 1f)
        LedgeGeometry.trace(width, height, stretch = -1f).forEach { (x, y) ->
            assertTrue("x=$x", x in -0.5f..(width + 0.5f))
            assertTrue("y=$y", y in -0.5f..(height + 0.5f))
        }
    }

    private fun assertStraightOuterEdge(emergeFromEnd: Boolean, outerAt: Float) {
        val points = LedgeGeometry.trace(width, height, emergeFromEnd = emergeFromEnd)
        points.groupBy { it.second }.values.forEach { band ->
            val outer = if (emergeFromEnd) band.maxOf { it.first } else band.minOf { it.first }
            assertEquals(outerAt, outer, 0.05f)
        }
    }

    private fun band(fraction: Float, stretch: Float): Pair<Float, Float> {
        val y = height * fraction
        val samples = LedgeGeometry.trace(width, height, stretch = stretch).filter { (_, sampleY) ->
            abs(sampleY - y) < 0.01f
        }
        assertTrue(samples.size >= 2)
        return samples.minOf { it.first } to samples.maxOf { it.first }
    }
}
