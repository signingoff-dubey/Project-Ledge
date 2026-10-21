package com.cedarworks.ledge

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.cedarworks.ledge.geometry.LedgeGeometry

/**
 * Droplet silhouette: a straight screen edge and a pointed inner contour.
 * [stretch] fills the neck while the drop is pulled out and thins it on the
 * way back. The straight edge stays put. [emergeFromEnd] is true when that
 * edge is the physical right side.
 */
@Immutable
class LiquidLedgeShape(
    private val stretch: Float = 0f,
    private val emergeFromEnd: Boolean = true,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val points = LedgeGeometry.trace(
            width = size.width,
            height = size.height,
            stretch = stretch,
            emergeFromEnd = emergeFromEnd,
        )
        if (points.size < 3) {
            return Outline.Rectangle(Rect(0f, 0f, size.width, size.height))
        }
        val path = Path().apply {
            moveTo(points[0].first, points[0].second)
            for (index in 1 until points.size) {
                lineTo(points[index].first, points[index].second)
            }
            close()
        }
        return Outline.Generic(path)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LiquidLedgeShape) return false
        return stretch == other.stretch && emergeFromEnd == other.emergeFromEnd
    }

    override fun hashCode(): Int {
        var result = stretch.hashCode()
        result = 31 * result + emergeFromEnd.hashCode()
        return result
    }
}

/** Droplet outline shared by every ledge panel at rest. */
val LedgeShape: Shape = LiquidLedgeShape()

/**
 * Silhouette for the panel currently emerging. [compositionLocalOf] so the
 * shell redraws on every frame of the liquid motion.
 */
val LocalLedgeShape = compositionLocalOf<Shape> { LedgeShape }
