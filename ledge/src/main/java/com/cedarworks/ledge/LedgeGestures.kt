package com.cedarworks.ledge

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlin.math.abs
import kotlin.math.hypot

internal enum class LedgeTapAction {
    Toggle,
    Close,
    None,
}

internal class LedgePlacement {
    var trayWidthPx by mutableFloatStateOf(0f)
    var peekPx by mutableFloatStateOf(1f)
    var hideSign by mutableFloatStateOf(1f)
    var containerHeightPx by mutableFloatStateOf(0f)
    var panelHeightPx by mutableFloatStateOf(0f)
}

private enum class DragAxis {
    None,
    Horizontal,
    Vertical,
}

internal fun Modifier.ledgeDrag(
    state: LedgeState,
    placement: LedgePlacement,
    tap: LedgeTapAction,
    reposition: Boolean = false,
    onAnchorDrag: (dy: Float) -> Unit = {},
): Modifier = pointerInput(state, placement, tap, reposition) {
    val touchSlop = viewConfiguration.touchSlop
    awaitEachGesture {
        val down = awaitFirstDown(
            requireUnconsumed = false,
            pass = PointerEventPass.Initial,
        )
        val pointerId = down.id
        val tracker = VelocityTracker()
        tracker.addPosition(down.uptimeMillis, down.position)
        var totalX = 0f
        var totalY = 0f
        var axis = DragAxis.None
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
            if (!change.pressed) {
                if (axis == DragAxis.Horizontal) {
                    dispatchFling(state, placement, tracker.calculateVelocity().x)
                } else if (axis == DragAxis.None && abs(totalX) <= touchSlop && abs(totalY) <= touchSlop) {
                    dispatchTap(state, tap)
                }
                break
            }
            val dx = change.position.x - change.previousPosition.x
            val dy = change.position.y - change.previousPosition.y
            totalX += dx
            totalY += dy
            tracker.addPosition(change.uptimeMillis, change.position)
            if (axis == DragAxis.None && hypot(totalX, totalY) > touchSlop) {
                axis = if (abs(totalX) > abs(totalY)) {
                    DragAxis.Horizontal
                } else if (reposition) {
                    DragAxis.Vertical
                } else {
                    break
                }
                change.consume()
                if (axis == DragAxis.Horizontal) {
                    dispatchDrag(state, placement, totalX)
                } else {
                    onAnchorDrag(totalY)
                }
                continue
            }
            if (axis != DragAxis.None) {
                change.consume()
                if (axis == DragAxis.Horizontal) {
                    dispatchDrag(state, placement, dx)
                } else {
                    onAnchorDrag(dy)
                }
            }
        }
    }
}

private fun dispatchDrag(state: LedgeState, placement: LedgePlacement, dx: Float) {
    val travel = (placement.trayWidthPx - placement.peekPx).coerceAtLeast(1f)
    state.dragBy(
        dragDeltaProgress(
            dx = dx,
            hideSign = placement.hideSign,
            travelPx = travel,
            trayWidthPx = placement.trayWidthPx,
        ),
    )
}

private fun dispatchFling(state: LedgeState, placement: LedgePlacement, velocityX: Float) {
    val travel = (placement.trayWidthPx - placement.peekPx).coerceAtLeast(1f)
    state.fling(
        openingVelocityPxPerSec = openingVelocity(velocityX, placement.hideSign),
        travelPx = travel,
    )
}

private fun dispatchTap(state: LedgeState, tap: LedgeTapAction) {
    when (tap) {
        LedgeTapAction.Toggle -> if (state.progress >= PositionOpenThreshold) state.close() else state.open()
        LedgeTapAction.Close -> state.close()
        LedgeTapAction.None -> Unit
    }
}
