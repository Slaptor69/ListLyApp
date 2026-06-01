package ru.misterpotz.listly.features.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.misterpotz.listly.domain.models.CollectionStatus
import ru.misterpotz.listly.features.rating.userRatingColor
import ru.misterpotz.listly.features.rating.userRatingDisplayText

internal const val EmptyUserNoteText = "Введите свою заметку"

internal fun userNoteDisplayText(note: String?): String {
    return note?.takeIf { it.isNotBlank() } ?: EmptyUserNoteText
}

@Composable
internal fun UserMediaSummaryPlate(
    status: CollectionStatus?,
    userRating: Int?,
    note: String?,
    modifier: Modifier = Modifier,
) {
    var expanded by remember(status, userRating, note) { mutableStateOf(false) }
    val displayText = userNoteDisplayText(note)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = true },
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Text(
                text = status?.title ?: "Статус",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall
            )
        }
        Text(
            text = displayText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = userRatingDisplayText(userRating),
            style = MaterialTheme.typography.labelSmall,
            color = userRatingColor(userRating)
        )
    }

    if (expanded) {
        AlertDialog(
            onDismissRequest = { expanded = false },
            title = { Text("Заметка") },
            text = {
                Text(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    text = displayText,
                    style = MaterialTheme.typography.bodyMedium
                )
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
internal fun UserNotePreviewPlate(
    note: String?,
    modifier: Modifier = Modifier,
) {
    UserMediaSummaryPlate(
        status = null,
        userRating = null,
        note = note,
        modifier = modifier
    )
}

@Composable
internal fun UserNoteEditorPlate(
    note: String?,
    canEdit: Boolean,
    isSaving: Boolean,
    modifier: Modifier = Modifier,
    onSave: (String) -> Unit,
) {
    val savedNote = note.orEmpty()
    var draftNote by remember(savedNote) { mutableStateOf(savedNote) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Заметка",
                style = MaterialTheme.typography.titleSmall
            )
            OutlinedTextField(
                value = draftNote,
                onValueChange = { draftNote = it },
                modifier = Modifier.fillMaxWidth(),
                enabled = canEdit && !isSaving,
                minLines = 3,
                maxLines = 6,
                placeholder = { Text(EmptyUserNoteText) }
            )
            if (!canEdit) {
                Text(
                    text = "Заметку можно написать после добавления в readlist",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        enabled = !isSaving && draftNote.trim() != savedNote,
                        onClick = { onSave(draftNote) }
                    ) {
                        Text(if (isSaving) "Сохраняем" else "Сохранить")
                    }
                }
            }
        }
    }
}
