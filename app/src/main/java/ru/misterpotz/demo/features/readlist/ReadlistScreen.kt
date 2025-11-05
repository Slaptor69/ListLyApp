package ru.misterpotz.demo.features.readlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.misterpotz.demo.appComponent
import ru.misterpotz.demo.ui.theme.DemoTheme
import ru.misterpotz.demo.ui.utils.ObserveLifecycleEvents
import ru.misterpotz.demo.ui.utils.StandardElmScreen
import ru.misterpotz.demo.utils.log


@Composable
fun ReadlistScreen() {
    StandardElmScreen(
        storeFactory = {
            "creating new readlist store".log()
            appComponent.readlistStoreFactory.create()
        },
        onEffect = {},
        body = { state, onEvent ->
            ReadlistScreenContent(state, onEvent)
        },
    )
}

@Composable
fun ReadlistScreenContent(state: ReadlistState, onEvent: (ReadlistEvent) -> Unit) {
    ObserveLifecycleEvents(
        onResume = { onEvent(ReadlistEvent.Ui.OnResume) },
        onPause = { onEvent(ReadlistEvent.Ui.OnPause) }
    )
    val items = state.mediaItems.content

    if (items.isNullOrEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Readlist is empty")
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items, key = { it.id }) { item ->
            ElevatedCard {
                Row(
                    Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        item.title,
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium
                    )
                    OutlinedButton(onClick = { }) { Text("Up") }
                    OutlinedButton(onClick = { }) { Text("Down") }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    DemoTheme {
        ReadlistScreen()
    }
}