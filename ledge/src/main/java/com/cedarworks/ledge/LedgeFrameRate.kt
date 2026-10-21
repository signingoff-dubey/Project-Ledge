package com.cedarworks.ledge

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.Display
import android.view.View
import android.view.Window
import kotlin.math.abs

internal data class RefreshMode(
    val width: Int,
    val height: Int,
    val refreshRate: Float,
)

/**
 * Highest refresh rate offered at the current resolution. A faster mode at a
 * different size is ignored so the slide does not switch resolution.
 */
internal fun fastestRefreshRate(
    currentWidth: Int,
    currentHeight: Int,
    modes: List<RefreshMode>,
): Float {
    if (modes.isEmpty()) return 0f
    val sameSize = modes.filter { it.width == currentWidth && it.height == currentHeight }
    val pool = if (sameSize.isNotEmpty()) sameSize else modes
    return pool.maxOf { it.refreshRate }
}

/**
 * Asks the display for its fastest rate while the dock is moving, then drops
 * the request when the dock is resting. The window preference is seamless-only,
 * so a panel that cannot switch cleanly stays on its current rate.
 */
internal object LedgeFrameRate {
    fun highestRefreshRate(window: Window): Float {
        val display = window.currentDisplay() ?: return 0f
        val mode = display.mode
        val modes = display.supportedModes.map {
            RefreshMode(it.physicalWidth, it.physicalHeight, it.refreshRate)
        }
        val fastest = fastestRefreshRate(mode.physicalWidth, mode.physicalHeight, modes)
        return if (fastest > 0f) fastest else display.refreshRate
    }

    fun setPreferredRefreshRate(window: Window, rate: Float) {
        val params = window.attributes
        if (abs(params.preferredRefreshRate - rate) < 0.5f) return
        params.preferredRefreshRate = rate
        window.attributes = params
    }

    fun vote(view: View, rate: Float) {
        if (Build.VERSION.SDK_INT < 35) return
        view.setRequestedFrameRate(
            if (rate > 0f) rate else View.REQUESTED_FRAME_RATE_CATEGORY_NO_PREFERENCE,
        )
    }

    private fun Window.currentDisplay(): Display? {
        return if (Build.VERSION.SDK_INT >= 30) {
            decorView.display ?: context.display
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay
        }
    }
}

internal tailrec fun Context.hostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.hostActivity()
    else -> null
}
