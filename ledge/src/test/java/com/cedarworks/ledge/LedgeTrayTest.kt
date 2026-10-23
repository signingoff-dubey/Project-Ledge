package com.cedarworks.ledge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LedgeTrayTest {
    @Test
    fun aShortWorkflowSlidesFartherThanOneColumn() {
        val width = workflowTrayWidthPx(columnCount = 1, columnWidthPx = 100f, gapPx = 12f, capPx = 1000f)
        assertTrue(width > 300f)
        assertTrue(width < 400f)
    }

    @Test
    fun theTrayMatchesTheColumnsWhenTheyFit() {
        val width = workflowTrayWidthPx(columnCount = 4, columnWidthPx = 100f, gapPx = 10f, capPx = 1000f)
        assertEquals(430f, width, 0.1f)
    }

    @Test
    fun aLongWorkflowStopsAtTheScreenCap() {
        val width = workflowTrayWidthPx(columnCount = 12, columnWidthPx = 100f, gapPx = 12f, capPx = 500f)
        assertEquals(500f, width, 0.1f)
    }

    @Test
    fun theCapNeverShrinksAColumn() {
        val width = workflowTrayWidthPx(columnCount = 1, columnWidthPx = 100f, gapPx = 12f, capPx = 80f)
        assertEquals(100f, width, 0.1f)
    }
}
