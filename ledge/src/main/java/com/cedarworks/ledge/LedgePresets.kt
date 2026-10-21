package com.cedarworks.ledge

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

class LedgeAction(
    val key: Any,
    val contentDescription: String,
    val onClick: () -> Unit,
    val active: Boolean = false,
    val icon: @Composable () -> Unit,
)

class LedgeToggleItem(
    val key: Any,
    val contentDescription: String,
    val checked: Boolean,
    val onCheckedChange: (Boolean) -> Unit,
    val icon: @Composable () -> Unit,
)

@Composable
fun CallLedge(
    title: String,
    muted: Boolean,
    onMute: () -> Unit,
    onEnd: () -> Unit,
    muteDescription: String,
    endDescription: String,
    muteIcon: @Composable () -> Unit,
    endIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalLedgeColors.current
    val metrics = LocalLedgeMetrics.current
    LedgeSurface(modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = metrics.verticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            PanelTitle(title, metrics)
            LedgeControl(
                onClick = onMute,
                contentDescription = muteDescription,
                shape = LedgeControlShape.TallOval,
                containerColor = if (muted) colors.controlActive else colors.control,
                contentColor = if (muted) colors.onControlActive else colors.onControl,
            ) {
                muteIcon()
            }
            LedgeControl(
                onClick = onEnd,
                contentDescription = endDescription,
                containerColor = colors.endCall,
                contentColor = colors.onEndCall,
            ) {
                endIcon()
            }
        }
    }
}

@Composable
fun ContactLedge(
    name: String,
    onCall: () -> Unit,
    callDescription: String,
    avatar: @Composable () -> Unit,
    callIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalLedgeColors.current
    val metrics = LocalLedgeMetrics.current
    LedgeSurface(modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = metrics.verticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            PanelTitle(name, metrics)
            Box(
                modifier = Modifier.size(metrics.avatarSize),
                contentAlignment = Alignment.Center,
            ) {
                avatar()
            }
            LedgeControl(
                onClick = onCall,
                contentDescription = callDescription,
                containerColor = colors.acceptCall,
                contentColor = colors.onAcceptCall,
            ) {
                callIcon()
            }
        }
    }
}

@Composable
fun ShortcutLedge(
    actions: List<LedgeAction>,
    modifier: Modifier = Modifier,
) {
    val colors = LocalLedgeColors.current
    val metrics = LocalLedgeMetrics.current
    LedgeSurface(modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = metrics.verticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            for (action in actions) {
                key(action.key) {
                    LedgeControl(
                        onClick = action.onClick,
                        contentDescription = action.contentDescription,
                        containerColor = if (action.active) colors.controlActive else colors.control,
                        contentColor = if (action.active) colors.onControlActive else colors.onControl,
                    ) {
                        action.icon()
                    }
                }
            }
        }
    }
}

@Composable
fun MediaLedge(
    title: String,
    playing: Boolean,
    repeating: Boolean,
    onPlayPause: () -> Unit,
    onRepeat: () -> Unit,
    playDescription: String,
    repeatDescription: String,
    artIcon: @Composable () -> Unit,
    playIcon: @Composable () -> Unit,
    repeatIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalLedgeColors.current
    val metrics = LocalLedgeMetrics.current
    LedgeSurface(modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = metrics.verticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                artIcon()
                PanelTitle(title, metrics)
            }
            LedgeControl(
                onClick = onPlayPause,
                contentDescription = playDescription,
            ) {
                playIcon()
            }
            LedgeControl(
                onClick = onRepeat,
                contentDescription = repeatDescription,
                containerColor = if (repeating) colors.controlActive else colors.control,
                contentColor = if (repeating) colors.onControlActive else colors.onControl,
            ) {
                repeatIcon()
            }
        }
    }
}

@Composable
fun AssistantLedge(
    title: String,
    listening: Boolean,
    onToggle: () -> Unit,
    actionDescription: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalLedgeColors.current
    val metrics = LocalLedgeMetrics.current
    val pulse by rememberInfiniteTransition(label = "assist").animateFloat(
        initialValue = 0.92f,
        targetValue = if (listening) 1.08f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (listening) 700 else 1800,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "assistPulse",
    )
    LedgeSurface(modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = metrics.verticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            PanelTitle(title, metrics)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                    val glowWidth = size.width * 0.62f * pulse
                    val glowHeight = size.height * 0.82f * pulse
                    val center = Offset(size.width / 2f, size.height * 0.46f)
                    drawOval(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                colors.orbCore.copy(alpha = if (listening) 0.95f else 0.82f),
                                colors.orbMid.copy(alpha = if (listening) 0.62f else 0.42f),
                                Color.Transparent,
                            ),
                            center = center,
                            radius = glowHeight / 2f,
                        ),
                        topLeft = Offset(center.x - glowWidth / 2f, center.y - glowHeight / 2f),
                        size = Size(glowWidth, glowHeight),
                    )
                }
            }
            LedgeControl(
                onClick = onToggle,
                contentDescription = actionDescription,
                shape = LedgeControlShape.Circle,
                containerColor = if (listening) colors.controlActive else colors.control,
                contentColor = if (listening) colors.onControlActive else colors.onControl,
            ) {
                Box(
                    Modifier
                        .size(if (listening) metrics.iconSize * 0.42f else metrics.iconSize * 0.5f)
                        .clip(if (listening) RoundedCornerShape(3.dp) else CircleShape)
                        .background(androidx.compose.material3.LocalContentColor.current),
                )
            }
        }
    }
}

@Composable
fun QuickSettingsLedge(
    toggles: List<LedgeToggleItem>,
    modifier: Modifier = Modifier,
) {
    val metrics = LocalLedgeMetrics.current
    LedgeSurface(modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = metrics.verticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            for (toggle in toggles) {
                key(toggle.key) {
                    LedgeToggle(
                        checked = toggle.checked,
                        onCheckedChange = toggle.onCheckedChange,
                        contentDescription = toggle.contentDescription,
                    ) {
                        toggle.icon()
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelTitle(text: String, metrics: LedgeMetrics) {
    Text(
        text = text,
        modifier = Modifier.widthIn(max = metrics.titleWidth),
        color = LocalLedgeColors.current.label,
        style = LedgeDefaults.labelStyle,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

