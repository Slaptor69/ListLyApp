package ru.misterpotz.listly.features.liststate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.misterpotz.listly.domain.models.CollectionStatus
import ru.misterpotz.listly.domain.models.ReadlistFolder
import ru.misterpotz.listly.features.folders.CreateFolderDialog

@Composable
internal fun UserMediaListActionButton(
    title: String,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        modifier = modifier
            .heightIn(min = 40.dp)
            .widthIn(min = 96.dp, max = 148.dp),
        enabled = !isLoading,
        shape = RoundedCornerShape(22.dp),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp),
        onClick = onClick
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
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
}

@Composable
internal fun UserMediaBadgesRow(
    status: CollectionStatus?,
    folderNames: List<String>,
    modifier: Modifier = Modifier,
) {
    val normalizedFolderNames = folderNames.map { it.trim() }.filter { it.isNotBlank() }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        status?.let { UserMediaBadge(it.title) }
        if (normalizedFolderNames.isNotEmpty()) {
            UserMediaBadge(folderBadgeTitle(normalizedFolderNames))
        }
    }
}

@Composable
private fun UserMediaBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun UserMediaListManagementDialog(
    currentStatus: CollectionStatus?,
    selectedFolders: List<ReadlistFolder>,
    availableFolders: List<ReadlistFolder>,
    inUserList: Boolean,
    onDismiss: () -> Unit,
    onCreateFolder: (ReadlistFolder) -> Unit,
    onSave: (CollectionStatus, List<ReadlistFolder>) -> Unit,
    onRemoveFromList: () -> Unit,
) {
    var draftStatus by remember(currentStatus) {
        mutableStateOf(currentStatus.takeIf { it in ManageableStatuses })
    }
    var draftFolders by remember(selectedFolders) {
        mutableStateOf(selectedFolders)
    }
    var createDialogVisible by remember { mutableStateOf(false) }
    val canSave = draftStatus != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Управление элементом") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Статус",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                ManageableStatuses.forEach { status ->
                    StatusRadioRow(
                        title = status.title,
                        selected = draftStatus == status,
                        onClick = { draftStatus = status }
                    )
                }
                if (!canSave) {
                    Text(
                        text = "Выберите статус, чтобы добавить тайтл в коллекцию",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Text(
                    text = "Папки",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (availableFolders.isEmpty()) {
                    Text(
                        text = "Папок пока нет",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    availableFolders.forEach { folder ->
                        val checked = draftFolders.any { it.sameFolder(folder) }
                        FolderCheckboxRow(
                            title = folder.title,
                            checked = checked,
                            onClick = {
                                draftFolders = if (checked) {
                                    draftFolders.filterNot { it.sameFolder(folder) }
                                } else {
                                    (draftFolders + folder).distinctBy { it.folderKey() }
                                }
                            }
                        )
                    }
                }
                TextButton(onClick = { createDialogVisible = true }) {
                    Text("Создать папку")
                }
                if (inUserList) {
                    TextButton(onClick = onRemoveFromList) {
                        Text(
                            text = "Убрать из моего списка",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = { draftStatus?.let { onSave(it, draftFolders) } }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )

    CreateFolderDialog(
        visible = createDialogVisible,
        onDismiss = { createDialogVisible = false },
        onFolderCreated = { folder ->
            onCreateFolder(folder)
            createDialogVisible = false
        }
    )
}

@Composable
private fun StatusRadioRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = selected,
                role = Role.RadioButton,
                onValueChange = { onClick() }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun FolderCheckboxRow(
    title: String,
    checked: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = { onClick() }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

internal fun userMediaListActionTitle(inUserList: Boolean): String {
    return if (inUserList) "В списке" else "Добавить"
}

private fun folderBadgeTitle(folderNames: List<String>): String {
    return if (folderNames.size == 1) {
        "Папка: ${folderNames.first()}"
    } else {
        "Папки: ${folderNames.size}"
    }
}

private fun ReadlistFolder.sameFolder(other: ReadlistFolder): Boolean {
    return folderKey() == other.folderKey()
}

private fun ReadlistFolder.folderKey(): String {
    return id ?: title.trim().lowercase()
}

private val ManageableStatuses = listOf(
    CollectionStatus.Watching,
    CollectionStatus.Completed,
    CollectionStatus.Planned,
    CollectionStatus.Dropped
)
