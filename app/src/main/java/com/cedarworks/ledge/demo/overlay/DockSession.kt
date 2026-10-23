package com.cedarworks.ledge.demo.overlay

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.IntentCompat
import com.cedarworks.ledge.LedgeEdge
import com.cedarworks.ledge.demo.DemoState
import com.cedarworks.ledge.demo.DockModule
import com.cedarworks.ledge.demo.decodeLines
import com.cedarworks.ledge.demo.encodeLines
import com.cedarworks.ledge.demo.pocketLine

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

    var modules by mutableStateOf(DockModule.parse(prefs.getString(KEY_MODULES, null)))
        private set

    var expanded by mutableStateOf(false)
        private set

    var overlayRunning by mutableStateOf(false)

    /** Usable vertical travel for the handle, in pixels. Written by the overlay. */
    var travelPx: Float = 1f

    val demo = DemoState()

    private var wasInCall = demo.inCall

    init {
        restoreExtras()
    }

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
        if (inCall && !wasInCall) onOpen()
        wasInCall = inCall
    }

    fun addModule(module: DockModule) {
        val next = DockModule.add(modules, module)
        if (next == modules) return
        modules = next
        prefs.edit().putString(KEY_MODULES, DockModule.encode(next)).apply()
        listener?.invoke()
    }

    fun removeModule(module: DockModule) {
        val next = DockModule.remove(modules, module)
        if (next == modules) return
        modules = next
        prefs.edit().putString(KEY_MODULES, DockModule.encode(next)).apply()
        listener?.invoke()
    }

    fun addMote(text: String) {
        val cleaned = text.trim().replace('\u001f', ' ').take(80)
        if (cleaned.isEmpty()) return
        demo.motes = (listOf(cleaned) + demo.motes).take(30)
        persistExtras()
    }

    fun addPocket(line: String) {
        val cleaned = line.trim().replace('\u001f', ' ').take(280)
        if (cleaned.isEmpty()) return
        demo.pocket = (listOf(cleaned) + demo.pocket).take(40)
        persistExtras()
    }

    fun clearPocket() {
        demo.pocket = emptyList()
        persistExtras()
    }

    fun captureShare(intent: Intent?, imageLabel: String): Boolean {
        if (intent?.action != Intent.ACTION_SEND) return false
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
        val stream = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
        val line = pocketLine(
            subject = subject,
            text = text,
            fileLabel = if (stream != null) imageLabel else null,
        ) ?: return false
        addPocket(line)
        return true
    }

    fun persistExtras() {
        prefs.edit()
            .putInt(KEY_COUNTER, demo.counter)
            .putInt(KEY_PROGRESS, demo.progressMark)
            .putInt(KEY_CUTS, demo.circleCuts)
            .putInt(KEY_COLOR, demo.colorIndex)
            .putBoolean(KEY_CAFFEINE, demo.caffeine)
            .putFloat(KEY_BRIGHTNESS, demo.brightness)
            .putInt(KEY_AUDIO, demo.audioRoute)
            .putLong(KEY_TIMER, demo.timerEndsAt)
            .putString(KEY_MOTES, encodeLines(demo.motes))
            .putString(KEY_POCKET, encodeLines(demo.pocket))
            .putString(KEY_PROMPT, demo.localPrompt)
            .putString(KEY_ANSWER, demo.localAnswer)
            .putString(KEY_SHORTCUT, demo.shortcutPackage)
            .apply()
        listener?.invoke()
    }

    private fun restoreExtras() {
        demo.counter = prefs.getInt(KEY_COUNTER, 0)
        demo.progressMark = prefs.getInt(KEY_PROGRESS, 0)
        demo.circleCuts = prefs.getInt(KEY_CUTS, 0)
        demo.colorIndex = prefs.getInt(KEY_COLOR, 0)
        demo.caffeine = prefs.getBoolean(KEY_CAFFEINE, false)
        demo.brightness = prefs.getFloat(KEY_BRIGHTNESS, 0.85f)
        demo.audioRoute = prefs.getInt(KEY_AUDIO, 0)
        demo.timerEndsAt = prefs.getLong(KEY_TIMER, 0L)
        demo.motes = decodeLines(prefs.getString(KEY_MOTES, null))
        demo.pocket = decodeLines(prefs.getString(KEY_POCKET, null))
        demo.localPrompt = prefs.getString(KEY_PROMPT, "").orEmpty()
        demo.localAnswer = prefs.getString(KEY_ANSWER, "").orEmpty()
        demo.shortcutPackage = prefs.getString(KEY_SHORTCUT, "").orEmpty()
    }

    companion object {
        private const val PREFS = "ledge_dock"
        private const val KEY_ANCHOR = "anchor"
        private const val KEY_EDGE_END = "edge_end"
        private const val KEY_PANEL = "panel"
        private const val KEY_MODULES = "modules"
        private const val KEY_COUNTER = "counter"
        private const val KEY_PROGRESS = "progress"
        private const val KEY_CUTS = "cuts"
        private const val KEY_COLOR = "color"
        private const val KEY_CAFFEINE = "caffeine"
        private const val KEY_BRIGHTNESS = "brightness"
        private const val KEY_AUDIO = "audio"
        private const val KEY_TIMER = "timer_end"
        private const val KEY_MOTES = "motes"
        private const val KEY_POCKET = "pocket"
        private const val KEY_PROMPT = "local_prompt"
        private const val KEY_ANSWER = "local_answer"
        private const val KEY_SHORTCUT = "shortcut"

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
