package ru.misterpotz.listly.features.poster

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
internal fun MediaPosterPlaceholder(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF243449),
                        Color(0xFF111827)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(30.dp)) {
            val strokeColor = Color.White.copy(alpha = 0.68f)
            val strokeWidth = 1.7.dp.toPx()
            val frameSize = Size(
                width = size.width * 0.78f,
                height = size.height * 0.62f
            )
            val frameTopLeft = Offset(
                x = (size.width - frameSize.width) / 2f,
                y = (size.height - frameSize.height) / 2f
            )

            drawRoundRect(
                color = strokeColor,
                topLeft = frameTopLeft,
                size = frameSize,
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                style = Stroke(width = strokeWidth)
            )

            val perforationWidth = 3.dp.toPx()
            val perforationHeight = 4.dp.toPx()
            val leftX = frameTopLeft.x + 4.dp.toPx()
            val rightX = frameTopLeft.x + frameSize.width - 4.dp.toPx() - perforationWidth
            listOf(0.24f, 0.50f, 0.76f).forEach { ratio ->
                val y = frameTopLeft.y + frameSize.height * ratio - perforationHeight / 2f
                drawRoundRect(
                    color = strokeColor,
                    topLeft = Offset(leftX, y),
                    size = Size(perforationWidth, perforationHeight),
                    cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                )
                drawRoundRect(
                    color = strokeColor,
                    topLeft = Offset(rightX, y),
                    size = Size(perforationWidth, perforationHeight),
                    cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                )
            }
        }
    }
}
