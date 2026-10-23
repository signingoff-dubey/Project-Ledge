package com.cedarworks.ledge.demo

import androidx.annotation.StringRes
import com.cedarworks.ledge.demo.R

enum class DockModule(
    val key: String,
    @param:StringRes val labelRes: Int,
) {
    Clipboard("clipboard", R.string.module_clipboard),
    Compass("compass", R.string.module_compass),
    Counter("counter", R.string.module_counter),
    Torch("torch", R.string.module_torch),
    AppShortcut("shortcut", R.string.module_shortcut),
    Bluetooth("bluetooth", R.string.bluetooth),
    Pocket("pocket", R.string.module_pocket),
    LocalModel("local_model", R.string.module_local_model),
    Progress("progress", R.string.module_progress),
    Motes("motes", R.string.module_motes),
    Scroll("scroll", R.string.module_scroll),
    CircleCut("circle_cut", R.string.module_circle_cut),
    AudioFlow("audio_flow", R.string.module_audio_flow),
    ColorPicker("color", R.string.module_color),
    Caffeine("caffeine", R.string.module_caffeine),
    Media("media", R.string.module_media),
    Brightness("brightness", R.string.module_brightness),
    Timer("timer", R.string.module_timer),
    AppLibrary("library", R.string.module_library),
    ;

    companion object {
        private val byKey = entries.associateBy { it.key }

        val starter = listOf(Media, Timer, Bluetooth, Torch, Clipboard, Brightness)

        fun parse(stored: String?): List<DockModule> {
            if (stored == null) return starter
            if (stored.isEmpty()) return emptyList()
            return stored.split(',').mapNotNull { byKey[it] }.distinct()
        }

        fun encode(modules: List<DockModule>): String = modules.joinToString(",") { it.key }

        fun add(current: List<DockModule>, module: DockModule): List<DockModule> {
            if (module in current) return current
            return current + module
        }

        fun remove(current: List<DockModule>, module: DockModule): List<DockModule> {
            return current.filterNot { it == module }
        }
    }
}

internal fun encodeLines(lines: List<String>): String =
    lines.joinToString("\u001f") { it.replace('\u001f', ' ') }

internal fun decodeLines(stored: String?): List<String> =
    stored?.split('\u001f')?.filter { it.isNotEmpty() } ?: emptyList()

/**
 * A short on-device draft. It never leaves the phone.
 * [prompt] is trimmed. An empty prompt returns an empty draft.
 */
fun localDraft(prompt: String): String {
    val cleaned = prompt.trim().replace(Regex("\\s+"), " ")
    if (cleaned.isEmpty()) return ""
    val words = cleaned.split(" ")
    val topic = words.take(6).joinToString(" ")
    val step = when (words.size % 3) {
        0 -> "Restate the goal in one sentence."
        1 -> "Do the smallest next step on this device."
        else -> "Check the result before adding more."
    }
    return "Local draft for $topic. $step"
}

/**
 * Text the user shared into Pocket. [fileLabel] is used when the share has
 * a stream and no text. Returns null when there is nothing to keep.
 */
fun pocketLine(subject: String?, text: String?, fileLabel: String?): String? {
    val cleanSubject = subject?.trim().orEmpty()
    val cleanText = text?.trim().orEmpty()
    val line = when {
        cleanText.isNotEmpty() && cleanSubject.isNotEmpty() -> "$cleanSubject — $cleanText"
        cleanText.isNotEmpty() -> cleanText
        cleanSubject.isNotEmpty() -> cleanSubject
        !fileLabel.isNullOrBlank() -> fileLabel.trim()
        else -> return null
    }
    return line.replace('\u001f', ' ').take(280)
}
