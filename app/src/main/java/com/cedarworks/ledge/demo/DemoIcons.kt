package com.cedarworks.ledge.demo

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

object DemoIcons {
    val Glasses: ImageVector by lazy {
        ImageVector.Builder(
            name = "Glasses",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = PathParser().parsePathString(
                    "M4.2,12a3.15,3.15 0,1 0,6.3 0a3.15,3.15 0,1 0,-6.3 0z" +
                        "M13.5,12a3.15,3.15 0,1 0,6.3 0a3.15,3.15 0,1 0,-6.3 0z" +
                        "M10.5,11.6L13.5,11.6",
                ).toNodes(),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.6f,
                strokeLineCap = StrokeCap.Round,
            )
        }.build()
    }
}
