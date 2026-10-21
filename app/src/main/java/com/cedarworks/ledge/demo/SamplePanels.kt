package com.cedarworks.ledge.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.cedarworks.ledge.AssistantLedge
import com.cedarworks.ledge.CallLedge
import com.cedarworks.ledge.ContactLedge
import com.cedarworks.ledge.LedgeAction
import com.cedarworks.ledge.LedgePanelScope
import com.cedarworks.ledge.LedgeToggleItem
import com.cedarworks.ledge.LocalLedgeMetrics
import com.cedarworks.ledge.MediaLedge
import com.cedarworks.ledge.QuickSettingsLedge
import com.cedarworks.ledge.ShortcutLedge

@Composable
fun LedgePanelScope.samplePanels(
    demo: DemoState,
    onMessage: (String) -> Unit,
) {
    if (demo.inCall) {
        panel(key = "call", contentDescription = stringResource(R.string.call)) {
            SampleCallPanel(demo, onMessage)
        }
        panel(key = "contact", contentDescription = stringResource(R.string.contact_name)) {
            SampleContactPanel(demo, onMessage)
        }
    }
    panel(key = "shortcuts", contentDescription = stringResource(R.string.shortcuts)) {
        SampleShortcutPanel(demo, onMessage)
    }
    panel(key = "media", contentDescription = stringResource(R.string.now_playing)) {
        SampleMediaPanel(demo)
    }
    panel(key = "assist", contentDescription = stringResource(R.string.assist)) {
        SampleAssistantPanel(demo, onMessage)
    }
    panel(key = "toggles", contentDescription = stringResource(R.string.quick_settings)) {
        SampleQuickSettingsPanel(demo)
    }
}

@Composable
private fun SampleCallPanel(demo: DemoState, onMessage: (String) -> Unit) {
    val ended = stringResource(R.string.call_ended)
    CallLedge(
        title = stringResource(R.string.call),
        muted = demo.muted,
        onMute = { demo.muted = !demo.muted },
        onEnd = {
            demo.inCall = false
            demo.muted = false
            onMessage(ended)
        },
        muteDescription = stringResource(if (demo.muted) R.string.unmute else R.string.mute),
        endDescription = stringResource(R.string.end_call),
        muteIcon = {
            PanelIcon(if (demo.muted) Icons.Rounded.MicOff else Icons.Rounded.Mic)
        },
        endIcon = { PanelIcon(Icons.Rounded.CallEnd) },
    )
}

@Composable
private fun SampleContactPanel(demo: DemoState, onMessage: (String) -> Unit) {
    val name = stringResource(R.string.contact_name)
    val calling = stringResource(R.string.calling, name)
    ContactLedge(
        name = name,
        onCall = {
            demo.inCall = true
            demo.muted = false
            onMessage(calling)
        },
        callDescription = stringResource(R.string.call_contact, name),
        avatar = { Monogram(stringResource(R.string.contact_initials)) },
        callIcon = { PanelIcon(Icons.Rounded.Call) },
    )
}

@Composable
private fun SampleShortcutPanel(demo: DemoState, onMessage: (String) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val focusOn = stringResource(R.string.focus_on)
    val focusOff = stringResource(R.string.focus_off)
    val saved = stringResource(R.string.saved_favorite)
    val removed = stringResource(R.string.removed_favorite)
    val opening = stringResource(R.string.open_track)
    ShortcutLedge(
        actions = listOf(
            LedgeAction(
                key = "focus",
                contentDescription = stringResource(R.string.focus),
                active = demo.focus,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    demo.focus = !demo.focus
                    onMessage(if (demo.focus) focusOn else focusOff)
                },
                icon = { PanelIcon(DemoIcons.Glasses) },
            ),
            LedgeAction(
                key = "music",
                contentDescription = stringResource(R.string.music),
                active = demo.playing,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    demo.playing = !demo.playing
                },
                icon = { PanelIcon(Icons.Rounded.MusicNote) },
            ),
            LedgeAction(
                key = "create",
                contentDescription = stringResource(R.string.create),
                active = demo.favorite,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    demo.favorite = !demo.favorite
                    onMessage(if (demo.favorite) saved else removed)
                },
                icon = { PanelIcon(Icons.Rounded.AutoAwesome) },
            ),
            LedgeAction(
                key = "open",
                contentDescription = stringResource(R.string.open_link),
                onClick = { onMessage(opening) },
                icon = { PanelIcon(Icons.Rounded.NorthEast) },
            ),
        ),
    )
}

@Composable
private fun SampleMediaPanel(demo: DemoState) {
    MediaLedge(
        title = stringResource(R.string.track_title),
        playing = demo.playing,
        repeating = demo.repeating,
        onPlayPause = { demo.playing = !demo.playing },
        onRepeat = { demo.repeating = !demo.repeating },
        playDescription = stringResource(if (demo.playing) R.string.pause else R.string.play),
        repeatDescription = stringResource(R.string.repeat),
        artIcon = { PanelIcon(Icons.Rounded.MusicNote) },
        playIcon = { PanelIcon(if (demo.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow) },
        repeatIcon = { PanelIcon(Icons.Rounded.Repeat) },
    )
}

@Composable
private fun SampleAssistantPanel(demo: DemoState, onMessage: (String) -> Unit) {
    val listening = stringResource(R.string.listening)
    val stopped = stringResource(R.string.assist_stopped)
    AssistantLedge(
        title = stringResource(R.string.assist),
        listening = demo.listening,
        onToggle = {
            demo.listening = !demo.listening
            onMessage(if (demo.listening) listening else stopped)
        },
        actionDescription = stringResource(
            if (demo.listening) R.string.assist_stop else R.string.assist_start,
        ),
    )
}

@Composable
private fun SampleQuickSettingsPanel(demo: DemoState) {
    val haptic = LocalHapticFeedback.current
    QuickSettingsLedge(
        toggles = listOf(
            toggle(
                key = "wifi",
                description = stringResource(R.string.wifi),
                checked = demo.wifi,
                onCheckedChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    demo.updateWifi(it)
                },
                icon = Icons.Rounded.Wifi,
            ),
            toggle(
                key = "bluetooth",
                description = stringResource(R.string.bluetooth),
                checked = demo.bluetooth,
                onCheckedChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    demo.updateBluetooth(it)
                },
                icon = Icons.Rounded.Bluetooth,
            ),
            toggle(
                key = "airplane",
                description = stringResource(R.string.airplane),
                checked = demo.airplane,
                onCheckedChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    demo.updateAirplane(it)
                },
                icon = Icons.Rounded.AirplanemodeActive,
            ),
            toggle(
                key = "flashlight",
                description = stringResource(R.string.flashlight),
                checked = demo.flashlight,
                onCheckedChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    demo.flashlight = it
                },
                icon = Icons.Rounded.FlashlightOn,
            ),
        ),
    )
}

private fun toggle(
    key: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
): LedgeToggleItem {
    return LedgeToggleItem(
        key = key,
        contentDescription = description,
        checked = checked,
        onCheckedChange = onCheckedChange,
        icon = { PanelIcon(icon) },
    )
}

@Composable
internal fun PanelIcon(image: androidx.compose.ui.graphics.vector.ImageVector) {
    Icon(
        imageVector = image,
        contentDescription = null,
        modifier = Modifier.size(LocalLedgeMetrics.current.iconSize),
    )
}

@Composable
private fun Monogram(initials: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFE7D3BE), Color(0xFF8D7B68)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            color = Color(0xFF2C2416),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
