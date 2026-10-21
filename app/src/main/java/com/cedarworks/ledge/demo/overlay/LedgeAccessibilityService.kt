package com.cedarworks.ledge.demo.overlay

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat

/**
 * Keeps the edge dock process available after Ledge leaves the screen.
 * Events are not read: no window text, no source nodes, and no keystrokes.
 */
class LedgeAccessibilityService : AccessibilityService() {
    private var lastEnsure = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        ensureDock()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        ensureDock()
    }

    override fun onInterrupt() = Unit

    private fun ensureDock() {
        if (!Settings.canDrawOverlays(this)) return
        val now = SystemClock.uptimeMillis()
        if (now - lastEnsure < 2_000L) return
        lastEnsure = now
        try {
            ContextCompat.startForegroundService(this, Intent(this, DockOverlayService::class.java))
        } catch (_: RuntimeException) {
            // Starting from the background can be refused. The next event tries again.
        }
    }
}
