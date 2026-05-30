package ru.misterpotz.listly.features.rating

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal fun userRatingDisplayText(rating: Int?): String {
    return rating?.let { "Оценка: $it/10" } ?: "Оценки нет"
}

@Composable
internal fun userRatingColor(rating: Int?): Color {
    return when (rating) {
        null -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.74f)
        in 0..2 -> Color(0xFFE53935)
        in 3..5 -> Color(0xFFFF9800)
        in 6..8 -> Color(0xFF9CCC65)
        else -> Color(0xFF2E7D32)
    }
}

@Composable
internal fun UserRatingSelectorButton(
    rating: Int?,
    enabled: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onRatingSelected: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val color = userRatingColor(rating)

    Surface(
        modifier = modifier.clickable(enabled = enabled && !isLoading) { expanded = true },
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = if (rating == null) 0.10f else 0.18f),
        contentColor = color
    ) {
        Box(
            modifier = Modifier.size(width = 96.dp, height = 36.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = rating?.let { "$it/10" } ?: "Оценки нет",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    if (expanded) {
        AlertDialog(
            onDismissRequest = { expanded = false },
            title = { Text("Оценка") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    RatingOptionsRow(
                        ratings = (0..5).toList(),
                        selectedRating = rating,
                        onRatingSelected = {
                            expanded = false
                            onRatingSelected(it)
                        }
                    )
                    RatingOptionsRow(
                        ratings = (6..10).toList(),
                        selectedRating = rating,
                        onRatingSelected = {
                            expanded = false
                            onRatingSelected(it)
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { expanded = false }) {
                    Text("Закрыть")
                }
            }
        )
    }
}

@Composable
private fun RatingOptionsRow(
    ratings: List<Int>,
    selectedRating: Int?,
    onRatingSelected: (Int) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ratings.forEach { rating ->
            val color = userRatingColor(rating)
            val selected = selectedRating == rating
            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onRatingSelected(rating) },
                shape = CircleShape,
                color = color.copy(alpha = if (selected) 1f else 0.18f),
                contentColor = if (selected) {
                    Color.White
                } else {
                    color
                }
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = rating.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
