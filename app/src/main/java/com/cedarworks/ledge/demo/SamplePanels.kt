package com.cedarworks.ledge.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.cedarworks.ledge.CallLedge
import com.cedarworks.ledge.ContactLedge
import com.cedarworks.ledge.LedgePanelScope
import com.cedarworks.ledge.LocalLedgeMetrics
import com.cedarworks.ledge.MediaLedge

@Composable
fun LedgePanelScope.samplePanels(
    demo: DemoState,
    onMessage: (String) -> Unit,
    modules: List<DockModule> = DockModule.starter,
    onChanged: () -> Unit = {},
    onTorch: (Boolean) -> Unit = { enabled -> demo.flashlight = enabled },
) {
    if (demo.inCall) {
        panel(key = "call", contentDescription = stringResource(R.string.call)) {
            SampleCallPanel(demo, onMessage)
        }
        panel(key = "contact", contentDescription = stringResource(R.string.contact_name)) {
            SampleContactPanel(demo, onMessage)
        }
    }
    for (module in modules) {
        val current = module
        panel(key = current.key, contentDescription = stringResource(current.labelRes)) {
            DockModulePanel(current, demo, onMessage, onChanged, onTorch)
        }
    }
    if (!demo.inCall && modules.isEmpty()) {
        panel(key = "empty", contentDescription = stringResource(R.string.dock_empty)) {
            EmptyDockColumn(onMessage)
        }
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
internal fun SampleMediaPanel(demo: DemoState) {
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
