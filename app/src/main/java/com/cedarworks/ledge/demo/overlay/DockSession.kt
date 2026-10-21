package com.cedarworks.ledge.demo.overlay

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.cedarworks.ledge.LedgeEdge
import com.cedarworks.ledge.demo.DemoState

/**
 * Position, edge, and sample actions shared by the home screen and the dock
 * drawn over other apps. The slider and the edge handle write the same values.
 */
@Stable
class DockSession private constructor(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private var listener: (() -> Unit)? = null

    var anchorFraction by mutableFloatStateOf(prefs.getFloat(KEY_ANCHOR, 0.42f))
        private set

    var edge by mutableStateOf(
        if (prefs.getBoolean(KEY_EDGE_END, true)) LedgeEdge.End else LedgeEdge.Start,
    )
        private set

    var selectedKey by mutableStateOf(prefs.getString(KEY_PANEL, "shortcuts") ?: "shortcuts")
        private set

    var expanded by mutableStateOf(false)
        private set

    var overlayRunning by mutableStateOf(false)

    /** Usable vertical travel for the handle, in pixels. Written by the overlay. */
    var travelPx: Float = 1f

    val demo = DemoState()

    private var wasInCall = demo.inCall

    fun updateAnchor(value: Float) {
        val next = value.coerceIn(0f, 1f)
        if (next == anchorFraction) return
        anchorFraction = next
        prefs.edit().putFloat(KEY_ANCHOR, next).apply()
        listener?.invoke()
    }

    fun updateEdge(value: LedgeEdge) {
        if (value == edge) return
        edge = value
        prefs.edit().putBoolean(KEY_EDGE_END, value == LedgeEdge.End).apply()
        listener?.invoke()
    }

    fun updateSelectedKey(value: String) {
        if (value == selectedKey) return
        selectedKey = value
        prefs.edit().putString(KEY_PANEL, value).apply()
        listener?.invoke()
    }

    fun updateExpanded(value: Boolean) {
        if (value == expanded) return
        expanded = value
        listener?.invoke()
    }

    fun setOverlayListener(listener: (() -> Unit)?) {
        this.listener = listener
    }

    fun syncCall(onOpen: () -> Unit) {
        val inCall = demo.inCall
        if (inCall && !wasInCall) {
            updateSelectedKey("call")
            onOpen()
        } else if (!inCall && (selectedKey == "call" || selectedKey == "contact")) {
            updateSelectedKey("shortcuts")
        }
        wasInCall = inCall
    }

    companion object {
        private const val PREFS = "ledge_dock"
        private const val KEY_ANCHOR = "anchor"
        private const val KEY_EDGE_END = "edge_end"
        private const val KEY_PANEL = "panel"

        @Volatile
        private var instance: DockSession? = null

        fun get(context: Context): DockSession {
            instance?.let { return it }
            return synchronized(this) {
                instance ?: DockSession(context).also { instance = it }
            }
        }
    }
}
