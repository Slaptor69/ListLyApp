package ru.misterpotz.listly.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.misterpotz.listly.appComponent
import ru.misterpotz.listly.domain.models.ReadlistFolder
import ru.misterpotz.listly.features.folders.CreateFolderIconButton
import ru.misterpotz.listly.features.folders.FolderNameDialog
import ru.misterpotz.listly.utils.toUserFriendlyMessage

@Composable
fun FolderManagementScreen(
    onBack: () -> Unit = {}
) {
    val repository = remember { appComponent.mediaItemRepository }
    val coroutineScope = rememberCoroutineScope()
    val folders by repository.getReadlistFolders().collectAsState(initial = emptyList())
    var editTarget by remember { mutableStateOf<ReadlistFolder?>(null) }
    var editedName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(repository) {
        runCatching { repository.refreshFolders() }
            .onSuccess { errorMessage = null }
            .onFailure { errorMessage = it.toUserFriendlyMessage() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Text(
                    text = "←",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Text(
                text = "Управление папками",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge
            )
            CreateFolderIconButton(
                onFolderCreated = { folder ->
                    coroutineScope.launch {
                        runCatching { repository.addReadlistFolder(folder) }
                            .onSuccess { errorMessage = null }
                            .onFailure { errorMessage = it.toUserFriendlyMessage() }
                    }
                }
            )
        }

        if (!errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage.orEmpty(),
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.error
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(folders, key = { it.title }) { folder ->
                FolderRow(
                    folder = folder,
                    onRename = {
                        editTarget = folder
                        editedName = folder.title
                    },
                    onDelete = {
                        coroutineScope.launch {
                            runCatching { repository.deleteReadlistFolder(folder) }
                                .onSuccess { errorMessage = null }
                                .onFailure { errorMessage = it.toUserFriendlyMessage() }
                        }
                    }
                )
            }
        }
    }

    val target = editTarget
    if (target != null) {
        FolderNameDialog(
            title = "Введите имя папки",
            value = editedName,
            onValueChange = { editedName = it },
            onDismiss = { editTarget = null },
            onConfirm = {
                val folderName = editedName.trim()
                if (folderName.isNotEmpty()) {
                    coroutineScope.launch {
                        runCatching { repository.renameReadlistFolder(target, folderName) }
                            .onSuccess { errorMessage = null }
                            .onFailure { errorMessage = it.toUserFriendlyMessage() }
                    }
                    editTarget = null
                }
            }
        )
    }
}

@Composable
private fun FolderRow(
    folder: ReadlistFolder,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = folder.title,
                modifier = Modifier.weight(1f)
            )
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Text(
                        text = "☰",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Изменить имя") },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Удалить") },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
