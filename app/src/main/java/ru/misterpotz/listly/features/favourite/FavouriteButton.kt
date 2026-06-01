package ru.misterpotz.listly.features.favourite

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
internal fun FavouriteButton(
    isFavourite: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val drawAsFavourite = isFavourite || isPressed
    val color = if (drawAsFavourite) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .size(48.dp)
            .toggleable(
                value = isFavourite,
                enabled = !isLoading,
                role = Role.Checkbox,
                interactionSource = interactionSource,
                indication = null,
                onValueChange = { onToggle() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(width = 28.dp, height = 26.dp)) {
            val heart = Path().apply {
                moveTo(size.width * 0.50f, size.height * 0.88f)
                cubicTo(
                    size.width * 0.08f,
                    size.height * 0.58f,
                    size.width * 0.03f,
                    size.height * 0.36f,
                    size.width * 0.17f,
                    size.height * 0.18f
                )
                cubicTo(
                    size.width * 0.29f,
                    size.height * 0.02f,
                    size.width * 0.45f,
                    size.height * 0.11f,
                    size.width * 0.50f,
                    size.height * 0.25f
                )
                cubicTo(
                    size.width * 0.55f,
                    size.height * 0.11f,
                    size.width * 0.71f,
                    size.height * 0.02f,
                    size.width * 0.83f,
                    size.height * 0.18f
                )
                cubicTo(
                    size.width * 0.97f,
                    size.height * 0.36f,
                    size.width * 0.92f,
                    size.height * 0.58f,
                    size.width * 0.50f,
                    size.height * 0.88f
                )
                close()
            }
            if (drawAsFavourite) {
                drawPath(
                    path = heart,
                    color = color.copy(alpha = if (isLoading) 0.42f else 1f)
                )
            } else {
                drawPath(
                    path = heart,
                    color = color.copy(alpha = if (isLoading) 0.42f else 1f),
                    style = Stroke(width = 1.8.dp.toPx())
                )
            }
        }
    }
}
