package ru.misterpotz.listly.features.status

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.misterpotz.listly.domain.models.CollectionStatus

@Composable
internal fun CollectionStatusSelector(
    status: CollectionStatus?,
    enabled: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    placeholder: String = "Статус",
    onStatusSelected: (CollectionStatus) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val title = status?.title ?: placeholder

    Box(modifier = modifier) {
        FilledTonalButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled && !isLoading,
            onClick = { expanded = true }
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onSecondary
                )
            } else {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            listOf(
                CollectionStatus.Completed,
                CollectionStatus.Watching,
                CollectionStatus.Planned,
                CollectionStatus.Dropped
            ).forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.title) },
                    trailingIcon = {
                        if (option == status) {
                            Text(
                                text = "✓",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    onClick = {
                        onStatusSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
