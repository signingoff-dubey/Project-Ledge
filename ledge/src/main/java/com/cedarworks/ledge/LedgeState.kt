package com.cedarworks.ledge

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

enum class LedgeValue {
    Closed,
    Open,
}

/**
 * Progress and resting position of a [LedgeHost].
 *
 * [progress] is `0` when the ledge is closed and `1` when it is fully open.
 * Drag and fling commands are applied on the frame clock started by [LedgeHost].
 */
@Stable
class LedgeState internal constructor(
    initialProgress: Float,
) {
    private val commands = Channel<LedgeCommand>(capacity = Channel.UNLIMITED)

    var progress by mutableFloatStateOf(initialProgress.coerceIn(0f, 1f))
        private set

    var settled by mutableStateOf(
        if (initialProgress >= PositionOpenThreshold) LedgeValue.Open else LedgeValue.Closed,
    )
        private set

    /** True once the ledge has moved far enough to dim the screen and trap back. */
    var isInteractive by mutableStateOf(false)
        private set

    /** True when the action row should scroll instead of dragging the ledge. */
    var scrollEnabled by mutableStateOf(false)
        private set

    /** True while the latest movement is away from the screen edge. */
    internal var emerging by mutableStateOf(true)
        private set

    /** When set, open and close snap instead of springing. */
    internal var reducedMotion by mutableStateOf(false)

    init {
        publish(progress)
    }

    fun open() {
        commands.trySend(LedgeCommand.Open)
    }

    fun close() {
        commands.trySend(LedgeCommand.Close)
    }

    internal fun dragBy(deltaProgress: Float) {
        if (deltaProgress == 0f) return
        commands.trySend(LedgeCommand.Drag(deltaProgress))
    }

    internal fun fling(openingVelocityPxPerSec: Float, travelPx: Float) {
        commands.trySend(LedgeCommand.Fling(openingVelocityPxPerSec, travelPx))
    }

    internal suspend fun runActor() {
        var stolen: LedgeCommand? = null
        while (coroutineContext.isActive) {
            val command = stolen ?: commands.receive()
            stolen = null
            stolen = when (command) {
                is LedgeCommand.Drag -> applyDrag(command)
                is LedgeCommand.Fling -> animateResolved(
                    target = settleTarget(progress, command.openingVelocityPxPerSec),
                    openingVelocityPxPerSec = command.openingVelocityPxPerSec,
                    travelPx = command.travelPx,
                )
                LedgeCommand.Open -> {
                    settled = LedgeValue.Open
                    if (progress >= 0.999f) null else animateProgress(1f, 0f)
                }
                LedgeCommand.Close -> {
                    settled = LedgeValue.Closed
                    if (progress <= 0.001f) null else animateProgress(0f, 0f)
                }
            }
        }
    }

    private fun applyDrag(first: LedgeCommand.Drag): LedgeCommand? {
        var delta = first.deltaProgress
        while (true) {
            val next = commands.tryReceive().getOrNull() ?: break
            if (next is LedgeCommand.Drag) {
                delta += next.deltaProgress
            } else {
                publish((progress + delta).coerceIn(0f, 1f))
                return next
            }
        }
        publish((progress + delta).coerceIn(0f, 1f))
        return null
    }

    private suspend fun animateResolved(
        target: Float,
        openingVelocityPxPerSec: Float,
        travelPx: Float,
    ): LedgeCommand? {
        settled = if (target >= 1f) LedgeValue.Open else LedgeValue.Closed
        val towardTarget = if (target >= 1f) {
            openingVelocityPxPerSec.coerceAtLeast(0f)
        } else {
            openingVelocityPxPerSec.coerceAtMost(0f)
        }
        val velocity = (towardTarget / travelPx.coerceAtLeast(1f)).coerceIn(-8f, 8f)
        if (abs(target - progress) < 0.001f && abs(velocity) < 0.05f) {
            publish(target)
            return null
        }
        return animateProgress(target, velocity)
    }

    private suspend fun animateProgress(target: Float, velocity: Float): LedgeCommand? {
        var stolen: LedgeCommand? = null
        val opening = target + 0.0001f >= progress
        val spec = if (reducedMotion) {
            snap<Float>()
        } else if (abs(velocity) < 0.2f) {
            tween(durationMillis = SlideDurationMillis, easing = SlideEasing)
        } else {
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = SlideSpringStiffness)
        }
        AnimationState(initialValue = progress, initialVelocity = velocity).animateTo(
            targetValue = target,
            animationSpec = spec,
        ) {
            val next = commands.tryReceive().getOrNull()
            if (next != null) {
                stolen = next
                cancelAnimation()
            } else {
                publish(value.coerceIn(0f, 1f), opening)
            }
        }
        if (stolen == null) publish(target, opening)
        return stolen
    }

    private fun publish(value: Float, emergingNow: Boolean? = null) {
        val next = value.coerceIn(0f, 1f)
        if (emergingNow != null) {
            emerging = emergingNow
        } else if (next != progress) {
            emerging = next > progress
        }
        progress = next
        val interactive = next > 0.02f
        if (isInteractive != interactive) isInteractive = interactive
        val canScroll = next > 0.995f
        if (scrollEnabled != canScroll) scrollEnabled = canScroll
    }

    companion object {
        val Saver: Saver<LedgeState, Float> = Saver(
            save = { it.progress },
            restore = { saved -> LedgeState(saved) },
        )
    }
}

/** Tap open and close. Long enough that a wide workflow still eases in. */
internal const val SlideDurationMillis = 960

/** Fling open and close. No bounce, soft enough to glide across the wider slide. */
internal const val SlideSpringStiffness = 120f

internal val SlideEasing = CubicBezierEasing(0.16f, 0.84f, 0.12f, 1f)

private sealed interface LedgeCommand {
    data class Drag(val deltaProgress: Float) : LedgeCommand

    data class Fling(
        val openingVelocityPxPerSec: Float,
        val travelPx: Float,
    ) : LedgeCommand

    data object Open : LedgeCommand

    data object Close : LedgeCommand
}
