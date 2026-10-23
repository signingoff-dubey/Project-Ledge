package com.cedarworks.ledge.demo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DockWorkflowTest {
    @Test
    fun aMissingWorkflowStartsWithTheStarterTools() {
        assertEquals(DockModule.starter, DockModule.parse(null))
    }

    @Test
    fun anEmptyWorkflowStaysEmpty() {
        assertTrue(DockModule.parse("").isEmpty())
    }

    @Test
    fun unknownKeysAreDroppedAndOrderIsKept() {
        val parsed = DockModule.parse("timer,nope,clipboard,timer")
        assertEquals(listOf(DockModule.Timer, DockModule.Clipboard), parsed)
        assertEquals("timer,clipboard", DockModule.encode(parsed))
    }

    @Test
    fun addAppendsOnceAndRemoveDrops() {
        val added = DockModule.add(listOf(DockModule.Media), DockModule.Timer)
        assertEquals(listOf(DockModule.Media, DockModule.Timer), added)
        assertEquals(added, DockModule.add(added, DockModule.Timer))
        assertEquals(listOf(DockModule.Media), DockModule.remove(added, DockModule.Timer))
    }

    @Test
    fun linesRoundTrip() {
        val stored = encodeLines(listOf("one", "two"))
        assertEquals(listOf("one", "two"), decodeLines(stored))
        assertTrue(decodeLines(null).isEmpty())
    }

    @Test
    fun localDraftStaysEmptyUntilThereIsAPrompt() {
        assertEquals("", localDraft("  "))
        val draft = localDraft("pack the bag")
        assertTrue(draft.startsWith("Local draft for pack the bag."))
    }

    @Test
    fun pocketKeepsTextAndFallsBackToAFileLabel() {
        assertEquals("Hello", pocketLine(subject = null, text = " Hello ", fileLabel = null))
        assertEquals("Note — Hello", pocketLine(subject = "Note", text = "Hello", fileLabel = null))
        assertEquals("Shared image", pocketLine(subject = " ", text = "", fileLabel = "Shared image"))
        assertNull(pocketLine(subject = "", text = "", fileLabel = null))
    }
}
