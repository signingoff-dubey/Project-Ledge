package com.cedarworks.ledge

import kotlin.math.PI
import kotlin.math.sin

/**
 * How much the dock's silhouette deforms while it slides out of the edge.
 *
 * The value is `0` when the dock is parked open or fully hidden, positive
 * while it is being pulled out, and negative while it compresses back in.
 * Reduced motion keeps the resting shape for the whole trip.
 */
internal object LiquidDockAnimator {
    fun stretch(progress: Float, emerging: Boolean, reducedMotion: Boolean): Float {
        if (reducedMotion) return 0f
        val travel = sin(PI.toFloat() * progress.coerceIn(0f, 1f)).coerceAtLeast(0f)
        return if (emerging) travel else -travel * 0.75f
    }

    fun reducedMotion(animatorDurationScale: Float): Boolean = animatorDurationScale <= 0f
}
