package ru.misterpotz.listly.features.folders

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.misterpotz.listly.domain.models.ReadlistFolder

@Composable
fun ReadlistFolderActionButton(
    inReadlist: Boolean,
    selectedFolders: List<ReadlistFolder>,
    folders: List<ReadlistFolder>,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    onFoldersSelected: (List<ReadlistFolder>) -> Unit,
    onRemoveFromReadlist: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var createDialogVisible by remember { mutableStateOf(false) }
    var draftFolders by remember(expanded, selectedFolders) { mutableStateOf(selectedFolders) }
    val buttonTitle = folderButtonTitle(selectedFolders)
    val buttonColors = if (selectedFolders.isEmpty()) {
        ButtonDefaults.filledTonalButtonColors()
    } else {
        ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    }

    Box {
        FilledTonalButton(
            modifier = modifier,
            enabled = !isLoading,
            colors = buttonColors,
            contentPadding = contentPadding,
            onClick = { expanded = true }
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onSecondary
                )
            } else {
                Text(
                    text = buttonTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 220.dp)
        ) {
            if (folders.isEmpty()) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Папок пока нет",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    enabled = false,
                    onClick = {}
                )
            }
            folders.forEach { folder ->
                val checked = draftFolders.any { it.sameFolder(folder) }
                DropdownMenuItem(
                    text = { Text(folder.title) },
                    leadingIcon = {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = null
                        )
                    },
                    onClick = {
                        draftFolders = if (checked) {
                            draftFolders.filterNot { it.sameFolder(folder) }
                        } else {
                            draftFolders + folder
                        }
                    }
                )
            }
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Применить",
                        color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    onClick = {
                        onFoldersSelected(draftFolders)
                    expanded = false
                }
            )
            CreateFolderDropdownMenuItem(
                onMenuDismiss = { expanded = false },
                onClick = {
                    createDialogVisible = true
                }
            )
            if (inReadlist) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Убрать из моего списка",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    onClick = {
                        onRemoveFromReadlist()
                        expanded = false
                    }
                )
            }
        }
    }

    CreateFolderDialog(
        visible = createDialogVisible,
        onDismiss = { createDialogVisible = false },
        onFolderCreated = { folder ->
            createDialogVisible = false
            onFoldersSelected((selectedFolders + folder).distinctBy { it.folderKey() })
        }
    )
}

private fun folderButtonTitle(selectedFolders: List<ReadlistFolder>): String {
    return when (selectedFolders.size) {
        0 -> "Папки"
        1 -> selectedFolders.first().title
        else -> "${selectedFolders.size} папки"
    }
}

private fun ReadlistFolder.sameFolder(other: ReadlistFolder): Boolean {
    return folderKey() == other.folderKey()
}

private fun ReadlistFolder.folderKey(): String {
    return id ?: title.trim().lowercase()
}

@Composable
fun CreateFolderDropdownMenuItem(
    onMenuDismiss: () -> Unit,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(
                text = "+ Создать новую папку",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        },
        onClick = {
            onMenuDismiss()
            onClick()
        }
    )
}

@Composable
fun CreateFolderIconButton(
    onFolderCreated: (ReadlistFolder) -> Unit,
) {
    var createDialogVisible by remember { mutableStateOf(false) }

    IconButton(onClick = { createDialogVisible = true }) {
        Text(
            text = "+",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }

    CreateFolderDialog(
        visible = createDialogVisible,
        onDismiss = { createDialogVisible = false },
        onFolderCreated = { folder ->
            onFolderCreated(folder)
            createDialogVisible = false
        }
    )
}

@Composable
fun FolderNameDialog(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

internal fun prepareFolderInput(folderName: String): Result<ReadlistFolder> {
    val normalizedName = folderName.trim()
    if (normalizedName.isEmpty()) {
        return Result.failure(IllegalArgumentException("Введите название папки"))
    }
    return Result.success(ReadlistFolder(normalizedName))
}

@Composable
fun CreateFolderDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onFolderCreated: (ReadlistFolder) -> Unit,
) {
    var folderName by remember(visible) { mutableStateOf("") }

    if (!visible) {
        return
    }

    FolderNameDialog(
        title = "Введите имя папки",
        value = folderName,
        onValueChange = { folderName = it },
        onDismiss = onDismiss,
        onConfirm = {
            prepareFolderInput(folderName).onSuccess(onFolderCreated)
        }
    )
}
