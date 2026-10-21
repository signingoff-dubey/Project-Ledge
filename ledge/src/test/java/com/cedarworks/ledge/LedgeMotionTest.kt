package com.cedarworks.ledge

import org.junit.Assert.assertEquals
import org.junit.Test

class LedgeMotionTest {
    @Test
    fun dragFromTheRightEdgeOpensWhenTheFingerMovesInward() {
        val delta = dragDeltaProgress(dx = -20f, hideSign = 1f, travelPx = 200f, trayWidthPx = 220f)
        assertEquals(0.1f, delta, 0.0001f)
    }

    @Test
    fun dragFromTheLeftEdgeOpensWhenTheFingerMovesInward() {
        val delta = dragDeltaProgress(dx = 20f, hideSign = -1f, travelPx = 200f, trayWidthPx = 220f)
        assertEquals(0.1f, delta, 0.0001f)
    }

    @Test
    fun dragIsIgnoredBeforeTheTrayIsMeasured() {
        val delta = dragDeltaProgress(dx = -20f, hideSign = 1f, travelPx = 200f, trayWidthPx = 0f)
        assertEquals(0f, delta, 0f)
    }

    @Test
    fun flingDirectionFollowsTheEdge() {
        assertEquals(1_500f, openingVelocity(velocityX = -1_500f, hideSign = 1f), 0f)
        assertEquals(1_500f, openingVelocity(velocityX = 1_500f, hideSign = -1f), 0f)
        assertEquals(-1_500f, openingVelocity(velocityX = 1_500f, hideSign = 1f), 0f)
    }

    @Test
    fun settleUsesDistanceWhenTheFlingIsSlow() {
        assertEquals(1f, settleTarget(progress = 0.45f, openingVelocityPxPerSec = 0f), 0f)
        assertEquals(0f, settleTarget(progress = 0.44f, openingVelocityPxPerSec = 0f), 0f)
    }

    @Test
    fun settleUsesAFastFlingOverDistance() {
        assertEquals(1f, settleTarget(progress = 0.1f, openingVelocityPxPerSec = 1_201f), 0f)
        assertEquals(0f, settleTarget(progress = 0.9f, openingVelocityPxPerSec = -1_201f), 0f)
    }
}
