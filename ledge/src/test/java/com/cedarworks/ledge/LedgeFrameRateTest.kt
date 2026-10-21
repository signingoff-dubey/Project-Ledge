package com.cedarworks.ledge

import org.junit.Assert.assertEquals
import org.junit.Test

class LedgeFrameRateTest {
    @Test
    fun picksTheFastestRateAtTheCurrentResolution() {
        val modes = listOf(
            RefreshMode(width = 1080, height = 2400, refreshRate = 60f),
            RefreshMode(width = 1080, height = 2400, refreshRate = 120f),
            RefreshMode(width = 1440, height = 3200, refreshRate = 165f),
        )
        assertEquals(120f, fastestRefreshRate(1080, 2400, modes), 0.01f)
    }

    @Test
    fun fallsBackToAnyModeWhenTheCurrentSizeIsMissing() {
        val modes = listOf(RefreshMode(width = 1080, height = 2400, refreshRate = 90f))
        assertEquals(90f, fastestRefreshRate(720, 1600, modes), 0.01f)
    }

    @Test
    fun anEmptyModeListHasNoPreference() {
        assertEquals(0f, fastestRefreshRate(1080, 2400, emptyList()), 0f)
    }
}
