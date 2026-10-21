package com.cedarworks.ledge

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.selection.toggleable

/**
 * Clipped, shadowed shell for one action column. Content is laid out in the
 * full panel; keep wide controls inside [LedgeMetrics.contentWidth] so they
 * stay inside the straight body of the drop.
 */
@Composable
fun LedgeSurface(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val metrics = LocalLedgeMetrics.current
    val colors = LocalLedgeColors.current
    val shape = LocalLedgeShape.current
    Box(
        modifier = modifier
            .width(metrics.panelWidth)
            .height(metrics.panelHeight)
            .shadow(
                elevation = 8.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.55f),
                spotColor = Color.Black.copy(alpha = 0.70f),
            )
            .border(1.dp, colors.hairline, shape)
            .clip(shape)
            .background(colors.panel)
            .background(
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.07f),
                    0.42f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.30f),
                ),
            ),
        content = content,
    )
}

@Composable
fun LedgeControl(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    shape: LedgeControlShape = LedgeControlShape.Pill,
    containerColor: Color = LocalLedgeColors.current.control,
    contentColor: Color = LocalLedgeColors.current.onControl,
    enabled: Boolean = true,
    icon: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    ControlChrome(
        modifier = modifier.semantics { this.contentDescription = contentDescription },
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        enabled = enabled,
        interaction = interaction,
        activate = Modifier.clickable(
            interactionSource = interaction,
            indication = ripple(color = contentColor.copy(alpha = 0.28f)),
            enabled = enabled,
            role = Role.Button,
            onClick = onClick,
        ),
        icon = icon,
    )
}

@Composable
fun LedgeToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    shape: LedgeControlShape = LedgeControlShape.Pill,
    icon: @Composable BoxScope.() -> Unit,
) {
    val colors = LocalLedgeColors.current
    val interaction = remember { MutableInteractionSource() }
    ControlChrome(
        modifier = modifier.semantics { this.contentDescription = contentDescription },
        shape = shape,
        containerColor = if (checked) colors.controlActive else colors.control,
        contentColor = if (checked) colors.onControlActive else colors.onControl,
        enabled = true,
        interaction = interaction,
        activate = Modifier.toggleable(
            value = checked,
            interactionSource = interaction,
            indication = ripple(color = colors.onControl.copy(alpha = 0.28f)),
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ),
        icon = icon,
    )
}

@Composable
private fun ControlChrome(
    shape: LedgeControlShape,
    containerColor: Color,
    contentColor: Color,
    enabled: Boolean,
    interaction: MutableInteractionSource,
    activate: Modifier,
    modifier: Modifier = Modifier,
    icon: @Composable BoxScope.() -> Unit,
) {
    val metrics = LocalLedgeMetrics.current
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) 0.96f else 1f, label = "controlPress")
    val outline = shape.toShape()
    Box(
        modifier = modifier
            .then(shape.sizeModifier(metrics))
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.4f
            }
            .clip(outline)
            .background(containerColor)
            .then(activate),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides contentColor,
        ) {
            icon()
        }
    }
}

private fun LedgeControlShape.toShape(): Shape = when (this) {
    LedgeControlShape.Circle -> CircleShape
    LedgeControlShape.Pill,
    LedgeControlShape.TallOval,
    -> RoundedCornerShape(50)
}

private fun LedgeControlShape.sizeModifier(metrics: LedgeMetrics): Modifier = when (this) {
    LedgeControlShape.Pill -> Modifier.width(metrics.contentWidth).height(metrics.pillHeight)
    LedgeControlShape.TallOval -> Modifier.width(metrics.ovalWidth).height(metrics.ovalHeight)
    LedgeControlShape.Circle -> Modifier.size(metrics.circleSize)
}
