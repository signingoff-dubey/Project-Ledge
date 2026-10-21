package com.cedarworks.ledge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidDockAnimatorTest {
    @Test
    fun restingPositionsKeepTheSilhouetteUndeformed() {
        assertEquals(0f, LiquidDockAnimator.stretch(0f, emerging = true, reducedMotion = false), 0.0001f)
        assertEquals(0f, LiquidDockAnimator.stretch(1f, emerging = false, reducedMotion = false), 0.0001f)
    }

    @Test
    fun theMiddleOfTheTripStretchesOutAndCompressesBack() {
        val opening = LiquidDockAnimator.stretch(0.5f, emerging = true, reducedMotion = false)
        val closing = LiquidDockAnimator.stretch(0.5f, emerging = false, reducedMotion = false)
        assertTrue(opening > 0.9f)
        assertTrue(closing < -0.7f)
    }

    @Test
    fun reducedMotionSkipsDeformation() {
        assertEquals(0f, LiquidDockAnimator.stretch(0.5f, emerging = true, reducedMotion = true), 0f)
        assertTrue(LiquidDockAnimator.reducedMotion(0f))
        assertTrue(!LiquidDockAnimator.reducedMotion(1f))
    }
}
