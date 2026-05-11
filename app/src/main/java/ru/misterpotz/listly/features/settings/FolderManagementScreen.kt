package ru.misterpotz.listly.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.misterpotz.listly.appComponent

@Composable
fun FolderManagementScreen(
    onBack: () -> Unit = {}
) {
    val repository = remember { appComponent.mediaItemRepository }
    val folders by repository.getReadlistFolders().collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("Назад")
        }
        Text(
            text = "Управление папками",
            style = MaterialTheme.typography.titleLarge
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(folders, key = { it.title }) { folder ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = folder.title,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
