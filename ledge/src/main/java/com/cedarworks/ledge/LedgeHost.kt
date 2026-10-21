package com.cedarworks.ledge

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.min
import kotlin.math.roundToInt

@DslMarker
annotation class LedgePanelDsl

@LedgePanelDsl
interface LedgePanelScope {
    fun panel(
        key: Any,
        contentDescription: String,
        content: @Composable () -> Unit,
    )
}

private class LedgePanelEntry(
    val key: Any,
    val contentDescription: String,
    val content: @Composable () -> Unit,
)

private class PanelScopeImpl : LedgePanelScope {
    val entries = mutableListOf<LedgePanelEntry>()

    override fun panel(
        key: Any,
        contentDescription: String,
        content: @Composable () -> Unit,
    ) {
        entries += LedgePanelEntry(key, contentDescription, content)
    }
}

/**
 * Remembers a [LedgeState] that survives rotation and process recreation.
 */
@Composable
fun rememberLedgeState(initiallyOpen: Boolean = false): LedgeState {
    return rememberSaveable(saver = LedgeState.Saver) {
        LedgeState(if (initiallyOpen) 1f else 0f)
    }
}

/**
 * Hosts a side ledge over [content].
 *
 * One panel rests on [edge]. Drag the handle along the edge to park it,
 * then swipe inward. The droplet slides out on a smooth ease, and the window
 * asks for the display's highest refresh rate for that motion. Tap the dimmed
 * page, swipe back outward, or use system back to close.
 *
 * [selectedKey] picks which declared panel is showing. [anchorFraction] is
 * `0` at the top of the safe area and `1` at the bottom.
 *
 * Pass translated [openDescription] and [closeDescription] strings.
 */
@Composable
fun LedgeHost(
    panels: @Composable LedgePanelScope.() -> Unit,
    modifier: Modifier = Modifier,
    state: LedgeState = rememberLedgeState(),
    edge: LedgeEdge = LedgeEdge.End,
    selectedKey: Any? = null,
    anchorFraction: Float = 0.5f,
    onAnchorFractionChange: (Float) -> Unit = {},
    colors: LedgeColors = LedgeDefaults.colors(),
    openDescription: String = "Open side actions",
    closeDescription: String = "Close side actions",
    content: @Composable () -> Unit,
) {
    val scope = PanelScopeImpl()
    scope.panels()
    val items = scope.entries
    val placement = remember { LedgePlacement() }
    var anchor by rememberSaveable { mutableFloatStateOf(anchorFraction.coerceIn(0f, 1f)) }
    LaunchedEffect(anchorFraction) {
        anchor = anchorFraction.coerceIn(0f, 1f)
    }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    var skipHaptic by remember { mutableStateOf(true) }
    SideEffect {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
        state.reducedMotion = LiquidDockAnimator.reducedMotion(scale)
    }

    LaunchedEffect(state) { state.runActor() }
    LaunchedEffect(state.settled) {
        if (skipHaptic) {
            skipHaptic = false
            return@LaunchedEffect
        }
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    if (LocalOnBackPressedDispatcherOwner.current != null) {
        BackHandler(enabled = state.isInteractive) { state.close() }
    }

    CompositionLocalProvider(LocalLedgeColors provides colors) {
        Box(modifier.fillMaxSize()) {
            content()
            if (items.isNotEmpty()) {
                LedgeLayer(
                    items = items,
                    selectedKey = selectedKey,
                    state = state,
                    placement = placement,
                    edge = edge,
                    anchorFraction = anchor,
                    onAnchorFractionChange = { next ->
                        val coerced = next.coerceIn(0f, 1f)
                        anchor = coerced
                        onAnchorFractionChange(coerced)
                    },
                    colors = colors,
                    openDescription = openDescription,
                    closeDescription = closeDescription,
                )
            }
        }
    }
}

@Composable
private fun LedgeLayer(
    items: List<LedgePanelEntry>,
    selectedKey: Any?,
    state: LedgeState,
    placement: LedgePlacement,
    edge: LedgeEdge,
    anchorFraction: Float,
    onAnchorFractionChange: (Float) -> Unit,
    colors: LedgeColors,
    openDescription: String,
    closeDescription: String,
) {
    val item = items.firstOrNull { it.key == selectedKey } ?: items.first()
    val density = LocalDensity.current
    val ltr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val physicalRight = if (ltr) edge == LedgeEdge.End else edge == LedgeEdge.Start
    val hideSign = if (physicalRight) 1f else -1f
    val alignment = if (physicalRight) Alignment.TopEnd else Alignment.TopStart
    val exclusionHeight = with(density) { 200.dp.toPx() }
    val liquidShape = remember(physicalRight) {
        LiquidLedgeShape(emergeFromEnd = physicalRight)
    }
    val view = LocalView.current
    val moving = !state.reducedMotion && state.progress > 0.001f && state.progress < 0.999f
    val boostHeld = remember { booleanArrayOf(false) }
    val boostRate = remember { floatArrayOf(0f) }
    SideEffect {
        val window = view.context.hostActivity()?.window ?: return@SideEffect
        if (boostHeld[0] != moving) {
            val rate = if (moving) LedgeFrameRate.highestRefreshRate(window) else 0f
            if (!moving || rate > 0f) {
                LedgeFrameRate.setPreferredRefreshRate(window, rate)
                boostRate[0] = rate
                boostHeld[0] = moving
                if (!moving) LedgeFrameRate.vote(view, 0f)
            }
        }
        if (moving) LedgeFrameRate.vote(view, boostRate[0])
    }
    DisposableEffect(view) {
        onDispose {
            val window = view.context.hostActivity()?.window
            if (window != null) LedgeFrameRate.setPreferredRefreshRate(window, 0f)
            LedgeFrameRate.vote(view, 0f)
            boostHeld[0] = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .zIndex(1f)
                .graphicsLayer { alpha = state.progress.coerceIn(0f, 1f) }
                .background(colors.scrim)
                .then(
                    if (state.isInteractive) {
                        Modifier.ledgeDrag(state, placement, LedgeTapAction.Close)
                    } else {
                        Modifier
                    },
                ),
        )
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical))
                .zIndex(2f)
                .onSizeChanged { placement.containerHeightPx = it.height.toFloat() },
        ) {
            val metrics = LedgeDefaults.metricsFor(maxHeight)
            val panelWidth = metrics.panelWidth + metrics.edgeMargin
            val peekPx = with(density) { metrics.peek.toPx() }
            val panelHeightPx = with(density) { metrics.panelHeight.toPx() }
            SideEffect {
                placement.peekPx = peekPx
                placement.hideSign = hideSign
                placement.panelHeightPx = panelHeightPx
            }
            val moveAnchor: (Float) -> Unit = { dy ->
                val span = (placement.containerHeightPx - placement.panelHeightPx).coerceAtLeast(1f)
                onAnchorFractionChange(anchorFraction + dy / span)
            }
            CompositionLocalProvider(LocalLedgeMetrics provides metrics) {
                Box(
                    Modifier
                        .align(alignment)
                        .width(panelWidth)
                        .height(metrics.panelHeight)
                        .onSizeChanged { placement.trayWidthPx = it.width.toFloat() }
                        .offset {
                            val whole = if (placement.trayWidthPx <= 0f) {
                                0
                            } else {
                                val travel = (placement.trayWidthPx - placement.peekPx).coerceAtLeast(1f)
                                ((1f - state.progress) * travel * placement.hideSign).roundToInt()
                            }
                            val span = (placement.containerHeightPx - panelHeightPx).coerceAtLeast(0f)
                            val dy = (span * anchorFraction.coerceIn(0f, 1f)).roundToInt()
                            IntOffset(whole, dy)
                        }
                        .graphicsLayer {
                            if (placement.trayWidthPx <= 0f) {
                                alpha = 0f
                                translationX = 0f
                            } else {
                                alpha = 1f
                                val travel = (placement.trayWidthPx - placement.peekPx).coerceAtLeast(1f)
                                val slide = (1f - state.progress) * travel * placement.hideSign
                                translationX = slide - slide.roundToInt()
                            }
                        }
                        .then(
                            if (state.scrollEnabled) {
                                Modifier
                            } else {
                                Modifier.ledgeDrag(state, placement, LedgeTapAction.None)
                            },
                        )
                        .semantics {
                            contentDescription = item.contentDescription
                            isTraversalGroup = true
                        },
                ) {
                    CompositionLocalProvider(LocalLedgeShape provides liquidShape) {
                        Box(Modifier.align(alignment)) {
                            item.content()
                        }
                    }
                }
                val open = state.settled == LedgeValue.Open
                Box(
                    Modifier
                        .align(alignment)
                        .zIndex(1f)
                        .width(metrics.edgeGrab)
                        .height(metrics.panelHeight)
                        .offset {
                            val span = (placement.containerHeightPx - panelHeightPx).coerceAtLeast(0f)
                            IntOffset(0, (span * anchorFraction.coerceIn(0f, 1f)).roundToInt())
                        }
                        .systemGestureExclusion { coordinates ->
                            val widthPx = coordinates.size.width.toFloat()
                            val heightPx = coordinates.size.height.toFloat()
                            val band = min(heightPx, exclusionHeight)
                            val top = (heightPx - band) / 2f
                            Rect(0f, top, widthPx, top + band)
                        }
                        .ledgeDrag(
                            state = state,
                            placement = placement,
                            tap = LedgeTapAction.Toggle,
                            reposition = true,
                            onAnchorDrag = moveAnchor,
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = if (open) closeDescription else openDescription
                            onClick {
                                if (state.settled == LedgeValue.Open || state.progress > 0.5f) {
                                    state.close()
                                } else {
                                    state.open()
                                }
                                true
                            }
                        },
                )
            }
        }
    }
}
