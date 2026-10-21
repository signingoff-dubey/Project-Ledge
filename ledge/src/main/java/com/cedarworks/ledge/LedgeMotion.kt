package com.cedarworks.ledge

/**
 * How far a horizontal drag moves the ledge, in progress units.
 * Positive values open the ledge. [hideSign] is `1` when the closed ledge
 * sits off the right edge and `-1` when it sits off the left edge.
 */
internal fun dragDeltaProgress(
    dx: Float,
    hideSign: Float,
    travelPx: Float,
    trayWidthPx: Float,
): Float {
    if (trayWidthPx <= 0f || travelPx <= 0f) return 0f
    return -dx * hideSign / travelPx
}

/** Velocity in pixels per second, positive when the fling is opening the ledge. */
internal fun openingVelocity(velocityX: Float, hideSign: Float): Float = -velocityX * hideSign

/**
 * Picks the resting progress after a drag. A quick fling wins over the
 * distance so a short, fast swipe still opens or closes.
 */
internal fun settleTarget(progress: Float, openingVelocityPxPerSec: Float): Float = when {
    openingVelocityPxPerSec > FlingOpenThreshold -> 1f
    openingVelocityPxPerSec < -FlingOpenThreshold -> 0f
    progress >= PositionOpenThreshold -> 1f
    else -> 0f
}

internal const val FlingOpenThreshold = 1_200f
internal const val PositionOpenThreshold = 0.45f
