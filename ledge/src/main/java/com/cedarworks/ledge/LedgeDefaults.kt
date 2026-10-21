package com.cedarworks.ledge

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class LedgeEdge {
    /** The start side. Left in left-to-right layouts. */
    Start,

    /** The end side. Right in left-to-right layouts. */
    End,
}

enum class LedgeControlShape {
    Pill,
    TallOval,
    Circle,
}

@Immutable
data class LedgeColors(
    val panel: Color,
    val hairline: Color,
    val control: Color,
    val controlActive: Color,
    val onControl: Color,
    val onControlActive: Color,
    val label: Color,
    val scrim: Color,
    val endCall: Color,
    val onEndCall: Color,
    val acceptCall: Color,
    val onAcceptCall: Color,
    val orbCore: Color,
    val orbMid: Color,
)

@Immutable
data class LedgeMetrics(
    val panelWidth: Dp,
    val panelHeight: Dp,
    val contentWidth: Dp,
    val titleWidth: Dp,
    val gap: Dp,
    val peek: Dp,
    val edgeMargin: Dp,
    val edgeGrab: Dp,
    val pillHeight: Dp,
    val ovalWidth: Dp,
    val ovalHeight: Dp,
    val circleSize: Dp,
    val iconSize: Dp,
    val avatarSize: Dp,
    val verticalPadding: Dp,
)

object LedgeDefaults {
    val endCall: Color = Color(0xFFE86A60)
    val acceptCall: Color = Color(0xFF5FD6A0)
    val activeControl: Color = Color(0xFFC9D7F8)

    val labelStyle: TextStyle = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.1.sp,
    )

    fun colors(darkPanels: Boolean = true): LedgeColors {
        return if (darkPanels) {
            LedgeColors(
                panel = Color(0xFF050505),
                hairline = Color.White.copy(alpha = 0.10f),
                control = Color(0xFF343438),
                controlActive = activeControl,
                onControl = Color.White,
                onControlActive = Color(0xFF1C2433),
                label = Color.White,
                scrim = Color.Black.copy(alpha = 0.38f),
                endCall = endCall,
                onEndCall = Color.White,
                acceptCall = acceptCall,
                onAcceptCall = Color.White,
                orbCore = Color.White,
                orbMid = Color(0xFFD5E2FF),
            )
        } else {
            LedgeColors(
                panel = Color(0xFFF7F7FA),
                hairline = Color.Black.copy(alpha = 0.08f),
                control = Color(0xFFE4E4EA),
                controlActive = Color(0xFF3A4C70),
                onControl = Color(0xFF1B1B1F),
                onControlActive = Color.White,
                label = Color(0xFF1B1B1F),
                scrim = Color.Black.copy(alpha = 0.32f),
                endCall = endCall,
                onEndCall = Color.White,
                acceptCall = Color(0xFF1F8A56),
                onAcceptCall = Color.White,
                orbCore = Color.White,
                orbMid = Color(0xFF8EA6DE),
            )
        }
    }

    fun metricsFor(availableHeight: Dp): LedgeMetrics {
        val desiredHeight = 220.dp
        val heightBudget = availableHeight - 16.dp
        val panelHeight = when {
            heightBudget >= desiredHeight -> desiredHeight
            heightBudget < 176.dp -> 176.dp
            else -> heightBudget
        }
        val scale = panelHeight.value / desiredHeight.value
        val panelWidth = 52.dp * scale
        val contentWidth = panelWidth * 0.72f
        return LedgeMetrics(
            panelWidth = panelWidth,
            panelHeight = panelHeight,
            contentWidth = contentWidth,
            titleWidth = panelWidth * 0.68f,
            gap = 4.dp * scale,
            peek = (8.dp * scale).coerceAtLeast(8.dp),
            edgeMargin = 0.dp,
            edgeGrab = 28.dp,
            pillHeight = 22.dp * scale,
            ovalWidth = contentWidth * 0.82f,
            ovalHeight = 40.dp * scale,
            circleSize = 20.dp * scale,
            iconSize = 14.dp * scale,
            avatarSize = contentWidth * 0.92f,
            verticalPadding = panelHeight * 0.22f,
        )
    }
}

val LocalLedgeColors = staticCompositionLocalOf { LedgeDefaults.colors() }

val LocalLedgeMetrics = staticCompositionLocalOf { LedgeDefaults.metricsFor(640.dp) }
